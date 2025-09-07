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
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link GraphInteractiveObjectUIContextAction} that simplifies handling of node/edge-specific actions
 */
public interface NodeAndEdgesUIContextAction extends GraphInteractiveObjectUIContextAction {
    @Override
    default boolean matches(Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        return matchesNodes(selection.stream().filter(ui -> ui instanceof JIPipeDesktopGraphNodeUI).map(ui -> (JIPipeDesktopGraphNodeUI)ui).collect(Collectors.toSet())) ||
                matchesEdges(selection.stream().filter(ui -> ui instanceof JIPipeDesktopGraphEdgeUI).map(ui -> (JIPipeDesktopGraphEdgeUI)ui).collect(Collectors.toSet()));
    }

    @Override
    default void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        runNodesAndEdges(canvasUI, selection.stream().filter(ui -> ui instanceof JIPipeDesktopGraphNodeUI).map(ui -> (JIPipeDesktopGraphNodeUI)ui).collect(Collectors.toSet()),
                selection.stream().filter(ui -> ui instanceof JIPipeDesktopGraphEdgeUI).map(ui -> (JIPipeDesktopGraphEdgeUI)ui).collect(Collectors.toSet()));
    }

    boolean matchesNodes(Set<JIPipeDesktopGraphNodeUI> selection);

    boolean matchesEdges(Set<JIPipeDesktopGraphEdgeUI> selection);

    void runNodesAndEdges(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> nodeSelection, Set<JIPipeDesktopGraphEdgeUI> edgeSelection);
}
