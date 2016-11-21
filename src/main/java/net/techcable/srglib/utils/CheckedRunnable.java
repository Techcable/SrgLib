package net.techcable.srglib.utils;

@FunctionalInterface
public interface CheckedRunnable<E extends Throwable> {
    void run() throws E;
}
