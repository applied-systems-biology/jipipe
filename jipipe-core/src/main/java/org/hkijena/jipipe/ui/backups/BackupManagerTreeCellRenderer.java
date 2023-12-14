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
 */

package org.hkijena.jipipe.ui.backups;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupItem;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupItemCollection;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultAlgorithmTree;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Renders the tree in {@link JIPipeResultAlgorithmTree}
 */
public class BackupManagerTreeCellRenderer extends JLabel implements TreeCellRenderer {
    private final Icon rootIcon = UIUtils.getIconFromResources("places/folder-blue.png");
    private final Icon collectionIcon = UIUtils.getIconFromResources("mimetypes/application-jipipe.png");
    private final Icon backupIcon = UIUtils.getIconFromResources("actions/clock.png");

    /**
     * Creates new renderer
     */
    public BackupManagerTreeCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        if (value instanceof DefaultMutableTreeNode) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof JIPipeProjectBackupItemCollection) {
                setIcon(collectionIcon);
                JIPipeProjectBackupItemCollection backupItemCollection = (JIPipeProjectBackupItemCollection) userObject;
                setText(backupItemCollection.renderName());
            } else if (userObject instanceof JIPipeProjectBackupItem) {
                JIPipeProjectBackupItem backupItem = (JIPipeProjectBackupItem) userObject;
                setIcon(backupIcon);
                setText(backupItem.getBackupTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                setIcon(rootIcon);
                setText("Backups");
            }
        } else {
            setIcon(null);
            setText("");
        }

        if (selected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
