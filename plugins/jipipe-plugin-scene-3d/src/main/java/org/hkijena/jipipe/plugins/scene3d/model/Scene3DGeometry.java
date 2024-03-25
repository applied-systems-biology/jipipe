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

package org.hkijena.jipipe.plugins.scene3d.model;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.scene3d.model.geometries.Scene3DMeshGeometry;

import java.awt.*;

public interface Scene3DGeometry extends Scene3DNode {

    /**
     * Returns a mesh equivalent of this geometry
     *
     * @param progressInfo the progress info
     * @return the mesh geometry
     */
    Scene3DMeshGeometry toMeshGeometry(JIPipeProgressInfo progressInfo);

    /**
     * Gets the color of this geometry
     *
     * @return the color
     */
    Color getColor();

    /**
     * Sets the color of this geometry
     *
     * @param color the color
     */
    void setColor(Color color);

    @Override
    default void copyMetadataFrom(Scene3DNode other) {
        Scene3DNode.super.copyMetadataFrom(other);
        if (other instanceof Scene3DGeometry) {
            setColor(((Scene3DGeometry) other).getColor());
        }
    }
}
