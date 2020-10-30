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

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * A {@link JIPipeResultDataSlotPreviewUI} that uses a {@link javax.swing.SwingWorker} to load previews in a separate thread.
 * It will automatically update the table when finished
 */
public abstract class JIPipeAsyncResultDataPlotPreviewUI extends JIPipeResultDataSlotPreviewUI {

    /**
     * Creates a new renderer
     *
     * @param table the table where the data is rendered in
     */
    public JIPipeAsyncResultDataPlotPreviewUI(JTable table) {
        super(table);
    }

    @Override
    public void render(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        JLabel label = new JLabel("Please wait ...", UIUtils.getIconFromResources("actions/hourglass-half.png"), JLabel.LEFT);
        add(label, BorderLayout.CENTER);
        Worker worker = new Worker(this, getRowStorageFolder(slot, row));
        worker.execute();
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
        if (getTable() != null) {
            if (getTable() instanceof JXTable)
                ((JXTable) getTable()).packAll();
            getTable().repaint();
        }
    }

    /**
     * Run in a different thread.
     * Should return the data
     *
     * @param storageFolder the folder where the data is stored
     * @return the data. if null, the widget will display "error"
     */
    protected abstract JIPipeData loadData(Path storageFolder);

    /**
     * The worker that generates the component
     */
    private static class Worker extends SwingWorker<Component, Object> {

        private final JIPipeAsyncResultDataPlotPreviewUI parent;
        private final Path storageFolder;
        private final int width = GeneralDataSettings.getInstance().getPreviewWidth();
        private final int height = GeneralDataSettings.getInstance().getPreviewHeight();

        private Worker(JIPipeAsyncResultDataPlotPreviewUI parent, Path storageFolder) {
            this.parent = parent;
            this.storageFolder = storageFolder;
        }

        @Override
        protected Component doInBackground() throws Exception {
            JIPipeData data = parent.loadData(storageFolder);
            if (data != null)
                return data.preview(width, height);
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
