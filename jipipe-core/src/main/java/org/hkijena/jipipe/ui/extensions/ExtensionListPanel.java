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
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.layouts.ModifiedFlowLayout;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.ScrollableSizeHint;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class ExtensionListPanel extends JIPipeWorkbenchPanel {

    public static final Paint GRADIENT_TOP = new LinearGradientPaint(0,
            0,
            0,
            64,
            new float[]{0f,1f},
            new Color[]{UIManager.getColor("Panel.background"), ColorUtils.setAlpha(UIManager.getColor("Panel.background"), 0)});
    public static final Paint GRADIENT_BOTTOM = new LinearGradientPaint(0,
            0,
            0,
            64,
            new float[]{0f,1f},
            new Color[]{ColorUtils.setAlpha(UIManager.getColor("Panel.background"), 0), UIManager.getColor("Panel.background")});

    private final JIPipeModernPluginManagerUI pluginManagerUI;
    private List<JIPipeExtension> plugins;
    private JXPanel listPanel;

    private JScrollPane scrollPane;

    private final Timer scrollToBeginTimer = new Timer(200, e -> scrollToBeginning());

    private final Deque<JIPipeExtension> infiniteScrollingQueue = new ArrayDeque<>();

    public ExtensionListPanel(JIPipeModernPluginManagerUI pluginManagerUI) {
        super(pluginManagerUI.getWorkbench());
        this.pluginManagerUI = pluginManagerUI;
        this.scrollToBeginTimer.setRepeats(false);
        initialize();
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
        add(scrollPane, BorderLayout.CENTER);
    }

    public List<JIPipeExtension> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<JIPipeExtension> plugins) {
        this.plugins = new ArrayList<>(plugins);
        this.plugins.sort(Comparator.comparing(this::isCoreDependency).thenComparing(dependency -> dependency.getMetadata().getName().toLowerCase(), NaturalOrderComparator.INSTANCE));
        listPanel.removeAll();
        revalidate();
        repaint();
        infiniteScrollingQueue.clear();
        for (JIPipeExtension plugin : this.plugins) {
            infiniteScrollingQueue.addLast(plugin);
        }
//        SwingUtilities.invokeLater(this::updateInfiniteScroll);
//        scrollToBeginTimer.restart();
        scrollToBeginning();
    }

    private void updateInfiniteScroll() {
        JScrollBar scrollBar =scrollPane.getVerticalScrollBar();
        if ((!scrollBar.isVisible() || (scrollBar.getValue() + scrollBar.getVisibleAmount()) > (scrollBar.getMaximum() - 100)) && !infiniteScrollingQueue.isEmpty()) {
            JIPipeExtension value = infiniteScrollingQueue.removeFirst();
            ExtensionItemPanel panel = new ExtensionItemPanel(pluginManagerUI, value);
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
