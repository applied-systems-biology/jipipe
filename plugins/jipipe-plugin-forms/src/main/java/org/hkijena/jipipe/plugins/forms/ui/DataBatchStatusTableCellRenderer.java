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

package org.hkijena.jipipe.plugins.forms.ui;

import org.hkijena.jipipe.plugins.settings.GeneralUISettings;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

public class DataBatchStatusTableCellRenderer extends JLabel implements TableCellRenderer {

    private final List<DataBatchStatus> iterationStepStatuses;
    private final Color colorInvalidSelected;
    private final Color colorInvalid;
    private final Color colorUnvisited;

    public DataBatchStatusTableCellRenderer(List<DataBatchStatus> iterationStepStatuses) {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.iterationStepStatuses = iterationStepStatuses;
        this.colorInvalidSelected = getColorInvalidSelected();
        this.colorInvalid = getColorInvalid();
        this.colorUnvisited = getColorUnvisited();
    }

    public static Color getColorUnvisited() {
        if (GeneralUISettings.getInstance().getTheme().isDark())
            return new Color(0x9A7E29);
        else
            return new Color(0xefd78e);
    }

    public static Color getColorVisited() {
        if (GeneralUISettings.getInstance().getTheme().isDark())
            return new Color(0x4C7236);
        else
            return new Color(0xb3ef8e);
    }

    public static Color getColorInvalid() {
        if (GeneralUISettings.getInstance().getTheme().isDark())
            return new Color(0x9A2929);
        else
            return new Color(0xef8e8e);
    }

    public static Color getColorInvalidSelected() {
        if (GeneralUISettings.getInstance().getTheme().isDark())
            return new Color(0x753232);
        else
            return new Color(0xb57979);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText("" + value);
        DataBatchStatus status = iterationStepStatuses.get(table.convertRowIndexToModel(row));
        if (isSelected) {
            if (status == DataBatchStatus.Invalid)
                setBackground(colorInvalidSelected);
            else
                setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            if (status == DataBatchStatus.Invalid)
                setBackground(colorInvalid);
            else if (status == DataBatchStatus.Unvisited)
                setBackground(colorUnvisited);
            else
                setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
