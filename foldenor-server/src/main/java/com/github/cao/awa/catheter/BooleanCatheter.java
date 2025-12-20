package com.github.cao.awa.catheter;

import com.github.cao.awa.catheter.action.*;
import com.github.cao.awa.catheter.matrix.MatrixFlockPos;
import com.github.cao.awa.catheter.matrix.MatrixPos;
import com.github.cao.awa.catheter.pair.IntegerAndBooleanPair;
import com.github.cao.awa.catheter.pair.Pair;
import com.github.cao.awa.catheter.receptacle.BooleanReceptacle;
import com.github.cao.awa.catheter.receptacle.IntegerReceptacle;
import com.github.cao.awa.catheter.receptacle.Receptacle;
import com.github.cao.awa.sinuatum.function.consumer.TriConsumer;
import com.github.cao.awa.sinuatum.function.QuinFunction;
import com.github.cao.awa.sinuatum.function.TriFunction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class BooleanCatheter {
    private static final Random RANDOM = new Random();
    private boolean[] targets;

    public BooleanCatheter(boolean[] targets) {
        this.targets = targets;
    }

    public static BooleanCatheter make(boolean... targets) {
        return new BooleanCatheter(targets);
    }

    public static BooleanCatheter makeCapacity(int size) {
        return new BooleanCatheter(array(size));
    }

    public static <X> BooleanCatheter of(boolean[] targets) {
        return new BooleanCatheter(targets);
    }

    public static BooleanCatheter of(Collection<Boolean> targets) {
        if (targets == null) {
            return new BooleanCatheter(array(0));
        }
        boolean[] delegate = new boolean[targets.size()];
        int index = 0;
        for (boolean target : targets) {
            delegate[index++] = target;
        }
        return new BooleanCatheter(delegate);
    }

    public BooleanCatheter each(final BooleanConsumer action) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            action.accept(b);
        }
        return this;
    }

    public BooleanCatheter each(final BooleanConsumer action, Runnable poster) {
        each(action);
        poster.run();
        return this;
    }

    public <X> BooleanCatheter each(X initializer, final BiConsumer<X, Boolean> action) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            action.accept(initializer, b);
        }
        return this;
    }

    public <X> BooleanCatheter each(X initializer, final BiConsumer<X, Boolean> action, Consumer<X> poster) {
        each(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public <X> BooleanCatheter overall(X initializer, final TriConsumer<X, Integer, Boolean> action) {
        final boolean[] ts = this.targets;
        int index = 0;
        for (boolean b : ts) {
            action.accept(initializer, index++, b);
        }
        return this;
    }

    public <X> BooleanCatheter overall(X initializer, final TriConsumer<X, Integer, Boolean> action, Consumer<X> poster) {
        overall(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public BooleanCatheter overall(final IntegerAndBooleanConsumer action) {
        final boolean[] ts = this.targets;
        int index = 0;
        for (boolean b : ts) {
            action.accept(index++, b);
        }
        return this;
    }

    public BooleanCatheter overall(final IntegerAndBooleanConsumer action, Runnable poster) {
        overall(action);
        poster.run();
        return this;
    }

    public BooleanCatheter insert(final IntegerAndBiBooleanPredicate maker) {
        final Map<Integer, IntegerAndBooleanPair> indexes = new HashMap<>();
        final BooleanReceptacle lastItem = new BooleanReceptacle(false);
        overall((index, item) -> {
            indexes.put(
                    index + indexes.size(), 
                    new IntegerAndBooleanPair(index, maker.test(index, item, lastItem.get()))
            );
            lastItem.set(item);
        });

        final boolean[] ts = this.targets;
        final boolean[] newDelegate = array(ts.length + indexes.size());
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
                    final IntegerAndBooleanPair item = indexes.get(index);
                    newDelegate[index] = item.booleanValue();
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

    public BooleanCatheter pluck(final IntegerAndBiBooleanPredicate maker) {
        final BooleanReceptacle lastItem = new BooleanReceptacle(false);
        return overallFilter((index, item) -> {
            if (maker.test(index, item, lastItem.get())) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public BooleanCatheter discardTo(final BooleanPredicate predicate) {
        BooleanCatheter result = BooleanCatheter.make();

        overallFilter((index, item) -> !predicate.test(item), result::reset);

        return result;
    }

    public <X> BooleanCatheter discardTo(final Predicate<X> predicate, BooleanFunction<X> converter) {
        BooleanCatheter result = BooleanCatheter.make();

        overallFilter((index, item) -> !predicate.test(converter.apply(item)), result::reset);

        return result;
    }

    public BooleanCatheter discardTo(final boolean initializer, final BiBooleanPredicate predicate) {
        BooleanCatheter result = BooleanCatheter.make();

        overallFilter((index, item) -> !predicate.test(item, initializer), result::reset);

        return result;
    }

    public BooleanCatheter orDiscardTo(final boolean succeed, final BooleanPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate);
    }

    public <X> BooleanCatheter orDiscardTo(final boolean succeed, final Predicate<X> predicate, BooleanFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate, converter);
    }

    public BooleanCatheter orDiscardTo(final boolean succeed, final boolean initializer, final BiBooleanPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(initializer, predicate);
    }

    public BooleanCatheter discard(final BooleanPredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item));
    }

    public <X> BooleanCatheter discard(final Predicate<X> predicate, BooleanFunction<X> converter) {
        return overallFilter((index, item) -> !predicate.test(converter.apply(item)));
    }

    public BooleanCatheter discard(final boolean initializer, final BiBooleanPredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item, initializer));
    }

    public BooleanCatheter orDiscard(final boolean succeed, final BooleanPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(predicate);
    }

    public <X> BooleanCatheter orDiscard(final boolean succeed, final Predicate<X> predicate, BooleanFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discard(predicate, converter);
    }

    public BooleanCatheter orDiscard(final boolean succeed, final boolean initializer, final BiBooleanPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(initializer, predicate);
    }

    public BooleanCatheter filterTo(final BooleanPredicate predicate) {
        return dump().filter(predicate);
    }

    public <X> BooleanCatheter filterTo(final Predicate<X> predicate, BooleanFunction<X> converter) {
        return dump().filter(predicate, converter);
    }

    public BooleanCatheter filterTo(final boolean initializer, final BiBooleanPredicate predicate) {
        return dump().filter(initializer, predicate);
    }

    public BooleanCatheter orFilterTo(final boolean succeed, final BooleanPredicate predicate) {
        return dump().orFilter(succeed, predicate);
    }

    public <X> BooleanCatheter orFilterTo(final boolean succeed, final Predicate<X> predicate, BooleanFunction<X> converter) {
        return dump().orFilter(succeed, predicate, converter);
    }

    public BooleanCatheter orFilterTo(final boolean succeed, final boolean initializer, final BiBooleanPredicate predicate) {
        return dump().orFilter(succeed, initializer, predicate);
    }

    public BooleanCatheter filter(final BooleanPredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    public <X> BooleanCatheter filter(final Predicate<X> predicate, BooleanFunction<X> converter) {
        return overallFilter((index, item) -> predicate.test(converter.apply(item)));
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public BooleanCatheter overallFilter(final IntegerAndBooleanPredicate predicate) {
        return overallFilter(predicate, x -> {});
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param predicate The filter predicate
     * @param discarding The discarded elements
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public BooleanCatheter overallFilter(final IntegerAndBooleanPredicate predicate, Consumer<boolean[]> discarding) {
        if (isEmpty()) {
            return this;
        }

        // 创建需要的变量和常量
        final boolean[] ts = this.targets;
        final int length = ts.length;
        final boolean[] deleting = array(length);
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        for (boolean target : ts) {
            // 符合条件的保留
            if (predicate.test(index, target)) {
                index++;
                continue;
            }

            // 不符合条件的将删除表设为true，后面会去掉
            // 并且将新数组的容量减一
            deleting[index++] = true;
            newDelegateSize--;
        }

        // 创建新数组
        final boolean[] newDelegate = array(newDelegateSize);
        final boolean[] discardingDelegate = array(length - newDelegateSize);
        int discardingDelegateIndex = 0;
        int newDelegateIndex = 0;
        index = 0;

        // 遍历添加所有元素
        for (boolean isDeleting : deleting) {
            // deleting 值为 true 则为被筛选掉的，忽略
            final boolean t = ts[index++];

            if (isDeleting) {
                discardingDelegate[discardingDelegateIndex++] = t;
            } else {
                // 不为 true 则加入新数组
                newDelegate[newDelegateIndex++] = t;
            }
        }

        discarding.accept(discardingDelegate);

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    public BooleanCatheter overallFilter(final boolean initializer, final IntegerAndBiBooleanPredicate predicate) {
        return overallFilter((index, item) -> predicate.test(index, item, initializer));
    }

    public BooleanCatheter filter(final boolean initializer, final BiBooleanPredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public BooleanCatheter orFilter(final boolean succeed, final BooleanPredicate predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public <X> BooleanCatheter orFilter(final boolean succeed, final Predicate<X> predicate, BooleanFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return filter(predicate, converter);
    }

    public BooleanCatheter orFilter(final boolean succeed, final boolean initializer, final BiBooleanPredicate predicate) {
        if (succeed) {
            return this;
        }
        return filter(initializer, predicate);
    }

    public BooleanCatheter removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        boolean[] newDelegate = array(count() - 1);
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


    public boolean isPresent() {
        return count() > 0;
    }

    public BooleanCatheter ifPresent(Consumer<BooleanCatheter> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public BooleanCatheter ifEmpty(Consumer<BooleanCatheter> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }


    public BooleanCatheter distinct() {
        final Map<Boolean, Boolean> map = new HashMap<>();
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

    public BooleanCatheter sort(Comparator<Boolean> comparator) {
        Boolean[] array = new Boolean[this.targets.length];
        int index = 0;
        for (boolean target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (boolean target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public BooleanCatheter holdTill(int index) {
        if (isEmpty()) {
            return this;
        }

        index = Math.min(index, this.targets.length);

        final boolean[] ts = this.targets;
        final boolean[] newDelegate = array(index);
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

    public BooleanCatheter holdTill(final BooleanPredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final boolean[] ts = this.targets;
        final boolean[] newDelegate = array(index);
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

    public BooleanCatheter whenFlock(final boolean source, final BiBooleanPredicate maker, BooleanConsumer consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public BooleanCatheter whenFlock(BiBooleanPredicate maker, BooleanConsumer consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public boolean flock(final boolean source, final BiBooleanPredicate maker) {
        final boolean[] ts = this.targets;
        boolean result = source;
        for (boolean b : ts) {
            result = maker.test(result, b);
        }
        return result;
    }

    public boolean flock(final BiBooleanPredicate maker) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        boolean result = length > 0 && ts[0];
        for (int i = 1; i < length; i++) {
            result = maker.test(result, ts[i]);
        }
        return result;
    }

    public <X> X alternate(final X source, final BiFunction<X, Boolean, X> maker) {
        final boolean[] ts = this.targets;
        X result = source;
        for (boolean b : ts) {
            result = maker.apply(result, b);
        }
        return result;
    }

    public <X> BooleanCatheter whenAlternate(final X source, final BiFunction<X, Boolean, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public <X> BooleanCatheter whenAlternate(BiFunction<X, Boolean, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(null, maker));
        return this;
    }

    public boolean alternate(final boolean source, final BiBooleanPredicate maker) {
        BooleanReceptacle result = new BooleanReceptacle(source);
        flock((older, newer) ->{
            result.and(maker.test(older, newer));
            return newer;
        });
        return result.get();
    }

    public BooleanCatheter whenAlternate(final boolean source, final BiBooleanPredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public BooleanCatheter whenAlternate(BiBooleanPredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(false, maker));
        return this;
    }

    public BooleanCatheter waiveTill(final int index) {
        if (isEmpty()) {
            return this;
        }

        final boolean[] ts = this.targets;
        final boolean[] newDelegate;
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

    public BooleanCatheter waiveTill(final BooleanPredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final boolean[] ts = this.targets;
        final boolean[] newDelegate;
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

    public BooleanCatheter till(final BooleanPredicate predicate) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                break;
            }
        }

        return this;
    }

    public int findTill(final BooleanPredicate predicate) {
        final boolean[] ts = this.targets;
        int index = 0;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                break;
            }
            index++;
        }

        return index;
    }

    public BooleanCatheter replace(final BooleanPredicate handler) {
        if (isEmpty()) {
            return this;
        }

        final boolean[] ts = this.targets;
        int index = 0;
        for (boolean b : ts) {
            ts[index++] = handler.test(b);
        }
        return this;
    }

    public BooleanCatheter vary(final BooleanPredicate handler) {
        return replace(handler);
    }

    public DoubleCatheter vary(final BooleanToDoubleFunction handler) {
        if (isEmpty()) {
            return DoubleCatheter.make();
        }

        final boolean[] ts = this.targets;
        final double[] array = new double[ts.length];
        int index = 0;
        for (boolean i : ts) {
            array[index++] = handler.applyAsDouble(i);
        }
        return DoubleCatheter.of(array);
    }

    public ByteCatheter vary(final BooleanToByteFunction handler) {
        if (isEmpty()) {
            return ByteCatheter.make();
        }

        final boolean[] ts = this.targets;
        final byte[] array = new byte[ts.length];
        int index = 0;
        for (boolean i : ts) {
            array[index++] = handler.applyAsByte(i);
        }
        return ByteCatheter.of(array);
    }

    public LongCatheter vary(final BooleanToLongFunction handler) {
        if (isEmpty()) {
            return LongCatheter.make();
        }

        final boolean[] ts = this.targets;
        final long[] array = new long[ts.length];
        int index = 0;
        for (boolean i : ts) {
            array[index++] = handler.applyAsLong(i);
        }
        return LongCatheter.of(array);
    }

    public IntCatheter vary(final BooleanToIntegerFunction handler) {
        if (isEmpty()) {
            return IntCatheter.make();
        }

        final boolean[] ts = this.targets;
        final int[] array = new int[ts.length];
        int index = 0;
        for (boolean i : ts) {
            array[index++] = handler.applyAsInteger(i);
        }
        return IntCatheter.of(array);
    }

    public <X> Catheter<X> vary(final BooleanFunction<X> handler) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final boolean[] ts = this.targets;
        final X[] array = xArray(ts.length);
        int index = 0;
        for (boolean b : ts) {
            array[index++] = handler.apply(b);
        }
        return new Catheter<>(array);
    }

    public <X> Catheter<X> vary(final BooleanFunction<X> handler, IntFunction<X[]> arrayGenerator) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final boolean[] ts = this.targets;
        final X[] array = arrayGenerator.apply(ts.length);
        int index = 0;
        for (boolean l : ts) {
            array[index++] = handler.apply(l);
        }
        return Catheter.of(array).arrayGenerator(arrayGenerator);
    }

    public BooleanCatheter whenAny(final BooleanPredicate predicate, final BooleanConsumer action) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                action.accept(b);
                break;
            }
        }
        return this;
    }

    public BooleanCatheter whenAll(final BooleanPredicate predicate, final Runnable action) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                continue;
            }
            return this;
        }
        action.run();
        return this;
    }

    public BooleanCatheter whenAll(final BooleanPredicate predicate, final BooleanConsumer action) {
        return whenAll(predicate, () -> each(action));
    }

    private BooleanCatheter whenNone(final BooleanPredicate predicate, final Runnable action) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasTrue() {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (b) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFalse() {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (!b) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAny(final BooleanPredicate predicate) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final BooleanPredicate predicate) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public boolean hasNone(final BooleanPredicate predicate) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                return false;
            }
        }
        return true;
    }

    public boolean findFirst(final BooleanPredicate predicate) {
        final boolean[] ts = this.targets;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                return b;
            }
        }
        return false;
    }

    public boolean findLast(final BooleanPredicate predicate) {
        final boolean[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final boolean t = ts[index--];
            if (predicate.test(t)) {
                return t;
            }
        }
        return false;
    }

    public <X> X whenFoundFirst(final BooleanPredicate predicate, BooleanFunction<X> function) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        for (boolean b : ts) {
            if (predicate.test(b)) {
                return function.apply(b);
            }
        }
        return null;
    }

    public <X> X whenFoundLast(final BooleanPredicate predicate, BooleanFunction<X> function) {
        final boolean[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final boolean t = ts[index--];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }


    public BooleanCatheter any(final BooleanConsumer consumer) {
        if (this.targets.length > 0) {
            boolean[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public BooleanCatheter first(final BooleanConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public BooleanCatheter tail(final BooleanConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public BooleanCatheter reverse() {
        if (isEmpty()) {
            return this;
        }

        final boolean[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        boolean temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public boolean max(final Comparator<Boolean> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public boolean min(final Comparator<Boolean> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public BooleanCatheter whenMax(final Comparator<Boolean> comparator, final BooleanConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public BooleanCatheter whenMin(final Comparator<Boolean> comparator, final BooleanConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    public int count() {
        return this.targets.length;
    }

    public BooleanCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public BooleanCatheter count(final IntegerReceptacle target) {
        target.set(count());
        return this;
    }

    public BooleanCatheter count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    public final BooleanCatheter append(final boolean... objects) {
        final boolean[] ts = this.targets;
        final boolean[] newDelegate = array(ts.length + objects.length);
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

    public BooleanCatheter append(final BooleanCatheter objects) {
        return append(objects.array());
    }

    public BooleanCatheter repeat(final int count) {
        final boolean[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public boolean fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, boolean item) {
        this.targets[index] = item;
    }

    public BooleanCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Boolean> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, BooleanCatheter input, final TriFunction<MatrixPos, Boolean, Boolean, X> action) {
        if (input.count() == count()) {
            final IntegerReceptacle index = new IntegerReceptacle(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final boolean inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public BooleanCatheter matrixMap(
            final int width,
            final int inputWidth,
            final BooleanCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Boolean, Boolean, Boolean> scanFlocked,
            final TriFunction<MatrixPos, Boolean, Boolean, Boolean> combineFlocked
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
        final BooleanCatheter newMatrix = BooleanCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final BooleanCatheter flockingCatheter = BooleanCatheter.makeCapacity(width);

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
                final boolean fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final boolean fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public <X> Catheter<X> matrixVary(final int width, boolean input, final TriFunction<MatrixPos, Boolean, Boolean, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public BooleanCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Boolean, Boolean> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Boolean, X> action) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final IntegerReceptacle w = new IntegerReceptacle(0);
        final IntegerReceptacle h = new IntegerReceptacle(0);

        final int matrixEdge = width - 1;

        return vary((boolean item) -> {
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

    public Catheter<BooleanCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<BooleanCatheter> results = Catheter.makeCapacity(sourceHeight);
        BooleanCatheter catheter = BooleanCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final boolean element = fetch(y * width + x);
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

    public BooleanCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public BooleanCatheter shuffle(RandomGenerator random) {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public BooleanCatheter swapShuffle(RandomGenerator random) {
        boolean[] elements = this.targets;
        int i = elements.length;

        for (int j = i; j > 1; --j) {
            int swapTo = random.nextInt(j);
            int swapFrom = j - 1;
            boolean fromElement = elements[swapFrom];
            boolean toElement = elements[swapTo];
            elements[swapTo] = fromElement;
            elements[swapFrom] = toElement;
        }

        return this;
    }

    public boolean has(boolean target) {
        if (target) {
            return hasTrue();
        }
        return hasFalse();
    }

    public boolean not(boolean target) {
        return !has(target);
    }

    public BooleanCatheter merge(BooleanCatheter other) {
        boolean otherHasTrue = other.hasTrue();
        boolean otherHasFalse = other.hasFalse();

        if (!hasTrue() && otherHasTrue) {
            return append(true);
        }

        if (!hasFalse() && otherHasFalse) {
            return append(false);
        }

        if (otherHasTrue) {
            return append(true);
        }

        if (otherHasFalse) {
            return append(false);
        }

        return this;
    }

    public BooleanCatheter dump() {
        return new BooleanCatheter(array());
    }

    public BooleanCatheter flat(BooleanFunction<BooleanCatheter> function) {
        if (isEmpty()) {
            return this;
        }

        Catheter<BooleanCatheter> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (boolean element : this.targets) {
            BooleanCatheter flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        this.targets = array(totalSize);
        int pos = 0;
        for (BooleanCatheter flat : catheter.targets) {
            System.arraycopy(flat.targets,
                    0,
                    this.targets,
                    pos,
                    flat.targets.length
            );
            pos += flat.targets.length;
        }
        return this;
    }

    public <X> Catheter<X> flatTo(BooleanFunction<Catheter<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Catheter<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (boolean element : this.targets) {
            Catheter<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        return Catheter.flatting(catheter, totalSize);
    }

    public <X> Catheter<X> flatToByCollection(BooleanFunction<Collection<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Collection<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (boolean element : this.targets) {
            Collection<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.size();
        }

        return Catheter.flattingCollection(catheter, totalSize);
    }

    public BooleanCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public BooleanCatheter reset(boolean[] targets) {
        this.targets = targets;
        return this;
    }

    public boolean[] array() {
        return this.targets.clone();
    }

    public boolean[] dArray() {
        return this.targets;
    }

    public Stream<Boolean> stream() {
        return list().stream();
    }

    public List<Boolean> list() {
        List<Boolean> list = new ArrayList<>();
        for (boolean l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Boolean> set() {
        Set<Boolean> set = new HashSet<>();
        for (boolean l : array()) {
            set.add(l);
        }
        return set;
    }

    public static void main(String[] args) {
        BooleanCatheter source = BooleanCatheter.make(
                true, false, true,
                false, true, false,
                true, false, true
        );
        BooleanCatheter input = BooleanCatheter.make(
                true, true, true,
                false, false, false,
                true, true, true
        );

        source.dump()
                .matrixHomoVary(3, input, (pos, sourceX, inputX) -> {
                    return (sourceX ? 10 : 5) + (inputX ? 20 : 15);
                })
                .matrixEach(3, (pos, item) -> {
                    System.out.println(pos);
                    System.out.println(item);
                });

        System.out.println("------");

        source.matrixMap(3, 3, input, (flockPos, sourcePos, inputPos, sourceX, inputX) -> {
                    return sourceX || inputX;
                }, (destPos, combine1, combine2) -> {
                    return combine1 && combine2;
                })
                .matrixEach(3, (pos, item) -> {
                    System.out.println(pos);
                    System.out.println(item);
                });


    }

    private static boolean[] array(int size) {
        return new boolean[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
