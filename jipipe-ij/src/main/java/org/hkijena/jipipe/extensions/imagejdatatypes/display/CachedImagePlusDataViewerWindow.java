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

package org.hkijena.jipipe.extensions.imagejdatatypes.display;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerCacheDataViewerWindow;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

public class CachedImagePlusDataViewerWindow extends JIPipeImageViewerCacheDataViewerWindow {
    private CustomDataLoader customDataLoader;

    public CachedImagePlusDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
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
        ImagePlus image;
        ROIListData rois = new ROIListData();
        if (customDataLoader != null) {
            customDataLoader.load(virtualData, progressInfo);
            image = customDataLoader.getImagePlus();
            rois = customDataLoader.getRois();
            customDataLoader.setImagePlus(null);
            customDataLoader.setRois(null);
        } else if (ImagePlusData.class.isAssignableFrom(virtualData.getDataClass())) {
            ImagePlusData data = (ImagePlusData) virtualData.getData(progressInfo);
            getImageViewer().setError(null);
            image = data.getViewedImage(false);
            if (data.getImage().getRoi() != null) {
                rois.add(data.getImage().getRoi());
            }
        } else if (OMEImageData.class.isAssignableFrom(virtualData.getDataClass())) {
            OMEImageData data = (OMEImageData) virtualData.getData(progressInfo);
            getImageViewer().setError(null);
            image = data.getImage();
            rois.addAll(data.getRois());
        } else {
            throw new UnsupportedOperationException();
        }
        image.setTitle(image.getTitle());
        if (!rois.isEmpty() || ImageViewerUISettings.getInstance().isAlwaysClearROIs()) {
            getImageViewer().clearOverlays();
            getImageViewer().addOverlay(rois);
        }
        getImageViewer().setImage(image);
        fitImageToScreenOnce();
    }

    /**
     * Used to override the data loading behavior
     */
    public abstract static class CustomDataLoader {
        private ImagePlus imagePlus;
        private ROIListData rois;

        public ImagePlus getImagePlus() {
            return imagePlus;
        }

        public void setImagePlus(ImagePlus imagePlus) {
            this.imagePlus = imagePlus;
        }

        public ROIListData getRois() {
            return rois;
        }

        public void setRois(ROIListData rois) {
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
