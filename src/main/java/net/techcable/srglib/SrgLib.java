package net.techcable.srglib;

import static com.google.common.base.Preconditions.*;

/**
 * Static utility methods for handling srg.
 */
public final class SrgLib {
    private SrgLib() {}

    /**
     * Checks if the given name is a valid java identifier
     *
     * Java identifiers are used for field or method names
     *
     * @param name the name to check
     */
    public static boolean isValidIdentifier(String name) {
        checkArgument(!name.isEmpty(), "Empty name: %s", name);
        return Character.isJavaIdentifierStart(name.codePointAt(0)) && name.codePoints()
                .skip(1) // Skip the first char, since we already checked it
                .allMatch(Character::isJavaIdentifierPart);
    }
}
