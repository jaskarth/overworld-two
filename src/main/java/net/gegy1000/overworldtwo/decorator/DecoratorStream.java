package net.gegy1000.overworldtwo.decorator;

import com.google.common.collect.AbstractIterator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public abstract class DecoratorStream<C> implements Stream<BlockPos> {
    private WorldAccess world;
    private ChunkGenerator generator;
    private Random random;
    private C config;
    private BlockPos origin;

    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    public final void open(WorldAccess world, ChunkGenerator generator, Random random, C config, BlockPos origin) {
        this.world = world;
        this.generator = generator;
        this.random = random;
        this.config = config;
        this.origin = origin;
        this.reset();
    }

    public final BlockPos next() {
        if (this.next(this.mutablePos, this.world, this.generator, this.random, this.config, this.origin)) {
            return this.mutablePos;
        }
        return null;
    }

    protected abstract void reset();

    protected abstract boolean next(
            BlockPos.Mutable output,
            WorldAccess world, ChunkGenerator generator,
            Random random, C config,
            BlockPos origin
    );

    private Stream<BlockPos> asStream() {
        Stream.Builder<BlockPos> builder = Stream.builder();

        BlockPos pos;
        while ((pos = this.next()) != null) {
            builder.add(pos.toImmutable());
        }

        return builder.build();
    }

    @Override
    public Stream<BlockPos> filter(Predicate<? super BlockPos> predicate) {
        return this.asStream().filter(predicate);
    }

    @Override
    public <R> Stream<R> map(Function<? super BlockPos, ? extends R> function) {
        return this.asStream().map(function);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super BlockPos> toIntFunction) {
        return this.asStream().mapToInt(toIntFunction);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super BlockPos> toLongFunction) {
        return this.asStream().mapToLong(toLongFunction);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super BlockPos> toDoubleFunction) {
        return this.asStream().mapToDouble(toDoubleFunction);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super BlockPos, ? extends Stream<? extends R>> function) {
        return this.asStream().flatMap(function);
    }

    @Override
    public IntStream flatMapToInt(Function<? super BlockPos, ? extends IntStream> function) {
        return this.asStream().flatMapToInt(function);
    }

    @Override
    public LongStream flatMapToLong(Function<? super BlockPos, ? extends LongStream> function) {
        return this.asStream().flatMapToLong(function);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super BlockPos, ? extends DoubleStream> function) {
        return this.asStream().flatMapToDouble(function);
    }

    @Override
    public Stream<BlockPos> distinct() {
        return this.asStream().distinct();
    }

    @Override
    public Stream<BlockPos> sorted() {
        return this.asStream().sorted();
    }

    @Override
    public Stream<BlockPos> sorted(Comparator<? super BlockPos> comparator) {
        return this.asStream().sorted(comparator);
    }

    @Override
    public Stream<BlockPos> peek(Consumer<? super BlockPos> consumer) {
        return this.asStream().peek(consumer);
    }

    @Override
    public Stream<BlockPos> limit(long l) {
        return this.asStream().limit(l);
    }

    @Override
    public Stream<BlockPos> skip(long count) {
        for (int i = 0; i < count; i++) {
            this.next();
        }
        return this;
    }

    @Override
    public void forEach(Consumer<? super BlockPos> consumer) {
        BlockPos pos;
        while ((pos = this.next()) != null) {
            consumer.accept(pos);
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super BlockPos> consumer) {
        this.forEach(consumer);
    }

    @Override
    public Object[] toArray() {
        return this.toArray(Object[]::new);
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> function) {
        List<BlockPos> collect = new ArrayList<>();

        BlockPos pos;
        while ((pos = this.next()) != null) {
            collect.add(pos.toImmutable());
        }

        A[] array = function.apply(collect.size());
        return collect.toArray(array);
    }

    @Override
    public BlockPos reduce(BlockPos init, BinaryOperator<BlockPos> op) {
        return this.asStream().reduce(init, op);
    }

    @Override
    public Optional<BlockPos> reduce(BinaryOperator<BlockPos> op) {
        return this.asStream().reduce(op);
    }

    @Override
    public <U> U reduce(U init, BiFunction<U, ? super BlockPos, U> map, BinaryOperator<U> op) {
        return this.asStream().reduce(init, map, op);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super BlockPos> accumulator, BiConsumer<R, R> combiner) {
        R result = supplier.get();

        BlockPos pos;
        while ((pos = this.next()) != null) {
            accumulator.accept(result, pos.toImmutable());
        }

        return result;
    }

    @Override
    public <R, A> R collect(Collector<? super BlockPos, A, R> collector) {
        A accumulator = collector.supplier().get();

        BlockPos pos;
        while ((pos = this.next()) != null) {
            collector.accumulator().accept(accumulator, pos);
        }

        return collector.finisher().apply(accumulator);
    }

    @Override
    public Optional<BlockPos> min(Comparator<? super BlockPos> comparator) {
        return this.asStream().min(comparator);
    }

    @Override
    public Optional<BlockPos> max(Comparator<? super BlockPos> comparator) {
        return this.asStream().max(comparator);
    }

    @Override
    public long count() {
        long count = 0;
        while (this.next() != null) {
            count++;
        }
        return count;
    }

    @Override
    public boolean anyMatch(Predicate<? super BlockPos> predicate) {
        return this.asStream().anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super BlockPos> predicate) {
        return this.asStream().allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super BlockPos> predicate) {
        return this.asStream().noneMatch(predicate);
    }

    @Override
    public Optional<BlockPos> findFirst() {
        return Optional.ofNullable(this.next());
    }

    @Override
    public Optional<BlockPos> findAny() {
        return Optional.ofNullable(this.next());
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return new AbstractIterator<BlockPos>() {
            @Override
            protected BlockPos computeNext() {
                BlockPos next = DecoratorStream.this.next();
                return next != null ? next : this.endOfData();
            }
        };
    }

    @Override
    public Spliterator<BlockPos> spliterator() {
        return this.asStream().spliterator();
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public Stream<BlockPos> sequential() {
        return this;
    }

    @Override
    public Stream<BlockPos> parallel() {
        return this;
    }

    @Override
    public Stream<BlockPos> unordered() {
        return this;
    }

    @Override
    public Stream<BlockPos> onClose(Runnable runnable) {
        return this;
    }

    @Override
    public void close() {
        this.world = null;
        this.generator = null;
        this.random = null;
        this.config = null;
        this.origin = null;
    }
}
