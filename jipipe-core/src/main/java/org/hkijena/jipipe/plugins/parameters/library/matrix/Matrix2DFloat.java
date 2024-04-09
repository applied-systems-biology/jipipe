/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.library.matrix;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.plugins.parameters.api.matrix.Matrix2D;

/**
 * Matrix containing {@link Float}
 */
@JsonSerialize(using = Matrix2D.Serializer.class)
@JsonDeserialize(using = Matrix2D.Deserializer.class)
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
