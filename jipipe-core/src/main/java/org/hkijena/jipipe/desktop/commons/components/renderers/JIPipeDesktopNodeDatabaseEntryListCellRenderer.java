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

package org.hkijena.jipipe.desktop.commons.components.renderers;

import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.api.nodes.database.CreateNewNodeByExampleDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Renderer for {@link JIPipeNodeDatabaseEntry} (supports only non-existing nodes fully)
 */
public class JIPipeDesktopNodeDatabaseEntryListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeDatabaseEntry> {

    private SolidColorIcon nodeColor;
    private JLabel nodeIcon;
    private JLabel nameLabel;
    private JLabel pathLabel;

    private JLabel alternativeLabel;

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopNodeDatabaseEntryListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        nodeColor = new SolidColorIcon(16, 50);
        nodeIcon = new JLabel();
        nameLabel = new JLabel();
        pathLabel = new JLabel();
        pathLabel.setForeground(Color.GRAY);
        alternativeLabel = new JLabel();
        alternativeLabel.setForeground(JIPipeDesktopModernMetalTheme.PRIMARY6);
        alternativeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));

        add(new JLabel(nodeColor), new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridheight = 3;
            }
        });
        add(nodeIcon, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                insets = new Insets(0, 4, 0, 4);
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(pathLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(alternativeLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 2;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeDatabaseEntry> list, JIPipeNodeDatabaseEntry obj, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (obj != null) {
            nameLabel.setText(obj.getName());
            nodeColor.setFillColor(obj.getFillColor());
            pathLabel.setText(obj.getLocationInfo().replace("\n", " > "));
            nodeIcon.setIcon(obj.getIcon());
        }

        if (obj instanceof CreateNewNodeByInfoDatabaseEntry) {
            JIPipeNodeInfo info = ((CreateNewNodeByInfoDatabaseEntry) obj).getNodeInfo();
            alternativeLabel.setForeground(JIPipeDesktopModernMetalTheme.PRIMARY6);
            if (info.getAliases().isEmpty()) {
                alternativeLabel.setText("");
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append("Alias: ");
                List<JIPipeNodeMenuLocation> alternativeMenuLocations = info.getAliases();
                for (int i = 0; i < alternativeMenuLocations.size(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    JIPipeNodeMenuLocation location = alternativeMenuLocations.get(i);
                    builder.append(location.getCategory().getName()).append(" > ").append(String.join(" > ", location.getMenuPath().split("\n")))
                            .append(" > ").append(StringUtils.orElse(location.getAlternativeName(), info.getName()));
                }
                alternativeLabel.setText(builder.toString());
            }
        } else if (obj instanceof CreateNewNodeByExampleDatabaseEntry) {
            alternativeLabel.setForeground(JIPipeDesktopModernMetalTheme.PRIMARY5);
            alternativeLabel.setText("Example");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
