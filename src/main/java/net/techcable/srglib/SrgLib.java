package net.techcable.srglib;

import com.google.common.base.Preconditions;

import net.techcable.srglib.mappings.Mappings;

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

    /**
     * Checks that all fields and methods in the mappings have the correct type information
     *
     * @param mappings the mappings to check
     */
    public static void checkConsistency(Mappings mappings) {
        mappings.forEachField((originalField, renamedField) -> checkArgument(
                originalField.mapTypes(mappings::getNewType).hasSameTypes(renamedField),
                "Remapped field data (%s) doesn't correspond to original types (%s)",
                originalField,
                renamedField
        ));
        mappings.forEachMethod((originalMethod, renamedMethod) -> checkArgument(
                originalMethod.mapTypes(mappings::getNewType).hasSameTypes(renamedMethod),
                "Remapped method data (%s) doesn't correspond to original types (%s)",
                originalMethod,
                renamedMethod
        ));
    }
}
