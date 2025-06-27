package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.JIPipe;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class JIPipeDesktopFileChooserNextPathTableCellRenderer extends JLabel implements TableCellRenderer {

    public JIPipeDesktopFileChooserNextPathTableCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if (value instanceof Path) {
            Path path = (Path) value;
            try {
                if (Files.isDirectory(path)) {
                    setIcon(getDirectoryIcon(path));
                } else {
                    setIcon(getFileIcon(path));
                }
            } catch (Throwable e) {
                setIcon(JIPipe.RESOURCES.getIcon32("file-error.png"));
            }
            setText((path).getFileName().toString());
        } else {
            setText("<N/A>");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }

    private Icon getFileIcon(Path path) {
        for (JIPipeDesktopFileChooserNextPathTypeMetadata pathType : JIPipeDesktopFileChooserNext.KNOWN_PATH_TYPES) {
            if (pathType.test(path)) {
                return pathType.getIcon();
            }
        }
        return JIPipe.RESOURCES.getIcon32("file.png");
    }

    private Icon getDirectoryIcon(Path path) {
        try {
            if (path.getFileName().toString().startsWith(".") || Files.isHidden(path)) {
                return JIPipe.RESOURCES.getIcon32("places/folder2-hidden.png");
            }
        } catch (Throwable ignored) {

        }
        for (JIPipeDesktopFileChooserNextPathTypeMetadata pathType : JIPipeDesktopFileChooserNext.KNOWN_PATH_TYPES) {
            if (pathType.test(path)) {
                return pathType.getIcon();
            }
        }
        return JIPipe.RESOURCES.getIcon32("places/folder2.png");
    }
}
