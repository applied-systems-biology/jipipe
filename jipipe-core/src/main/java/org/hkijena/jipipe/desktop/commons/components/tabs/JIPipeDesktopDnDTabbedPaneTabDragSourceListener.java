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

import java.awt.dnd.*;

public class JIPipeDesktopDnDTabbedPaneTabDragSourceListener implements DragSourceListener {
    @Override
    public void dragEnter(DragSourceDragEvent e) {
        e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
    }

    @Override
    public void dragExit(DragSourceEvent e) {
        e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
        // glassPane.setTargetRect(0, 0, 0, 0);
        // glassPane.setPoint(new Point(-1000, -1000));
        // glassPane.repaint();
    }

    @Override
    public void dragOver(DragSourceDragEvent e) {
        // Point glassPt = e.getLocation();
        // JComponent glassPane = (JComponent) e.getDragSourceContext();
        // SwingUtilities.convertPointFromScreen(glassPt, glassPane);
        // int targetIdx = getTargetTabIndex(glassPt);
        // if (getTabAreaBounds().contains(glassPt) && targetIdx >= 0 &&
        //     targetIdx != dragTabIndex && targetIdx != dragTabIndex + 1) {
        //   e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
        //   glassPane.setCursor(DragSource.DefaultMoveDrop);
        // } else {
        //   e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
        //   glassPane.setCursor(DragSource.DefaultMoveNoDrop);
        // }
    }

    @Override
    public void dragDropEnd(DragSourceDropEvent e) {
        // dragTabIndex = -1;
        // glassPane.setVisible(false);
    }

    @Override
    public void dropActionChanged(DragSourceDragEvent e) {
        /* not needed */
    }
}
