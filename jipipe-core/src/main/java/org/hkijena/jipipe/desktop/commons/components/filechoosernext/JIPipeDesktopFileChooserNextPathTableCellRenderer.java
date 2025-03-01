package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.hkijena.jipipe.utils.UIUtils;

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

        if(value instanceof Path) {
            Path path = (Path) value;
            try {
                if(Files.isDirectory(path)) {
                    setIcon(getDirectoryIcon(path));
                }
                else {
                    setIcon(getFileIcon(path));
                }
            }
            catch(Throwable e) {
                setIcon(UIUtils.getIcon32FromResources("file-error.png"));
            }
            setText((path).getFileName().toString());
        }
        else {
            setText("<N/A>");
        }

        if(isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        }
        else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }

    private Icon getFileIcon(Path path) {
        return UIUtils.getIcon32FromResources("file.png");
    }

    private Icon getDirectoryIcon(Path path) {
        try {
            if (path.getFileName().toString().startsWith(".") || Files.isHidden(path)) {
                return UIUtils.getIcon32FromResources("places/folder2-hidden.png");
            }
        }
        catch (Throwable ignored) {

        }
        return UIUtils.getIcon32FromResources("places/folder2.png");
    }
}
