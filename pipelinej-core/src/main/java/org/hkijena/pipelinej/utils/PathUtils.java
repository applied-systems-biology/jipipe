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
 */

package org.hkijena.pipelinej.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utilities for handling paths
 */
public class PathUtils {
    private PathUtils() {

    }

    /**
     * Finds a file in the specified folder with given extension
     *
     * @param folder    the path
     * @param extension Should contain the dot
     * @return null if no file was found
     */
    public static Path findFileByExtensionIn(Path folder, String extension) {
        try {
            return Files.list(folder).filter(p -> Files.isRegularFile(p) && p.toString().endsWith(extension)).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
