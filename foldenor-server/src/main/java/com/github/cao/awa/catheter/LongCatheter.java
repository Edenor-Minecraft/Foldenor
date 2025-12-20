package com.github.cao.awa.catheter;

import com.github.cao.awa.catheter.action.*;
import com.github.cao.awa.catheter.pair.IntegerAndLongPair;
import com.github.cao.awa.catheter.matrix.MatrixFlockPos;
import com.github.cao.awa.catheter.matrix.MatrixPos;
import com.github.cao.awa.catheter.receptacle.BooleanReceptacle;
import com.github.cao.awa.catheter.receptacle.IntegerReceptacle;
import com.github.cao.awa.catheter.receptacle.LongReceptacle;
import com.github.cao.awa.sinuatum.function.consumer.TriConsumer;
import com.github.cao.awa.sinuatum.function.QuinFunction;
import com.github.cao.awa.sinuatum.function.TriFunction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.LongStream;

public class LongCatheter {
    private static final Random RANDOM = new Random();
    private long[] targets;

    public LongCatheter(long[] targets) {
        this.targets = targets;
    }

    public static LongCatheter make(long... targets) {
        return new LongCatheter(targets);
    }

    public static LongCatheter makeCapacity(int size) {
        return new LongCatheter(array(size));
    }

    public static LongCatheter of(long[] targets) {
        return new LongCatheter(targets);
    }

    public static LongCatheter of(Collection<Long> targets) {
        if (targets == null) {
            return new LongCatheter(array(0));
        }
        long[] delegate = new long[targets.size()];
        int index = 0;
        for (long target : targets) {
            delegate[index++] = target;
        }
        return new LongCatheter(delegate);
    }

    public LongCatheter each(final LongConsumer action) {
        final long[] ts = this.targets;
        for (long l : ts) {
            action.accept(l);
        }
        return this;
    }

    public LongCatheter each(final LongConsumer action, final Runnable poster) {
        each(action);
        poster.run();
        return this;
    }

    public <X> LongCatheter each(final X initializer, final BiConsumer<X, Long> action) {
        final long[] ts = this.targets;
        for (long l : ts) {
            action.accept(initializer, l);
        }
        return this;
    }

    public <X> LongCatheter each(X initializer, final BiConsumer<X, Long> action, Consumer<X> poster) {
        each(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public <X> LongCatheter overall(X initializer, final TriConsumer<X, Integer, Long> action) {
        final long[] ts = this.targets;
        int index = 0;
        for (long l : ts) {
            action.accept(initializer, index++, l);
        }
        return this;
    }

    public <X> LongCatheter overall(X initializer, final TriConsumer<X, Integer, Long> action, Consumer<X> poster) {
        overall(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public LongCatheter overall(final IntegerAndLongConsumer action) {
        final long[] ts = this.targets;
        int index = 0;
        for (long l : ts) {
            action.accept(index++, l);
        }
        return this;
    }

    public LongCatheter overall(final IntegerAndLongConsumer action, Runnable poster) {
        overall(action);
        poster.run();
        return this;
    }

    public LongCatheter insert(final IntegerAndBiToLongFunction maker) {
        final Map<Integer, IntegerAndLongPair> indexes = new HashMap<>();
        final LongReceptacle lastItem = new LongReceptacle(0);
        overall((index, item) -> {
            indexes.put(
                    index + indexes.size(),
                    new IntegerAndLongPair(index, maker.apply(index, item, lastItem.get()))
            );
            lastItem.set(item);
        });

        final long[] ts = this.targets;
        final long[] newDelegate = array(ts.length + indexes.size());
        final IntegerReceptacle lastIndex = new IntegerReceptacle(0);
        final IntegerReceptacle lastDest = new IntegerReceptacle(0);
        IntCatheter.of(indexes.keySet())
                .sort()
                .each(index -> {
                    if (lastIndex.get() != index) {
                        final int maxCopyLength = Math.min(
                                newDelegate.length - lastDest.get() - 1,
                                index - lastIndex.get()
                        );
                        System.arraycopy(
                                ts,
                                lastIndex.get(),
                                newDelegate,
                                lastDest.get(),
                                maxCopyLength
                        );
                    }
                    final IntegerAndLongPair item = indexes.get(index);
                    newDelegate[index] = item.longValue();
                    lastIndex.set(item.intValue());
                    lastDest.set(index + 1);
                }, () -> {
                    System.arraycopy(
                            ts,
                            lastIndex.get(),
                            newDelegate,
                            lastDest.get(),
                            newDelegate.length - lastDest.get()
                    );
                });

        this.targets = newDelegate;

        return this;
    }

    public LongCatheter pluck(final IntegerAndBiLongPredicate maker) {
        if (isEmpty()) {
            return this;
        }

        final LongReceptacle lastItem = new LongReceptacle(0);
        return overallFilter((index, item) -> {
            if (maker.test(index, item, lastItem.get())) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public LongCatheter discardTo(final LongPredicate predicate) {
        LongCatheter result = LongCatheter.make();

        overallFilter((index, item) -> !predicate.test(item), result::reset);

        return result;
    }

    public <X> LongCatheter discardTo(final Predicate<X> predicate, LongFunction<X> converter) {
        LongCatheter result = LongCatheter.make();

        overallFilter((index, item) -> !predicate.test(converter.apply(item)), result::reset);

        return result;
    }

    public LongCatheter discardTo(final long initializer, final BiLongPredicate predicate) {
        LongCatheter result = LongCatheter.make();

        overallFilter((index, item) -> !predicate.test(item, initializer), result::reset);

        return result;
    }

    public LongCatheter orDiscardTo(final boolean succeed, final LongPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate);
    }

    public <X> LongCatheter orDiscardTo(final boolean succeed, final Predicate<X> predicate, LongFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate, converter);
    }

    public LongCatheter orDiscardTo(final boolean succeed, final long initializer, final BiLongPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(initializer, predicate);
    }

    public LongCatheter discard(final LongPredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item));
    }

    public <X> LongCatheter discard(final Predicate<X> predicate, LongFunction<X> converter) {
        return overallFilter((index, item) -> !predicate.test(converter.apply(item)));
    }

    public LongCatheter discard(final long initializer, final BiLongPredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item, initializer));
    }

    public LongCatheter orDiscard(final boolean succeed, final LongPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(predicate);
    }

    public <X> LongCatheter orDiscard(final boolean succeed, final Predicate<X> predicate, LongFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discard(predicate, converter);
    }

    public LongCatheter orDiscard(final boolean succeed, final long initializer, final BiLongPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(initializer, predicate);
    }

    public LongCatheter filterTo(final LongPredicate predicate) {
        return dump().filter(predicate);
    }

    public <X> LongCatheter filterTo(final Predicate<X> predicate, LongFunction<X> converter) {
        return dump().filter(predicate, converter);
    }

    public LongCatheter filterTo(final long initializer, final BiLongPredicate predicate) {
        return dump().filter(initializer, predicate);
    }

    public LongCatheter orFilterTo(final boolean succeed, final LongPredicate predicate) {
        return dump().orFilter(succeed, predicate);
    }

    public <X> LongCatheter orFilterTo(final boolean succeed, final Predicate<X> predicate, LongFunction<X> converter) {
        return dump().orFilter(succeed, predicate, converter);
    }

    public LongCatheter orFilterTo(final boolean succeed, final long initializer, final BiLongPredicate predicate) {
        return dump().orFilter(succeed, initializer, predicate);
    }

    public LongCatheter filter(final LongPredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    public <X> LongCatheter filter(final Predicate<X> predicate, LongFunction<X> converter) {
        return overallFilter((index, item) -> predicate.test(converter.apply(item)));
    }

    public LongCatheter filter(final long initializer, final BiLongPredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public LongCatheter orFilter(final boolean succeed, final LongPredicate predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public <X> LongCatheter orFilter(final boolean succeed, final Predicate<X> predicate, LongFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return filter(predicate, converter);
    }

    public LongCatheter orFilter(final boolean succeed, final long initializer, final BiLongPredicate predicate) {
        if (succeed) {
            return this;
        }
        return filter(initializer, predicate);
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public LongCatheter overallFilter(final IntegerAndLongPredicate predicate) {
        return overallFilter(predicate, x -> {
        });
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param predicate  The filter predicate
     * @param discarding The discarded elements
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public LongCatheter overallFilter(final IntegerAndLongPredicate predicate, final Consumer<long[]> discarding) {
        if (isEmpty()) {
            return this;
        }

        // 创建需要的变量和常量
        final long[] ts = this.targets;
        final int length = ts.length;
        final boolean[] deleting = new boolean[length];
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        for (long target : ts) {
            // 符合条件的保留
            if (predicate.test(index, target)) {
                index++;
                continue;
            }

            // 不符合条件的设为null，后面会去掉
            // 并且将新数组的容量减一
            deleting[index++] = true;
            newDelegateSize--;
        }

        // 创建新数组
        final long[] newDelegate = array(newDelegateSize);
        final long[] discardingDelegate = array(length - newDelegateSize);
        int newDelegateIndex = 0;
        int discardingDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        for (boolean isDeleting : deleting) {
            final long t = ts[index++];

            // deleting 值为true则为被筛选掉的，忽略
            if (isDeleting) {
                discardingDelegate[discardingDelegateIndex++] = t;
            } else {
                // 不为1则加入新数组
                newDelegate[newDelegateIndex++] = t;
            }
        }

        discarding.accept(discardingDelegate);

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    public LongCatheter overallFilter(final long initializer, final IntegerAndBiLongPredicate predicate) {
        return overallFilter((index, item) -> predicate.test(index, item, initializer));
    }

    public boolean isPresent() {
        return count() > 0;
    }

    public LongCatheter ifPresent(Consumer<LongCatheter> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public LongCatheter ifEmpty(Consumer<LongCatheter> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }

    public LongCatheter removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        long[] newDelegate = array(count() - 1);
        if (index > 0) {
            System.arraycopy(
                    this.targets,
                    0,
                    newDelegate,
                    0,
                    index
            );
        }

        System.arraycopy(
                this.targets,
                index + 1,
                newDelegate,
                index,
                count() - 1 - index
        );

        this.targets = newDelegate;

        return this;
    }

    public LongCatheter distinct() {
        if (isEmpty()) {
            return this;
        }

        final Map<Long, Boolean> map = new HashMap<>();
        return filter(
                item -> {
                    if (map.getOrDefault(item, false)) {
                        return false;
                    }
                    map.put(item, true);
                    return true;
                }
        );
    }

    public LongCatheter sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public LongCatheter sort(Comparator<Long> comparator) {
        Long[] array = new Long[this.targets.length];
        int index = 0;
        for (long target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (long target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public LongCatheter holdTill(int index) {
        if (isEmpty()) {
            return this;
        }

        index = Math.min(index, this.targets.length);

        final long[] ts = this.targets;
        final long[] newDelegate = array(index);
        if (index > 0) {
            System.arraycopy(
                    ts,
                    0,
                    newDelegate,
                    0,
                    index
            );
        }
        this.targets = newDelegate;

        return this;
    }

    public LongCatheter holdTill(final LongPredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final long[] ts = this.targets;
        final long[] newDelegate = array(index);
        if (index > 0) {
            System.arraycopy(
                    ts,
                    0,
                    newDelegate,
                    0,
                    index
            );
        }
        this.targets = newDelegate;

        return this;
    }

    public LongCatheter whenFlock(final long source, final BiLongFunction maker, LongConsumer consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public LongCatheter whenFlock(BiLongFunction maker, LongConsumer consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public <X> X alternate(final X source, final BiFunction<X, Long, X> maker) {
        final long[] ts = this.targets;
        X result = source;
        for (long l : ts) {
            result = maker.apply(result, l);
        }
        return result;
    }

    public long flock(final BiLongFunction maker) {
        final long[] ts = this.targets;
        final int length = ts.length;
        long result = length > 0 ? ts[0] : 0;
        for (int i = 1; i < length; i++) {
            result = maker.apply(result, ts[i]);
        }
        return result;
    }

    public <X> LongCatheter whenAlternate(final X source, final BiFunction<X, Long, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public <X> LongCatheter whenAlternate(BiFunction<X, Long, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(null, maker));
        return this;
    }

    public long flock(final long source, final BiLongFunction maker) {
        final long[] ts = this.targets;
        long result = source;
        for (long l : ts) {
            result = maker.apply(result, l);
        }
        return result;
    }

    public boolean alternate(final boolean source, final BiLongPredicate maker) {
        BooleanReceptacle result = new BooleanReceptacle(source);
        flock((older, newer) -> {
            result.and(maker.test(older, newer));
            return newer;
        });
        return result.get();
    }

    public LongCatheter whenAlternate(final boolean source, final BiLongPredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public LongCatheter whenAlternate(BiLongPredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(false, maker));
        return this;
    }

    public LongCatheter waiveTill(final int index) {
        if (isEmpty()) {
            return this;
        }

        final long[] ts = this.targets;
        final long[] newDelegate;
        if (index >= ts.length) {
            newDelegate = array(0);
        } else {
            newDelegate = array(ts.length - index + 1);
            System.arraycopy(
                    ts,
                    index - 1,
                    newDelegate,
                    0,
                    newDelegate.length
            );
        }
        this.targets = newDelegate;

        return this;
    }

    public LongCatheter waiveTill(final LongPredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final long[] ts = this.targets;
        final long[] newDelegate;
        if (index >= ts.length) {
            newDelegate = array(0);
        } else {
            newDelegate = array(ts.length - index + 1);
            System.arraycopy(
                    ts,
                    index - 1,
                    newDelegate,
                    0,
                    newDelegate.length
            );
        }
        this.targets = newDelegate;

        return this;
    }

    public LongCatheter till(final LongPredicate predicate) {
        final long[] ts = this.targets;
        for (long l : ts) {
            if (predicate.test(l)) {
                break;
            }
        }

        return this;
    }

    public int findTill(final LongPredicate predicate) {
        final long[] ts = this.targets;
        int index = 0;
        for (long l : ts) {
            if (predicate.test(l)) {
                break;
            }
            index++;
        }

        return index;
    }

    public LongCatheter replace(final LongUnaryOperator handler) {
        if (isEmpty()) {
            return this;
        }

        final long[] ts = this.targets;
        int index = 0;
        for (long t : ts) {
            ts[index++] = handler.applyAsLong(t);
        }
        return this;
    }

    public BooleanCatheter vary(final LongPredicate handler) {
        if (isEmpty()) {
            return BooleanCatheter.make();
        }

        final long[] ts = this.targets;
        final boolean[] array = new boolean[ts.length];
        int index = 0;
        for (long t : ts) {
            array[index++] = handler.test(t);
        }
        return BooleanCatheter.of(array);
    }

    public ByteCatheter vary(final LongToByteFunction handler) {
        if (isEmpty()) {
            return ByteCatheter.make();
        }
        final long[] ts = this.targets;
        final byte[] array = new byte[ts.length];
        int index = 0;
        for (long t : ts) {
            array[index++] = handler.applyAsByte(t);
        }
        return ByteCatheter.of(array);
    }

    public DoubleCatheter vary(final LongToDoubleFunction handler) {
        if (isEmpty()) {
            return DoubleCatheter.make();
        }
        final long[] ts = this.targets;
        final double[] array = new double[ts.length];
        int index = 0;
        for (long t : ts) {
            array[index++] = handler.applyAsDouble(t);
        }
        return DoubleCatheter.of(array);
    }

    public IntCatheter vary(final LongToIntFunction handler) {
        if (isEmpty()) {
            return IntCatheter.make();
        }

        final long[] ts = this.targets;
        final int[] array = new int[ts.length];
        int index = 0;
        for (long t : ts) {
            array[index++] = handler.applyAsInt(t);
        }
        return IntCatheter.of(array);
    }

    public LongCatheter vary(final LongUnaryOperator handler) {
        return replace(handler);
    }

    public <X> Catheter<X> vary(final LongFunction<X> handler) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final long[] ts = this.targets;
        final X[] array = xArray(ts.length);
        int index = 0;
        for (long t : ts) {
            array[index++] = handler.apply(t);
        }
        return Catheter.of(array);
    }

    public <X> Catheter<X> vary(final LongFunction<X> handler, IntFunction<X[]> arrayGenerator) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final long[] ts = this.targets;
        final X[] array = arrayGenerator.apply(ts.length);
        int index = 0;
        for (long l : ts) {
            array[index++] = handler.apply(l);
        }
        return Catheter.of(array).arrayGenerator(arrayGenerator);
    }

    public LongCatheter whenAny(final LongPredicate predicate, final LongConsumer action) {
        final long[] ts = this.targets;
        for (long t : ts) {
            if (predicate.test(t)) {
                action.accept(t);
                break;
            }
        }
        return this;
    }

    public LongCatheter whenAll(final LongPredicate predicate, final Runnable action) {
        final long[] ts = this.targets;
        for (long t : ts) {
            if (predicate.test(t)) {
                continue;
            }
            return this;
        }
        action.run();
        return this;
    }

    public LongCatheter whenAll(final LongPredicate predicate, final LongConsumer action) {
        return whenAll(predicate, () -> each(action));
    }

    private LongCatheter whenNone(final LongPredicate predicate, final Runnable action) {
        final long[] ts = this.targets;
        for (long l : ts) {
            if (predicate.test(l)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final LongPredicate predicate) {
        final long[] ts = this.targets;
        for (long l : ts) {
            if (predicate.test(l)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final LongPredicate predicate) {
        final long[] ts = this.targets;
        for (long l : ts) {
            if (predicate.test(l)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public boolean hasNone(final LongPredicate predicate) {
        final long[] ts = this.targets;
        for (long l : ts) {
            if (predicate.test(l)) {
                return false;
            }
        }
        return true;
    }

    public long findFirst(final LongPredicate predicate) {
        final long[] ts = this.targets;
        for (long l : ts) {
            if (predicate.test(l)) {
                return l;
            }
        }
        return 0;
    }

    public long findLast(final LongPredicate predicate) {
        final long[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final long t = ts[index--];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0;
    }

    public <X> X whenFoundFirst(final LongPredicate predicate, LongFunction<X> function) {
        final long[] ts = this.targets;
        for (long l : ts) {
            if (predicate.test(l)) {
                return function.apply(l);
            }
        }
        return null;
    }

    public <X> X whenFoundLast(final LongPredicate predicate, LongFunction<X> function) {
        final long[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final long t = ts[index--];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public LongCatheter any(final LongConsumer consumer) {
        if (this.targets.length > 0) {
            long[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public LongCatheter first(final LongConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public LongCatheter tail(final LongConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public LongCatheter reverse() {
        if (isEmpty()) {
            return this;
        }

        final long[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        long temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public long max(final Comparator<Long> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public long min(final Comparator<Long> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public LongCatheter whenMax(final Comparator<Long> comparator, final LongConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public LongCatheter whenMin(final Comparator<Long> comparator, final LongConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    public int count() {
        return this.targets.length;
    }

    public LongCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public LongCatheter count(final IntegerReceptacle target) {
        target.set(count());
        return this;
    }

    public LongCatheter count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    public final LongCatheter append(final long... objects) {
        final long[] ts = this.targets;
        final long[] newDelegate = array(ts.length + objects.length);
        System.arraycopy(
                ts,
                0,
                newDelegate,
                0,
                ts.length
        );
        System.arraycopy(
                objects,
                0,
                newDelegate,
                ts.length,
                objects.length
        );
        this.targets = newDelegate;
        return this;
    }

    public LongCatheter append(final LongCatheter objects) {
        return append(objects.array());
    }

    public LongCatheter repeat(final int count) {
        final long[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public long fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, long item) {
        this.targets[index] = item;
    }

    public LongCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Long> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, LongCatheter input, final TriFunction<MatrixPos, Long, Long, X> action) {
        if (input.count() == count()) {
            final IntegerReceptacle index = new IntegerReceptacle(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final long inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public LongCatheter matrixMap(
            final int width,
            final int inputWidth,
            final LongCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Long, Long, Long> scanFlocked,
            final TriFunction<MatrixPos, Long, Long, Long> combineFlocked
    ) {
        final int inputHeight = input.count() / inputWidth;
        final int sourceHeight = count() / width;

        boolean homoMatrix = inputHeight == sourceHeight && width == inputWidth;

        // 矩阵计算时 A(h, w) B(h, w) 中的 A(w) 必须等于 B(h)
        // 其中 h 是高度而 w 是宽度，因此自身的 width 必须等于输入的 height
        if (width != inputHeight && !homoMatrix) {
            throw new IllegalArgumentException("The matrix cannot be constructed because input height does not match to source width");
        }

        // 创建矩阵，大小是 A(h)B(w)
        final LongCatheter newMatrix = LongCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final LongCatheter flockingCatheter = LongCatheter.makeCapacity(width);

        return newMatrix.matrixReplace(inputWidth, (pos, ignored) -> {
            final int posX = pos.x();
            final int posY = pos.y();

            int flockingIndex = 0;
            int inputY = 0;
            int sourceX = 0;
            while (sourceX < width) {

                // 这些 pos 和计算无关，用于让使用者自定义判断在矩阵中如何变换数据的
                final MatrixFlockPos flockPos = new MatrixFlockPos(
                        posX,
                        posY
                );
                final MatrixPos inputPos = new MatrixPos(
                        posX,
                        inputY
                );
                final MatrixPos sourcePos = new MatrixPos(
                        sourceX,
                        posY
                );

                // 获取自身的值
                final long fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final long fetchedInput = input.fetch(inputY * inputWidth + posX);

                // 追加到 flock 组中
                flockingCatheter.fetch(
                        flockingIndex++,
                        scanFlocked.apply(
                                flockPos,
                                sourcePos,
                                inputPos,
                                fetchedSource,
                                fetchedInput
                        )
                );

                inputY++;
                sourceX++;
            }

            // 对矩阵的每个参数累加对应列的结果
            return flockingCatheter.flock((current, next) -> combineFlocked.apply(pos, current, next));
        });
    }

    public <X> Catheter<X> matrixVary(final int width, long input, final TriFunction<MatrixPos, Long, Long, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public LongCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Long, Long> action) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final IntegerReceptacle w = new IntegerReceptacle(0);
        final IntegerReceptacle h = new IntegerReceptacle(0);

        final int matrixEdge = width - 1;

        return replace(item -> {
            final int wValue = w.get();
            final int hValue = h.get();

            if (wValue == matrixEdge) {
                w.set(0);
                h.set(hValue + 1);
            } else {
                w.set(wValue + 1);
            }
            return action.apply(new MatrixPos(wValue, hValue), item);
        });
    }

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Long, X> action) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final IntegerReceptacle w = new IntegerReceptacle(0);
        final IntegerReceptacle h = new IntegerReceptacle(0);

        final int matrixEdge = width - 1;

        return vary((long item) -> {
            final int hValue = h.get();
            final int wValue = w.get();

            if (wValue == matrixEdge) {
                w.set(0);
                h.set(hValue + 1);
            } else {
                w.set(wValue + 1);
            }
            return action.apply(new MatrixPos(wValue, hValue), item);
        });
    }

    public Catheter<LongCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<LongCatheter> results = Catheter.makeCapacity(sourceHeight);
        LongCatheter catheter = LongCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final long element = fetch(y * width + x);
                catheter.fetch(
                        x,
                        element
                );
            }
            results.fetch(
                    y,
                    catheter.dump()
            );
        }

        return results;
    }

    public LongCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public LongCatheter shuffle(RandomGenerator random) {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public LongCatheter swapShuffle(RandomGenerator random) {
        long[] elements = this.targets;
        int i = elements.length;

        for (int j = i; j > 1; --j) {
            int swapTo = random.nextInt(j);
            int swapFrom = j - 1;
            long fromElement = elements[swapFrom];
            long toElement = elements[swapTo];
            elements[swapTo] = fromElement;
            elements[swapFrom] = toElement;
        }

        return this;
    }

    public boolean has(long target) {
        return hasAny(t -> t == target);
    }

    public boolean not(long target) {
        return !has(target);
    }

    public LongCatheter merge(LongCatheter other) {
        return append(other.filter(this::not));
    }

    public LongCatheter dump() {
        return new LongCatheter(array());
    }

    public LongCatheter flat(LongFunction<LongCatheter> function) {
        return arrayFlat(generator -> function.apply(generator).targets);
    }

    public LongCatheter arrayFlat(LongArrayFunction function) {
        if (isEmpty()) {
            return this;
        }

        long[][] longs = new long[count()][];
        int totalSize = 0;

        int index = 0;
        long[] targets = this.targets;
        for (long element : targets) {
            long[] flatting = function.apply(element);
            longs[index++] = flatting;
            totalSize += flatting.length;
        }

        targets = array(totalSize);
        int pos = 0;
        for (long[] flat : longs) {
            for (long l : flat) {
                targets[pos++] = l;
            }
        }

        this.targets = targets;

        return this;
    }

    public LongCatheter streamFlat(LongFunction<LongStream> function) {
        if (isEmpty()) {
            return this;
        }

        long[][] longs = new long[count()][];
        int totalSize = 0;

        int index = 0;
        long[] targets = this.targets;
        for (long element : targets) {
            long[] flatting = function.apply(element).toArray();
            longs[index++] = flatting;
            totalSize += flatting.length;
        }

        targets = array(totalSize);
        int pos = 0;
        for (long[] flat : longs) {
            for (long l : flat) {
                targets[pos++] = l;
            }
        }

        this.targets = targets;

        return this;
    }

    public <X> Catheter<X> flatTo(LongFunction<Catheter<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Catheter<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (long element : this.targets) {
            Catheter<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        return Catheter.flatting(catheter, totalSize);
    }

    public <X> Catheter<X> collectionFlatTo(LongFunction<Collection<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Collection<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (long element : this.targets) {
            Collection<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.size();
        }

        return Catheter.flattingCollection(catheter, totalSize);
    }

    public <X> Catheter<X> arrayFlatTo(LongFunction<X[]> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<X[]> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (long element : this.targets) {
            X[] flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.length;
        }

        return Catheter.flattingArray(catheter, totalSize);
    }

    public LongCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public LongCatheter reset(long[] targets) {
        this.targets = targets;
        return this;
    }

    public long[] array() {
        return this.targets.clone();
    }

    public long[] dArray() {
        return this.targets;
    }

    public LongStream stream() {
        return LongStream.of(array());
    }

    public List<Long> list() {
        List<Long> list = new ArrayList<>();
        for (long l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Long> set() {
        Set<Long> set = new HashSet<>();
        for (long l : array()) {
            set.add(l);
        }
        return set;
    }

    public static void main(String[] args) {
        LongCatheter source = LongCatheter.make(
                1, 2, 3, 4, 5, 6, 7, 8
        );

        System.out.println("???");

        System.out.println(source.removeWithIndex(4).list());

        System.out.println("???");
    }

    private static long[] array(int size) {
        return new long[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
