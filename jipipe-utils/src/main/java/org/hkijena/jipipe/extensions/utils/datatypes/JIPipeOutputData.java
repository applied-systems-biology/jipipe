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

package org.hkijena.jipipe.extensions.utils.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;

import java.nio.file.Path;

@JIPipeDocumentation(name = "JIPipe output", description = "Output of a JIPipe run")
public class JIPipeOutputData extends FolderData {
    /**
     * Initializes file data from a file
     *
     * @param path File path
     */
    public JIPipeOutputData(Path path) {
        super(path);
    }

    public static JIPipeOutputData importFrom(Path folder) {
        return new JIPipeOutputData(FolderData.importFrom(folder).getPath());
    }
}
