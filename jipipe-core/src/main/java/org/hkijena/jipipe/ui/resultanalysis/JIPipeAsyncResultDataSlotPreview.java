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
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * A {@link JIPipeResultDataSlotPreview} that uses a {@link javax.swing.SwingWorker} to load previews in a separate thread.
 * It will automatically update the table when finished
 */
public abstract class JIPipeAsyncResultDataSlotPreview extends JIPipeResultDataSlotPreview {

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
    public JIPipeAsyncResultDataSlotPreview(JIPipeProjectWorkbench workbench, JTable table, JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, JIPipeExportedDataAnnotation dataAnnotation) {
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
            worker = new Worker(this, getSlot().getSlotStoragePath(), getRowStorageFolder(getSlot(), getRow(), getDataAnnotation()));
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
            return JIPipe.importData(new JIPipeFileSystemReadDataStorage(new JIPipeProgressInfo(), storageFolder), getSlot().getAcceptedDataType(), new JIPipeProgressInfo());
        else {
            if (Files.exists(storageFolder))
                return JIPipe.importData(new JIPipeFileSystemReadDataStorage(new JIPipeProgressInfo(), storageFolder), JIPipe.getDataTypes().getById(getDataAnnotation().getTrueDataType()), new JIPipeProgressInfo());
            else
                return new JIPipeEmptyData();
        }
    }

    /**
     * The worker that generates the component
     */
    private static class Worker extends SwingWorker<Component, Object> {

        private final JIPipeAsyncResultDataSlotPreview parent;
        private final Path slotStoragePath;
        private final Path storageFolder;
        private final int previewSize = GeneralDataSettings.getInstance().getPreviewSize();

        private Worker(JIPipeAsyncResultDataSlotPreview parent, Path slotStoragePath, Path storageFolder) {
            this.parent = parent;
            this.slotStoragePath = slotStoragePath;
            this.storageFolder = storageFolder;
        }

        @Override
        protected Component doInBackground() throws Exception {

            Path internalPath = slotStoragePath.relativize(storageFolder);

            // Attempt to load the thumbnail first
            Path thumbnailRootPath = slotStoragePath.resolve("thumbnail").resolve(internalPath);
            Path thumbnailMetadataPath = thumbnailRootPath.resolve("thumbnails.json");

            if (Files.exists(thumbnailMetadataPath)) {
                try {
                    JIPipeDataThumbnailsMetadata metadata = JsonUtils.readFromFile(thumbnailMetadataPath, JIPipeDataThumbnailsMetadata.class);
                    JIPipeDataThumbnailsMetadata.Thumbnail thumbnail = metadata.selectBestThumbnail(new Dimension(previewSize, previewSize));
                    if (thumbnail != null) {
                        Path thumbnailImagePath = thumbnailRootPath.resolve(thumbnail.getImageFile());
                        BufferedImage bufferedImage = ImageIO.read(thumbnailImagePath.toFile());
                        bufferedImage = BufferedImageUtils.scaleImageToFit(bufferedImage, previewSize, previewSize);
                        return new JLabel(new ImageIcon(bufferedImage));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Failed to load thumbnail -> Load data
            JIPipeData data = parent.loadData(storageFolder);
            if (data != null)
                return data.preview(previewSize, previewSize);
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
