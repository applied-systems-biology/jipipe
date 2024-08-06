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

package org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopImageFrameComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.utils.SizeFitMode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopPipelineQuickGuidePanel extends JIPipeDesktopWorkbenchPanel {

    private final JIPipePipelineGraphEditorUI graphEditorUI;

    public JIPipeDesktopPipelineQuickGuidePanel(JIPipeDesktopWorkbench desktopWorkbench, JIPipePipelineGraphEditorUI graphEditorUI) {
        super(desktopWorkbench);
        this.graphEditorUI = graphEditorUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        add(formPanel, BorderLayout.CENTER);

        formPanel.addWideToForm(new JIPipeDesktopImageFrameComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-create-nodes.png"), false, SizeFitMode.FitHeight, true));
    }
}
