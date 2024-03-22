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

package org.hkijena.jipipe.desktop.app.extensions;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopFlowLayout;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.ScrollableSizeHint;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class JIPipeDesktopExtensionListPanel extends JIPipeDesktopWorkbenchPanel {

    public static final int MIN_VISIBLE_ITEMS = 30;

    private final JIPipeDesktopModernPluginManagerUI pluginManagerUI;
    private final Deque<JIPipePlugin> infiniteScrollingQueue = new ArrayDeque<>();
    private List<JIPipePlugin> plugins;
    private JXPanel listPanel;
    private JScrollPane scrollPane;
    private final Timer scrollToBeginTimer = new Timer(200, e -> scrollToBeginning());

    public JIPipeDesktopExtensionListPanel(JIPipeDesktopModernPluginManagerUI pluginManagerUI) {
        super(pluginManagerUI.getDesktopWorkbench());
        this.pluginManagerUI = pluginManagerUI;
        this.scrollToBeginTimer.setRepeats(false);
        initialize();
    }

    private boolean isCoreDependency(JIPipeDependency dependency) {
        if (dependency instanceof JIPipeJavaPlugin)
            return ((JIPipeJavaPlugin) dependency).isCoreExtension();
        return false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        listPanel = new JXPanel(new JIPipeDesktopFlowLayout(FlowLayout.CENTER));
        listPanel.setScrollableWidthHint(ScrollableSizeHint.HORIZONTAL_STRETCH);
        listPanel.setScrollableHeightHint(ScrollableSizeHint.VERTICAL_STRETCH);
        scrollPane = new JScrollPane(listPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        add(scrollPane, BorderLayout.CENTER);
    }

    public List<JIPipePlugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<JIPipePlugin> plugins) {
        this.plugins = new ArrayList<>(plugins);
        this.plugins.sort(Comparator.comparing(this::isCoreDependency).thenComparing(dependency -> dependency.getMetadata().getName().toLowerCase(), NaturalOrderComparator.INSTANCE));
        listPanel.removeAll();
        revalidate();
        repaint();
        infiniteScrollingQueue.clear();
        for (JIPipePlugin plugin : this.plugins) {
            infiniteScrollingQueue.addLast(plugin);
        }
//        SwingUtilities.invokeLater(this::updateInfiniteScroll);
//        scrollToBeginTimer.restart();
        scrollToBeginning();
    }

    private void updateInfiniteScroll() {
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        if ((listPanel.getComponentCount() < MIN_VISIBLE_ITEMS || !scrollBar.isVisible() || (scrollBar.getValue() + scrollBar.getVisibleAmount()) > (scrollBar.getMaximum() - 100)) && !infiniteScrollingQueue.isEmpty()) {
            JIPipePlugin value = infiniteScrollingQueue.removeFirst();
            JIPipeDesktopExtensionItemPanel panel = new JIPipeDesktopExtensionItemPanel(pluginManagerUI, value);
            listPanel.add(panel);
            revalidate();
            repaint();
            SwingUtilities.invokeLater(this::updateInfiniteScroll);
        }
    }

    private void scrollToBeginning() {
        scrollPane.getVerticalScrollBar().setValue(0);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            updateInfiniteScroll();
        });
        SwingUtilities.invokeLater(this::updateInfiniteScroll);
    }
}
