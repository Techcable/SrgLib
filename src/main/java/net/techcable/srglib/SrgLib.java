package net.techcable.srglib;

import java.util.regex.Pattern;

/**
 * Static utility methods for handling srg.
 */
public final class SrgLib {
    private SrgLib() {}
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[\\w_<>$]+");

    /**
     * Checks if the given name is a valid java identifier
     *
     * Java identifiers are used for field or method names
     *
     * @param name the name to check
     */
    public static boolean isValidIdentifier(String name) {
        return IDENTIFIER_PATTERN.matcher(name).matches();
    }
}
