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

package org.hkijena.jipipe.plugins.graphannotation.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopFormGraphEditorToolPanel;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopToggleableGraphEditorTool;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

/**
 * Base properties usable by all tools that create {@link org.hkijena.jipipe.plugins.graphannotation.nodes.TextBoxAnnotationGraphNode}
 */
public class BaseTextBoxAnnotationGraphNodeToolProperties<T extends JIPipeDesktopToggleableGraphEditorTool> extends JIPipeDesktopFormGraphEditorToolPanel<T> {
    public BaseTextBoxAnnotationGraphNodeToolProperties(JIPipeDesktopGraphEditorUI graphEditorUI, T tool) {
        super(graphEditorUI, tool);
    }

    @Override
    public final void initializeContent() {
        super.initializeContent();

        initializeColorPaletteContent();
        initializeAdditionalContent();

        getFormPanel().addWideToForm(UIUtils.createLeftAlignedButton("Close tool", JIPipe.RESOURCES.getIcon16("actions/message-close.png"), () -> {
            getGraphEditorUI().selectTool(null);
        }));
    }

    protected void initializeAdditionalContent() {

    }

    private void initializeColorPaletteContent() {

    }
}
