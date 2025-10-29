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

package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.contextpanel.JIPipeDesktopGraphEditorContextPanelIsland;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopGraphEditorToolPanel<T extends JIPipeDesktopToggleableGraphEditorTool> extends JIPipeDesktopGraphEditorContextPanelIsland {

    private final T tool;

    public JIPipeDesktopGraphEditorToolPanel(JIPipeDesktopGraphEditorUI graphEditorUI, T tool) {
       super(graphEditorUI);
        this.tool = tool;
    }

    public T getTool() {
        return tool;
    }

    @Override
    protected Icon getTitleIcon() {
        return tool.getIcon();
    }

    @Override
    protected String getTitle() {
        return tool.getName();
    }
}
