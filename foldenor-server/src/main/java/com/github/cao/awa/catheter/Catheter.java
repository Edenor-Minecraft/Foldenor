package com.github.cao.awa.catheter;

import com.github.cao.awa.catheter.action.*;
import com.github.cao.awa.catheter.matrix.MatrixFlockPos;
import com.github.cao.awa.catheter.matrix.MatrixPos;
import com.github.cao.awa.catheter.pair.IntegerAndExtraPair;
import com.github.cao.awa.catheter.receptacle.BooleanReceptacle;
import com.github.cao.awa.catheter.receptacle.Receptacle;
import com.github.cao.awa.catheter.receptacle.IntegerReceptacle;
import com.github.cao.awa.sinuatum.function.consumer.TriConsumer;
import com.github.cao.awa.sinuatum.function.QuinFunction;
import com.github.cao.awa.sinuatum.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Catheter<T> {
    private static final Random RANDOM = new Random();
    protected T[] targets;
    protected IntFunction<T[]> arrayGenerator;

    public Catheter(T[] targets) {
        this.targets = targets;
    }

    @SafeVarargs
    public static <X> Catheter<X> make(X... targets) {
        return new Catheter<>(targets);
    }

    public static <X> Catheter<X> makeCapacity(int size) {
        return new Catheter<>(xArray(size));
    }

    public static <X> Catheter<X> makeCapacity(int size, IntFunction<X[]> arrayGenerator) {
        return new Catheter<>(xArray(arrayGenerator, size)).arrayGenerator(arrayGenerator);
    }

    public static <X> Catheter<X> of(X[] targets) {
        return new Catheter<>(targets);
    }

    public Catheter<T> arrayGenerator(IntFunction<T[]> arrayGenerator) {
        this.arrayGenerator = arrayGenerator;
        return this;
    }

    public Catheter<T> flat(Function<T, Catheter<T>> function) {
        if (isEmpty()) {
            return this;
        }

        Catheter<Catheter<T>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (T element : this.targets) {
            Catheter<T> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        this.targets = array(totalSize);
        int pos = 0;
        for (Catheter<T> flat : catheter.targets) {
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

    public <X> Catheter<X> flatTo(Function<T, Catheter<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Catheter<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (T element : this.targets) {
            Catheter<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        return flatting(catheter, totalSize);
    }

    public <X> Catheter<X> arrayFlatTo(Function<T, X[]> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<X[]> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (T element : this.targets) {
            X[] flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.length;
        }

        return flattingArray(catheter, totalSize);
    }

    public <X> Catheter<X> collectionFlatTo(Function<T, Collection<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Collection<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (T element : this.targets) {
            Collection<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.size();
        }

        return flattingCollection(catheter, totalSize);
    }

    @SuppressWarnings("unchecked")
    public static <X> Catheter<X> flatting(Catheter<Catheter<X>> catheter, int totalSize) {
        Catheter<X> result = Catheter.makeCapacity(totalSize);

        if (totalSize == 0) {
            return result;
        }

        int pos = 0;
        Object[] catheters = catheter.targets;
        int i = 0;
        while (i != catheters.length) {
            Catheter<X> c = (Catheter<X>) catheters[i];
            for (X target : c.targets) {
                result.targets[pos++] = target;
            }

            i++;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <X> Catheter<X> flattingCollection(Catheter<Collection<X>> catheter, int totalSize) {
        Catheter<X> result = Catheter.makeCapacity(totalSize);

        if (totalSize == 0) {
            return result;
        }

        int pos = 0;
        Object[] catheters = catheter.targets;
        int i = 0;
        while (i != catheters.length) {
            Collection<X> c = (Collection<X>) catheters[i];
            for (X element : c) {
                result.targets[pos++] = element;
            }

            i++;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <X> Catheter<X> flattingArray(Catheter<X[]> catheter, int totalSize) {
        Catheter<X> result = Catheter.makeCapacity(totalSize);

        if (totalSize == 0) {
            return result;
        }

        int pos = 0;
        Object[] catheters = catheter.targets;
        int i = 0;
        while (i != catheters.length) {
            X[] c = (X[]) catheters[i];
            for (X element : c) {
                result.targets[pos++] = element;
            }

            i++;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <X> Catheter<X> of(Collection<X> targets) {
        if (targets == null) {
            return new Catheter<>(xArray(0));
        }
        return new Catheter<>((X[]) targets.toArray(Object[]::new));
    }

    @SuppressWarnings("unchecked")
    public static <X> Catheter<X> of(Collection<X> targets, IntFunction<X[]> arrayGenerator) {
        if (targets == null) {
            return new Catheter<>(xArray(arrayGenerator, 0)).arrayGenerator(arrayGenerator);
        }
        return new Catheter<>(targets.toArray(arrayGenerator)).arrayGenerator(arrayGenerator);
    }

    public Catheter<T> each(final Consumer<T> action) {
        final T[] ts = this.targets;
        for (T t : ts) {
            action.accept(t);
        }
        return this;
    }

    public Catheter<T> each(final Consumer<T> action, Runnable poster) {
        each(action);
        poster.run();
        return this;
    }

    public <X> Catheter<T> each(X initializer, final BiConsumer<X, T> action) {
        final T[] ts = this.targets;
        for (T t : ts) {
            action.accept(initializer, t);
        }
        return this;
    }

    public <X> Catheter<T> each(X initializer, final BiConsumer<X, T> action, Consumer<X> poster) {
        each(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public <X> Catheter<T> overall(X initializer, final TriConsumer<X, Integer, T> action) {
        final T[] ts = this.targets;
        int index = 0;
        for (T t : ts) {
            action.accept(initializer, index++, t);
        }
        return this;
    }

    public <X> Catheter<T> overall(X initializer, final TriConsumer<X, Integer, T> action, Consumer<X> poster) {
        overall(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public Catheter<T> overall(final IntegerAndExtraConsumer<T> action) {
        final T[] ts = this.targets;
        int index = 0;
        for (T t : ts) {
            action.accept(index++, t);
        }
        return this;
    }

    public Catheter<T> overall(final IntegerAndExtraConsumer<T> action, Runnable poster) {
        overall(action);
        poster.run();
        return this;
    }

    public Catheter<T> insert(final IntegerAndBiExtraToExtraFunction<T> maker) {
        final Map<Integer, IntegerAndExtraPair<T>> indexes = new HashMap<>();
        final Receptacle<T> lastItem = new Receptacle<>(null);
        overall((index, item) -> {
            T result = maker.apply(index, item, lastItem.get());
            if (result != null) {
                indexes.put(index + indexes.size(), new IntegerAndExtraPair<>(index, result));
            }
            lastItem.set(item);
        });

        final T[] ts = this.targets;
        final T[] newDelegate = array(ts.length + indexes.size());
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
                    final IntegerAndExtraPair<T> item = indexes.get(index);
                    newDelegate[index] = item.xValue();
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

    public Catheter<T> pluck(final IntegerAndBiExtraPredicate<? super T> maker) {
        if (isEmpty()) {
            return this;
        }

        final Receptacle<T> lastItem = new Receptacle<>(null);
        return overallFilter((index, item) -> {
            if (maker.test(index, item, lastItem.get())) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public <X> Catheter<T> discardTo(final Predicate<X> predicate, Function<? super T, X> converter) {
        Catheter<T> result = Catheter.make();

        overallFilter((index, item) -> !predicate.test(converter.apply(item)), result::reset);

        return result;
    }

    public Catheter<T> discardTo(final Predicate<? super T> predicate) {
        Catheter<T> result = Catheter.make();

        overallFilter((index, item) -> !predicate.test(item), result::reset);

        return result;
    }

    /**
     * Discarding items that matched given predicate.
     *
     * @param initializer A constant to passed to next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> discardTo(final X initializer, final BiPredicate<? super T, X> predicate) {
        Catheter<T> result = Catheter.make();

        overallFilter((index, item) -> !predicate.test(item, initializer), result::reset);

        return result;
    }

    /**
     * Discarding items that matched given predicate.
     *
     * @param succeed   Direct done discard? When succeed true, cancel filter instantly
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public Catheter<T> orDiscardTo(final boolean succeed, final Predicate<? super T> predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate);
    }

    /**
     * Discarding items that matched given predicate.
     *
     * @param succeed   Direct done discard? When succeed true, cancel filter instantly
     * @param predicate The filter predicate
     * @param converter The converter that make T vary to X
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public <X> Catheter<T> orDiscardTo(final boolean succeed, final Predicate<X> predicate, Function<? super T, X> converter) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate, converter);
    }

    /**
     * Discarding items that matched given predicate.
     *
     * @param succeed     Direct done discard? When succeed true, cancel filter instantly
     * @param initializer A constant to passed to next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> orDiscardTo(final boolean succeed, final X initializer, final BiPredicate<? super T, X> predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(initializer, predicate);
    }

    public <X> Catheter<T> discard(final Predicate<X> predicate, Function<? super T, X> converter) {
        return overallFilter((index, item) -> !predicate.test(converter.apply(item)));
    }

    public Catheter<T> discard(final Predicate<? super T> predicate) {
        return overallFilter((index, item) -> !predicate.test(item));
    }

    /**
     * Discarding items that matched given predicate.
     *
     * @param initializer A constant to passed to next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> discard(final X initializer, final BiPredicate<? super T, X> predicate) {
        return overallFilter((index, item) -> !predicate.test(item, initializer));
    }

    /**
     * Discarding items that matched given predicate.
     *
     * @param succeed   Direct done discard? When succeed true, cancel filter instantly
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public Catheter<T> orDiscard(final boolean succeed, final Predicate<? super T> predicate) {
        if (succeed) {
            return this;
        }
        return discard(predicate);
    }

    /**
     * Discarding items that matched given predicate.
     *
     * @param succeed   Direct done discard? When succeed true, cancel filter instantly
     * @param predicate The filter predicate
     * @param converter The converter that make T vary to X
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public <X> Catheter<T> orDiscard(final boolean succeed, final Predicate<X> predicate, Function<? super T, X> converter) {
        if (succeed) {
            return this;
        }
        return discard(predicate, converter);
    }

    /**
     * Discarding items that matched given predicate.
     *
     * @param succeed     Direct done discard? When succeed true, cancel filter instantly
     * @param initializer A constant to passed to next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> orDiscard(final boolean succeed, final X initializer, final BiPredicate<? super T, X> predicate) {
        if (succeed) {
            return this;
        }
        return discard(initializer, predicate);
    }

    public <X> Catheter<T> filterTo(final Predicate<X> predicate, Function<? super T, X> converter) {
        return dump().filter(predicate, converter);
    }

    public Catheter<T> filterTo(final Predicate<? super T> predicate) {
        return dump().filter(predicate);
    }

    public <X> Catheter<T> filterTo(final X initializer, final BiPredicate<? super T, X> predicate) {
        return dump().filter(initializer, predicate);
    }

    public <X> Catheter<T> filter(final Predicate<X> predicate, Function<? super T, X> converter) {
        return overallFilter((index, item) -> predicate.test(converter.apply(item)));
    }

    public Catheter<T> filter(final Predicate<? super T> predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param initializer A constant to passed to next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> filter(final X initializer, final BiPredicate<? super T, X> predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public Catheter<T> overallFilter(final IntegerAndExtraPredicate<? super T> predicate) {
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
    public Catheter<T> overallFilter(final IntegerAndExtraPredicate<? super T> predicate, final Consumer<T[]> discarding) {
        if (isEmpty()) {
            return this;
        }

        // 创建需要的变量和常量
        final T[] ts = this.targets;
        final boolean[] deleting = new boolean[ts.length];
        int newDelegateSize = ts.length;
        int index = 0;

        // 遍历所有元素
        for (T target : ts) {
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
        final T[] newDelegate = array(newDelegateSize);
        final T[] discardingDelegate = array(ts.length - newDelegateSize);
        int newDelegateIndex = 0;
        int discardingDelegateIndex = 0;

        // 遍历所有元素
        index = 0;
        for (boolean isDeleting : deleting) {
            // deleting为true则为被筛选掉的，放入discarding
            T t = ts[index++];
            if (isDeleting) {
                discardingDelegate[discardingDelegateIndex++] = t;
            } else {
                // 不为null则加入新数组
                newDelegate[newDelegateIndex++] = t;
            }
        }

        discarding.accept(discardingDelegate);

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    public <X, R> Catheter<R> filteringVary(final Predicate<X> predicate, final Function<X, R> handler, Function<? super T, X> converter) {
        return overallVaryFilter((index, item) -> predicate.test(item), handler, converter);
    }

    public <X> Catheter<X> filteringVary(final Predicate<? super T> predicate, final Function<T, X> handler) {
        return overallVaryFilter((index, item) -> predicate.test(item), handler);
    }

    public <I, X, R> Catheter<R> filteringVary(final I initializer, final BiPredicate<X, I> predicate, Function<X, R> handler) {
        return overallVaryFilter((index, item) -> predicate.test(item, initializer), handler, null);
    }

    public <X> Catheter<X> overallVaryFilter(final IntegerAndExtraPredicate<T> predicate, final Function<T, X> handler) {
        return overallVaryFilter(predicate, handler, null);
    }

    @SuppressWarnings("unchecked")
    public <X, Y> Catheter<X> overallVaryFilter(final IntegerAndExtraPredicate<Y> predicate, final Function<Y, X> handler, Function<? super T, Y> converter) {
        if (isEmpty()) {
            return Catheter.make();
        }

        // 创建需要的变量和常量
        final T[] ts = this.targets;
        final Y[] converted = converter == null ? (Y[]) this.targets : xArray(count());
        if (converter == null) {
            converter = x -> (Y) x;
        }
        int newDelegateSize = ts.length;
        int index = 0;

        // 遍历所有元素
        for (T target : ts) {
            // 符合条件的保留
            Y convertedTarget = converter.apply(target);
            if (predicate.test(index, convertedTarget)) {
                converted[index] = convertedTarget;
                index++;
                continue;
            }

            // 不符合条件的设为null，后面会去掉
            // 并且将新数组的容量减一
            ts[index++] = null;
            newDelegateSize--;
        }

        // 创建新数组
        final X[] newDelegate = xArray(newDelegateSize);
        int newDelegateIndex = 0;

        // 遍历所有元素
        for (Y t : converted) {
            // 为null则为被筛选掉的，忽略
            if (t == null) {
                continue;
            }

            // 不为null则加入新数组
            newDelegate[newDelegateIndex++] = handler.apply(t);
        }

        return Catheter.make(newDelegate);
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param initializer A constant to passed to the next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> overallFilter(final X initializer, final IntegerAndBiDiffExtraPredicate<? super T, X> predicate) {
        return overallFilter((index, item) -> predicate.test(index, item, initializer));
    }

    public Catheter<T> orFilterTo(final boolean succeed, final Predicate<? super T> predicate) {
        return dump().orFilter(succeed, predicate);
    }

    public <X> Catheter<T> orFilterTo(final boolean succeed, final Predicate<X> predicate, Function<? super T, X> converter) {
        return dump().orFilter(succeed, predicate, converter);
    }

    public <X> Catheter<T> orFilterTo(final boolean succeed, final X initializer, final BiPredicate<? super T, X> predicate) {
        return dump().orFilter(succeed, initializer, predicate);
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param succeed   Direct done filter? When succeed true, cancel filter instantly
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public Catheter<T> orFilter(final boolean succeed, final Predicate<? super T> predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param succeed   Direct done filter? When succeed true, cancel filter instantly
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public <X> Catheter<T> orFilter(final boolean succeed, final Predicate<X> predicate, Function<? super T, X> converter) {
        if (succeed) {
            return this;
        }
        return filter(predicate, converter);
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param succeed     Direct done filter? When succeed true, cancel filter instantly
     * @param initializer A constant to passed to the next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> orFilter(final boolean succeed, final X initializer, final BiPredicate<? super T, X> predicate) {
        if (succeed) {
            return this;
        }
        return filter(initializer, predicate);
    }

    public Catheter<T> distinct() {
        if (isEmpty()) {
            return this;
        }

        final Map<T, Boolean> map = new HashMap<>();
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

    public Catheter<T> sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public Catheter<T> sort(Comparator<T> comparator) {
        Arrays.sort(this.targets, comparator);
        return this;
    }

    public Catheter<T> holdTill(int index) {
        if (isEmpty()) {
            return this;
        }

        index = Math.min(index, this.targets.length);

        final T[] ts = this.targets;
        final T[] newDelegate = array(index);
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

    public Catheter<T> holdTill(final Predicate<? super T> predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final T[] ts = this.targets;
        final T[] newDelegate = array(index);
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

    public Catheter<T> whenFlock(final T source, final BiFunction<T, T, T> maker, Consumer<T> consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public Catheter<T> whenFlock(BiFunction<T, T, T> maker, Consumer<T> consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public T flock(final T source, final BiFunction<T, T, T> maker) {
        final T[] ts = this.targets;
        T result = source;
        for (T t : ts) {
            result = maker.apply(result, t);
        }
        return result;
    }

    public <X> X alternate(final X source, final BiFunction<X, T, X> maker) {
        final T[] ts = this.targets;
        X result = source;
        for (T t : ts) {
            result = maker.apply(result, t);
        }
        return result;
    }

    public <X> Catheter<T> whenAlternate(final X source, final BiFunction<X, T, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public <X> Catheter<T> whenAlternate(BiFunction<X, T, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(null, maker));
        return this;
    }

    public boolean alternate(final boolean source, final BiPredicate<T, T> maker) {
        BooleanReceptacle result = new BooleanReceptacle(source);
        flock((older, newer) ->{
            if (older != null) {
                result.and(maker.test(older, newer));
            }
            return newer;
        });
        return result.get();
    }

    public Catheter<T> whenAlternate(final boolean source, final BiPredicate<T, T> maker, BooleanConsumer consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public Catheter<T> whenAlternate(BiPredicate<T, T> maker, BooleanConsumer consumer) {
        consumer.accept(alternate(false, maker));
        return this;
    }

    @Nullable
    public T flock(final BiFunction<T, T, T> maker) {
        final T[] ts = this.targets;
        final int length = ts.length;
        T result = length > 0 ? ts[0] : null;
        if (result != null) {
            for (int i = 1; i < length; i++) {
                result = maker.apply(result, ts[i]);
            }
        }
        return result;
    }

    public Catheter<T> waiveTill(final int index) {
        if (isEmpty()) {
            return this;
        }

        final T[] ts = this.targets;
        final T[] newDelegate;
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

    public Catheter<T> waiveTill(final Predicate<? super T> predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final T[] ts = this.targets;
        final T[] newDelegate;
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

    public Catheter<T> till(final Predicate<? super T> predicate) {
        final T[] ts = this.targets;
        for (T t : ts) {
            if (predicate.test(t)) {
                break;
            }
        }

        return this;
    }

    public int findTill(final Predicate<? super T> predicate) {
        final T[] ts = this.targets;
        int index = 0;
        for (T t : ts) {
            if (predicate.test(t)) {
                break;
            }
            index++;
        }

        return index;
    }

    public Catheter<T> replace(final Function<T, T> handler) {
        if (isEmpty()) {
            return this;
        }

        final T[] ts = this.targets;
        int index = 0;
        for (T t : ts) {
            ts[index++] = handler.apply(t);
        }
        return this;
    }

    public IntCatheter varyInt(final ToIntFunction<T> handler) {
        if (isEmpty()) {
            return IntCatheter.make();
        }

        final T[] ts = this.targets;
        final int[] array = new int[ts.length];
        int index = 0;
        for (T t : ts) {
            array[index++] = handler.applyAsInt(t);
        }
        return IntCatheter.of(array);
    }

    public LongCatheter varyLong(final ToLongFunction<T> handler) {
        if (isEmpty()) {
            return LongCatheter.make();
        }

        final T[] ts = this.targets;
        final long[] array = new long[ts.length];
        int index = 0;
        for (T t : ts) {
            array[index++] = handler.applyAsLong(t);
        }
        return LongCatheter.of(array);
    }

    public DoubleCatheter varyDouble(final ToDoubleFunction<T> handler) {
        if (isEmpty()) {
            return DoubleCatheter.make();
        }

        final T[] ts = this.targets;
        final double[] array = new double[ts.length];
        int index = 0;
        for (T t : ts) {
            array[index++] = handler.applyAsDouble(t);
        }
        return DoubleCatheter.of(array);
    }

    public ByteCatheter varyByte(final ToByteFunction<T> handler) {
        if (isEmpty()) {
            return ByteCatheter.make();
        }

        final T[] ts = this.targets;
        final byte[] array = new byte[ts.length];
        int index = 0;
        for (T t : ts) {
            array[index++] = handler.applyAsByte(t);
        }
        return ByteCatheter.of(array);
    }

    public <X> Catheter<X> vary(final Function<T, X> handler, IntFunction<X[]> arrayGenerator) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final T[] ts = this.targets;
        final X[] array = arrayGenerator.apply(ts.length);
        int index = 0;
        for (T t : ts) {
            array[index++] = handler.apply(t);
        }
        return Catheter.of(array).arrayGenerator(arrayGenerator);
    }


    public BooleanCatheter varyBoolean(final Predicate<? super T> handler) {
        if (isEmpty()) {
            return BooleanCatheter.make();
        }

        final T[] ts = this.targets;
        final boolean[] array = new boolean[ts.length];
        int index = 0;
        for (T t : ts) {
            array[index++] = handler.test(t);
        }
        return BooleanCatheter.of(array);
    }

    public <X> Catheter<X> varyTo(final Function<T, X> handler) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final T[] ts = this.targets;
        final X[] array = xArray(ts.length);
        int index = 0;
        for (T t : ts) {
            array[index++] = handler.apply(t);
        }
        return Catheter.of(array);
    }

    public Catheter<T> whenAny(final Predicate<? super T> predicate, final Consumer<T> action) {
        final T[] ts = this.targets;
        for (T t : ts) {
            if (predicate.test(t)) {
                action.accept(t);
                break;
            }
        }
        return this;
    }

    public Catheter<T> whenAll(final Predicate<? super T> predicate, final Runnable action) {
        final T[] ts = this.targets;
        for (T t : ts) {
            if (predicate.test(t)) {
                continue;
            }
            return this;
        }
        action.run();
        return this;
    }

    public Catheter<T> whenAll(final Predicate<? super T> predicate, final Consumer<T> action) {
        return whenAll(predicate, () -> each(action));
    }

    private Catheter<T> whenNone(final Predicate<? super T> predicate, final Runnable action) {
        final T[] ts = this.targets;
        for (T t : ts) {
            if (predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final Predicate<? super T> predicate) {
        final T[] ts = this.targets;
        for (T t : ts) {
            if (predicate.test(t)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final Predicate<? super T> predicate) {
        final T[] ts = this.targets;
        for (T t : ts) {
            if (predicate.test(t)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public boolean hasNone(final Predicate<? super T> predicate) {
        final T[] ts = this.targets;
        for (T t : ts) {
            if (predicate.test(t)) {
                return false;
            }
        }
        return true;
    }

    public T findFirst(final Predicate<? super T> predicate) {
        final T[] ts = this.targets;
        for (T t : ts) {
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }

    public T findLast(final Predicate<? super T> predicate) {
        final T[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final T t = ts[index--];
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }

    public <X> X whenFoundFirst(final Predicate<? super T> predicate, Function<T, X> function) {
        final T[] ts = this.targets;
        for (T t : ts) {
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public <X> X whenFoundLast(final Predicate<? super T> predicate, Function<T, X> function) {
        final T[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final T t = ts[index--];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public Catheter<T> any(final Consumer<T> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(select(this.targets, RANDOM));
        }
        return this;
    }

    public Optional<T> optionalFirst() {
        if (this.targets.length > 0) {
            return Optional.ofNullable(this.targets[0]);
        }
        return Optional.empty();
    }

    public Catheter<T> firstOrNull(final Consumer<T> consumer) {
        consumer.accept(this.targets.length > 0 ? this.targets[0] : null);
        return this;
    }

    public Catheter<T> first(final Consumer<T> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public Catheter<T> tailOrNull(final Consumer<T> consumer) {
        consumer.accept(this.targets.length > 0 ? this.targets[this.targets.length - 1] : null);
        return this;
    }

    public Catheter<T> tail(final Consumer<T> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public Optional<T> optionalTail() {
        if (this.targets.length > 0) {
            return Optional.ofNullable(this.targets[this.targets.length - 1]);
        }
        return Optional.empty();
    }

    public Catheter<T> reverse() {
        if (isEmpty()) {
            return this;
        }

        final T[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        T temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public T max(final Comparator<T> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public T min(final Comparator<T> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public Optional<T> selectMax(final Comparator<T> comparator) {
        return Optional.ofNullable(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
    }

    public Optional<T> selectMin(final Comparator<T> comparator) {
        return Optional.ofNullable(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
    }

    public Catheter<T> whenMax(final Comparator<T> comparator, final Consumer<T> action) {
        final T t = flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
        if (t != null) {
            action.accept(t);
        }
        return this;
    }

    public Catheter<T> whenMin(final Comparator<T> comparator, final Consumer<T> action) {
        final T t = flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
        if (t != null) {
            action.accept(t);
        }
        return this;
    }

    public Catheter<T> exists() {
        return filter(Objects::nonNull);
    }

    public int count() {
        return this.targets.length;
    }

    public Catheter<T> count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public Catheter<T> count(final Receptacle<Integer> target) {
        target.set(count());
        return this;
    }

    public Catheter<T> count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    @SafeVarargs
    public final Catheter<T> append(final T... objects) {
        final T[] ts = this.targets;
        final T[] newDelegate = array(ts.length + objects.length);
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

    public Catheter<T> append(final Catheter<T> objects) {
        return append(objects.array());
    }

    public Catheter<T> remove(T target) {
        if (isEmpty()) {
            return this;
        }

        if (target == null) {
            return exists();
        }

        if (count() == 1 && target.equals(fetch(0))) {
            this.targets = array(0);
            return this;
        }

        int i = 0;
        int edge = this.targets.length - 1;
        boolean found = false;

        while (i != edge) {
            if (target.equals(fetch(i))) {
                found = true;
                break;
            }
            i++;
        }

        if (!found) {
            return this;
        }

        return removeWithIndex(i);
    }

    public Catheter<T> removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        int edge = count() - 1;
        T[] newDelegate = array(edge);
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
                edge - index
        );

        this.targets = newDelegate;

        return this;
    }

    public boolean isPresent() {
        return count() > 0;
    }

    public Catheter<T> ifPresent(Consumer<Catheter<T>> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public Catheter<T> ifEmpty(Consumer<Catheter<T>> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }

    public Catheter<T> repeat(final int count) {
        final T[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public T fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, T item) {
        this.targets[index] = item;
    }

    public Catheter<T> matrixEach(final int width, final BiConsumer<MatrixPos, T> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X, Y> Catheter<Y> matrixHomoVary(final int width, Catheter<X> input, final TriFunction<MatrixPos, T, X, Y> action) {
        if (input.count() == count()) {
            final IntegerReceptacle index = new IntegerReceptacle(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final X inputX = input.fetch(indexValue);
                final Y result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public <X, Y> Catheter<Y> matrixMap(
            final int width,
            final int inputWidth,
            final Catheter<X> input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, T, X, Y> scanFlocked,
            final TriFunction<MatrixPos, Y, Y, Y> combineFlocked
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
        final Catheter<T> newMatrix = Catheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final Catheter<Y> flockingCatheter = Catheter.makeCapacity(width);

        return newMatrix.matrixVary(inputWidth, (pos, ignored) -> {
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
                final T fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final X fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public Catheter<T> matrixTranspose(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int height = count() / width;

        final T[] newDelegate = array(this.targets.length);
        int newDelegateIndex = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                newDelegate[newDelegateIndex++] = fetch(y * width + x);
            }
        }

        this.targets = newDelegate;

        return this;
    }

    public <X, Y> Catheter<Y> matrixVary(final int width, X input, final TriFunction<MatrixPos, T, X, Y> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public Catheter<T> matrixReplace(final int width, final BiFunction<MatrixPos, T, T> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, T, X> action) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final IntegerReceptacle w = new IntegerReceptacle(0);
        final IntegerReceptacle h = new IntegerReceptacle(0);

        final int matrixEdge = width - 1;

        return varyTo((T item) -> {
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

    public Catheter<Catheter<T>> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<Catheter<T>> results = Catheter.makeCapacity(sourceHeight);
        Catheter<T> catheter = Catheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final T element = fetch(y * width + x);
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

    public Catheter<T> shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public Catheter<T> shuffle(RandomGenerator random) {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public Catheter<T> swapShuffle(RandomGenerator random) {
        T[] elements = this.targets;
        int i = elements.length;

        for (int j = i; j > 1; --j) {
            int swapTo = random.nextInt(j);
            int swapFrom = j - 1;
            T fromElement = elements[swapFrom];
            T toElement = elements[swapTo];
            elements[swapTo] = fromElement;
            elements[swapFrom] = toElement;
        }

        return this;
    }

    public static  <R> Catheter<R> combineSet(Catheter<Set<R>> catheter) {
        AtomicInteger size = new AtomicInteger();
        catheter.each(set -> size.addAndGet(set.size()));

        Catheter<R> result = Catheter.makeCapacity(size.get());

        AtomicInteger index = new AtomicInteger();
        catheter.each(set -> {
            for (R r : set) {
                result.fetch(index.getAndAdd(1), r);
            }
        });

        return result;
    }

    public static  <R> Catheter<R> combineList(Catheter<List<R>> catheter) {
        AtomicInteger size = new AtomicInteger();
        catheter.each(set -> size.addAndGet(set.size()));

        Catheter<R> result = Catheter.makeCapacity(size.get());

        AtomicInteger index = new AtomicInteger();
        catheter.each(set -> {
            for (R r : set) {
                result.fetch(index.getAndAdd(1), r);
            }
        });

        return result;
    }

    public static  <R> Catheter<R> combineCollection(Catheter<Collection<R>> catheter) {
        AtomicInteger size = new AtomicInteger();
        catheter.each(set -> size.addAndGet(set.size()));

        Catheter<R> result = Catheter.makeCapacity(size.get());

        AtomicInteger index = new AtomicInteger();
        catheter.each(set -> {
            for (R r : set) {
                result.fetch(index.getAndAdd(1), r);
            }
        });

        return result;
    }

    public <R> R varyMap(R identity, BiConsumer<R, T> mapper) {
        each(value -> mapper.accept(identity, value));
        return identity;
    }

    public boolean has(T target) {
        return hasAny(t -> Objects.equals(t, target));
    }

    public boolean not(T target) {
        return !has(target);
    }

    public Catheter<T> merge(Catheter<T> other) {
        return append(other.filter(this::not));
    }

    public Catheter<T> dump() {
        return new Catheter<>(array());
    }

    public Catheter<T> reset() {
        this.targets = array(0);
        return this;
    }

    public Catheter<T> reset(T[] targets) {
        this.targets = targets;
        return this;
    }

    public T[] safeArray() {
        T[] array = array(count());
        int index = 0;
        for (T target : this.targets) {
            array[index++] = target;
        }
        return array;
    }

    public T[] array() {
        return this.targets.clone();
    }

    public T[] dArray() {
        return this.targets;
    }

    public Stream<T> stream() {
        return Arrays.stream(safeArray());
    }

    public List<T> list() {
        return Arrays.asList(array());
    }

    public Set<T> set() {
        return new HashSet<>(list());
    }

    public static void main(String[] args) {
        test4();
    }

    public static void test4() {
        Map<String, String> identity = new HashMap<>();

        Catheter<Integer> catheter = Catheter.make(1,2,3,4,5,6,7,8,9,10);

        catheter.varyMap(identity, (i,value) -> i.put(String.valueOf(value), String.valueOf(value)));

        System.out.println(identity);
    }

    public static void test3() {
        Catheter<Set<String>> catheter = Catheter.makeCapacity(5);

        Set<String> set1 = new HashSet<>();
        IntStream.of(1,2,3,4,5,6).mapToObj(String::valueOf).forEach(set1::add);

        Set<String> set2 = new HashSet<>();
        IntStream.of(11,22,33,44,55,66).mapToObj(String::valueOf).forEach(set2::add);

        Set<String> set3 = new HashSet<>();
        IntStream.of(111,222,333,444,555,666).mapToObj(String::valueOf).forEach(set3::add);

        Set<String> set4 = new HashSet<>();
        IntStream.of(1111,2222,3333,4444,5555,6666).mapToObj(String::valueOf).forEach(set4::add);

        Set<String> set5 = new HashSet<>();
        IntStream.of(11111,22222,33333,44444,55555,66666).mapToObj(String::valueOf).forEach(set5::add);

        catheter.fetch(0, set1);
        catheter.fetch(1, set2);
        catheter.fetch(2, set3);
        catheter.fetch(3, set4);
        catheter.fetch(4, set5);

        Catheter<String> combinedCather = Catheter.combineSet(catheter);

        combinedCather.each(System.out::println);
    }

    public static void test2() {
        Catheter<Long> strings = Catheter.make(
                1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L
        );

        strings.filteringVary((number) -> number > 3, String::valueOf)
                .each(str -> {
                    System.out.println(str + "!");
                });
    }

    public static void test() {
        System.out.println("####");
        long[] catheterLongs = RANDOM.longs(16384 * 256).toArray();
        long[] streamLongs = catheterLongs.clone();

        System.out.println("-- Catheter");

        LongCatheter strings = LongCatheter.make(
                catheterLongs
        );

        long start = System.currentTimeMillis();

        LongCatheter c1 = strings
                .arrayFlat(l -> {
                    long[] sp = new long[8];
//            for (int i = 0; i < sp.length; i++) {
//                sp[i] = (int) (l / (i + 1));
//            }
                    Arrays.fill(sp, 1);
                    return sp;
//            return Catheter.of(sp);
                })
                .replace(i -> (long) Math.sqrt(i * i * i));

        System.out.println("Flat done in " + (System.currentTimeMillis() - start) + "ms");

//        c1.each(chars -> {
//            int x = chars * chars;
//        });

        c1.each(i -> {

        });

        System.out.println("Done in " + (System.currentTimeMillis() - start) + "ms");

        System.out.println("-- Stream");

        LongStream s = Arrays.stream(streamLongs);

        start = System.currentTimeMillis();

        LongStream s1 = s
                .flatMap(l -> {
                    long[] sp = new long[8];
//            for (int i = 0; i < sp.length; i++) {
//                sp[i] = (int) (l / (i + 1));
//            }
                    Arrays.fill(sp, 1);
                    return Arrays.stream(sp);
                })
                .map(i -> (long) Math.sqrt(i * i * i));

        System.out.println("Flat done in " + (System.currentTimeMillis() - start) + "ms");

//        s1.forEach(chars -> {
//            int x = chars * chars;
//        });

        s1.forEach(i -> {

        });

        System.out.println("Done in " + (System.currentTimeMillis() - start) + "ms");
    }

    @SuppressWarnings("unchecked")
    private T[] array(int size) {
        if (this.arrayGenerator != null) {
            return this.arrayGenerator.apply(size);
        }
        return (T[]) new Object[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }

    private static <X> X[] xArray(IntFunction<X[]> generator, int size) {
        return generator.apply(size);
    }

    public static <T> T select(T[] array, int index) {
        return array.length > index ? array[index] : array[array.length - 1];
    }

    public static <T> T select(T[] array, Random random) {
        return array[random.nextInt(array.length)];
    }
}
