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

package org.hkijena.jipipe.plugins.imagejdatatypes.display;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imageviewer.legacy.ImageLegacyCacheDataViewerWindow;

public class ImagePlusDataLegacyCacheDataViewerWindow extends ImageLegacyCacheDataViewerWindow {
    private CustomDataLoader customDataLoader;

    public ImagePlusDataLegacyCacheDataViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
    }

    public CustomDataLoader getCustomDataLoader() {
        return customDataLoader;
    }

    public void setCustomDataLoader(CustomDataLoader customDataLoader) {
        this.customDataLoader = customDataLoader;
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        ImagePlusData image;
        ROI2DListData rois = new ROI2DListData();
        if (customDataLoader != null) {
            customDataLoader.load(virtualData, progressInfo);
            image = new ImagePlusData(customDataLoader.getImagePlus());
            rois = customDataLoader.getRois();
            customDataLoader.setImagePlus(null);
            customDataLoader.setRois(null);
        } else if (ImagePlusData.class.isAssignableFrom(virtualData.getDataClass())) {
            ImagePlusData data = (ImagePlusData) virtualData.getData(progressInfo);
            getImageViewer().setError(null);
            image = new ImagePlusData(data.getViewedImage(false));
            if (data.getImage().getRoi() != null) {
                rois.add(data.getImage().getRoi());
            }
            image.copyMetadata(data);
        } else if (OMEImageData.class.isAssignableFrom(virtualData.getDataClass())) {
            OMEImageData data = (OMEImageData) virtualData.getData(progressInfo);
            getImageViewer().setError(null);
            image = new ImagePlusData(data.getImage());
            rois.addAll(data.getRois());
        } else {
            throw new UnsupportedOperationException();
        }
        getImageViewer().clearOverlays();
        getImageViewer().setImageData(image);
        if (!rois.isEmpty()) {
            getImageViewer().addOverlay(rois);
        }
        fitImageToScreenOnce();
    }

    /**
     * Used to override the data loading behavior
     */
    public abstract static class CustomDataLoader {
        private ImagePlus imagePlus;
        private ROI2DListData rois;

        public ImagePlus getImagePlus() {
            return imagePlus;
        }

        public void setImagePlus(ImagePlus imagePlus) {
            this.imagePlus = imagePlus;
        }

        public ROI2DListData getRois() {
            return rois;
        }

        public void setRois(ROI2DListData rois) {
            this.rois = rois;
        }

        /**
         * The data loading operation.
         * It should set the values of the current class
         *
         * @param virtualData  the virtual data
         * @param progressInfo the progress info
         */
        public abstract void load(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo);
    }
}
