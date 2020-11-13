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

package org.hkijena.jipipe.extensions.filesystem.dataypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;

import java.nio.file.Path;

/**
 * Data that stores a folder
 */
@JIPipeDocumentation(name = "Folder")
public class FolderData extends PathData {

    /**
     * Initializes file data from a file
     *
     * @param path File path
     */
    public FolderData(Path path) {
        super(path);
    }

    private FolderData() {
    }

    public static FolderData importFrom(Path storageFilePath) {
        return new FolderData(PathData.importFrom(storageFilePath).getPath());
    }
}
