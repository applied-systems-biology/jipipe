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

import javax.swing.*;
import javax.swing.plaf.metal.MetalTabbedPaneUI;
import java.awt.*;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.InvalidDnDOperationException;
import java.util.Optional;

public class JIPipeDesktopDnDTabbedPaneTabDragGestureListener implements DragGestureListener {
    @Override
    public void dragGestureRecognized(DragGestureEvent e) {
        Optional.ofNullable(e.getComponent())
                .filter(c -> c instanceof JIPipeDesktopDnDTabbedPane).map(c -> (JIPipeDesktopDnDTabbedPane) c)
                .filter(tabbedPane -> tabbedPane.getTabCount() > 1)
                .ifPresent(tabbedPane -> {
                    Point tabPt = e.getDragOrigin();
                    int idx = tabbedPane.indexAtLocation(tabPt.x, tabPt.y);
                    int selIdx = tabbedPane.getSelectedIndex();
                    // When a tab runs rotation occurs, a tab that is not the target is dragged.
                    // pointed out by Arjen
                    boolean isTabRunsRotated = !(tabbedPane.getUI() instanceof MetalTabbedPaneUI)
                            && tabbedPane.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT
                            && idx != selIdx;
                    tabbedPane.dragTabIndex = isTabRunsRotated ? selIdx : idx;
                    if (tabbedPane.dragTabIndex >= 0 && tabbedPane.isEnabledAt(tabbedPane.dragTabIndex)) {
                        tabbedPane.initGlassPane(tabPt);
                        try {
                            e.startDrag(DragSource.DefaultMoveDrop, new JIPipeDesktopDnDTabbedPaneTabTransferable(tabbedPane), new JIPipeDesktopDnDTabbedPaneTabDragSourceListener());
                        } catch (InvalidDnDOperationException ex) {
                            throw new IllegalStateException(ex);
                        }
                    }
                });
    }
}
