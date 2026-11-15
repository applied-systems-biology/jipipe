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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasNotificationsManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.EdgesOnlyUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.util.Set;

public class CopyEdgeUIContextAction implements EdgesOnlyUIContextAction {

    /**
     * The id of the shortcut action that triggers this context action
     * (see {@link org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions}).
     */
    public static final String SHORTCUT_ACTION_ID = JIPipeShortcutActions.EDGE_COPY.id();

    @Override
    public String getName() {
        return "Copy edges";
    }

    @Override
    public String getDescription() {
        return "Copies the selected edges to the clipboard";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/edit-copy.png");
    }

    @Override
    public String getShortcutActionId() {
        return SHORTCUT_ACTION_ID;
    }

    @Override
    public boolean showInMultiSelectionPanel() {
        return false;
    }

    @Override
    public boolean matchesEdges(Set<JIPipeDesktopGraphEdgeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void runEdges(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphEdgeUI> selection) {
        try {
            String json = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(selection.stream().map(JIPipeDesktopGraphEdgeUI::toConnection).toList());
            UIUtils.copyToClipboard(json);
            canvasUI.getNotificationsManager().addNotification("Copied " + selection.size() + " edges",
                    getIcon(), JIPipeDesktopGraphCanvasNotificationsManager.NotificationType.Info);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
