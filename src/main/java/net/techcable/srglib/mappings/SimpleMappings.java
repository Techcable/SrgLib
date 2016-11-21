package net.techcable.srglib.mappings;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;

import net.techcable.srglib.FieldData;
import net.techcable.srglib.JavaType;
import net.techcable.srglib.MethodData;

import static com.google.common.base.Preconditions.*;
import static java.util.Objects.*;

/* package */ class SimpleMappings implements MutableMappings {
    private final BiMap<JavaType, JavaType> classes;
    private final BiMap<MethodData, MethodData> methods;
    private final BiMap<FieldData, FieldData> fields;

    private SimpleMappings(
            BiMap<JavaType, JavaType> classes,
            BiMap<MethodData, MethodData> methods,
            BiMap<FieldData, FieldData> fields
    ) {
        this.classes = requireNonNull(classes, "Null types");
        this.methods = requireNonNull(methods, "Null methods");
        this.fields = requireNonNull(fields, "Null fields");
    }

    /* package */
    static MutableMappings create() {
        return create(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    /* package */
    static MutableMappings create(
            Map<JavaType, JavaType> initialClasses,
            Map<MethodData, MethodData> initialMethods,
            Map<FieldData, FieldData> initialFields
    ) {
        return new SimpleMappings(
                HashBiMap.create(initialClasses),
                HashBiMap.create(initialMethods),
                HashBiMap.create(initialFields)
        );
    }

    @Override
    public void putClass(JavaType original, JavaType renamed) {
        checkArgument(original.isReferenceType(), "Original type isn't a reference type: %s", original);
        checkArgument(renamed.isReferenceType(), "Renamed type isn't a reference type: %s", renamed);
        if (original.equals(renamed)) {
            classes.remove(original);
        } else {
            classes.put(original, renamed);
        }
    }

    @Override
    public void putMethod(MethodData original, MethodData renamed) {
        checkArgument(
                original.mapTypes(this::getNewType).hasSameTypes(renamed),
                "Remapped method data types (%s) don't correspond to original types (%s)",
                renamed,
                original
        );
        methods.put(original, renamed);
    }

    @Override
    public void putField(FieldData original, FieldData renamed) {
        checkArgument(
                original.mapTypes(this::getNewType).hasSameTypes(renamed),
                "Remapped field data (%s) doesn't correspond to original types (%s)",
                renamed,
                original
        );
        fields.put(original, renamed);
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
    public ImmutableMappings snapshot() {
        return ImmutableMappings.create(
                ImmutableBiMap.copyOf(this.classes),
                ImmutableBiMap.copyOf(this.methods),
                ImmutableBiMap.copyOf(this.fields)
        );
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
    public MutableMappings inverted() {
        return new SimpleMappings(classes.inverse(), methods.inverse(), fields.inverse());
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
    public boolean equals(Object otherObj) {
        if (this == otherObj) return true;
        if (otherObj == null) return false;
        if (otherObj.getClass() == SimpleMappings.class) {
            SimpleMappings other = (SimpleMappings) otherObj;
            return classes.equals(other.classes) && methods.equals(other.methods) && fields.equals(other.fields);
        } else if (otherObj instanceof Mappings) {
            return this.snapshot().equals(((Mappings) otherObj).snapshot());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = classes.hashCode();
        result = 31 * result + methods.hashCode();
        result = 31 * result + fields.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return snapshot().toString();
    }
}
