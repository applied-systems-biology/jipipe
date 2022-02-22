package org.hkijena.jipipe.extensions.imagej2.util;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;

public class ImageJDataSetDataImageViewerCustomLoader extends CachedImagePlusDataViewerWindow.CustomDataLoader {
    @Override
    public void load(JIPipeVirtualData virtualData, JIPipeProgressInfo progressInfo) {
        ImageJ2DatasetData data = (ImageJ2DatasetData) virtualData.getData(progressInfo);
        setImagePlus(data.wrap().getImage());
        setRois(new ROIListData());
    }
}
