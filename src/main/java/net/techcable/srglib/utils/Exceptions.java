package net.techcable.srglib.utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Exceptions {
    private Exceptions() {}

    public static <T, U> BiConsumer<T, U> sneakyThrowing(CheckedBiConsumer<T, U, Throwable> consumer) {
        return (first, second) -> {
            try {
                consumer.accept(first, second);
            } catch (Throwable e) {
                throw sneakyThrow(e);
            }
        };
    }

    public static  <T> Consumer<T> sneakyThrowing(CheckedConsumer<T, Throwable> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Throwable e) {
                throw sneakyThrow(e);
            }
        };
    }

    public static Runnable sneakyThrowing(CheckedRunnable<Throwable> r) {
        return () -> {
            try {
                r.run();
            } catch (Throwable t) {
                throw sneakyThrow(t);
            }
        };
    }

    public static <T> Supplier<T> sneakyThrowing(CheckedSupplier<T, Throwable> r) {
        return () -> {
            try {
                return r.get();
            } catch (Throwable t) {
                throw sneakyThrow(t);
            }
        };
    }

    public static AssertionError sneakyThrow(Throwable t) {
        throw sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked") // This is intentional :p
    private static <T extends Throwable> AssertionError sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }
}
