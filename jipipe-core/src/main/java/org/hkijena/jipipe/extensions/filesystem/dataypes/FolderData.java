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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeFastThumbnail;

import java.nio.file.Path;

/**
 * Data that stores a folder
 */
@JIPipeDocumentation(name = "Folder", description = "A path to a directory")
@JIPipeFastThumbnail
public class FolderData extends PathData {

    public FolderData(Path path) {
        super(path);
    }

    public FolderData(String path) {
        super(path);
    }

    public static FolderData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new FolderData(PathData.importData(storage, progressInfo).getPath());
    }
}
