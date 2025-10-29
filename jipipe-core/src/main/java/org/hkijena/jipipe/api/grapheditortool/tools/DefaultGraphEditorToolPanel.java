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

package org.hkijena.jipipe.api.grapheditortool.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopFormGraphEditorToolPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.plugins.graphannotation.tools.EditAnnotationGraphNodeTool;
import org.hkijena.jipipe.utils.UIUtils;

public class DefaultGraphEditorToolPanel extends JIPipeDesktopFormGraphEditorToolPanel<DefaultGraphEditorTool> {
    public DefaultGraphEditorToolPanel(JIPipeDesktopGraphEditorUI graphEditorUI, DefaultGraphEditorTool tool) {
        super(graphEditorUI, tool);
    }

    @Override
    public void initializeContent() {
        super.initializeContent();

        getFormPanel().addWideToForm(UIUtils.createLeftAlignedButton("Select all", JIPipe.RESOURCES.getIcon16("actions/stock_select-all.png"), () -> {
            getGraphEditorUI().getSelectionManager().selectAll();
        }));
        getFormPanel().addWideToForm(UIUtils.createLeftAlignedButton("Edit annotations", JIPipe.RESOURCES.getIcon16("actions/pencil.png"), () -> {
            getGraphEditorUI().selectToolByClass(EditAnnotationGraphNodeTool.class);
        }));
    }
}
