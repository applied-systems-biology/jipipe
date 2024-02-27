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

package org.hkijena.jipipe.ui.components.renderers;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathListCellRenderer extends JLabel implements ListCellRenderer<Path> {

    private final Icon iconUnknown;
    private final Icon iconFile;
    private final Icon iconFolder;

    public PathListCellRenderer() {
        iconUnknown = UIUtils.getIconFromResources("data-types/path.png");
        iconFile = UIUtils.getIconFromResources("data-types/file.png");
        iconFolder = UIUtils.getIconFromResources("data-types/folder.png");
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Path> list, Path value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            setText(value.toString());
            if (Files.isRegularFile(value)) {
                setIcon(iconFile);
            } else if (Files.isDirectory(value)) {
                setIcon(iconFolder);
            } else {
                setIcon(iconUnknown);
            }
        } else {
            setText("<Empty path>");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
