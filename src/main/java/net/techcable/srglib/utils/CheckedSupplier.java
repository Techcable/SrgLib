package net.techcable.srglib.utils;

public interface CheckedSupplier<T, E extends Throwable> {
    T get() throws E;
}
