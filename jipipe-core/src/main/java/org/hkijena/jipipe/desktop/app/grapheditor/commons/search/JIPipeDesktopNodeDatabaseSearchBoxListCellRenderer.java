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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.search;

import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.api.nodes.database.CreateNewNodeByExampleDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidJIPipeDesktopColorIcon;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class JIPipeDesktopNodeDatabaseSearchBoxListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeDatabaseEntry> {

    public static final Color COLOR_CREATE = new Color(0x5CB85C);
    public static final Color COLOR_NAVIGATE = new Color(0x1c71d8);

    private final SolidJIPipeDesktopColorIcon icon;
    private final JLabel actionLabel;

    private final JLabel alternativeLabel;
    private final JLabel algorithmLabel;
    private final JLabel menuLabel;

    /**
     * Creates a new instance
     */
    public JIPipeDesktopNodeDatabaseSearchBoxListCellRenderer() {
        setLayout(new GridBagLayout());
        setOpaque(true);
        setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));


        icon = new SolidJIPipeDesktopColorIcon(16, 50);
        JLabel iconLabel = new JLabel(icon);
        Insets border = new Insets(2, 4, 2, 2);
        add(iconLabel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridheight = 3;
                anchor = WEST;
                insets = border;
            }
        });

        actionLabel = new JLabel();
        add(actionLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                anchor = WEST;
                insets = border;
            }
        });
        algorithmLabel = new JLabel();
        add(algorithmLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 0;
                anchor = WEST;
                insets = border;
            }
        });
        menuLabel = new JLabel();
        menuLabel.setForeground(Color.GRAY);
        menuLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        add(menuLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 1;
                anchor = WEST;
                insets = border;
            }
        });

        alternativeLabel = new JLabel();
        alternativeLabel.setForeground(JIPipeDesktopModernMetalTheme.PRIMARY6);
        alternativeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        add(alternativeLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 2;
                anchor = WEST;
                insets = border;
            }
        });

        JPanel glue = new JPanel();
        glue.setOpaque(false);
        add(glue, new GridBagConstraints() {
            {
                gridx = 3;
                weightx = 1;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeDatabaseEntry> list, JIPipeNodeDatabaseEntry value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value.exists()) {
            icon.setFillColor(value.getFillColor());
            icon.setBorderColor(value.getBorderColor());
            actionLabel.setText("Navigate");
            actionLabel.setForeground(COLOR_NAVIGATE);
        } else {
            actionLabel.setText("Create");
            actionLabel.setForeground(COLOR_CREATE);
            icon.setFillColor(UIUtils.DARK_THEME ? Color.BLACK : Color.WHITE);
            icon.setBorderColor(value.getFillColor());
        }

        algorithmLabel.setText(value.getName());
        algorithmLabel.setIcon(value.getIcon());
        menuLabel.setText(value.getCategory().replace("\n", " > "));

        if (value instanceof CreateNewNodeByInfoDatabaseEntry) {
            JIPipeNodeInfo info = ((CreateNewNodeByInfoDatabaseEntry) value).getNodeInfo();
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
                    builder.append(location.getCategory().getName()).append(" > ").append(String.join(" > ", location.getMenuPath().split("\n"))).append(" > ")
                            .append(StringUtils.orElse(location.getAlternativeName(), info.getName()));
                }
                alternativeLabel.setText(builder.toString());
            }
        } else if (value instanceof CreateNewNodeByExampleDatabaseEntry) {
            JIPipeNodeExample example = ((CreateNewNodeByExampleDatabaseEntry) value).getExample();
            JIPipeNodeInfo info = example.getNodeInfo();

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
