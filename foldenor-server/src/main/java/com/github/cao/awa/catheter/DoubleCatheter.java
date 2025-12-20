package com.github.cao.awa.catheter;

import com.github.cao.awa.catheter.action.*;
import com.github.cao.awa.catheter.matrix.MatrixFlockPos;
import com.github.cao.awa.catheter.matrix.MatrixPos;
import com.github.cao.awa.catheter.pair.IntegerAndDoublePair;
import com.github.cao.awa.catheter.receptacle.BooleanReceptacle;
import com.github.cao.awa.catheter.receptacle.DoubleReceptacle;
import com.github.cao.awa.catheter.receptacle.Receptacle;
import com.github.cao.awa.catheter.receptacle.IntegerReceptacle;
import com.github.cao.awa.sinuatum.function.consumer.TriConsumer;
import com.github.cao.awa.sinuatum.function.QuinFunction;
import com.github.cao.awa.sinuatum.function.TriFunction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

public class DoubleCatheter {
    private static final Random RANDOM = new Random();
    private double[] targets;

    public DoubleCatheter(double[] targets) {
        this.targets = targets;
    }

    public static DoubleCatheter make(double... targets) {
        return new DoubleCatheter(targets);
    }

    public static DoubleCatheter makeCapacity(int size) {
        return new DoubleCatheter(array(size));
    }

    public static <X> DoubleCatheter of(double[] targets) {
        return new DoubleCatheter(targets);
    }

    public static DoubleCatheter of(Collection<Double> targets) {
        if (targets == null) {
            return new DoubleCatheter(array(0));
        }
        double[] delegate = new double[targets.size()];
        int index = 0;
        for (double target : targets) {
            delegate[index++] = target;
        }
        return new DoubleCatheter(delegate);
    }

    public DoubleCatheter each(final DoubleConsumer action) {
        final double[] ts = this.targets;
        for (double d : ts) {
            action.accept(d);
        }
        return this;
    }

    public DoubleCatheter each(final DoubleConsumer action, Runnable poster) {
        each(action);
        poster.run();
        return this;
    }

    public <X> DoubleCatheter each(X initializer, final BiConsumer<X, Double> action) {
        final double[] ts = this.targets;
        for (double d : ts) {
            action.accept(initializer, d);
        }
        return this;
    }

    public <X> DoubleCatheter each(X initializer, final BiConsumer<X, Double> action, Consumer<X> poster) {
        each(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public <X> DoubleCatheter overall(X initializer, final TriConsumer<X, Integer, Double> action) {
        final double[] ts = this.targets;
        int index = 0;
        for (double d : ts) {
            action.accept(initializer, index++, d);
        }
        return this;
    }

    public <X> DoubleCatheter overall(X initializer, final TriConsumer<X, Integer, Double> action, Consumer<X> poster) {
        overall(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public DoubleCatheter overall(final IntegerAndDoubleConsumer action) {
        final double[] ts = this.targets;
        int index = 0;
        for (double d : ts) {
            action.accept(index++, d);
        }
        return this;
    }

    public DoubleCatheter overall(final IntegerAndDoubleConsumer action, Runnable poster) {
        overall(action);
        poster.run();
        return this;
    }

    public DoubleCatheter insert(final IntegerAndBiDoubleToDoubleFunction maker) {
        final Map<Integer, IntegerAndDoublePair> indexes = new HashMap<>();
        final DoubleReceptacle lastItem = new DoubleReceptacle(0);
        overall((index, item) -> {
            indexes.put(
                    index + indexes.size(), 
                    new IntegerAndDoublePair(index, maker.apply(index, item, lastItem.get()))
            );
            lastItem.set(item);
        });

        final double[] ts = this.targets;
        final double[] newDelegate = array(ts.length + indexes.size());
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
                    final IntegerAndDoublePair item = indexes.get(index);
                    newDelegate[index] = item.doubleValue();
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

    public DoubleCatheter pluck(final IntegerAndBiDoublePredicate maker) {
        final DoubleReceptacle lastItem = new DoubleReceptacle(0);
        return overallFilter((index, item) -> {
            if (maker.test(index, item, lastItem.get())) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public DoubleCatheter discardTo(final DoublePredicate predicate) {
        DoubleCatheter result = DoubleCatheter.make();

        overallFilter((index, item) -> !predicate.test(item), result::reset);

        return result;
    }

    public <X> DoubleCatheter discardTo(final Predicate<X> predicate, DoubleFunction<X> converter) {
        DoubleCatheter result = DoubleCatheter.make();

        overallFilter((index, item) -> !predicate.test(converter.apply(item)), result::reset);

        return result;
    }

    public DoubleCatheter discardTo(final double initializer, final BiDoublePredicate predicate) {
        DoubleCatheter result = DoubleCatheter.make();

        overallFilter((index, item) -> !predicate.test(item, initializer), result::reset);

        return result;
    }

    public DoubleCatheter orDiscardTo(final boolean succeed, final DoublePredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate);
    }

    public <X> DoubleCatheter orDiscardTo(final boolean succeed, final Predicate<X> predicate, DoubleFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate, converter);
    }

    public DoubleCatheter orDiscardTo(final boolean succeed, final double initializer, final BiDoublePredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(initializer, predicate);
    }

    public DoubleCatheter discard(final DoublePredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item));
    }

    public <X> DoubleCatheter discard(final Predicate<X> predicate, DoubleFunction<X> converter) {
        return overallFilter((index, item) -> !predicate.test(converter.apply(item)));
    }

    public DoubleCatheter discard(final double initializer, final BiDoublePredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item, initializer));
    }

    public DoubleCatheter orDiscard(final boolean succeed, final DoublePredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(predicate);
    }

    public <X> DoubleCatheter orDiscard(final boolean succeed, final Predicate<X> predicate, DoubleFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discard(predicate, converter);
    }

    public DoubleCatheter orDiscard(final boolean succeed, final double initializer, final BiDoublePredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(initializer, predicate);
    }

    public DoubleCatheter filterTo(final DoublePredicate predicate) {
        return dump().filter(predicate);
    }

    public <X> DoubleCatheter filterTo(final Predicate<X> predicate, DoubleFunction<X> converter) {
        return dump().filter(predicate, converter);
    }

    public DoubleCatheter filterTo(final double initializer, final BiDoublePredicate predicate) {
        return dump().filter(initializer, predicate);
    }

    public DoubleCatheter orFilterTo(final boolean succeed, final DoublePredicate predicate) {
        return dump().orFilter(succeed, predicate);
    }

    public <X> DoubleCatheter orFilterTo(final boolean succeed, final Predicate<X> predicate, DoubleFunction<X> converter) {
        return dump().orFilter(succeed, predicate, converter);
    }

    public DoubleCatheter orFilterTo(final boolean succeed, final double initializer, final BiDoublePredicate predicate) {
        return dump().orFilter(succeed, initializer, predicate);
    }

    public DoubleCatheter filter(final DoublePredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    public <X> DoubleCatheter filter(final Predicate<X> predicate, DoubleFunction<X> converter) {
        return overallFilter((index, item) -> predicate.test(converter.apply(item)));
    }

    public DoubleCatheter filter(final double initializer, final BiDoublePredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public DoubleCatheter orFilter(final boolean succeed, final DoublePredicate predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public <X> DoubleCatheter orFilter(final boolean succeed, final Predicate<X> predicate, DoubleFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return filter(predicate, converter);
    }

    public DoubleCatheter orFilter(final boolean succeed, final double initializer, final BiDoublePredicate predicate) {
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
    public DoubleCatheter overallFilter(final IntegerAndDoublePredicate predicate) {
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
    public DoubleCatheter overallFilter(final IntegerAndDoublePredicate predicate, Consumer<double[]> discarding) {
        if (isEmpty()) {
            return this;
        }

        // 创建需要的变量和常量
        final double[] ts = this.targets;
        final int length = ts.length;
        final boolean[] deleting = new boolean[length];
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        for (double target : ts) {
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
        final double[] newDelegate = array(newDelegateSize);
        final double[] discardingDelegate = array(length - newDelegateSize);
        int discardingDelegateIndex = 0;
        int newDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        for (boolean isDeleting : deleting) {
            // deleting 值为true则为被筛选掉的，加入discarding
            final double t = ts[index++];

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

    public DoubleCatheter overallFilter(final double initializer, final IntegerAndBiDoublePredicate predicate) {
        return overallFilter((index, item) -> predicate.test(index, item, initializer));
    }

    public DoubleCatheter distinct() {
        final Map<Double, Boolean> map = new HashMap<>();
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

    public DoubleCatheter sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public DoubleCatheter sort(Comparator<Double> comparator) {
        Double[] array = new Double[this.targets.length];
        int index = 0;
        for (double target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (double target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public DoubleCatheter holdTill(int index) {
        if (isEmpty()) {
            return this;
        }

        index = Math.min(index, this.targets.length);

        final double[] ts = this.targets;
        final double[] newDelegate = array(index);
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

    public DoubleCatheter holdTill(final DoublePredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final double[] ts = this.targets;
        final double[] newDelegate = array(index);
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

    public DoubleCatheter whenFlock(final double source, final BiDoubleToDoubleFunction maker, DoubleConsumer consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public DoubleCatheter whenFlock(BiDoubleToDoubleFunction maker, DoubleConsumer consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public double flock(final double source, final BiDoubleToDoubleFunction maker) {
        final double[] ts = this.targets;
        double result = source;
        for (double d : ts) {
            result = maker.applyAsDouble(result, d);
        }
        return result;
    }

    public double flock(final BiDoubleToDoubleFunction maker) {
        final double[] ts = this.targets;
        final int length = ts.length;
        double result = length > 0 ? ts[0] : 0;
        for (int i = 1; i < length; i++) {
            result = maker.applyAsDouble(result, ts[i]);
        }
        return result;
    }

    public <X> X alternate(final X source, final BiFunction<X, Double, X> maker) {
        final double[] ts = this.targets;
        X result = source;
        for (double d : ts) {
            result = maker.apply(result, d);
        }
        return result;
    }

    public <X> DoubleCatheter whenAlternate(final X source, final BiFunction<X, Double, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public <X> DoubleCatheter whenAlternate(BiFunction<X, Double, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(null, maker));
        return this;
    }

    public boolean alternate(final boolean source, final BiDoublePredicate maker) {
        BooleanReceptacle result = new BooleanReceptacle(source);
        flock((older, newer) ->{
            result.and(maker.test(older, newer));
            return newer;
        });
        return result.get();
    }

    public DoubleCatheter whenAlternate(final boolean source, final BiDoublePredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public DoubleCatheter whenAlternate(BiDoublePredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(false, maker));
        return this;
    }

    public DoubleCatheter waiveTill(final int index) {
        if (isEmpty()) {
            return this;
        }

        final double[] ts = this.targets;
        final double[] newDelegate;
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

    public DoubleCatheter waiveTill(final DoublePredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final double[] ts = this.targets;
        final double[] newDelegate;
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

    public DoubleCatheter till(final DoublePredicate predicate) {
        final double[] ts = this.targets;
        for (double d : ts) {
            if (predicate.test(d)) {
                break;
            }
        }

        return this;
    }

    public int findTill(final DoublePredicate predicate) {
        final double[] ts = this.targets;
        int index = 0;
        for (double d : ts) {
            if (predicate.test(d)) {
                break;
            }
        }

        return index;
    }

    public DoubleCatheter replace(final DoubleUnaryOperator handler) {
        if (isEmpty()) {
            return this;
        }

        final double[] ts = this.targets;
        int index = 0;
        for (double d : ts) {
            ts[index++] = handler.applyAsDouble(d);
        }
        return this;
    }

    public DoubleCatheter vary(final DoubleUnaryOperator handler) {
        return replace(handler);
    }

    public IntCatheter vary(final DoubleToIntFunction handler) {
        if (isEmpty()) {
            return IntCatheter.make();
        }

        final double[] ts = this.targets;
        final int[] array = new int[ts.length];
        int index = 0;
        for (double d : ts) {
            array[index++] = handler.applyAsInt(d);
        }
        return IntCatheter.of(array);
    }

    public LongCatheter vary(final DoubleToLongFunction handler) {
        if (isEmpty()) {
            return LongCatheter.make();
        }

        final double[] ts = this.targets;
        final long[] array = new long[ts.length];
        int index = 0;
        for (double d : ts) {
            array[index++] = handler.applyAsLong(d);
        }
        return LongCatheter.of(array);
    }

    public BooleanCatheter vary(final DoublePredicate handler) {
        if (isEmpty()) {
            return BooleanCatheter.make();
        }

        final double[] ts = this.targets;
        final boolean[] array = new boolean[ts.length];
        int index = 0;
        for (double d : ts) {
            array[index++] = handler.test(d);
        }
        return BooleanCatheter.of(array);
    }

    public ByteCatheter vary(final DoubleToByteFunction handler) {
        if (isEmpty()) {
            return ByteCatheter.make();
        }

        final double[] ts = this.targets;
        final byte[] array = new byte[ts.length];
        int index = 0;
        for (double d : ts) {
            array[index++] = handler.applyAsByte(d);
        }
        return ByteCatheter.of(array);
    }

    public <X> Catheter<X> vary(final DoubleFunction<X> handler) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final double[] ts = this.targets;
        final X[] array = xArray(ts.length);
        int index = 0;
        for (double d : ts) {
            array[index++] = handler.apply(d);
        }
        return new Catheter<>(array);
    }

    public <X> Catheter<X> vary(final DoubleFunction<X> handler, IntFunction<X[]> arrayGenerator) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final double[] ts = this.targets;
        final X[] array = arrayGenerator.apply(ts.length);
        int index = 0;
        for (double d : ts) {
            array[index++] = handler.apply(d);
        }
        return Catheter.of(array).arrayGenerator(arrayGenerator);
    }

    public DoubleCatheter whenAny(final DoublePredicate predicate, final DoubleConsumer action) {
        final double[] ts = this.targets;
        for (double d : ts) {
            if (predicate.test(d)) {
                action.accept(d);
                break;
            }
        }
        return this;
    }

    public DoubleCatheter whenAll(final DoublePredicate predicate, final Runnable action) {
        final double[] ts = this.targets;
        for (double d : ts) {
            if (predicate.test(d)) {
                continue;
            }
            return this;
        }
        action.run();
        return this;
    }

    public DoubleCatheter whenAll(final DoublePredicate predicate, final DoubleConsumer action) {
        return whenAll(predicate, () -> each(action));
    }

    private DoubleCatheter whenNone(final DoublePredicate predicate, final Runnable action) {
        final double[] ts = this.targets;
        for (double d : ts) {
            if (predicate.test(d)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final DoublePredicate predicate) {
        final double[] ts = this.targets;
        for (double d : ts) {
            if (predicate.test(d)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final DoublePredicate predicate) {
        final double[] ts = this.targets;
        for (double d : ts) {
            if (predicate.test(d)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public boolean hasNone(final DoublePredicate predicate) {
        final double[] ts = this.targets;
        for (double d : ts) {
            if (predicate.test(d)) {
                return false;
            }
        }
        return true;
    }

    public double findFirst(final DoublePredicate predicate) {
        final double[] ts = this.targets;
        for (double d : ts) {
            if (predicate.test(d)) {
                return d;
            }
        }
        return 0.0D;
    }

    public double findLast(final DoublePredicate predicate) {
        final double[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final double t = ts[index--];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0.0D;
    }

    public <X> X whenFoundFirst(final DoublePredicate predicate, DoubleFunction<X> function) {
        final double[] ts = this.targets;
        for (double d : ts) {
            if (predicate.test(d)) {
                return function.apply(d);
            }
        }
        return null;
    }

    public <X> X whenFoundLast(final DoublePredicate predicate, DoubleFunction<X> function) {
        final double[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final double t = ts[index--];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public DoubleCatheter any(final DoubleConsumer consumer) {
        if (this.targets.length > 0) {
            double[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public DoubleCatheter first(final DoubleConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public DoubleCatheter tail(final DoubleConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public DoubleCatheter reverse() {
        if (isEmpty()) {
            return this;
        }

        final double[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        double temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public double max(final Comparator<Double> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public double min(final Comparator<Double> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public DoubleCatheter whenMax(final Comparator<Double> comparator, final DoubleConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public DoubleCatheter whenMin(final Comparator<Double> comparator, final DoubleConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    public int count() {
        return this.targets.length;
    }

    public DoubleCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public DoubleCatheter count(final IntegerReceptacle target) {
        target.set(count());
        return this;
    }

    public DoubleCatheter count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    @SafeVarargs
    public final DoubleCatheter append(final double... objects) {
        final double[] ts = this.targets;
        final double[] newDelegate = array(ts.length + objects.length);
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

    public DoubleCatheter append(final DoubleCatheter objects) {
        return append(objects.array());
    }

    public DoubleCatheter repeat(final int count) {
        final double[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public double fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, double item) {
        this.targets[index] = item;
    }

    public DoubleCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Double> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, DoubleCatheter input, final TriFunction<MatrixPos, Double, Double, X> action) {
        if (input.count() == count()) {
            final IntegerReceptacle index = new IntegerReceptacle(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final double inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public DoubleCatheter matrixMap(
            final int width,
            final int inputWidth,
            final DoubleCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Double, Double, Double> scanFlocked,
            final TriFunction<MatrixPos, Double, Double, Double> combineFlocked
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
        final DoubleCatheter newMatrix = DoubleCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final DoubleCatheter flockingCatheter = DoubleCatheter.makeCapacity(width);

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
                final double fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final double fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public <X> Catheter<X> matrixVary(final int width, double input, final TriFunction<MatrixPos, Double, Double, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public DoubleCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Double, Double> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Double, X> action) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final IntegerReceptacle w = new IntegerReceptacle(0);
        final IntegerReceptacle h = new IntegerReceptacle(0);

        final int matrixEdge = width - 1;

        return vary((double item) -> {
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

    public Catheter<DoubleCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<DoubleCatheter> results = Catheter.makeCapacity(sourceHeight);
        DoubleCatheter catheter = DoubleCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final double element = fetch(y * width + x);
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

    public DoubleCatheter removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        double[] newDelegate = array(count() - 1);
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

    public DoubleCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public DoubleCatheter shuffle(RandomGenerator random) {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public DoubleCatheter swapShuffle(RandomGenerator random) {
        double[] elements = this.targets;
        int i = elements.length;

        for (int j = i; j > 1; --j) {
            int swapTo = random.nextInt(j);
            int swapFrom = j - 1;
            double fromElement = elements[swapFrom];
            double toElement = elements[swapTo];
            elements[swapTo] = fromElement;
            elements[swapFrom] = toElement;
        }

        return this;
    }

    public boolean has(double target) {
        return hasAny(t -> t == target);
    }

    public boolean not(double target) {
        return !has(target);
    }

    public DoubleCatheter merge(DoubleCatheter other) {
        return append(other.filter(this::not));
    }

    public boolean isPresent() {
        return count() > 0;
    }

    public DoubleCatheter ifPresent(Consumer<DoubleCatheter> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public DoubleCatheter ifEmpty(Consumer<DoubleCatheter> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }

    public DoubleCatheter dump() {
        return new DoubleCatheter(array());
    }

    public DoubleCatheter flat(DoubleFunction<DoubleCatheter> function) {
        if (isEmpty()) {
            return this;
        }

        Catheter<DoubleCatheter> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (double element : this.targets) {
            DoubleCatheter flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        this.targets = array(totalSize);
        int pos = 0;
        for (DoubleCatheter flat : catheter.targets) {
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

    public <X> Catheter<X> flatTo(DoubleFunction<Catheter<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Catheter<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (double element : this.targets) {
            Catheter<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        return Catheter.flatting(catheter, totalSize);
    }

    public <X> Catheter<X> flatToByCollection(DoubleFunction<Collection<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Collection<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (double element : this.targets) {
            Collection<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.size();
        }

        return Catheter.flattingCollection(catheter, totalSize);
    }

    public DoubleCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public DoubleCatheter reset(double[] targets) {
        this.targets = targets;
        return this;
    }

    public double[] array() {
        return this.targets.clone();
    }

    public double[] dArray() {
        return this.targets;
    }

    public DoubleStream stream() {
        return DoubleStream.of(array());
    }

    public List<Double> list() {
        List<Double> list = new ArrayList<>();
        for (double l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Double> set() {
        Set<Double> set = new HashSet<>();
        for (double l : array()) {
            set.add(l);
        }
        return set;
    }

    public static void main(String[] args) {
        DoubleCatheter source = DoubleCatheter.make(
                3, 3, 3,
                4, 1, 1,
                5, 9, 9
        );
        DoubleCatheter input = DoubleCatheter.make(
                1, 0, 0,
                0, 1, 0,
                0, 0, 1
        );

        source.dump()
                .matrixHomoVary(3, input, (pos, sourceX, inputX) -> {
                    return sourceX - inputX;
                })
                .matrixEach(3, (pos, item) -> {
                    System.out.println(pos);
                    System.out.println(item);
                });

        System.out.println("------");

        source.matrixMap(3, 3, input, (flockPos, sourcePos, inputPos, sourceX, inputX) -> {
                    return sourceX * inputX;
                }, (destPos, combine1, combine2) -> {
                    return combine1 + combine2;
                })
                .matrixEach(3, (pos, item) -> {
                    System.out.println(pos);
                    System.out.println(item);
                });


    }

    private static double[] array(int size) {
        return new double[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
