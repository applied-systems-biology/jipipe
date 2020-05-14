package org.hkijena.acaq5.extensions.parameters.collections;

/**
 * Matrix containing {@link Float}
 */
public class Matrix2DFloatParameter extends Matrix2DParameter<Float> {

    /**
     * Creates a new object
     */
    public Matrix2DFloatParameter() {
        super(Float.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Matrix2DFloatParameter(Matrix2DFloatParameter other) {
        super(other);
    }

    @Override
    protected Float createNewEntry() {
        return 0f;
    }
}
