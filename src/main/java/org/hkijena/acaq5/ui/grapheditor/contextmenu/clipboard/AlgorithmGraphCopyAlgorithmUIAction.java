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

package org.hkijena.acaq5.ui.grapheditor.contextmenu.clipboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.AlgorithmUIAction;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Set;
import java.util.stream.Collectors;

public class AlgorithmGraphCopyAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQAlgorithmUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(ACAQAlgorithmGraphCanvasUI canvasUI, Set<ACAQAlgorithmUI> selection) {
        ACAQGraph graph = canvasUI.getAlgorithmGraph()
                .extract(selection.stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet()), true);
        try {
            String json = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(graph);
            StringSelection stringSelection = new StringSelection(json);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);
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
        return UIUtils.getIconFromResources("copy.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }
}
