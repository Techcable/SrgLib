package net.techcable.srglib.utils;

public interface CheckedConsumer<T, E extends Throwable> {
    void accept(T obj) throws E;
}
