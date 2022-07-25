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

package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class AddBookmarkNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        for (JIPipeNodeUI ui : selection) {
            if (!ui.getNode().isBookmarked()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        for (JIPipeNodeUI ui : selection) {
            ui.getNode().setBookmarked(true);
            ui.getNode().getEventBus().post(new JIPipeParameterCollection.ParameterChangedEvent(ui.getNode(), "jipipe:node:bookmarked"));
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

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
