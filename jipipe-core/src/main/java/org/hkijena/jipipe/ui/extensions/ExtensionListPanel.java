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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExtensionListPanel extends JIPipeWorkbenchPanel {

    private final List<JIPipeDependency> plugins;
    private JXPanel listPanel;

    public ExtensionListPanel(JIPipeWorkbench workbench, List<JIPipeDependency> plugins) {
        super(workbench);
        this.plugins = new ArrayList<>(plugins);
        this.plugins.sort(Comparator.comparing(this::isCoreDependency).thenComparing(dependency -> dependency.getMetadata().getName()));
        initialize();
        lazyLoad();
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
        JScrollPane scrollPane = new JScrollPane(listPanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void lazyLoad() {
        for (JIPipeDependency plugin : plugins) {
            ExtensionItemPanel itemPanel = new ExtensionItemPanel(getWorkbench(), plugin);
            listPanel.add(itemPanel);
        }
    }
}
