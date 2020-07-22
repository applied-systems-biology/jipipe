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

package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.FileObject;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.GroupAction;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copy of {@link net.imagej.ui.swing.updater.FileTable.FileTableModel} for usage in JIPipe
 */
public class FileTableModel extends AbstractTableModel {

    final static int NAME_COLUMN = 0;
    final static int ACTION_COLUMN = 1;
    final static int SITE_COLUMN = 2;
    protected Map<FileObject, Integer> fileToRow;
    protected List<FileObject> rowToFile;
    private FilesCollection files;

    public FileTableModel(final FilesCollection files) {
        this.files = files;
        updateMappings();
    }

    public void setFiles(final Iterable<FileObject> files) {
        setFiles(this.files.clone(files));
    }

    public void setFiles(final FilesCollection files) {
        this.files = files;
        fileToRow = null;
        rowToFile = null;
        updateMappings();
        fireTableChanged(new TableModelEvent(this));
    }

    @Override
    public int getColumnCount() {
        return 3; // Name of file, status, update site
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        switch (columnIndex) {
            case NAME_COLUMN:
            case SITE_COLUMN:
                return String.class; // filename / update site
            case ACTION_COLUMN:
                return FileObject.Action.class; // status/action
            default:
                return Object.class;
        }
    }

    @Override
    public String getColumnName(final int column) {
        switch (column) {
            case NAME_COLUMN:
                return "Name";
            case ACTION_COLUMN:
                return "Status/Action";
            case SITE_COLUMN:
                return "Update Site";
            default:
                throw new Error("Column out of range");
        }
    }

    public FileObject getEntry(final int rowIndex) {
        return rowToFile.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return files.size();
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        updateMappings();
        if (row < 0 || row >= files.size()) return null;
        final FileObject file = rowToFile.get(row);
        switch (column) {
            case NAME_COLUMN:
                return file.getFilename(true);
            case ACTION_COLUMN:
                return file.getAction();
            case SITE_COLUMN:
                return file.updateSite;
        }
        throw new RuntimeException("Unhandled column: " + column);
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex == ACTION_COLUMN;
    }

    @Override
    public void setValueAt(final Object value, final int row, final int column) {
        if (column == ACTION_COLUMN) {
            final GroupAction action = (GroupAction) value;
            final FileObject file = getFileFromModel(row);
            action.setAction(files, file);
            fireFileChanged(file);
        }
    }

    public FileObject getFileFromModel(int row) {
        return rowToFile.get(row);
    }

    public void fireRowChanged(final int row) {
        fireTableRowsUpdated(row, row);
    }

    public void fireFileChanged(final FileObject file) {
        updateMappings();
        final Integer row = fileToRow.get(file);
        if (row != null) fireRowChanged(row.intValue());
    }

    protected void updateMappings() {
        if (fileToRow != null) return;
        fileToRow = new HashMap<>();
        rowToFile = new ArrayList<>();
        // the table may be sorted, and we need the model's row
        int i = 0;
        for (final FileObject f : files) {
            fileToRow.put(f, new Integer(i++));
            rowToFile.add(f);
        }
    }
}
