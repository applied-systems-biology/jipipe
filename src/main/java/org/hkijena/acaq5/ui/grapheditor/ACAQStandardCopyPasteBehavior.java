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

package org.hkijena.acaq5.ui.grapheditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.extensionbuilder.ACAQJsonExtensionAlgorithmGraphUI;
import org.hkijena.acaq5.utils.JsonUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Map;
import java.util.Set;

/**
 * Implements copy & paste behavior for {@link ACAQAlgorithmGraphCompartmentUI} and {@link ACAQJsonExtensionAlgorithmGraphUI}
 */
public class ACAQStandardCopyPasteBehavior implements ACAQAlgorithmGraphCopyPasteBehavior {
    private ACAQAlgorithmGraphEditorUI editorUI;

    /**
     * Creates a new instance
     *
     * @param editorUI the graph editor
     */
    public ACAQStandardCopyPasteBehavior(ACAQAlgorithmGraphEditorUI editorUI) {
        this.editorUI = editorUI;
    }

    @Override
    public void copy(Set<ACAQGraphNode> algorithms) {
        ACAQAlgorithmGraph graph = editorUI.getAlgorithmGraph().extract(algorithms, true);
        try {
            String json = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(graph);
            StringSelection selection = new StringSelection(json);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cut(Set<ACAQGraphNode> algorithms) {
        copy(algorithms);
        for (ACAQGraphNode algorithm : ImmutableSet.copyOf(algorithms)) {
            editorUI.getAlgorithmGraph().removeNode(algorithm);
        }
    }

    @Override
    public void paste() {
        try {
            String json = getStringFromClipboard();
            if (json != null) {
                ACAQAlgorithmGraph graph = JsonUtils.getObjectMapper().readValue(json, ACAQAlgorithmGraph.class);

                // Replace project compartment with IOInterface
                for (ACAQGraphNode node : graph.getAlgorithmNodes().values()) {
                    if (node instanceof ACAQCompartmentOutput) {
                        node.setDeclaration(ACAQAlgorithmRegistry.getInstance().getDeclarationById("io-interface"));
                    }
                }

                String compartment = editorUI.getCompartment();
                for (ACAQGraphNode algorithm : graph.getAlgorithmNodes().values()) {
                    algorithm.setCompartment(compartment);
                }
                Map<String, ACAQGraphNode> idMap = editorUI.getAlgorithmGraph().mergeWith(graph);

                // Try to move copied algorithm to the mouse position
                if (idMap.size() == 1) {
                    editorUI.getCanvasUI().tryMoveNodeToMouse(idMap.values().iterator().next());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getStringFromClipboard() {
        String ret = "";
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();

        Transferable clipTf = sysClip.getContents(null);

        if (clipTf != null) {

            if (clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    ret = (String) clipTf
                            .getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }
}
