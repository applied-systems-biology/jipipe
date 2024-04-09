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

package org.hkijena.jipipe.desktop.commons.components.pickers;

import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopImageJDataExporterListCellRenderer;

import java.awt.*;
import java.util.Comparator;

public class JIPipeDesktopImageJDataExporterPicker extends JIPipeDesktopPickerDialog<ImageJDataExporter> {
    public JIPipeDesktopImageJDataExporterPicker(Window parent) {
        super(parent);
        setCellRenderer(new JIPipeDesktopImageJDataExporterListCellRenderer());
        setItemComparator(Comparator.comparing(ImageJDataExporter::getName));
    }

    @Override
    protected String getSearchString(ImageJDataExporter item) {
        return item.getName();
    }
}
