package org.hkijena.jipipe.extensions.clij2.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow;

public class CLIJImageViewerCustomDataLoader extends CachedImagePlusDataViewerWindow.CustomDataLoader {
    @Override
    public void load(JIPipeVirtualData virtualData, JIPipeProgressInfo progressInfo) {
        JIPipeData data = virtualData.getData(progressInfo);
        if (data instanceof ImagePlusData) {
            setImagePlus(((ImagePlusData) data).getImage());
        } else if (data instanceof CLIJImageData) {
            setImagePlus(((CLIJImageData) data).pull().getImage());
        }
        setRois(new ROIListData());
    }
}
