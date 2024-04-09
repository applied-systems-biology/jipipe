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
import java.awt.*;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * See <a href="https://github.com/aterai/java-swing-tips/blob/master/DnDTabbedPane/src/java/example/MainPanel.java">...</a>
 */
public class JIPipeDesktopDnDTabbedPane extends JTabbedPane {
    private static final int LINE_SIZE = 3;
    private static final int RWH = 20;
    private static final int BUTTON_SIZE = 30; // XXX 30 is magic number of scroll button size

    private final JIPipeDesktopDnDTabbedPaneGhostGlassPane glassPane = new JIPipeDesktopDnDTabbedPaneGhostGlassPane(this);
    protected int dragTabIndex = -1;

    // For Debug: >>>
    protected boolean hasGhost = false;
    protected boolean isPaintScrollArea = false;
    // <<<

    protected Rectangle rectBackward = new Rectangle();
    protected Rectangle rectForward = new Rectangle();

    protected JIPipeDesktopDnDTabbedPane(int tabPlacement) {
        super(tabPlacement);
        glassPane.setName("GlassPane");
        new DropTarget(glassPane, DnDConstants.ACTION_COPY_OR_MOVE, new JIPipeDesktopDnDTabbedPaneTabDropTargetListener(), true);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_COPY_OR_MOVE, new JIPipeDesktopDnDTabbedPaneTabDragGestureListener());
    }

    public static boolean isTopBottomTabPlacement(int tabPlacement) {
        return tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM;
    }

    public void clickArrowButton(String actionKey) {
        JButton scrollForwardButton = null;
        JButton scrollBackwardButton = null;
        for (Component c : getComponents()) {
            if (c instanceof JButton) {
                if (Objects.isNull(scrollForwardButton)) {
                    scrollForwardButton = (JButton) c;
                } else if (Objects.isNull(scrollBackwardButton)) {
                    scrollBackwardButton = (JButton) c;
                }
            }
        }
        JButton button = "scrollTabsForwardAction".equals(actionKey) ? scrollForwardButton : scrollBackwardButton;
        Optional.ofNullable(button)
                .filter(JButton::isEnabled)
                .ifPresent(JButton::doClick);

        // // ArrayIndexOutOfBoundsException
        // Optional.ofNullable(getActionMap())
        //   .map(am -> am.get(actionKey))
        //   .filter(Action::isEnabled)
        //   .ifPresent(a -> a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, 0)));
        // // ActionMap map = getActionMap();
        // // if (Objects.nonNull(map)) {
        // //   Action action = map.get(actionKey);
        // //   if (Objects.nonNull(action) && action.isEnabled()) {
        // //     action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, 0));
        // //   }
        // // }
    }

    public void autoScrollTest(Point glassPt) {
        Rectangle r = getTabAreaBounds();
        if (isTopBottomTabPlacement(getTabPlacement())) {
            rectBackward.setBounds(r.x, r.y, RWH, r.height);
            rectForward.setBounds(r.x + r.width - RWH - BUTTON_SIZE, r.y, RWH + BUTTON_SIZE, r.height);
        } else {
            rectBackward.setBounds(r.x, r.y, r.width, RWH);
            rectForward.setBounds(r.x, r.y + r.height - RWH - BUTTON_SIZE, r.width, RWH + BUTTON_SIZE);
        }
        rectBackward = SwingUtilities.convertRectangle(getParent(), rectBackward, glassPane);
        rectForward = SwingUtilities.convertRectangle(getParent(), rectForward, glassPane);
        if (rectBackward.contains(glassPt)) {
            clickArrowButton("scrollTabsBackwardAction");
        } else if (rectForward.contains(glassPt)) {
            clickArrowButton("scrollTabsForwardAction");
        }
    }

    protected int getTargetTabIndex(Point glassPt) {
        Point tabPt = SwingUtilities.convertPoint(glassPane, glassPt, this);
        Point d = isTopBottomTabPlacement(getTabPlacement()) ? new Point(1, 0) : new Point(0, 1);
        return IntStream.range(0, getTabCount()).filter(i -> {
            Rectangle r = getBoundsAt(i);
            r.translate(-r.width * d.x / 2, -r.height * d.y / 2);
            return r.contains(tabPt);
        }).findFirst().orElseGet(() -> {
            int count = getTabCount();
            Rectangle r = getBoundsAt(count - 1);
            r.translate(r.width * d.x / 2, r.height * d.y / 2);
            return r.contains(tabPt) ? count : -1;
        });
//     for (int i = 0; i < getTabCount(); i++) {
//       Rectangle r = getBoundsAt(i);
//       r.translate(-r.width * d.x / 2, -r.height * d.y / 2);
//       if (r.contains(tabPt)) {
//         return i;
//       }
//     }
//     Rectangle r = getBoundsAt(getTabCount() - 1);
//     r.translate(r.width * d.x / 2, r.height * d.y / 2);
//     return r.contains(tabPt) ? getTabCount() : -1;
    }

    protected void convertTab(int prev, int next) {
        if (next < 0 || prev == next) {
            // This check is needed if tab content is null.
            return;
        }
        final Component cmp = getComponentAt(prev);
        final Component tab = getTabComponentAt(prev);
        final String title = getTitleAt(prev);
        final Icon icon = getIconAt(prev);
        final String tip = getToolTipTextAt(prev);
        final boolean isEnabled = isEnabledAt(prev);
        int tgtIndex = prev > next ? next : next - 1;
        remove(prev);
        insertTab(title, icon, cmp, tip, tgtIndex);
        setEnabledAt(tgtIndex, isEnabled);
        // When you drag'n'drop a disabled tab, it finishes enabled and selected.
        // pointed out by dlorde
        if (isEnabled) {
            setSelectedIndex(tgtIndex);
        }
        // I have a component in all tabs (JLabel with an X to close the tab) and when i move a tab the component disappear.
        // pointed out by Daniel Dario Morales Salas
        setTabComponentAt(tgtIndex, tab);
    }

    protected void initTargetLine(int next) {
        boolean isLeftOrRightNeighbor = next < 0 || dragTabIndex == next || next - dragTabIndex == 1;
        if (isLeftOrRightNeighbor) {
            glassPane.setTargetRect(0, 0, 0, 0);
            return;
        }
        Optional.ofNullable(getBoundsAt(Math.max(0, next - 1))).ifPresent(boundsRect -> {
            final Rectangle r = SwingUtilities.convertRectangle(this, boundsRect, glassPane);
            int a = Math.min(next, 1); // a = (next == 0) ? 0 : 1;
            if (isTopBottomTabPlacement(getTabPlacement())) {
                glassPane.setTargetRect(r.x + r.width * a - LINE_SIZE / 2, r.y, LINE_SIZE, r.height);
            } else {
                glassPane.setTargetRect(r.x, r.y + r.height * a - LINE_SIZE / 2, r.width, LINE_SIZE);
            }
        });
    }

    protected void initGlassPane(Point tabPt) {
        getRootPane().setGlassPane(glassPane);
        if (hasGhost) {
            Component c = Optional.ofNullable(getTabComponentAt(dragTabIndex))
                    .orElseGet(() -> new JLabel(getTitleAt(dragTabIndex)));
            Dimension d = c.getPreferredSize();
            BufferedImage image = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            SwingUtilities.paintComponent(g2, c, glassPane, 0, 0, d.width, d.height);
            g2.dispose();
            glassPane.setImage(image);
            // Rectangle rect = getBoundsAt(dragTabIndex);
            // BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            // Graphics2D g2 = image.createGraphics();
            // paint(g2);
            // g2.dispose();
            // if (rect.x < 0) {
            //   rect.translate(-rect.x, 0);
            // }
            // if (rect.y < 0) {
            //   rect.translate(0, -rect.y);
            // }
            // if (rect.x + rect.width > image.getWidth()) {
            //   rect.width = image.getWidth() - rect.x;
            // }
            // if (rect.y + rect.height > image.getHeight()) {
            //   rect.height = image.getHeight() - rect.y;
            // }
            // glassPane.setImage(image.getSubimage(rect.x, rect.y, rect.width, rect.height));
            // // rect.x = Math.max(0, rect.x); // rect.x < 0 ? 0 : rect.x;
            // // rect.y = Math.max(0, rect.y); // rect.y < 0 ? 0 : rect.y;
            // // image = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
            // // glassPane.setImage(image);
        }
        Point glassPt = SwingUtilities.convertPoint(this, tabPt, glassPane);
        glassPane.setPoint(glassPt);
        glassPane.setVisible(true);
    }

    protected Rectangle getTabAreaBounds() {
        Rectangle tabbedRect = getBounds();
        // XXX: Rectangle compRect = getSelectedComponent().getBounds();
        // pointed out by daryl. NullPointerException: i.e. addTab("Tab", null)
        // Component comp = getSelectedComponent();
        // int idx = 0;
        // while (Objects.isNull(comp) && idx < getTabCount()) {
        //   comp = getComponentAt(idx++);
        // }

        Rectangle compRect = Optional.ofNullable(getSelectedComponent())
                .map(Component::getBounds)
                .orElseGet(Rectangle::new);
        // // TEST:
        // Rectangle compRect = Optional.ofNullable(getSelectedComponent())
        //   .map(Component::getBounds)
        //   .orElseGet(() -> IntStream.range(0, getTabCount())
        //     .mapToObj(this::getComponentAt)
        //     .map(Component::getBounds)
        //     .findFirst()
        //     .orElseGet(Rectangle::new));
        int tabPlacement = getTabPlacement();
        if (isTopBottomTabPlacement(tabPlacement)) {
            tabbedRect.height = tabbedRect.height - compRect.height;
            if (tabPlacement == BOTTOM) {
                tabbedRect.y += compRect.y + compRect.height;
            }
        } else {
            tabbedRect.width = tabbedRect.width - compRect.width;
            if (tabPlacement == RIGHT) {
                tabbedRect.x += compRect.x + compRect.width;
            }
        }
        // if (tabPlacement == TOP) {
        //   tabbedRect.height = tabbedRect.height - compRect.height;
        // } else if (tabPlacement == BOTTOM) {
        //   tabbedRect.y = tabbedRect.y + compRect.y + compRect.height;
        //   tabbedRect.height = tabbedRect.height - compRect.height;
        // } else if (tabPlacement == LEFT) {
        //   tabbedRect.width = tabbedRect.width - compRect.width;
        // } else if (tabPlacement == RIGHT) {
        //   tabbedRect.x = tabbedRect.x + compRect.x + compRect.width;
        //   tabbedRect.width = tabbedRect.width - compRect.width;
        // }
        tabbedRect.grow(2, 2);
        return tabbedRect;
    }
}

