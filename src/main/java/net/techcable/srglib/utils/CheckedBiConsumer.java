package net.techcable.srglib.utils;

public interface CheckedBiConsumer<T, U, E extends Throwable> {
    void accept(T first, U second) throws E;
}
