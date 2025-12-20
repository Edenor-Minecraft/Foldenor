package com.github.cao.awa.catheter;

import com.github.cao.awa.catheter.action.*;
import com.github.cao.awa.catheter.matrix.MatrixFlockPos;
import com.github.cao.awa.catheter.matrix.MatrixPos;
import com.github.cao.awa.catheter.pair.IntegerAndBytePair;
import com.github.cao.awa.catheter.receptacle.BooleanReceptacle;
import com.github.cao.awa.catheter.receptacle.ByteReceptacle;
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
import java.util.stream.StreamSupport;

public class ByteCatheter {
    private static final Random RANDOM = new Random();
    private byte[] targets;

    public ByteCatheter(byte[] targets) {
        this.targets = targets;
    }

    public static ByteCatheter make(byte... targets) {
        return new ByteCatheter(targets);
    }

    public static ByteCatheter makeCapacity(int size) {
        return new ByteCatheter(array(size));
    }

    public static <X> ByteCatheter of(byte[] targets) {
        return new ByteCatheter(targets);
    }

    public static ByteCatheter of(Collection<Byte> targets) {
        if (targets == null) {
            return new ByteCatheter(array(0));
        }
        byte[] delegate = new byte[targets.size()];
        int index = 0;
        for (byte target : targets) {
            delegate[index++] = target;
        }
        return new ByteCatheter(delegate);
    }

    public ByteCatheter each(final ByteConsumer action) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            action.accept(b);
        }
        return this;
    }

    public ByteCatheter each(final ByteConsumer action, Runnable poster) {
        each(action);
        poster.run();
        return this;
    }

    public <X> ByteCatheter each(X initializer, final BiConsumer<X, Byte> action) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            action.accept(initializer, b);
        }
        return this;
    }

    public <X> ByteCatheter each(X initializer, final BiConsumer<X, Byte> action, Consumer<X> poster) {
        each(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public <X> ByteCatheter overall(X initializer, final TriConsumer<X, Integer, Byte> action) {
        final byte[] ts = this.targets;
        int index = 0;
        for (byte b : ts) {
            action.accept(initializer, index++, b);
        }
        return this;
    }

    public <X> ByteCatheter overall(X initializer, final TriConsumer<X, Integer, Byte> action, Consumer<X> poster) {
        overall(initializer, action);
        poster.accept(initializer);
        return this;
    }

    public ByteCatheter overall(final IntegerAndByteConsumer action) {
        final byte[] ts = this.targets;
        int index = 0;
        for (byte b : ts) {
            action.accept(index++, b);
        }
        return this;
    }

    public ByteCatheter overall(final IntegerAndByteConsumer action, Runnable poster) {
        overall(action);
        poster.run();
        return this;
    }

    public ByteCatheter insert(final IntegerAndBiByteToByteFunction maker) {
        final Map<Integer, IntegerAndBytePair> indexes = new HashMap<>();
        final ByteReceptacle lastItem = ByteReceptacle.of();
        overall((index, item) -> {
            indexes.put(
                    index + indexes.size(), 
                    new IntegerAndBytePair(index, maker.apply(index, item, lastItem.get()))
            );
            lastItem.set(item);
        });

        final byte[] ts = this.targets;
        final byte[] newDelegate = array(ts.length + indexes.size());
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
                    final IntegerAndBytePair item = indexes.get(index);
                    newDelegate[index] = item.byteValue();
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

    public ByteCatheter pluck(final IntegerAndBiBytePredicate maker) {
        final ByteReceptacle lastItem = ByteReceptacle.of();
        return overallFilter((index, item) -> {
            if (maker.test(index, item, lastItem.get())) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public ByteCatheter discardTo(final BytePredicate predicate) {
        ByteCatheter result = ByteCatheter.make();

        overallFilter((index, item) -> !predicate.test(item), result::reset);

        return result;
    }

    public <X> ByteCatheter discardTo(final Predicate<X> predicate, ByteFunction<X> converter) {
        ByteCatheter result = ByteCatheter.make();

        overallFilter((index, item) -> !predicate.test(converter.apply(item)), result::reset);

        return result;
    }

    public ByteCatheter discardTo(final byte initializer, final BiBytePredicate predicate) {
        ByteCatheter result = ByteCatheter.make();

        overallFilter((index, item) -> !predicate.test(item, initializer), result::reset);

        return result;
    }

    public ByteCatheter orDiscardTo(final boolean succeed, final BytePredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate);
    }

    public <X> ByteCatheter orDiscardTo(final boolean succeed, final Predicate<X> predicate, ByteFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discardTo(predicate, converter);
    }

    public ByteCatheter orDiscardTo(final boolean succeed, final byte initializer, final BiBytePredicate predicate) {
        if (succeed) {
            return this;
        }
        return discardTo(initializer, predicate);
    }

    public ByteCatheter discard(final BytePredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item));
    }

    public <X> ByteCatheter discard(final Predicate<X> predicate, ByteFunction<X> converter) {
        return overallFilter((index, item) -> !predicate.test(converter.apply(item)));
    }

    public ByteCatheter discard(final byte initializer, final BiBytePredicate predicate) {
        return overallFilter((index, item) -> !predicate.test(item, initializer));
    }

    public ByteCatheter orDiscard(final boolean succeed, final BytePredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(predicate);
    }

    public <X> ByteCatheter orDiscard(final boolean succeed, final Predicate<X> predicate, ByteFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return discard(predicate, converter);
    }

    public ByteCatheter orDiscard(final boolean succeed, final byte initializer, final BiBytePredicate predicate) {
        if (succeed) {
            return this;
        }
        return discard(initializer, predicate);
    }

    public ByteCatheter filterTo(final BytePredicate predicate) {
        return dump().filter(predicate);
    }

    public <X> ByteCatheter filterTo(final Predicate<X> predicate, ByteFunction<X> converter) {
        return dump().filter(predicate, converter);
    }

    public ByteCatheter filterTo(final byte initializer, final BiBytePredicate predicate) {
        return dump().filter(initializer, predicate);
    }

    public ByteCatheter orFilterTo(final boolean succeed, final BytePredicate predicate) {
        return dump().orFilter(succeed, predicate);
    }

    public <X> ByteCatheter orFilterTo(final boolean succeed, final Predicate<X> predicate, ByteFunction<X> converter) {
        return dump().orFilter(succeed, predicate, converter);
    }

    public ByteCatheter orFilterTo(final boolean succeed, final byte initializer, final BiBytePredicate predicate) {
        return dump().orFilter(succeed, initializer, predicate);
    }

    public ByteCatheter filter(final BytePredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    public <X> ByteCatheter filter(final Predicate<X> predicate, ByteFunction<X> converter) {
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
    public ByteCatheter overallFilter(final IntegerAndBytePredicate predicate) {
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
    public ByteCatheter overallFilter(final IntegerAndBytePredicate predicate, Consumer<byte[]> discarding) {
        if (isEmpty()) {
            return this;
        }

        // 创建需要的变量和常量
        final byte[] ts = this.targets;
        final int length = ts.length;
        final boolean[] deleting = new boolean[length];
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        for (byte target : ts) {
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
        final byte[] newDelegate = array(newDelegateSize);
        final byte[] discardingDelegate = array(length - newDelegateSize);
        int discardingDelegateIndex = 0;
        int newDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        for (boolean isDeleting : deleting) {
            // deleting 值为true则为被筛选掉的，忽略
            final byte t = ts[index++];

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

    public ByteCatheter overallFilter(final byte initializer, final IntegerAndBiBytePredicate predicate) {
        return overallFilter((index, item) -> predicate.test(index, item, initializer));
    }

    public ByteCatheter filter(final byte initializer, final BiBytePredicate predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public ByteCatheter orFilter(final boolean succeed, final BytePredicate predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public <X> ByteCatheter orFilter(final boolean succeed, final Predicate<X> predicate, ByteFunction<X> converter) {
        if (succeed) {
            return this;
        }
        return filter(predicate, converter);
    }

    public ByteCatheter orFilter(final boolean succeed, final byte initializer, final BiBytePredicate predicate) {
        if (succeed) {
            return this;
        }
        return filter(initializer, predicate);
    }

    public ByteCatheter distinct() {
        final Map<Byte, Boolean> map = new HashMap<>();
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

    public ByteCatheter removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        byte[] newDelegate = array(count() - 1);
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

    public ByteCatheter ifPresent(Consumer<ByteCatheter> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public ByteCatheter ifEmpty(Consumer<ByteCatheter> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }

    public ByteCatheter sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public ByteCatheter sort(Comparator<Byte> comparator) {
        Byte[] array = new Byte[this.targets.length];
        int index = 0;
        for (byte target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (byte target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public ByteCatheter holdTill(int index) {
        if (isEmpty()) {
            return this;
        }

        index = Math.min(index, this.targets.length);

        final byte[] ts = this.targets;
        final byte[] newDelegate = array(index);
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

    public ByteCatheter holdTill(final BytePredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final byte[] ts = this.targets;
        final byte[] newDelegate = array(index);
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

    public ByteCatheter whenFlock(final byte source, final BiByteToByteFunction maker, ByteConsumer consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public ByteCatheter whenFlock(BiByteToByteFunction maker, ByteConsumer consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public byte flock(final byte source, final BiByteToByteFunction maker) {
        final byte[] ts = this.targets;
        byte result = source;
        for (byte b : ts) {
            result = maker.applyAsByte(result, b);
        }
        return result;
    }

    public byte flock(final BiByteToByteFunction maker) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        byte result = length > 0 ? ts[0] : 0;
        for (int i = 1; i < length; i++) {
            result = maker.applyAsByte(result, ts[i]);
        }
        return result;
    }

    public <X> X alternate(final X source, final BiFunction<X, Byte, X> maker) {
        X result = source;
        final byte[] ts = this.targets;
        for (byte b : ts) {
            result = maker.apply(result, b);
        }
        return result;
    }

    public <X> ByteCatheter whenAlternate(final X source, final BiFunction<X, Byte, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public <X> ByteCatheter whenAlternate(BiFunction<X, Byte, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(null, maker));
        return this;
    }

    public boolean alternate(final boolean source, final BiBytePredicate maker) {
        BooleanReceptacle result = new BooleanReceptacle(source);
        flock((older, newer) ->{
            result.and(maker.test(older, newer));
            return newer;
        });
        return result.get();
    }

    public ByteCatheter whenAlternate(final boolean source, final BiBytePredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public ByteCatheter whenAlternate(BiBytePredicate maker, BooleanConsumer consumer) {
        consumer.accept(alternate(false, maker));
        return this;
    }

    public ByteCatheter waiveTill(final int index) {
        if (isEmpty()) {
            return this;
        }

        final byte[] ts = this.targets;
        final byte[] newDelegate;
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

    public ByteCatheter waiveTill(final BytePredicate predicate) {
        if (isEmpty()) {
            return this;
        }

        final int index = findTill(predicate);

        final byte[] ts = this.targets;
        final byte[] newDelegate;
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

    public ByteCatheter till(final BytePredicate predicate) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            if (predicate.test(b)) {
                break;
            }
        }

        return this;
    }

    public int findTill(final BytePredicate predicate) {
        final byte[] ts = this.targets;
        int index = 0;
        for (byte b : ts) {
            if (predicate.test(b)) {
                break;
            }
            index++;
        }

        return index;
    }

    public ByteCatheter replace(final ByteUnaryOperator handler) {
        if (isEmpty()) {
            return this;
        }

        final byte[] ts = this.targets;
        int index = 0;
        for (byte b : ts) {
            ts[index++] = handler.applyAsByte(b);
        }
        return this;
    }

    public BooleanCatheter vary(final BytePredicate handler) {
        if (isEmpty()) {
            return BooleanCatheter.make();
        }

        final byte[] ts = this.targets;
        final boolean[] array = new boolean[ts.length];
        int index = 0;
        for (byte i : ts) {
            array[index++] = handler.test(i);
        }
        return BooleanCatheter.of(array);
    }

    public DoubleCatheter vary(final ByteToDoubleFunction handler) {
        if (isEmpty()) {
            return DoubleCatheter.make();
        }

        final byte[] ts = this.targets;
        final double[] array = new double[ts.length];
        int index = 0;
        for (byte i : ts) {
            array[index++] = handler.applyAsDouble(i);
        }
        return DoubleCatheter.of(array);
    }

    public ByteCatheter vary(final ByteUnaryOperator handler) {
        return replace(handler);
    }

    public LongCatheter vary(final ByteToLongFunction handler) {
        if (isEmpty()) {
            return LongCatheter.make();
        }

        final byte[] ts = this.targets;
        final long[] array = new long[ts.length];
        int index = 0;
        for (byte i : ts) {
            array[index++] = handler.applyAsLong(i);
        }
        return LongCatheter.of(array);
    }

    public IntCatheter vary(final ByteToIntegerFunction handler) {
        if (isEmpty()) {
            return IntCatheter.make();
        }

        final byte[] ts = this.targets;
        final int[] array = new int[ts.length];
        int index = 0;
        for (byte i : ts) {
            array[index++] = handler.applyAsInteger(i);
        }
        return IntCatheter.of(array);
    }

    public <X> Catheter<X> vary(final ByteFunction<X> handler) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final byte[] ts = this.targets;
        final X[] array = xArray(ts.length);
        int index = 0;
        for (byte b : ts) {
            array[index++] = handler.apply(b);
        }
        return Catheter.of(array);
    }

    public <X> Catheter<X> vary(final ByteFunction<X> handler, IntFunction<X[]> arrayGenerator) {
        if (isEmpty()) {
            return Catheter.make();
        }

        final byte[] ts = this.targets;
        final X[] array = arrayGenerator.apply(ts.length);
        int index = 0;
        for (byte l : ts) {
            array[index++] = handler.apply(l);
        }
        return Catheter.of(array).arrayGenerator(arrayGenerator);
    }

    public ByteCatheter whenAny(final BytePredicate predicate, final ByteConsumer action) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            if (predicate.test(b)) {
                action.accept(b);
                break;
            }
        }
        return this;
    }

    public ByteCatheter whenAll(final BytePredicate predicate, final Runnable action) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            if (predicate.test(b)) {
                continue;
            }
            return this;
        }
        action.run();
        return this;
    }

    public ByteCatheter whenAll(final BytePredicate predicate, final ByteConsumer action) {
        return whenAll(predicate, () -> each(action));
    }

    private ByteCatheter whenNone(final BytePredicate predicate, final Runnable action) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            if (predicate.test(b)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final BytePredicate predicate) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            if (predicate.test(b)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final BytePredicate predicate) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            if (predicate.test(b)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public boolean hasNone(final BytePredicate predicate) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            if (predicate.test(b)) {
                return false;
            }
        }
        return true;
    }

    public byte findFirst(final BytePredicate predicate) {
        final byte[] ts = this.targets;
        for (byte b : ts) {
            if (predicate.test(b)) {
                return b;
            }
        }
        return 0;
    }

    public byte findLast(final BytePredicate predicate) {
        final byte[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final byte t = ts[index--];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0;
    }

    public <X> X whenFoundFirst(final BytePredicate predicate, ByteFunction<X> function) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        for (byte b : ts) {
            if (predicate.test(b)) {
                return function.apply(b);
            }
        }
        return null;
    }

    public <X> X whenFoundLast(final BytePredicate predicate, ByteFunction<X> function) {
        final byte[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final byte t = ts[index--];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public ByteCatheter any(final ByteConsumer consumer) {
        if (this.targets.length > 0) {
            byte[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public ByteCatheter first(final ByteConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public ByteCatheter tail(final ByteConsumer consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public ByteCatheter reverse() {
        if (isEmpty()) {
            return this;
        }

        final byte[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        byte temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public byte max(final Comparator<Byte> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public byte min(final Comparator<Byte> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public ByteCatheter whenMax(final Comparator<Byte> comparator, final ByteConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public ByteCatheter whenMin(final Comparator<Byte> comparator, final ByteConsumer action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    public int count() {
        return this.targets.length;
    }

    public ByteCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public ByteCatheter count(final IntegerReceptacle target) {
        target.set(count());
        return this;
    }

    public ByteCatheter count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    public final ByteCatheter append(final byte... objects) {
        final byte[] ts = this.targets;
        final byte[] newDelegate = array(ts.length + objects.length);
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

    public ByteCatheter append(final ByteCatheter objects) {
        return append(objects.array());
    }

    public ByteCatheter repeat(final int count) {
        final byte[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public byte fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, byte item) {
        this.targets[index] = item;
    }

    public ByteCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Byte> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, ByteCatheter input, final TriFunction<MatrixPos, Byte, Byte, X> action) {
        if (input.count() == count()) {
            final IntegerReceptacle index = new IntegerReceptacle(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final byte inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public ByteCatheter matrixMap(
            final int width,
            final int inputWidth,
            final ByteCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Byte, Byte, Byte> scanFlocked,
            final TriFunction<MatrixPos, Byte, Byte, Byte> combineFlocked
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
        final ByteCatheter newMatrix = ByteCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final ByteCatheter flockingCatheter = ByteCatheter.makeCapacity(width);

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
                final byte fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final byte fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public <X> Catheter<X> matrixVary(final int width, byte input, final TriFunction<MatrixPos, Byte, Byte, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public ByteCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Byte, Byte> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Byte, X> action) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final IntegerReceptacle w = new IntegerReceptacle(0);
        final IntegerReceptacle h = new IntegerReceptacle(0);

        final int matrixEdge = width - 1;

        return vary((byte item) -> {
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

    public Catheter<ByteCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<ByteCatheter> results = Catheter.makeCapacity(sourceHeight);
        ByteCatheter catheter = ByteCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final byte element = fetch(y * width + x);
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

    public ByteCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public ByteCatheter shuffle(RandomGenerator random) {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public ByteCatheter swapShuffle(RandomGenerator random) {
        byte[] elements = this.targets;
        int i = elements.length;

        for (int j = i; j > 1; --j) {
            int swapTo = random.nextInt(j);
            int swapFrom = j - 1;
            byte fromElement = elements[swapFrom];
            byte toElement = elements[swapTo];
            elements[swapTo] = fromElement;
            elements[swapFrom] = toElement;
        }

        return this;
    }

    public boolean has(byte target) {
        return hasAny(t -> t == target);
    }

    public boolean not(byte target) {
        return !has(target);
    }

    public ByteCatheter merge(ByteCatheter other) {
        return append(other.filter(this::not));
    }

    public ByteCatheter dump() {
        return new ByteCatheter(array());
    }

    public ByteCatheter flat(ByteFunction<ByteCatheter> function) {
        if (isEmpty()) {
            return this;
        }

        Catheter<ByteCatheter> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (byte element : this.targets) {
            ByteCatheter flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        this.targets = array(totalSize);
        int pos = 0;
        for (ByteCatheter flat : catheter.targets) {
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

    public <X> Catheter<X> flatTo(ByteFunction<Catheter<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Catheter<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (byte element : this.targets) {
            Catheter<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.count();
        }

        return Catheter.flatting(catheter, totalSize);
    }

    public <X> Catheter<X> flatToByCollection(ByteFunction<Collection<X>> function) {
        if (isEmpty()) {
            return Catheter.make();
        }

        Catheter<Collection<X>> catheter = Catheter.makeCapacity(count());
        int totalSize = 0;

        int index = 0;
        for (byte element : this.targets) {
            Collection<X> flatting = function.apply(element);
            catheter.fetch(index++, flatting);
            totalSize += flatting.size();
        }

        return Catheter.flattingCollection(catheter, totalSize);
    }

    public ByteCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public ByteCatheter reset(byte[] targets) {
        this.targets = targets;
        return this;
    }

    public byte[] array() {
        return this.targets.clone();
    }

    public byte[] dArray() {
        return this.targets;
    }

    public Stream<Byte> stream() {
        return list().stream();
    }

    public List<Byte> list() {
        List<Byte> list = new ArrayList<>();
        for (byte l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Byte> set() {
        Set<Byte> set = new HashSet<>();
        for (byte l : array()) {
            set.add(l);
        }
        return set;
    }

    private static byte[] array(int size) {
        return new byte[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
