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

package org.hkijena.jipipe.plugins.nodetemplate;

import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Set;

/**
 * Renderer for {@link JIPipeNodeInfo}
 */
public class JIPipeNodeTemplateListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeTemplate> {

    public static final Color COLOR_GLOBAL =
            new Color(0x2372BE);
    public static final Color COLOR_PROJECT =
            new Color(0x6B40AA);
    public static final Color COLOR_EXTENSION =
            new Color(0x4098AA);

    private final JComponent parent;
    private final Border defaultBorder;
    private final Border selectedBorder;
    private final Set<JIPipeNodeTemplate> projectTemplateList;
    private JLabel nodeIcon;
    private JLabel nameLabel;
    private JLabel descriptionLabel;
    private JLabel pathLabel;
    private JLabel storageLabel;
    private boolean showDescriptions = true;


    /**
     * Creates a new renderer
     *
     * @param projectTemplateList templates that are in project
     */
    public JIPipeNodeTemplateListCellRenderer(JComponent parent, Set<JIPipeNodeTemplate> projectTemplateList) {
        this.parent = parent;
        this.projectTemplateList = projectTemplateList;
        this.defaultBorder = BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(4),
                UIUtils.createControlBorder());
        this.selectedBorder = BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(4),
                UIUtils.createControlBorder(UIUtils.COLOR_SUCCESS));
        setOpaque(true);
        setBorder(defaultBorder);
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        nodeIcon = new JLabel();
        nameLabel = new JLabel();
        descriptionLabel = new JLabel();
        descriptionLabel.setForeground(Color.GRAY);
        descriptionLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        pathLabel = new JLabel();
        pathLabel.setForeground(Color.GRAY);
        pathLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 10));
        storageLabel = new JLabel();

        add(nodeIcon, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 0;
                gridy = 0;
                insets = new Insets(0, 4, 0, 4);
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 1;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(storageLabel, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 2;
                gridy = 0;
                fill = NONE;
            }
        });
        add(descriptionLabel, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 1;
                gridy = 2;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(pathLabel, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 1;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeTemplate> list, JIPipeNodeTemplate obj, int index, boolean isSelected, boolean cellHasFocus) {

        int availableWidth = parent.getWidth() - list.getInsets().left - list.getInsets().right;
        setMinimumSize(new Dimension(availableWidth, 16));

        if (showDescriptions) {
            setMaximumSize(new Dimension(availableWidth, 75));
        } else {
            setMaximumSize(new Dimension(availableWidth, 50));
        }
        setPreferredSize(getMaximumSize());
        setFont(list.getFont());

        if (obj != null) {
            setTruncatedText(nameLabel, obj.getName(), list);
            if (showDescriptions) {
                String description = obj.getDescription().toPlainText().trim();
                if (StringUtils.isNullOrEmpty(description) && obj.getGraph().getGraphNodes().size() == 1) {
                    JIPipeGraphNode graphNode = obj.getGraph().getGraphNodes().iterator().next();
                    description = graphNode.getInfo().getDescription().toPlainText();
                }
                setTruncatedText(descriptionLabel, description, list);
            } else {
                descriptionLabel.setText(null);
            }
            setTruncatedText(pathLabel, obj.getLocationInfo(), list);
            nodeIcon.setIcon(obj.getIconImage());

            if (obj.isFromExtension()) {
                storageLabel.setForeground(COLOR_EXTENSION);
                storageLabel.setText("Plugin");
            } else if (projectTemplateList.contains(obj)) {
                storageLabel.setForeground(COLOR_PROJECT);
                storageLabel.setText("Project");
            } else {
                storageLabel.setForeground(COLOR_GLOBAL);
                storageLabel.setText("Global");
            }
        }

        setBorder(isSelected ? selectedBorder : defaultBorder);

        return this;
    }

    private void setTruncatedText(JLabel label, String text, JList<?> list) {
        if (text.length() > 100) {
            text = text.substring(0, 100) + " ...";
        }
        FontMetrics fm = label.getFontMetrics(label.getFont());
        int availableWidth = parent.getWidth() - list.getInsets().left - list.getInsets().right;
        label.setText(StringUtils.limitWithEllipsis(text, availableWidth, fm));
    }
}
