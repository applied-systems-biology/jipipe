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

package org.hkijena.jipipe.desktop.app.parameterreference;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReference;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroup;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Renders an {@link JIPipeDataSlot} in a {@link JTree}
 */
public class JIPipeDesktopParameterReferenceGroupCollectionTreeCellRenderer extends JPanel implements TreeCellRenderer {

    private final JIPipeDesktopGraphNodeParameterReferenceGroupCollectionEditorUI editorUI;
    private final JLabel iconLabel;
    private final JLabel mainLabel;
    private final JLabel infoLabel;

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopParameterReferenceGroupCollectionTreeCellRenderer(JIPipeDesktopGraphNodeParameterReferenceGroupCollectionEditorUI editorUI) {
        this.editorUI = editorUI;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setLayout(new GridBagLayout());

        iconLabel = new JLabel();
        add(iconLabel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                insets = new Insets(0, 4, 0, 4);
            }
        });

        infoLabel = new JLabel();
        infoLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 11));
        add(infoLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                weightx = 1;
                fill = HORIZONTAL;
            }
        });

        mainLabel = new JLabel();
        add(mainLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                weightx = 1;
                fill = HORIZONTAL;
            }
        });
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        Object o = ((DefaultMutableTreeNode) value).getUserObject();
        if (o instanceof GraphNodeParameterReferenceGroup) {
            GraphNodeParameterReferenceGroup group = (GraphNodeParameterReferenceGroup) o;
            mainLabel.setText(StringUtils.orElse(group.getName(), "<No name>"));
            infoLabel.setText(group.getContent().size() == 1 ? "1 parameter" : group.getContent().size() + " parameters");
            if (StringUtils.isNullOrEmpty(group.getName())) {
                iconLabel.setIcon(UIUtils.getIconFromResources("emblems/warning.png"));
            } else {
                iconLabel.setIcon(UIUtils.getIconFromResources("actions/configure.png"));
            }
        } else if (o instanceof GraphNodeParameterReference) {
            GraphNodeParameterReference reference = (GraphNodeParameterReference) o;
            JIPipeParameterAccess access = reference.resolve(editorUI.getParameterTree());
            if (access != null) {
                mainLabel.setText(reference.getName(editorUI.getParameterTree()));
                infoLabel.setText(JIPipe.getParameterTypes().getInfoByFieldClass(access.getFieldClass()).getName());
                iconLabel.setIcon(UIUtils.getIconFromResources("data-types/parameters.png"));
            } else {
                mainLabel.setText("Not found!");
                infoLabel.setText("No parameter " + reference.getPath());
                iconLabel.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            }
        } else {
            infoLabel.setText(null);
            mainLabel.setText(null);
            iconLabel.setIcon(null);
        }

        // Update status
        if (selected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
