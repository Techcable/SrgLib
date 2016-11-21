package net.techcable.srglib.mappings;

import net.techcable.srglib.FieldData;
import net.techcable.srglib.JavaType;
import net.techcable.srglib.MethodData;

/**
 * Mappings that can be modified
 */
public interface MutableMappings extends Mappings {

    /**
     * Set a class's new name.
     *
     * @param original the original name
     * @param renamed the class's new name
     * @throws IllegalArgumentException if the class isn't a reference type
     */
    void putClass(JavaType original, JavaType renamed);

    /**
     * Set a method's new name, ensuring the signatures match.
     * <p>
     * After mapping the method's signature to the new type names the signatures must match,
     * so that {@code original.mapTypes(mappings::getNewType)} equals the new types.
     * </p>
     *
     * @param original the original method data
     * @param renamed the new method data
     * @throws IllegalArgumentException if the signatures mismatch
     */
    void putMethod(MethodData original, MethodData renamed);

    /**
     * Set the method's new name, remapping the signature to the new type names.
     *
     * @param original the original method data
     * @param newName the new method name
     */
    default void putMethod(MethodData original, String newName) {
        putMethod(original, original.withName(newName));
    }

    /**
     * Set a fields's new name, ensuring the signatures match.
     * <p>
     * After mapping the method's signature to the new type names the signatures must match,
     * so that {@code original.mapTypes(mappings::getNewType)} equals the new types.
     * </p>
     *
     * @param original the original method data
     * @param renamed the new method data
     * @throws IllegalArgumentException if the signatures mismatch
     */
    void putField(FieldData original, FieldData renamed);

    /**
     * Set a fields's new name.
     *
     * @param original the original method data
     * @param newName the new name
     */
    default void putField(FieldData original, String newName) {
        putField(original, original.mapTypes(this::getNewType).withName(newName));
    }


    /**
     * Return an inverted view of the mappings, switching the original and renamed.
     * <p>
     * Changes in this mapping will be reflected in the resulting view, and vice versa.
     * </p>
     *
     * @return an inverted view
     */
    @Override
    MutableMappings inverted();

    /**
     * Create a new mutable mappings object, with no contents.
     *
     * @return a new mutable mappings
     */
    static MutableMappings create() {
        return SimpleMappings.create();
    }
}
