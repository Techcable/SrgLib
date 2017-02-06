package net.techcable.srglib.mappings;

import java.util.HashMap;

import com.google.common.collect.HashBiMap;

import net.techcable.srglib.FieldData;
import net.techcable.srglib.JavaType;
import net.techcable.srglib.MethodData;

import static com.google.common.base.Preconditions.*;

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
    default void putMethod(MethodData original, MethodData renamed) {
        checkArgument(
                original.mapTypes(this::getNewType).hasSameTypes(renamed),
                "Remapped method data types (%s) don't correspond to original types (%s)",
                renamed,
                original
        );
        putMethod(original, renamed.getName());
    }

    /**
     * Set the method's new name.
     *
     * @param original the original method data
     * @param newName the new method name
     */
    void putMethod(MethodData original, String newName);

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
    default void putField(FieldData original, FieldData renamed) {
        checkArgument(
                original.mapTypes(this::getNewType).hasSameTypes(renamed),
                "Remapped field data (%s) doesn't correspond to original types (%s)",
                renamed,
                original
        );
        putField(original, renamed.getName());
    }

    /**
     * Set a fields's new name.
     *
     * @param original the original method data
     * @param newName the new name
     */
    void putField(FieldData original, String newName);

    /**
     * Return an inverted copy of the mappings, switching the original and renamed.
     * <p>
     * Changes in this mapping will <b>not</b> be reflected in the resulting view
     * </p>
     *
     * @return an inverted copy
     */
    @Override
    Mappings inverted();

    /**
     * Create a new mutable mappings object, with no contents.
     *
     * @return a new mutable mappings
     */
    static MutableMappings create() {
        return new SimpleMappings(HashBiMap.create(), new HashMap<>(), new HashMap<>());
    }
}
