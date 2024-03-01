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

package org.hkijena.jipipe.ui.datatracer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeDataInfoCellRenderer;
import org.hkijena.jipipe.ui.cache.JIPipeDataTableRowUI;
import org.hkijena.jipipe.ui.cache.exporters.JIPipeDataExporterRun;
import org.hkijena.jipipe.ui.components.renderers.JIPipeComponentCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.resultanalysis.renderers.JIPipeAnnotationTableCellRenderer;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * UI that displays a {@link JIPipeDataTable} that is cached
 */
public class DataTracerDataTableUI extends JIPipeWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener {
    private Store<JIPipeDataTable> dataTableStore;
    private JXTable table;
    private DataTracerTableModel dataTableModel;

    /**
     * @param workbenchUI    the workbench UI
     * @param dataTableStore The slot
     */
    public DataTracerDataTableUI(JIPipeWorkbench workbenchUI, Store<JIPipeDataTable> dataTableStore) {
        super(workbenchUI);
        this.dataTableStore = dataTableStore;

        initialize();
        reloadTable();
        GeneralDataSettings.getInstance().getParameterChangedEventEmitter().subscribeWeak(this);
    }

    public JIPipeDataTable getDataTable() {
        return dataTableStore.get();
    }

    public void setDataTable(Store<JIPipeDataTable> dataTableStore) {
        this.dataTableStore = dataTableStore;
        reloadTable();
    }

    private void reloadTable() {
        dataTableModel = new DataTracerTableModel(table, dataTableStore);
        table.setModel(dataTableModel);
        if (GeneralDataSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            TableColumn column = columnModel.getColumn(i);
            column.setHeaderRenderer(new WrapperColumnHeaderRenderer(dataTableStore));
        }
        table.setAutoCreateRowSorter(true);
        UIUtils.packDataTable(table);
        columnModel.getColumn(1).setPreferredWidth(GeneralDataSettings.getInstance().getPreviewSize());
        SwingUtilities.invokeLater(dataTableModel::updateRenderedPreviews);

        if (dataTableModel.getColumnCount() > 0) {
            table.getColumnExt(0).setMinWidth(100);
            table.getColumnExt(0).setMaxWidth(100);
            table.getColumnExt(0).setPreferredWidth(100);
        }
        if (dataTableModel.getColumnCount() > 1) {
            int previewSize = GeneralDataSettings.getInstance().getPreviewSize() * 2;
            table.getColumnExt(1).setMinWidth(previewSize);
            table.getColumnExt(1).setMaxWidth(previewSize);
            table.getColumnExt(1).setPreferredWidth(previewSize);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        table = new JXTable();
        if (GeneralDataSettings.getInstance().isGenerateCachePreviews())
            table.setRowHeight(GeneralDataSettings.getInstance().getPreviewSize());
        else
            table.setRowHeight(25);
        table.setDefaultRenderer(JIPipeDataInfo.class, new JIPipeDataInfoCellRenderer());
        table.setDefaultRenderer(Component.class, new JIPipeComponentCellRenderer());
        table.setDefaultRenderer(JIPipeTextAnnotation.class, new JIPipeAnnotationTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        add(table, BorderLayout.CENTER);
        add(table.getTableHeader(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int[] selectedRows = table.getSelectedRows();
                    if (selectedRows.length > 0)
                        handleSlotRowDefaultAction(selectedRows[0], table.columnAtPoint(e.getPoint()));
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    showContextMenu(e);
                }
            }
        });
    }

    private void showContextMenu(MouseEvent e) {
        int viewRow = table.rowAtPoint(e.getPoint());
        int viewCol = table.columnAtPoint(e.getPoint());
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            table.setRowSelectionInterval(viewRow, viewRow);
            Object objectAtColumn;
            JIPipeDataTable dataTable = dataTableModel.getDataTable();
            int dataAnnotationColumn = -1;
            if (viewCol >= 0) {
                int modelColumn = table.convertColumnIndexToModel(viewCol);
                objectAtColumn = table.getModel().getValueAt(modelRow,
                        modelColumn);
                dataAnnotationColumn = modelColumn >= 0 ? dataTableModel.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(modelColumn)) : -1;
            } else {
                objectAtColumn = null;
            }

            JPopupMenu popupMenu = new JPopupMenu();

            // Show/open with for data
            if (dataAnnotationColumn >= 0) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(modelRow, dataAnnotationColumn);
                popupMenu.add(UIUtils.createMenuItem("Show data annotation", "Shows the data annotation '" + dataAnnotation.getName() + "'",
                        UIUtils.getIconFromResources("actions/search.png"), () -> handleSlotRowDefaultAction(viewRow, viewCol)));
            }

            // Show/open with controls
            popupMenu.add(UIUtils.createMenuItem("Show", "Shows the data", UIUtils.getIconFromResources("actions/search.png"), () -> handleSlotRowDefaultAction(viewRow, 0)));

            {
                JMenu openWithMenu = new JMenu();
                openWithMenu.setText("Open with ...");

                Class<? extends JIPipeData> dataClass = dataTable.getDataClass(modelRow);
                String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
                for (JIPipeDataDisplayOperation displayOperation : JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId)) {
                    openWithMenu.add(UIUtils.createMenuItem(displayOperation.getName(), displayOperation.getDescription(), displayOperation.getIcon(),
                            () -> displayOperation.display(dataTable, modelRow, getWorkbench(), false)));
                }
                popupMenu.add(openWithMenu);
            }

            if (dataAnnotationColumn >= 0) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(modelRow, dataAnnotationColumn);
                JMenu openWithMenu = new JMenu();
                openWithMenu.setText("Open " + dataAnnotation.getName() + " with ...");

                Class<? extends JIPipeData> dataClass = dataAnnotation.getDataClass();
                String datatypeId = JIPipe.getInstance().getDatatypeRegistry().getIdOf(dataClass);
                for (JIPipeDataDisplayOperation displayOperation : JIPipe.getInstance().getDatatypeRegistry().getSortedDisplayOperationsFor(datatypeId)) {
                    openWithMenu.add(UIUtils.createMenuItem(displayOperation.getName(), displayOperation.getDescription(), displayOperation.getIcon(),
                            () -> displayOperation.displayDataAnnotation(dataTable, modelRow, dataAnnotation, getWorkbench())));
                }
                popupMenu.add(openWithMenu);
            }

            // Trace
            if(getWorkbench() instanceof JIPipeProjectWorkbench) {
                popupMenu.add(UIUtils.createMenuItem("Trace ...",
                        "Allows to trace how the selected data was generated",
                        UIUtils.getIconFromResources("actions/footsteps.png"),
                        () -> traceData(dataTable.getDataContext(modelRow).getId())));
            }

            // String (preview)
            if (objectAtColumn instanceof String) {
                popupMenu.addSeparator();
                popupMenu.add(UIUtils.createMenuItem("Copy string representation", "Copies the string '" + objectAtColumn + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/edit-copy.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(objectAtColumn))));
            }

            // Annotations
            if (objectAtColumn instanceof JIPipeTextAnnotation) {
                popupMenu.addSeparator();
                String annotationName = ((JIPipeTextAnnotation) objectAtColumn).getName();
                String annotationValue = ((JIPipeTextAnnotation) objectAtColumn).getValue();
                String annotationNameAndValue = annotationName + "=" + annotationValue;
                String filterExpression = annotationName + " == " + "\"" + MacroUtils.escapeString(annotationValue) + "\"";
                popupMenu.add(UIUtils.createMenuItem("Copy " + annotationName + " name", "Copies the string '" + annotationName + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/edit-copy.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(annotationName))));
                popupMenu.add(UIUtils.createMenuItem("Copy " + annotationName + " value", "Copies the string '" + annotationValue + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/edit-copy.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(annotationValue))));
                popupMenu.add(UIUtils.createMenuItem("Copy " + annotationName + " name and value", "Copies the string '" + annotationNameAndValue + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/edit-copy.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(annotationNameAndValue))));
                popupMenu.add(UIUtils.createMenuItem("Copy " + annotationName + " as filter", "Copies the string '" + filterExpression + "' into the clipboard",
                        UIUtils.getIconFromResources("actions/filter.png"), () -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(filterExpression))));
            }

            popupMenu.addSeparator();

            popupMenu.add(UIUtils.createMenuItem("Export", "Exports the data", UIUtils.getIconFromResources("actions/document-export.png"),
                    () -> {
                        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export row " + modelRow);
                        if (path != null) {
                            Path directory = path.getParent();
                            String name = path.getFileName().toString();
                            JIPipeDataExporterRun run = new JIPipeDataExporterRun(dataTable.getData(modelRow, JIPipeData.class, new JIPipeProgressInfo()),
                                    directory, name);
                            JIPipeRunExecuterUI.runInDialog(getWorkbench(), SwingUtilities.getWindowAncestor(this), run, new JIPipeRunnerQueue("Export"));
                        }
                    }));

            if (dataAnnotationColumn >= 0) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(modelRow, dataAnnotationColumn);
                popupMenu.add(UIUtils.createMenuItem("Export " + dataAnnotation.getName(), "Exports the data annotation '" + dataAnnotation.getName() + "'", UIUtils.getIconFromResources("actions/document-export.png"),
                        () -> {
                            Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export row " + modelRow);
                            if (path != null) {
                                Path directory = path.getParent();
                                String name = path.getFileName().toString();
                                JIPipeDataExporterRun run = new JIPipeDataExporterRun(dataAnnotation.getData(JIPipeData.class, new JIPipeProgressInfo()),
                                        directory, name);
                                JIPipeRunExecuterUI.runInDialog(getWorkbench(), SwingUtilities.getWindowAncestor(this), run, new JIPipeRunnerQueue("Export"));
                            }
                        }));
            }

            popupMenu.show(table, e.getX(), e.getY());
        }
    }

    private void traceData(String id) {
        DataTracerUI.openWindow((JIPipeProjectWorkbench) getWorkbench(), id);
    }

    private void openSearchExpressionEditor(SearchTextField searchTextField) {
        Set<JIPipeExpressionParameterVariableInfo> variables = new HashSet<>();
        for (int i = 0; i < table.getModel().getColumnCount(); i++) {
            variables.add(new JIPipeExpressionParameterVariableInfo(table.getModel().getColumnName(i), table.getModel().getColumnName(i), ""));
        }
        String result = ExpressionBuilderUI.showDialog(getWorkbench().getWindow(), searchTextField.getText(), variables);
        if (result != null) {
            searchTextField.setText(result);
        }
    }

    private void handleSlotRowDefaultAction(int selectedRow, int selectedColumn) {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            int row = table.getRowSorter().convertRowIndexToModel(selectedRow);
            int dataAnnotationColumn = selectedColumn >= 0 ? dataTableModel.toDataAnnotationColumnIndex(table.convertColumnIndexToModel(selectedColumn)) : -1;
            JIPipeDataTableRowUI rowUI = new JIPipeDataTableRowUI(getWorkbench(), dataTableStore, row);
            rowUI.handleDefaultActionOrDisplayDataAnnotation(dataAnnotationColumn);
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (isDisplayable() && "preview-size".equals(event.getKey())) {
            reloadTable();
        }
    }

    /**
     * Renders the column header of {@link DataTracerTableModel}
     */
    public static class WrapperColumnHeaderRenderer implements TableCellRenderer {
        private final Store<JIPipeDataTable> dataTableStore;

        /**
         * Creates a new instance
         *
         * @param dataTableStore The table reference
         */
        public WrapperColumnHeaderRenderer(Store<JIPipeDataTable> dataTableStore) {
            this.dataTableStore = dataTableStore;
        }

        /**
         * Converts the column index to an annotation column index, or returns -1 if the column is not one
         *
         * @param columnIndex absolute column index
         * @return relative annotation column index, or -1
         */
        public int toAnnotationColumnIndex(int columnIndex) {
            JIPipeDataTable dataTable = dataTableStore.get();
            if (dataTable != null) {
                if (columnIndex >= dataTable.getDataAnnotationColumnNames().size() + 3)
                    return columnIndex - dataTable.getDataAnnotationColumnNames().size() - 3;
                else
                    return -1;
            } else {
                return -1;
            }
        }

        /**
         * Converts the column index to a data annotation column index, or returns -1 if the column is not one
         *
         * @param columnIndex absolute column index
         * @return relative data annotation column index, or -1
         */
        public int toDataAnnotationColumnIndex(int columnIndex) {
            JIPipeDataTable dataTable = dataTableStore.get();
            if (dataTable != null) {
                if (columnIndex < dataTable.getDataAnnotationColumnNames().size() + 3 && (columnIndex - 3) < dataTable.getDataAnnotationColumnNames().size()) {
                    return columnIndex - 3;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
            JIPipeDataTable dataTable = dataTableStore.get();
            int modelColumn = table.convertColumnIndexToModel(column);
            if (modelColumn < 3) {
                return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (toDataAnnotationColumnIndex(modelColumn) != -1) {
                if (dataTable != null) {
                    String info = dataTable.getDataAnnotationColumnNames().get(toDataAnnotationColumnIndex(modelColumn));
                    String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                            UIUtils.getIconFromResources("data-types/data-annotation.png"),
                            info);
                    return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
                } else {
                    return new JLabel("NA");
                }
            } else {
                if (dataTable != null) {
                    int annotationColumnIndex = toAnnotationColumnIndex(modelColumn);
                    if (annotationColumnIndex < dataTable.getTextAnnotationColumnNames().size()) {
                        String info = dataTable.getTextAnnotationColumnNames().get(annotationColumnIndex);
                        String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                                UIUtils.getIconFromResources("data-types/annotation.png"),
                                info);
                        return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
                    } else {
                        return defaultRenderer.getTableCellRendererComponent(table, "Annotation", isSelected, hasFocus, row, column);
                    }
                } else {
                    return new JLabel("NA");
                }
            }
        }
    }
}
