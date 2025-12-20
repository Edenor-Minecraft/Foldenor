package com.github.cao.awa.catheter;

import com.github.cao.awa.catheter.action.*;
import com.github.cao.awa.catheter.matrix.MatrixFlockPos;
import com.github.cao.awa.catheter.matrix.MatrixPos;
import com.github.cao.awa.catheter.pair.IntegerPair;
import com.github.cao.awa.catheter.receptacle.BooleanReceptacle;
import com.github.cao.awa.catheter.receptacle.Receptacle;
import com.github.cao.awa.catheter.receptacle.IntegerReceptacle;
import com.github.cao.awa.sinuatum.function.consumer.TriConsumer;
import com.github.cao.awa.sinuatum.function.QuinFunction;
import com.github.cao.awa.sinuatum.function.TriFunction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class IntCatheter {
    private static final Random RANDOM = new Random();
    private int[] targets;

    public IntCatheter(int[] targets) {
        this.targets = targets;
    }

    public static IntCatheter make(int... targets) {
        return new IntCatheter(targets);
    }

    public static IntCatheter makeCapacity(int size) {
        return new IntCatheter(array(size));
    }

    public static <X> IntCatheter of(int[] targets) {
        return new IntCatheter(targets);
    }

    public static IntCatheter of(Collection<Integer> targets) {
        if (targets == null) {
            return new IntCatheter(array(0));
        }
        int[] delegate = new int[targets.size()];
        int index = 0;
        for (int target : targets) {
            delegate[index++] = target;
        }
        return new IntCatheter(delegate);
    }

    public IntCatheter each(final IntConsumer action) {
        final int[] ts = this.targets;
        for (int i : ts) {
            action.accept(i);
        }
        return this;
    }

    public IntCatheter each(final IntConsumer action, Runnable poster) {
        each(action);
        poster.run();
        return this;
    }

    public <X> IntCatheter each(X initializer, final BiConsumer<X, Integer> action) {
        final int[] ts = this.targets;
        for (int i : ts) {
            action.accept(initializer, i);
        }
        return this;
    }

    public <X> IntCatheter each(X initializer, final BiConsumer<X, Integer> action, Consumer<X> poster) {
        each(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public <X> IntCatheter overall(X initializer, final TriConsumer<X, Integer, Integer> action) {
        final int[] ts = this.targets;
        int index = 0;
        for (int i : ts) {
            action.accept(initializer, index++, i);
        }
        return this;
    }

    public <X> IntCatheter overall(X initializer, final TriConsumer<X, Integer, Integer> action, Consumer<X> poster) {
        overall(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public IntCatheter overall(final BiIntegerConsumer action) {
        final int[] ts = this.targets;
        int index = 0;
        for (int i : ts) {
            action.accept(index++, i);
        }
        return this;
    }

    public IntCatheter overall(final BiIntegerConsumer action, Runnable poster) {
        overall(action);
        poster.run();
        return this;
    }

    public IntCatheter insert(final TriIntegerToIntegerFunction maker) {
        final Map<Integer, IntegerPair> indexes = new HashMap<>();
        final IntegerReceptacle lastItem = new IntegerReceptacle(0);
        overall((index, item) -> {
            indexes.put(
                    index + indexes.size(),
                    new IntegerPair(index, maker.apply(index, item, lastItem.get()))
            );
            lastItem.set(item);
        });

        final int[] ts = this.targets;
        final int[] newDelegate = array(ts.length + indexes.size());
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
                    final IntegerPair item = indexes.get(index);
                    newDelegate[index] = item.second();
                    lastIndex.set(item.first());
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

    public IntCatheter pluck(final TriIntegerPredicate maker) {
        final IntegerReceptacle lastItem = new IntegerReceptacle(0);
        return overallFilter((index, item) -> {
            if (maker.test(index, item, lastItem.get())) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public IntCatheter discardTo(final IntPredicate predicate) {
        IntCatheter result = IntCatheter.make();

        overallFilter((index, item) -> !predicate.test(item), result::reset);

        return result;
    }

    public <X> IntCatheter discardTo(final Predicate<X> predicate, IntFunction<X> converter) {
        IntCatheter result = IntCatheter.make();

        overallFilter((index, item) -> !predicate.test(converter.apply(item)), result::reset);

        return result;
    }

    public IntCatheter discardTo(final int initializer, final BiIntegerPredicate predicate) {
        IntCatheter result = IntCatheter.make();

        overallFilter((index, item) -> !predicate.test(item, initializer), result::reset);

        return result;
    }

    public IntCatheter orDiscardTo(final boolean succeed, final IntPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate);
    }

    public <X> IntCatheter orDiscardTo(final boolean succeed, final Predicate<X> predicate, IntFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate, converter);
    }

    public IntCatheter orDiscardTo(final boolean succeed, final int initializer, final BiIntegerPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(initializer, predicate);
    }

    public IntCatheter discard(final IntPredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item));
    }

    public <X> IntCatheter discard(final Predicate<X> predicate, IntFunction<X> converter) {
        return overallFilter((index, item) -> !predicate.test(converter.apply(item)));
    }

    public IntCatheter discard(final int initializer, final BiIntegerPredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item, initializer));
    }

    public IntCatheter orDiscard(final boolean succeed, final IntPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(predicate);
    }

    public <X> IntCatheter orDiscard(final boolean succeed, final Predicate<X> predicate, IntFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discard(predicate, converter);
    }

    public IntCatheter orDiscard(final boolean succeed, final int initializer, final BiIntegerPredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(initializer, predicate);
    }

    public IntCatheter filterTo(final IntPredicate predicate) {
        return dump().filter(predicate);
    }

    public <X> IntCatheter filterTo(final Predicate<X> predicate, IntFunction<X> converter) {
        return dump().filter(predicate, converter);
    }

    public IntCatheter filterTo(final int initializer, final BiIntegerPredicate predicate) {
        return dump().filter(initializer, predicate);
    }

    public IntCatheter orFilterTo(final boolean succeed, final IntPredicate predicate) {
        return dump().orFilter(succeed, predicate);
    }

    public <X> IntCatheter orFilterTo(final boolean succeed, final Predicate<X> predicate, IntFunction<X> converter) {
        return dump().orFilter(succeed, predicate, converter);
    }

    public IntCatheter orFilterTo(final boolean succeed, final int initializer, final BiIntegerPredicate predicate) {
        return dump().orFilter(succeed, initializer, predicate);
    }

    public IntCatheter filter(final IntPredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    public <X> IntCatheter filter(final Predicate<X> predicate, IntFunction<X> converter) {
        return overallFilter((index, item) -> predicate.test(converter.apply(item)));
    }

    public IntCatheter filter(final int initializer, final BiIntegerPredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public IntCatheter orFilter(final boolean succeed, final IntPredicate predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public <X> IntCatheter orFilter(final boolean succeed, final Predicate<X> predicate, IntFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return filter(predicate, converter);
    }

    public IntCatheter orFilter(final boolean succeed, final int initializer, final BiIntegerPredicate predicate) {
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
    public IntCatheter overallFilter(final BiIntegerPredicate predicate) {
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
    public IntCatheter overallFilter(final BiIntegerPredicate predicate, Consumer<int[]> discarding) {
        if (isEmpty()) {
            return this;
        }

        // 创建需要的变量和常量
        final int[] ts = this.targets;
        final int length = ts.length;
        final boolean[] deleting = new boolean[length];
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        for (int target : ts) {
            // 符合条件的保留
            if (predicate.test(index, target)) {
                index++;
                continue;
            }

            // 不符合条件的标记deleting为true，后面会去掉
            // 并且将新数组的容量减一
            deleting[index++] = true;
            newDelegateSize--;
        }

        // 创建新数组
        final int[] newDelegate = array(newDelegateSize);
        final int[] discardingDelegate = array(length - newDelegateSize);
        int discardingDelegateIndex = 0;
        int newDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        for (boolean isDeleting : deleting) {
            // deleting 值为true则为被筛选掉的，加入discarding
            final int t = ts[index++];

            if (isDeleting) {
                discardingDelegate[discardingDelegateIndex++] = t;
            } else {
                // 不为true则加入新数组
                newDelegate[newDelegateIndex++] = t;
            }
        }

        discarding.accept(discardingDelegate);

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    public IntCatheter overallFilter(final int initializer, final TriIntegerPredicate predicate) {
        return overallFilter((index, item) -> predicate.test(index, item, initializer));
    }

    public boolean isPresent() {
        return count() > 0;
    }

    public IntCatheter ifPresent(Consumer<IntCatheter> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public IntCatheter ifEmpty(Consumer<IntCatheter> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }

    public IntCatheter removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        int[] newDelegate = array(count() - 1);
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

    public IntCatheter distinct() {
        final Map<Integer, Boolean> map = new HashMap<>();
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

    public IntCatheter sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public IntCatheter sort(Comparator<Integer> comparator) {
        Integer[] array = new Integer[this.targets.length];
        int index = 0;
        for (int target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (int target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public IntCatheter holdTill(int index) {
        if (isEmpty()) {
            return this;
        }

        index = Math.min(index, this.targets.length);

        final int[] ts = this.targets;
        final int[] newDelegate = array(index);
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

    public IntCatheter holdTill(final IntPredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final int[] ts = this.targets;
        final int[] newDelegate = array(index);
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

    public IntCatheter whenFlock(final int source, final BiIntegerToIntegerFunction maker, IntConsumer consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public IntCatheter whenFlock(BiIntegerToIntegerFunction maker, IntConsumer consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public int flock(final int source, final BiIntegerToIntegerFunction maker) {
        final int[] ts = this.targets;
        int result = source;
        for (int i : ts) {
            result = maker.applyAsInt(result, i);
        }
        return result;
    }

    public int flock(final BiIntegerToIntegerFunction maker) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int result = length > 0 ? ts[0] : 0;
        for (int i : ts) {
            result = maker.applyAsInt(result, i);
        }
        return result;
    }

    public <X> X alternate(final X source, final BiFunction<X, Integer, X> maker) {
        final int[] ts = this.targets;
        X result = source;
        for (int i : ts) {
            result = maker.apply(result, i);
        }
        return result;
    }

    public <X> IntCatheter whenAlternate(final X source, final BiFunction<X, Integer, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public <X> IntCatheter whenAlternate(BiFunction<X, Integer, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(null, maker));
        return this;
    }

    public boolean alternate(final boolean source, final BiIntegerPredicate maker) {
        BooleanReceptacle result = new BooleanReceptacle(source);
        flock((older, newer) ->{
            result.and(maker.test(older, newer));
            return newer;
        });
        return result.get();
    }

    public IntCatheter whenAlternate(final boolean source, final BiIntegerPredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public IntCatheter whenAlternate(BiIntegerPredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(false, maker));
        return this;
    }

    public IntCatheter waiveTill(final int index) {
        if (isEmpty()) {
            return this;
        }

        final int[] ts = this.targets;
        final int[] newDelegate;
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

    public IntCatheter waiveTill(final IntPredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final int[] ts = this.targets;
        final int[] newDelegate;
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

    public IntCatheter till(final IntPredicate predicate) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                break;
            }
        }

        return this;
    }

    public int findTill(final IntPredicate predicate) {
        final int[] ts = this.targets;
        int index = 0;
        for (int i : ts) {
            if (predicate.test(i)) {
                break;
            }
            index++;
        }

        return index;
    }

    public IntCatheter replace(final IntUnaryOperator handler) {
        if (isEmpty()) {
            return this;
        }

        final int[] ts = this.targets;
        int index = 0;
        for (int i : ts) {
            ts[index++] = handler.applyAsInt(i);
        }
        return this;
    }

    public ByteCatheter vary(final IntegerToByteFunction handler) {
        if (isEmpty()) {
            return ByteCatheter.make();
        }

        final int[] ts = this.targets;
        final byte[] array = new byte[ts.length];
        int index = 0;
        for (int i : ts) {
            array[index++] = handler.applyAsByte(i);
        }
        return ByteCatheter.of(array);
    }

    public BooleanCatheter vary(final IntPredicate handler) {
        if (isEmpty()) {
            return BooleanCatheter.make();
        }

        final int[] ts = this.targets;
        final boolean[] array = new boolean[ts.length];
        int index = 0;
        for (int i : ts) {
            array[index++] = handler.test(i);
        }
        return BooleanCatheter.of(array);
    }

    public DoubleCatheter vary(final IntToDoubleFunction handler) {
        if (isEmpty()) {
            return DoubleCatheter.make();
        }

        final int[] ts = this.targets;
        final double[] array = new double[ts.length];
        int index = 0;
        for (int i : ts) {
            array[index++] = handler.applyAsDouble(i);
        }
        return DoubleCatheter.of(array);
    }

    public IntCatheter vary(final IntUnaryOperator handler) {
        return replace(handler);
    }

    public LongCatheter vary(final IntToLongFunction handler) {
        if (isEmpty()) {
            return LongCatheter.make();
        }

        final int[] ts = this.targets;
        final long[] array = new long[ts.length];
        int index = 0;
        for (int i : ts) {
            array[index++] = handler.applyAsLong(i);
        }
        return LongCatheter.of(array);
    }

    public <X> Catheter<X> vary(final IntFunction<X> handler) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final int[] ts = this.targets;
        final X[] array = xArray(ts.length);
        int index = 0;
        for (int i : ts) {
            array[index++] = handler.apply(i);
        }
        return Catheter.of(array);
    }

    public <X> Catheter<X> vary(final IntFunction<X> handler, IntFunction<X[]> arrayGenerator) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final int[] ts = this.targets;
        final X[] array = arrayGenerator.apply(ts.length);
        int index = 0;
        for (int i : ts) {
            array[index++] = handler.apply(i);
        }
        return Catheter.of(array).arrayGenerator(arrayGenerator);
    }

    public IntCatheter whenAny(final IntPredicate predicate, final IntConsumer action) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                action.accept(i);
                break;
            }
        }
        return this;
    }

    public IntCatheter whenAll(final IntPredicate predicate, final Runnable action) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                continue;
            }
            return this;
        }
        action.run();
        return this;
    }

    public IntCatheter whenAll(final IntPredicate predicate, final IntConsumer action) {
        return whenAll(predicate, () -> each(action));
    }

    private IntCatheter whenNone(final IntPredicate predicate, final Runnable action) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final IntPredicate predicate) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final IntPredicate predicate) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public boolean hasNone(final IntPredicate predicate) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                return false;
            }
        }
        return true;
    }

    public int findFirst(final IntPredicate predicate) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                return i;
            }
        }
        return 0;
    }

    public int findLast(final IntPredicate predicate) {
        final int[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final int t = ts[index--];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0;
    }

    public <X> X whenFoundFirst(final IntPredicate predicate, IntFunction<X> function) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                return function.apply(i);
            }
        }
        return null;
    }

    public <X> X whenFoundLast(final IntPredicate predicate, IntFunction<X> function) {
        final int[] ts = this.targets;
        for (int i : ts) {
            if (predicate.test(i)) {
                return function.apply(i);
            }
        }
        return null;
    }

    public IntCatheter any(final IntConsumer consumer) {
        if (this.targets.length > 0) {
            int[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public IntCatheter first(final IntConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public IntCatheter tail(final IntConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public IntCatheter reverse() {
        if (isEmpty()) {
            return this;
        }

        final int[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        int temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public int max(final Comparator<Integer> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public int min(final Comparator<Integer> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public IntCatheter whenMax(final Comparator<Integer> comparator, final IntConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public IntCatheter whenMin(final Comparator<Integer> comparator, final IntConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    public int count() {
        return this.targets.length;
    }

    public IntCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public IntCatheter count(final Receptacle<Integer> target) {
        target.set(count());
        return this;
    }

    public IntCatheter count(final IntConsumer consumer) {
        consumer.accept(count());
        return this;
    }

    public final IntCatheter append(final int... objects) {
        final int[] ts = this.targets;
        final int[] newDelegate = array(ts.length + objects.length);
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

    public IntCatheter append(final IntCatheter objects) {
        return append(objects.array());
    }

    public IntCatheter repeat(final int count) {
        final int[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public int fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, int item) {
        this.targets[index] = item;
    }

    public IntCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Integer> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, IntCatheter input, final TriFunction<MatrixPos, Integer, Integer, X> action) {
        if (input.count() == count()) {
            final IntegerReceptacle index = new IntegerReceptacle(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final int inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public IntCatheter matrixMap(
            final int width,
            final int inputWidth,
            final IntCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Integer, Integer, Integer> scanFlocked,
            final TriFunction<MatrixPos, Integer, Integer, Integer> combineFlocked
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
        final IntCatheter newMatrix = IntCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final IntCatheter flockingCatheter = IntCatheter.makeCapacity(width);

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
                final int fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final int fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public <X> Catheter<X> matrixVary(final int width, int input, final TriFunction<MatrixPos, Integer, Integer, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public IntCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Integer, Integer> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Integer, X> action) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final IntegerReceptacle w = new IntegerReceptacle(0);
        final IntegerReceptacle h = new IntegerReceptacle(0);

        final int matrixEdge = width - 1;

        return vary((int item) -> {
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

    public Catheter<IntCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<IntCatheter> results = Catheter.makeCapacity(sourceHeight);
        IntCatheter catheter = IntCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final int element = fetch(y * width + x);
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

    public IntCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public IntCatheter shuffle(RandomGenerator random) {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public IntCatheter swapShuffle(RandomGenerator random) {
        int[] elements = this.targets;
        int i = elements.length;

        for (int j = i; j > 1; --j) {
            int swapTo = random.nextInt(j);
            int swapFrom = j - 1;
            int fromElement = elements[swapFrom];
            int toElement = elements[swapTo];
            elements[swapTo] = fromElement;
            elements[swapFrom] = toElement;
        }

        return this;
    }

    public boolean has(int target) {
        return hasAny(t -> t == target);
    }

    public boolean not(int target) {
        return !has(target);
    }

    public IntCatheter merge(IntCatheter other) {
        return append(other.filter(this::not));
    }

    public IntCatheter dump() {
        return new IntCatheter(array());
    }

    public IntCatheter flat(IntFunction<IntCatheter> function) {
        return arrayFlat(generator -> function.apply(generator).targets);
    }

    public IntCatheter arrayFlat(IntegerArrayFunction function) {
        if (isEmpty()) {
            return this;
        }

        int[][] longs = new int[count()][];
        int totalSize = 0;

        int index = 0;
        int[] targets = this.targets;
        for (int element : targets) {
            int[] flatting = function.apply(element);
            longs[index++] = flatting;
            totalSize += flatting.length;
        }

        targets = array(totalSize);
        int pos = 0;
        for (int[] flat : longs) {
            for (int l : flat) {
                targets[pos++] = l;
            }
        }

        this.targets = targets;

        return this;
    }

    public <X> Catheter<X> flatTo(IntFunction<Catheter<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Catheter<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (int element : this.targets) {
            Catheter<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        return Catheter.flatting(catheter, totalSize);
    }

    public <X> Catheter<X> flatToByCollection(IntFunction<Collection<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Collection<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (int element : this.targets) {
            Collection<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.size();
        }

        return Catheter.flattingCollection(catheter, totalSize);
    }

    public IntCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public IntCatheter reset(int[] targets) {
        this.targets = targets;
        return this;
    }

    public int[] array() {
        return this.targets.clone();
    }

    public int[] dArray() {
        return this.targets;
    }

    public IntStream stream() {
        return IntStream.of(array());
    }

    public List<Integer> list() {
        List<Integer> list = new ArrayList<>();
        for (int l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Integer> set() {
        Set<Integer> set = new HashSet<>();
        for (int l : array()) {
            set.add(l);
        }
        return set;
    }

    public static void main(String[] args) {
        IntCatheter source = IntCatheter.make(
                1, 2, 3, 4, 5, 6, 7, 8
        );

        System.out.println("???");

        System.out.println(source.removeWithIndex(4).list());

        System.out.println("???");
    }

    private static int[] array(int size) {
        return new int[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
