package net.techcable.srglib.mappings;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.techcable.srglib.FieldData;
import net.techcable.srglib.JavaType;
import net.techcable.srglib.MethodData;

import static java.util.Objects.*;

/**
 * A mapping from one set of source names to another.
 */
public interface Mappings {
    /**
     * Get the remapped class name, given the original class name.
     * <p>
     * Returns the original class if no mapping is found.
     * </p>
     *
     * @param original the original class name
     * @return the new class name
     */
    default JavaType getNewClass(String original) {
        return getNewClass(JavaType.fromName(original));
    }


    /**
     * Get the remapped class, given the original class, or the original if no class is found.
     *
     * @param original the original class
     * @return the new class
     * @throws IllegalArgumentException if the class isn't a reference type
     */
    JavaType getNewClass(JavaType original);


    /**
     * Get the remapped type, given the original type, or the original if the type is found.
     * <p>
     * If type is an array, it remaps the innermost element type.
     * If the type is a class, the result is the same as invoking {@link #getNewClass(JavaType)}
     * If the type is a primitive, it returns the same element type.
     * </p>
     *
     * @param original the original type
     * @return the new type
     */
    default JavaType getNewType(JavaType original) {
        return requireNonNull(original, "Null type").mapClass(this::getNewClass);
    }

    /**
     * Get the remapped method data, given the original data.
     * <p>
     * Automatically remaps class names in the signature as needed,
     * even if the method name remains the same.
     * </p>
     *
     * @param original the original method data
     * @return the remapped method data
     */
    MethodData getNewMethod(MethodData original);

    /**
     * Get the remapped field data, given the original data.
     * <p>
     * Automatically remaps class names in the signature as needed,
     * even if the field name remains the same.
     * </p>
     *
     * @param original the original field data
     * @return the remapped field data
     */
    FieldData getNewField(FieldData original);

    /**
     * Return an immutable snapshot of these mappings.
     *
     * @return an immutable snapshot
     */
    default ImmutableMappings snapshot() {
        return ImmutableMappings.copyOf(this);
    }

    /**
     * Return an inverted copy of the mappings, switching the original and renamed.
     * <p>
     * Even if this mapping's underlying source is mutable,
     * changes in this mapping will not be reflected in the resulting view.
     * </p>
     *
     * @return an inverted copy
     */
    default Mappings inverted() {
        return snapshot().inverted();
    }

    /**
     * Return the original classes known to these mappings.
     *
     * @return the original classes.
     */
    Set<JavaType> classes();


    /**
     * Return the original methods known to these mappings.
     *
     * @return the original methods.
     */
    Set<MethodData> methods();

    /**
     * Return the original fields known to these mappings.
     *
     * @return the original fields.
     */
    Set<FieldData> fields();

    default boolean contains(JavaType type) {
        return classes().contains(type);
    }

    default boolean contains(MethodData methodData) {
        return methods().contains(methodData);
    }

    default boolean contains(FieldData fieldData) {
        return fields().contains(fieldData);
    }

    default void forEachClass(BiConsumer<JavaType, JavaType> action) {
        classes().forEach((original) -> action.accept(original, getNewType(original)));
    }

    default void forEachMethod(BiConsumer<MethodData, MethodData> action) {
        methods().forEach((original) -> action.accept(original, getNewMethod(original)));
    }

    default void forEachField(BiConsumer<FieldData, FieldData> action) {
        fields().forEach((original) -> action.accept(original, getNewField(original)));
    }

    /**
     * Transform all the original data in the specified mapping, using this mapping.
     *
     * This is useful for {@link #createRenamingMappings(UnaryOperator, Function, Function)},
     * since renaming mappings have no 'original' data of their own, and so can't be directly output to a file.
     * The returned mapping data is guaranteed to have the same originals as the data of the old mapping data.
     *
     * @return the transformed data
     */
    default Mappings transform(Mappings original) {
        ImmutableBiMap.Builder<JavaType, JavaType> types = ImmutableBiMap.builder();
        ImmutableBiMap.Builder<MethodData, MethodData> methods = ImmutableBiMap.builder();
        ImmutableBiMap.Builder<FieldData, FieldData> fields = ImmutableBiMap.builder();
        original.classes().forEach(originalType -> {
            JavaType newType = this.getNewType(originalType);
            types.put(originalType, newType);
        });
        original.methods().forEach(originalMethodData -> {
            MethodData newMethodData = this.getNewMethod(originalMethodData);
            methods.put(originalMethodData, newMethodData);
        });
        original.fields().forEach(originalFieldData -> {
            FieldData newFieldData = this.getNewField(originalFieldData);
            fields.put(originalFieldData, newFieldData);
        });
        return ImmutableMappings.create(types.build(), methods.build(), fields.build());
    }

    /**
     * Return an immutable empty mappings instance.
     *
     * @return an immutable empty mappings object.
     */
    static ImmutableMappings empty() {
        return ImmutableMappings.EMPTY;
    }

    /**
     * Chain the specified mappings together, using the renamed result of each mapping as the original for the next
     *
     * @param mappings the mappings to chain together
     */
    static Mappings chain(Mappings... mappings) {
        return chain(ImmutableList.copyOf(mappings));
    }

    /**
     * Chain the specified mappings together, using the renamed result of each mapping as the original for the next
     *
     * @param mappings the mappings to chain together
     */
    static Mappings chain(ImmutableList<? extends Mappings> mappings) {
        ImmutableMappings chained = empty();
        for (int i = 0; i < mappings.size(); i++) {
            Mappings mapping = mappings.get(i);
            ImmutableBiMap.Builder<JavaType, JavaType> classes = ImmutableBiMap.builder();
            ImmutableBiMap.Builder<MethodData, MethodData> methods = ImmutableBiMap.builder();
            ImmutableBiMap.Builder<FieldData, FieldData> fields = ImmutableBiMap.builder();
            ImmutableMappings inverted = chained.inverted();

            // If we encounter a new name, add it to the set
            mapping.forEachClass((original, renamed) -> {
                if (!inverted.contains(original)) {
                    classes.put(original, renamed);
                }
            });
            mapping.forEachField((original, renamed) -> {
                if (!inverted.contains(original)) {
                    // We need to make sure the originals we put in the map have the oldest possible type name to remain consistent
                    // Since inverted is a map of new->old, use the old type name if we've ever seen this class before
                    fields.put(original.mapTypes(inverted::getNewType), renamed);
                }
            });
            mapping.forEachMethod((original, renamed) -> {
                if (!inverted.contains(original)) {
                    methods.put(original.mapTypes(inverted::getNewType), renamed);
                }
            });
            // Now run all our current chain through the mapping to get our new result
            chained.forEachClass((original, renamed) -> {
                renamed = mapping.getNewType(renamed);
                classes.put(original, renamed);
            });
            chained.forEachField((original, renamed) -> {
                renamed = mapping.getNewField(renamed);
                fields.put(original, renamed);
            });
            chained.forEachMethod((original, renamed) -> {
                renamed = mapping.getNewMethod(renamed);
                methods.put(original, renamed);
            });
            chained = ImmutableMappings.create(classes.build(), methods.build(), fields.build());
        }
        return chained;
    }


    /**
     * Mappings which rename classes/methods/fields dynamically, based entirely on transformer functions.
     * <p>
     * Unlike most other mappings, these mappings have no fields, methods, or classes of their own,
     * and just rename whatever they are given.
     * In order to use them with class data, use them to {@link #transform(Mappings)} some other set of mappings.
     * </p>
     * <p>
     * The functions are expected to be 'pure', and always give the same output for any input.
     * The type transformer is only called for references, and can't remap primitives.
     * A 'null' transformer does nothing, and simply returns the existing name.
     * Method and field signatures are automatically remapped.
     * </p>
     *
     * @param typeTransformer the function to transform/rename the classes
     * @param methodRenamer   the function to rename the methods
     * @param fieldRenamer    the function to rename the fields
     * @return a mapping which remaps members using the specified methods
     */
    static Mappings createRenamingMappings(
            @Nullable UnaryOperator<JavaType> typeTransformer,
            @Nullable Function<MethodData, String> methodRenamer,
            @Nullable Function<FieldData, String> fieldRenamer
    ) {
        return new RenamingMappings(typeTransformer, methodRenamer, fieldRenamer);
    }

    /**
     * Mappings which dynamically remap classes from one package into another.
     * <p>
     * Unlike other mappings, these mappings have no fields, methods, or classes of their own,
     * and just rename whatever they are given, similar to {@link #createRenamingMappings(UnaryOperator, Function, Function)}.
     * Method and field signatures are automatically remapped.
     * </p>
     *
     * @param packages the packages to remap
     * @return a package mapping
     */
    static Mappings createPackageMappings(ImmutableMap<String, String> packages) {
        return createRenamingMappings((original) -> {
            String originalPackage = original.getPackageName();
            String newPackage = packages.get(originalPackage);
            if (newPackage != null) {
                return JavaType.fromName(newPackage + "." + original.getSimpleName());
            } else {
                return original;
            }
        }, null, null);
    }
}
