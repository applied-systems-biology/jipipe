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

package org.hkijena.jipipe.plugins.parameters.library.table;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Implements the access to table cell
 */
public class ParameterTableCellAccess implements JIPipeParameterAccess {

    private final JIPipeParameterAccess parent;
    private final ParameterTable table;
    private final int row;
    private final int column;

    /**
     * Creates a new instance
     *
     * @param parent the parent access
     * @param table  the table
     * @param row    the row
     * @param column the column
     */
    public ParameterTableCellAccess(JIPipeParameterAccess parent, ParameterTable table, int row, int column) {
        this.parent = parent;
        this.table = table;
        this.row = row;
        this.column = column;
    }

    @Override
    public String getKey() {
        return parent.getKey() + "/" + row + "," + column;
    }

    @Override
    public String getName() {
        return table.getColumnName(column);
    }

    @Override
    public String getDescription() {
        return table.getColumnInfo(column).getDescription();
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return null;
    }

    @Override
    public <T extends Annotation> List<T> getAnnotationsOfType(Class<T> klass) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public Class<?> getFieldClass() {
        return table.getColumnClass(column);
    }

    @Override
    public <T> T get(Class<T> klass) {
        return (T) table.getValueAt(row, column);
    }

    @Override
    public <T> boolean set(T value) {
        table.setValueAt(value, row, column);
        return true;
    }

    @Override
    public JIPipeParameterCollection getSource() {
        return parent.getSource();
    }

    @Override
    public double getPriority() {
        return 0;
    }

    @Override
    public String getShortKey() {
        return getKey();
    }

    @Override
    public int getUIOrder() {
        return 0;
    }
}
