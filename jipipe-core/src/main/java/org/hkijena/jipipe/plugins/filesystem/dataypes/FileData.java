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

package org.hkijena.jipipe.plugins.filesystem.dataypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeFastThumbnail;

import java.nio.file.Path;

/**
 * Data containing a file
 */
@SetJIPipeDocumentation(name = "File", description = "A path to a file")
@JIPipeFastThumbnail
public class FileData extends PathData {

    public FileData(Path path) {
        super(path);
    }

    public FileData(String path) {
        super(path);
    }

    public static FileData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new FileData(PathData.importData(storage, progressInfo).getPath());
    }
}
