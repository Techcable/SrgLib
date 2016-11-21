package net.techcable.srglib;

import static java.util.Objects.*;

/**
 * An array type
 */
/* package */ final class ArrayType implements JavaType {
    private final JavaType elementType;
    public ArrayType(JavaType elementType) {
        this.elementType = requireNonNull(elementType);
    }

    /**
     * Return the element type of this array.
     *
     * @return the element type
     */
    @Override
    public JavaType getElementType() {
        return elementType;
    }

    @Override
    public JavaTypeSort getSort() {
        return JavaTypeSort.ARRAY_TYPE;
    }

    @Override
    public String getInternalName() {
        return elementType.getInternalName() + "[]";
    }

    @Override
    public String getDescriptor() {
        return "[" + elementType.getDescriptor();
    }

    @Override
    public String getName() {
        return elementType.getName() + "[]";
    }

    private JavaType innermostType;
    private JavaType getInnermostType() {
        if (innermostType == null) {
            JavaType innermostType = this.elementType;
            while (innermostType instanceof ArrayType) {
                innermostType = ((ArrayType) innermostType).elementType;
            }
            this.innermostType = innermostType;
        }
        return innermostType;
    }

    @Override
    public int hashCode() {
        // Invert the bits to distribute it better
        return ~getInnermostType().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null
                && obj.getClass() == ArrayType.class
                && this.getInnermostType().equals(((ArrayType) obj).getInnermostType());
    }

    @Override
    public String toString() {
        return getName();
    }
}
