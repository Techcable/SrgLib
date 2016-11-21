package net.techcable.srglib;

import java.util.function.UnaryOperator;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.*;

/**
 * A method's declaring type, name, and parameter types, uniquely identifying it in a jar.
 */
public final class MethodData {
    private final JavaType declaringType;
    private final String name;
    private final MethodSignature signature;

    private MethodData(JavaType declaringType, String name, MethodSignature signature) {
        this.declaringType = requireNonNull(declaringType, "Null declaring type");
        this.name = requireNonNull(name, "Null name");
        this.signature = requireNonNull(signature, "Null method descriptor");
        if (!SrgLib.isValidIdentifier(name)) {
            throw new IllegalArgumentException("Invalid method name: " + name);
        }
    }

    /**
     * Return the type that declared this method.
     *
     * @return the declaring type
     */
    public JavaType getDeclaringType() {
        return declaringType;
    }

    /**
     * Return the name of this method
     *
     * @return the method name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the parameters types of this method.
     *
     * @return the parameter types.
     */
    public ImmutableList<JavaType> getParameterTypes() {
        return signature.getParameterTypes();
    }

    /**
     * Get the return type of this method.
     *
     * @return the return type
     */
    public JavaType getReturnType() {
        return signature.getReturnType();
    }

    /**
     * Return the internal name of this method.
     * <p>
     * Internal method names are in the format {@code ${internal type name}/${method name}}
     * </p>
     *
     * @return the internal name
     */
    public String getInternalName() {
        return declaringType.getInternalName() + "/" + name;
    }

    /**
     * Return the method's signature.
     *
     * @return the method's signature.
     */
    public MethodSignature getSignature() {
        return signature;
    }

    public MethodData withSignature(MethodSignature signature) {
        if (signature.equals(this.signature)) {
            return this;
        } else {
            return create(declaringType, name, signature);
        }
    }

    public MethodData mapSignature(UnaryOperator<JavaType> transformer) {
        return withSignature(signature.mapTypes(transformer));
    }

    public MethodData mapTypes(UnaryOperator<JavaType> transformer) {
        return mapSignature(transformer).withDeclaringType(transformer.apply(declaringType));
    }

    public boolean hasSameTypes(MethodData other) {
        requireNonNull(other, "Null other data!");
        return this.declaringType.equals(other.declaringType)
                && this.signature.equals(other.signature);
    }

    public MethodData withReturnType(JavaType returnType) {
        if (returnType.equals(this.getReturnType())) return this;
        return new MethodData(declaringType, name, signature);
    }

    public MethodData withName(String name) {
        if (name.equals(this.name)) return this;
        return new MethodData(declaringType, name, signature);
    }

    public MethodData withDeclaringType(JavaType declaringType) {
        if (declaringType.equals(this.declaringType)) return this;
        return new MethodData(declaringType, name, signature);
    }

    @Override
    public int hashCode() {
        return declaringType.hashCode() ^ name.hashCode() ^ signature.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other != null
                && other.getClass() == MethodData.class
                && this.name.equals(((MethodData) other).name)
                && this.hasSameTypes((MethodData) other);
    }

    @Override
    public String toString() {
        return this.declaringType.getName() +
                "." +
                name +
                signature.toString();
    }

    private static final JavaType[] EMPTY_TYPE_ARRAY = new JavaType[0];

    /**
     * Create a new method data object with the specified name and type.
     *
     * @param declaringType  the type that declared the method
     * @param name           the name of the method
     * @param parameterTypes the parameter types of this method
     * @param returnType     the return type of this method
     * @return the created method data
     */
    public static MethodData create(
            JavaType declaringType,
            String name,
            ImmutableList<JavaType> parameterTypes,
            JavaType returnType
    ) {
        return create(declaringType, name, MethodSignature.create(parameterTypes, returnType));
    }


    /**
     * Create a new method data object with the specified name and signature.
     *
     * @param declaringType the type that declared the method
     * @param name          the name of the method
     * @param signature     the method's signature.
     * @return the created method data
     */
    public static MethodData create(
            JavaType declaringType,
            String name,
            MethodSignature signature
    ) {
        return new MethodData(declaringType, name, signature);
    }

    public static MethodData fromInternalName(String joinedName, MethodSignature signature) {
        int index = joinedName.lastIndexOf('/');
        checkArgument(index >= 0 && index < joinedName.length() - 1, "Invalid internal name: %s", joinedName);
        JavaType declaringType = JavaType.fromInternalName(joinedName.substring(0, index));
        String name = joinedName.substring(index + 1);
        return create(declaringType, name, signature);
    }
}
