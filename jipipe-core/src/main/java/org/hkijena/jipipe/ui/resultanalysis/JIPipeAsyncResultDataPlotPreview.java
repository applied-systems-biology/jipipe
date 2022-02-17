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

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * A {@link JIPipeResultDataSlotPreview} that uses a {@link javax.swing.SwingWorker} to load previews in a separate thread.
 * It will automatically update the table when finished
 */
public abstract class JIPipeAsyncResultDataPlotPreview extends JIPipeResultDataSlotPreview {

    private Worker worker;

    /**
     * Creates a new renderer
     *
     * @param workbench      the workbench
     * @param table          the table where the data is rendered in
     * @param slot           the data slot
     * @param row            the row
     * @param dataAnnotation the data annotation (Optional)
     */
    public JIPipeAsyncResultDataPlotPreview(JIPipeProjectWorkbench workbench, JTable table, JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, JIPipeExportedDataAnnotation dataAnnotation) {
        super(workbench, table, slot, row, dataAnnotation);
        initialize();
    }

    private void initialize() {
        JLabel label = new JLabel("Please wait ...", UIUtils.getIconFromResources("actions/hourglass-half.png"), JLabel.LEFT);
        add(label, BorderLayout.CENTER);
    }

    @Override
    public void renderPreview() {
        if (worker == null) {
            worker = new Worker(this, getRowStorageFolder(getSlot(), getRow(), getDataAnnotation()));
            worker.execute();
        }
    }

    private void setPreview(Component component) {
        removeAll();
        if (component == null) {
            add(new JLabel("N/A"), BorderLayout.CENTER);
        } else {
            add(component, BorderLayout.WEST);
        }
        revalidate();
        repaint();
        refreshTable();
    }

    /**
     * Run in a different thread.
     * Should return the data
     *
     * @param storageFolder the folder where the data is stored
     * @return the data. if null, the widget will display "error"
     */
    protected JIPipeData loadData(Path storageFolder) {
        if (getDataAnnotation() == null)
            return JIPipe.importData(storageFolder, getSlot().getAcceptedDataType(), new JIPipeProgressInfo());
        else {
            if (Files.exists(storageFolder))
                return JIPipe.importData(storageFolder, JIPipe.getDataTypes().getById(getDataAnnotation().getTrueDataType()), new JIPipeProgressInfo());
            else
                return new JIPipeEmptyData();
        }
    }

    /**
     * The worker that generates the component
     */
    private static class Worker extends SwingWorker<Component, Object> {

        private final JIPipeAsyncResultDataPlotPreview parent;
        private final Path storageFolder;
        private final int width = GeneralDataSettings.getInstance().getPreviewSize();

        private Worker(JIPipeAsyncResultDataPlotPreview parent, Path storageFolder) {
            this.parent = parent;
            this.storageFolder = storageFolder;
        }

        @Override
        protected Component doInBackground() throws Exception {
            JIPipeData data = parent.loadData(storageFolder);
            if (data != null)
                return data.preview(width, width);
            else
                return null;
        }

        @Override
        protected void done() {
            try {
                parent.setPreview(get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                parent.setPreview(null);
            }
        }
    }


}
