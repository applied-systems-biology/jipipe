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

package org.hkijena.jipipe.api.compat.ui;

import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathType;

public class FolderImageJDataImporterUI extends AbstractPathImageJDataImporterUI {
    /**
     * @param workbench the workbench
     * @param importer  the importer
     */
    public FolderImageJDataImporterUI(JIPipeWorkbench workbench, ImageJDataImportOperation importer) {
        super(workbench, importer);
    }

    @Override
    public PathType getPathType() {
        return PathType.DirectoriesOnly;
    }
}
