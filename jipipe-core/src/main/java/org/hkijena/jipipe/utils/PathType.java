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

package org.hkijena.jipipe.utils;

import javax.swing.*;

/**
 * Determines the type of path
 */
public enum PathType {
    FilesOnly(JFileChooser.FILES_ONLY),
    DirectoriesOnly(JFileChooser.DIRECTORIES_ONLY),
    FilesAndDirectories(JFileChooser.FILES_AND_DIRECTORIES);


    private final int nativeValue;

    PathType(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    @Override
    public String toString() {
        return switch (this) {
            case FilesOnly -> "Only files";
            case DirectoriesOnly -> "Only directories";
            case FilesAndDirectories -> "Files or directories";
        };
    }

    /**
     * Returns the native value that corresponds to {@link JFileChooser}
     * @return the native value
     */
    public int getNativeValue() {
        return nativeValue;
    }
}
