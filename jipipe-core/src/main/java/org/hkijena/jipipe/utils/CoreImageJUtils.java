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

package org.hkijena.jipipe.utils;

import ij.IJ;
import net.imagej.updater.FilesCollection;
import org.scijava.util.AppUtils;

import java.io.File;
import java.nio.file.Path;

public class CoreImageJUtils {
    public static Path getImageJUpdaterRoot() {
        String imagejDirProperty = System.getProperty("imagej.dir");
        final File imagejRoot = imagejDirProperty != null ? new File(imagejDirProperty) :
                AppUtils.getBaseDirectory("ij.dir", FilesCollection.class, "updater");
        return imagejRoot.toPath();
    }

    public static boolean supportsNativeImport(Path path) {
        String fileNameString = path.getFileName().toString().toLowerCase();
        // tiff, dicom, fits, pgm, jpeg, bmp, gif
        if(fileNameString.endsWith(".tiff") || fileNameString.endsWith(".tif")) {
            return !fileNameString.endsWith(".ome.tif") && !fileNameString.endsWith(".ome.tiff");
        }
        if(fileNameString.endsWith(".dcm")) {
            return true;
        }
        if(fileNameString.endsWith(".pgm")) {
            return true;
        }
        if(fileNameString.endsWith(".jpg") || fileNameString.endsWith(".jpeg")) {
            return true;
        }
        if(fileNameString.endsWith(".bmp")) {
            return true;
        }
        if(fileNameString.endsWith(".gif")) {
            return true;
        }

        return false;
    }
}
