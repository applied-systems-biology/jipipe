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

package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeAsyncResultDataPlotPreviewUI;

import javax.swing.*;
import java.nio.file.Path;

public class ROIDataPreviewUI extends JIPipeAsyncResultDataPlotPreviewUI {
    /**
     * Creates a new renderer
     *
     * @param table the table where the data is rendered in
     */
    public ROIDataPreviewUI(JTable table) {
        super(table);
    }

    @Override
    protected JIPipeData loadData(Path storageFolder) {
        return new ROIListData(storageFolder);
    }
}
