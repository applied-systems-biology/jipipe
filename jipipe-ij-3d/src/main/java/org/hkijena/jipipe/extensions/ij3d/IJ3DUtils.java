/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ij3d;

import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DLabel;
import mcib3d.geom.Object3DSurface;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Voxel3D;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IJ3DUtils {
    /**
     * Duplicates an {@link Object3D}
     * @param other the object to copy
     * @return the copied object
     */
    public static Object3D duplicateObject3D(Object3D other) {
        // TODO: Handle other Object3D cases
        List<Voxel3D> voxels = new ArrayList<>();
        for (Voxel3D voxel : other.getVoxels()) {
            voxels.add(new Voxel3D(voxel.x, voxel.y, voxel.z, voxel.value));
        }
        Object3DVoxels result = new Object3DVoxels(voxels);
        result.setCalibration(other.getResXY(), other.getResZ(), other.getUnits());
        return result;
    }

}
