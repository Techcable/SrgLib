package net.techcable.srglib;

import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.*;
import static java.util.Objects.*;

/**
 * A field's name and declaring type.
 */
public final class FieldData {
    private final JavaType declaringType;
    private final String name;

    private FieldData(JavaType declaringType, String name) {
        this.declaringType = requireNonNull(declaringType, "Null declaring type");
        this.name = requireNonNull(name, "Null name");
        checkArgument(SrgLib.isValidIdentifier(name), "Invalid name: %s", name);
    }

    /**
     * Return the declaring type of this field
     *
     * @return the declaring type
     */
    public JavaType getDeclaringType() {
        return declaringType;
    }

    /**
     * Return the name of this field
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the internal name of this field.
     * <p>
     * Internal method names are in the format ${internal type name}/${field name}.
     * </p>
     *
     * @return the internal name
     */
    public String getInternalName() {
        return this.declaringType.getInternalName() + "/" + this.name;
    }

    public FieldData withName(String name) {
        return new FieldData(declaringType, name);
    }

    public boolean hasSameTypes(FieldData other) {
        requireNonNull(other, "Null other data!");
        return this.declaringType.equals(other.declaringType);
    }

    public FieldData withDeclaringType(JavaType declaringType) {
        return new FieldData(declaringType, name);
    }

    public FieldData mapTypes(UnaryOperator<JavaType> transformer) {
        return new FieldData(transformer.apply(declaringType), name);
    }


    public static FieldData create(JavaType declaringType, String name) {
        return new FieldData(declaringType, name);
    }


    @Override
    public int hashCode() {
        return declaringType.hashCode() ^ name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null
                && obj.getClass() == FieldData.class
                && ((FieldData) obj).declaringType.equals(this.declaringType)
                && ((FieldData) obj).name.equals(this.name);
    }

    @Override
    public String toString() {
        return getInternalName();
    }

    public static FieldData fromInternalName(String internalName) {
        int index = internalName.lastIndexOf('/');
        checkArgument(index >= 0 && index < internalName.length() - 1, "Invalid internal name: %s", internalName);
        JavaType declaringType = JavaType.fromInternalName(internalName.substring(0, index));
        String name = internalName.substring(index + 1);
        return create(declaringType, name);
    }
}
