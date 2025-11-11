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

package org.hkijena.jipipe.plugins.nodetemplate;

import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.desktop.commons.components.pickers.JIPipeDesktopPickerDialog;

import java.awt.*;

public class JIPipeNodeTemplatePickerDialog extends JIPipeDesktopPickerDialog<JIPipeNodeTemplate> {

    public JIPipeNodeTemplatePickerDialog(Window parent) {
        super(parent);
        setCellRenderer(new JIPipeNodeTemplateSimpleListCellRenderer());
    }

    @Override
    protected String getSearchString(JIPipeNodeTemplate item) {
        return item.getName() + item.getDescription().getBody();
    }
}
