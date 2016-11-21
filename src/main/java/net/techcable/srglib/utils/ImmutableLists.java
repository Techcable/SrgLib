package net.techcable.srglib.utils;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

public final class ImmutableLists {
    private ImmutableLists() {}


    private static class ImmutableListCollector<E> implements Collector<E, ImmutableList.Builder<E>, ImmutableList<E>> {
        private ImmutableListCollector() {}
        public static ImmutableListCollector INSTANCE = new ImmutableListCollector();
        @Override
        public Supplier<ImmutableList.Builder<E>> supplier() {
            return ImmutableList::builder;
        }

        @Override
        public BiConsumer<ImmutableList.Builder<E>, E> accumulator() {
            return ImmutableList.Builder::add;
        }

        private static <E> ImmutableList.Builder<E> combine(ImmutableList.Builder<E> first, ImmutableList.Builder<E> second) {
            return first.addAll(second.build());
        }

        @Override
        public BinaryOperator<ImmutableList.Builder<E>> combiner() {
            return ImmutableListCollector::combine;
        }

        @Override
        public Function<ImmutableList.Builder<E>, ImmutableList<E>> finisher() {
            return ImmutableList.Builder::build;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return ImmutableSet.of();
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> Collector<E, ?, ImmutableList<E>> collector() {
        return ImmutableListCollector.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T, U> ImmutableList<U> transform(ImmutableList<T> original, Function<T, U> transformer) {
        int size = requireNonNull(original, "Null original list").size();
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            T originalElement = original.get(i);
            U newElement = transformer.apply(originalElement);
            checkNotNull(newElement, "Transformer produced null value for input: %s", originalElement);
            result[i] = newElement;
        }
        return (ImmutableList<U>) ImmutableList.copyOf(result);
    }


    public static <T> String joinToString(ImmutableList<T> list, Function<T, String> asString, String delimiter) {
        return joinToString(list, asString, delimiter, "", "");
    }

    public static <T> String joinToString(
            ImmutableList<T> list,
            Function<T, String> asString,
            String delimiter,
            String prefix,
            String suffix
    ) {
        int size = requireNonNull(list, "Null list").size();
        int delimiterLength = requireNonNull(delimiter, "Null delimiter").length();
        int prefixLength = requireNonNull(prefix, "Null prefix").length();
        int suffixLength = requireNonNull(suffix, "Null suffix").length();
        String[] strings = new String[size];
        int neededChars = prefixLength + suffixLength + (Math.max(0, size - 1)) * delimiterLength;
        for (int i = 0; i < size; i++) {
            T element = list.get(i);
            String str = asString.apply(element);
            strings[i] = str;
            neededChars += str.length();
        }
        char[] result = new char[neededChars];
        int resultSize = 0;
        prefix.getChars(0, prefixLength, result, resultSize);
        resultSize += prefixLength;
        for (int i = 0; i < size; i++) {
            String str = strings[i];
            if (i > 0) {
                // Prefix it with the delimiter
                delimiter.getChars(0, delimiterLength, result, resultSize);
                resultSize += delimiterLength;
            }
            int length = str.length();
            str.getChars(0, length, result, resultSize);
            resultSize += length;
        }
        suffix.getChars(0, suffixLength, result, resultSize);
        resultSize += suffixLength;
        assert result.length == resultSize;
        return String.valueOf(result);
    }
}
