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

package org.hkijena.jipipe.desktop.commons.components.tabs;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.Optional;

public class JIPipeDesktopDnDTabbedPaneTabDropTargetListener implements DropTargetListener {
    private static final Point HIDDEN_POINT = new Point(0, -1000);

    private static Optional<JIPipeDesktopDnDTabbedPaneGhostGlassPane> getGhostGlassPane(Component c) {
        return Optional.ofNullable(c).filter(JIPipeDesktopDnDTabbedPaneGhostGlassPane.class::isInstance).map(JIPipeDesktopDnDTabbedPaneGhostGlassPane.class::cast);
    }

    @Override
    public void dragEnter(DropTargetDragEvent e) {
        getGhostGlassPane(e.getDropTargetContext().getComponent()).ifPresent(glassPane -> {
            // Only accept tab moves. A glass pane left visible from an
            // earlier drag must not hijack foreign drags (folders, node
            // templates, ...)
            Transferable t = e.getTransferable();
            if (t.isDataFlavorSupported(JIPipeDesktopDnDTabbedPaneTabTransferable.FLAVOR) && glassPane.tabbedPane.dragTabIndex >= 0) {
                e.acceptDrag(e.getDropAction());
            } else {
                e.rejectDrag();
            }
        });
    }

    @Override
    public void dragExit(DropTargetEvent e) {
        // Component c = e.getDropTargetContext().getComponent();
        // System.out.println("DropTargetListener#dragExit: " + c.getName());
        getGhostGlassPane(e.getDropTargetContext().getComponent()).ifPresent(glassPane -> {
            // XXX: glassPane.setVisible(false);
            glassPane.setPoint(HIDDEN_POINT);
            glassPane.setTargetRect(0, 0, 0, 0);
            glassPane.repaint();
        });
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent e) {
        /* not needed */
    }

    @Override
    public void dragOver(DropTargetDragEvent e) {
        Component c = e.getDropTargetContext().getComponent();
        getGhostGlassPane(c).ifPresent(glassPane -> {
            Point glassPt = e.getLocation();

            JIPipeDesktopDnDTabbedPane tabbedPane = glassPane.tabbedPane;
            tabbedPane.initTargetLine(tabbedPane.getTargetTabIndex(glassPt));
            tabbedPane.autoScrollTest(glassPt);

            glassPane.setPoint(glassPt);
            glassPane.repaint();
        });
    }

    @Override
    public void drop(DropTargetDropEvent e) {
        Component c = e.getDropTargetContext().getComponent();
        getGhostGlassPane(c).ifPresent(glassPane -> {
            JIPipeDesktopDnDTabbedPane tabbedPane = glassPane.tabbedPane;
            boolean completed = false;
            try {
                Transferable t = e.getTransferable();
                completed = tabbedPane.performDrop(t, e.getLocation());
            } finally {
                // Always end the drag: a glass pane left visible would hijack
                // every subsequent drag and drop operation in the window
                tabbedPane.finishTabDrag();
            }
            e.dropComplete(completed);
        });
    }
}
