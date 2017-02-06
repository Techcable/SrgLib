package net.techcable.srglib.mappings;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableBiMap;

import net.techcable.srglib.FieldData;
import net.techcable.srglib.JavaType;
import net.techcable.srglib.MethodData;
import net.techcable.srglib.SrgLib;
import net.techcable.srglib.utils.ImmutableMaps;

import static com.google.common.base.Preconditions.*;
import static java.util.Objects.*;

public final class ImmutableMappings implements Mappings {
    private final ImmutableBiMap<JavaType, JavaType> classes;
    private final ImmutableBiMap<MethodData, MethodData> methods;
    private final ImmutableBiMap<FieldData, FieldData> fields;
    /* package */ static final ImmutableMappings EMPTY = new ImmutableMappings(ImmutableBiMap.of(), ImmutableBiMap.of(), ImmutableBiMap.of());

    private ImmutableMappings(
            ImmutableBiMap<JavaType, JavaType> classes,
            ImmutableBiMap<MethodData, MethodData> methods,
            ImmutableBiMap<FieldData, FieldData> fields
    ) {
        this.classes = requireNonNull(classes, "Null types");
        this.methods = requireNonNull(methods, "Null methods");
        this.fields = requireNonNull(fields, "Null fields");
    }

    @Override
    public JavaType getNewClass(JavaType original) {
        checkArgument(original.isReferenceType(), "Type isn't a reference type: %s", original);
        return classes.getOrDefault(requireNonNull(original), original);
    }

    @Override
    public MethodData getNewMethod(MethodData original) {
        MethodData result = methods.get(requireNonNull(original));
        if (result != null) {
            return result;
        } else {
            return original.mapTypes(this::getNewType);
        }
    }

    @Override
    public FieldData getNewField(FieldData original) {
        FieldData result = fields.get(requireNonNull(original));
        if (result != null) {
            return result;
        } else {
            return original.mapTypes(this::getNewType);
        }
    }

    @Override
    public Set<JavaType> classes() {
        return classes.keySet();
    }

    @Override
    public Set<MethodData> methods() {
        return methods.keySet();
    }

    @Override
    public Set<FieldData> fields() {
        return fields.keySet();
    }

    @Override
    public ImmutableMappings snapshot() {
        return this;
    }

    @Nullable
    private ImmutableMappings inverted;
    @Override
    public ImmutableMappings inverted() {
        ImmutableMappings inverted = this.inverted;
        return inverted != null ? inverted : (this.inverted = invert0());
    }

    private ImmutableMappings invert0() {
        ImmutableMappings inverted = new ImmutableMappings(this.classes.inverse(), this.methods.inverse(), this.fields.inverse());
        inverted.inverted = this;
        return inverted;
    }

    public static ImmutableMappings copyOf(
            Map<JavaType, JavaType> originalClasses,
            Map<MethodData, String> methodNames,
            Map<FieldData, String> fieldNames
    ) {
        ImmutableBiMap<JavaType, JavaType> classes = ImmutableBiMap.copyOf(originalClasses); // Defensive copy to an ImmutableBiMap
        // No consistency check needed since we're building type-information from scratch
        ImmutableBiMap.Builder<MethodData, MethodData> methods = ImmutableBiMap.builder();
        ImmutableBiMap.Builder<FieldData, FieldData> fields = ImmutableBiMap.builder();
        methodNames.forEach((originalData, newName) -> {
            MethodData newData = originalData
                    .mapTypes((oldType) -> oldType.mapClass(oldClass -> classes.getOrDefault(oldClass, oldClass)))
                    .withName(newName);
            methods.put(originalData, newData);
        });
        fieldNames.forEach((originalData, newName) -> {
            FieldData newData = FieldData.create(
                    originalData.getDeclaringType().mapClass(oldClass -> classes.getOrDefault(oldClass, oldClass)),
                    newName
            );
            fields.put(originalData, newData);
        });
        return new ImmutableMappings(
                ImmutableBiMap.copyOf(classes),
                methods.build(),
                fields.build()
        );
    }

    /**
     * Create new ImmutableMappings with the specified data.
     * <p>
     * NOTE: {@link #copyOf(Map, Map, Map)} may be preferable,
     * as it automatically remaps method signatures for you.
     * </p>
     *
     * @param classes the class data mappings
     * @param methods the method data mappings
     * @param fields the field data mappings
     * @throws IllegalArgumentException if any of the types in the fields or methods don't match the type data
     * @return immutable mappings with the specified data
     */
    public static ImmutableMappings create(
            ImmutableBiMap<JavaType, JavaType> classes,
            ImmutableBiMap<MethodData, MethodData> methods,
            ImmutableBiMap<FieldData, FieldData> fields
    ) {
        ImmutableMappings result = new ImmutableMappings(classes, methods, fields);
        SrgLib.checkConsistency(result);
        return result;
    }

    public static ImmutableMappings copyOf(Mappings other) {
        if (other instanceof ImmutableMappings) {
            return (ImmutableMappings) other;
        } else if (other instanceof SimpleMappings) {
            return other.snapshot();
        } else {
            return create(
                    ImmutableMaps.createBiMap(other.classes(), other::getNewType),
                    ImmutableMaps.createBiMap(other.methods(), other::getNewMethod),
                    ImmutableMaps.createBiMap(other.fields(), other::getNewField)
            );
        }
    }

    @Override
    public void forEachClass(BiConsumer<JavaType, JavaType> action) {
        classes.forEach(action);
    }

    @Override
    public void forEachMethod(BiConsumer<MethodData, MethodData> action) {
        methods.forEach(action);
    }

    @Override
    public void forEachField(BiConsumer<FieldData, FieldData> action) {
        fields.forEach(action);
    }

    @Override
    public int hashCode() {
        return classes.hashCode() ^ methods.hashCode() ^ fields.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj.getClass() == ImmutableMappings.class) {
            return this.classes.equals(((ImmutableMappings) obj).classes)
                    && this.methods.equals(((ImmutableMappings) obj).methods)
                    && this.fields.equals(((ImmutableMappings) obj).fields);
        } else if (obj instanceof Mappings) {
            return this.equals(((Mappings) obj).snapshot());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Mappings.class)
                .add("classes", ImmutableMaps.joinToString(
                        classes,
                        (original, renamed) -> String.format("  %s = %s", original.getName(), renamed.getName()),
                        "\n", "{", "}"
                ))
                .add("methods", ImmutableMaps.joinToString(
                        methods,
                        (original, renamed) -> String.format("  %s = %s", original, renamed),
                        "\n", "{\n", "\n}"
                ))
                .add("fields", ImmutableMaps.joinToString(
                        fields,
                        (original, renamed) -> String.format("  %s = %s", original, renamed),
                        "\n", "{\n", "\n}"
                ))
                .toString();
    }
}
