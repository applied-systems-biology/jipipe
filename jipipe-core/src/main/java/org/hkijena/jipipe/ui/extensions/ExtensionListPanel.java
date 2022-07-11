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
 *
 */

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.layouts.ModifiedFlowLayout;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.ScrollableSizeHint;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class ExtensionListPanel extends JIPipeWorkbenchPanel {

    private List<JIPipeDependency> plugins;
    private JXPanel listPanel;

    private JScrollPane scrollPane;

    private final Timer scrollToBeginTimer = new Timer(200, e -> scrollToBeginning());

    private final Deque<JIPipeDependency> infiniteScrollingQueue = new ArrayDeque<>();

    public ExtensionListPanel(JIPipeWorkbench workbench, List<JIPipeDependency> plugins) {
        super(workbench);
        this.scrollToBeginTimer.setRepeats(false);
        initialize();
        setPlugins(plugins);
    }

    private boolean isCoreDependency(JIPipeDependency dependency) {
        if(dependency instanceof JIPipeJavaExtension)
            return ((JIPipeJavaExtension) dependency).isCoreExtension();
        return false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        listPanel = new JXPanel(new ModifiedFlowLayout(FlowLayout.CENTER));
        listPanel.setScrollableWidthHint(ScrollableSizeHint.HORIZONTAL_STRETCH);
        listPanel.setScrollableHeightHint(ScrollableSizeHint.VERTICAL_STRETCH);
        scrollPane = new JScrollPane(listPanel);
//        scrollPane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
//        scrollPane.getVerticalScrollBar().setUnitIncrement(1);
        add(scrollPane, BorderLayout.CENTER);
    }

    public List<JIPipeDependency> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<JIPipeDependency> plugins) {
        this.plugins = new ArrayList<>(plugins);
        this.plugins.sort(Comparator.comparing(this::isCoreDependency).thenComparing(dependency -> dependency.getMetadata().getName()));
        listPanel.removeAll();
        revalidate();
        repaint();
        infiniteScrollingQueue.clear();
        for (JIPipeDependency plugin : this.plugins) {
            infiniteScrollingQueue.addLast(plugin);
        }
        SwingUtilities.invokeLater(this::updateInfiniteScroll);
        scrollToBeginTimer.restart();
    }

    private void updateInfiniteScroll() {
        JScrollBar scrollBar =scrollPane.getVerticalScrollBar();
        if ((!scrollBar.isVisible() || (scrollBar.getValue() + scrollBar.getVisibleAmount()) > (scrollBar.getMaximum() - 32)) && !infiniteScrollingQueue.isEmpty()) {
            JIPipeDependency value = infiniteScrollingQueue.removeFirst();
            ExtensionItemPanel panel = new ExtensionItemPanel(getWorkbench(), value);
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
    }
}
