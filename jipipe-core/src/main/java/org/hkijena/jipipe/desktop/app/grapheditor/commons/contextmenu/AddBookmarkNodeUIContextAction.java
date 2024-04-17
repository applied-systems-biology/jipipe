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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class AddBookmarkNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            if (!ui.getNode().isBookmarked()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            ui.getNode().setBookmarked(true);
            ui.getNode().emitParameterChangedEvent("jipipe:node:bookmarked");
        }
    }

    @Override
    public String getName() {
        return "Bookmark";
    }

    @Override
    public String getDescription() {
        return "Add the selected nodes to the bookmark list";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/bookmark-new.png");
    }

}