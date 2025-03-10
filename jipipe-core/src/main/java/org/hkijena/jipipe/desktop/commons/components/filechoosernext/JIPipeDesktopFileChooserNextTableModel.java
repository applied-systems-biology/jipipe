package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import org.hkijena.jipipe.utils.PathUtils;
import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class JIPipeDesktopFileChooserNextTableModel implements TableModel {

    private final Path directory;
    private final boolean showFiles;
    private final boolean showHidden;
    private final List<Path> children = new ArrayList<>();
    private final TLongList sizes = new TLongArrayList();
    private final List<LocalDateTime> dates = new ArrayList<>();
    private boolean successful = false;

    public JIPipeDesktopFileChooserNextTableModel(Path directory, boolean showFiles, boolean showHidden) {
        this.directory = directory;
        this.showFiles = showFiles;
        this.showHidden = showHidden;
        discover();
    }

    private void discover() {
        if (directory == null) {
            return;
        }
        try {
            if (Files.isDirectory(directory)) {
                successful = true;
                for (Path path : PathUtils.listDirectory(directory)) {
                    try {

                        if (!showHidden && (path.getFileName().toString().startsWith(".") || Files.isHidden(path))) {
                            continue;
                        }

                        long size;
                        LocalDateTime date = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
                        if (Files.isDirectory(path)) {
                            size = -PathUtils.listDirectory(path).size();
                        } else {
                            if (!showFiles) {
                                continue;
                            }
                            size = Files.size(path);
                        }

                        this.children.add(path);
                        this.sizes.add(size);
                        this.dates.add(date);
                    } catch (Throwable ignored) {
                        successful = false;
                    }
                }

            } else {
                successful = false;
            }
        } catch (Throwable ignored) {
            successful = false;
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public int getRowCount() {
        return children.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Nls
    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Name";
            case 1:
                return "Size";
            case 2:
                return "Date";
            default:
                return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Path.class;
            case 1:
                return Long.class;
            case 2:
                return LocalDateTime.class;
            default:
                return Object.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= 0 && rowIndex < children.size()) {
            switch (columnIndex) {
                case 0:
                    return children.get(rowIndex);
                case 1:
                    return sizes.get(rowIndex);
                case 2:
                    return dates.get(rowIndex);
                default:
                    return null;
            }
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

    }

    @Override
    public void addTableModelListener(TableModelListener l) {

    }

    @Override
    public void removeTableModelListener(TableModelListener l) {

    }

    public Path getDirectory() {
        return directory;
    }
}
