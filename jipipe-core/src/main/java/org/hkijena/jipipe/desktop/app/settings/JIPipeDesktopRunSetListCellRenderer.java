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

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectRunSetsConfiguration;
import org.hkijena.jipipe.api.run.JIPipeProjectRunSet;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartitionConfiguration;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopRunSetListCellRenderer extends JPanel implements ListCellRenderer<JIPipeProjectRunSet> {

    private final JIPipeProject project;
    private final JIPipeProjectRunSetsConfiguration runSetsConfiguration;
    private final SolidColorIcon colorIcon = new SolidColorIcon(12, 44, Color.BLACK, Color.DARK_GRAY);
    private final JLabel nameLabel = new JLabel();
    private final JPanel indicatorPanel = new JPanel();

    public JIPipeDesktopRunSetListCellRenderer(JIPipeProject project, JIPipeProjectRunSetsConfiguration runSetsConfiguration) {
        this.project = project;
        this.runSetsConfiguration = runSetsConfiguration;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        Insets insets = new Insets(4, 4, 4, 4);
        add(new JLabel(colorIcon), new GridBagConstraints(0,
                0,
                1,
                2,
                0,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE,
                insets,
                0,
                0));
        nameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        add(nameLabel, new GridBagConstraints(1,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                insets,
                0,
                0));

        indicatorPanel.setOpaque(false);
        indicatorPanel.setLayout(new BoxLayout(indicatorPanel, BoxLayout.X_AXIS));
        add(indicatorPanel, new GridBagConstraints(1,
                1,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                insets,
                0,
                0));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeProjectRunSet> list, JIPipeProjectRunSet value, int index, boolean isSelected, boolean cellHasFocus) {
        setFont(list.getFont());
        indicatorPanel.removeAll();
        if (value != null) {
            nameLabel.setText(StringUtils.orElse(value.getName(), "Unnamed"));
            colorIcon.setFillColor(value.getColor().isEnabled() ? value.getColor().getContent() : Color.WHITE);

            indicatorPanel.add(new JLabel(StringUtils.formatPluralS(value.getNodes().size(), "node"), UIUtils.getIconFromResources("actions/graph-node.png"), JLabel.LEFT));
            if(!value.canResolveAllNodes(project)) {
                indicatorPanel.add(Box.createHorizontalStrut(8));
                indicatorPanel.add(new JLabel("Issues detected", UIUtils.getIconFromResources("emblems/warning.png"), JLabel.LEFT));
            }
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeProjectRunSetsConfiguration getRunSetsConfiguration() {
        return runSetsConfiguration;
    }
}
