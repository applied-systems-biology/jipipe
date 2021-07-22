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

package org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.stream.Collectors;

public class AlgorithmGraphCopyNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        JIPipeGraph graph = canvasUI.getGraph()
                .extract(selection.stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet()), true);
        try {
            String json = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(graph);
            StringSelection stringSelection = new StringSelection(json);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);
            canvasUI.getWorkbench().sendStatusBarText("Copied " + selection.size() + " nodes");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "Copy";
    }

    @Override
    public String getDescription() {
        return "Copies the selection to the clipboard";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-copy.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK, true);
    }
}
