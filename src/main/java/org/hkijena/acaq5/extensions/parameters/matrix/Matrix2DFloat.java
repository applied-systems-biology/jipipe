package org.hkijena.acaq5.extensions.parameters.matrix;

/**
 * Matrix containing {@link Float}
 */
public class Matrix2DFloat extends Matrix2D<Float> {

    /**
     * Creates a new object
     */
    public Matrix2DFloat() {
        super(Float.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Matrix2DFloat(Matrix2DFloat other) {
        super(other);
    }

    @Override
    protected Float createNewEntry() {
        return 0f;
    }
}
