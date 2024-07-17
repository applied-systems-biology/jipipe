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

package org.hkijena.jipipe.plugins.opencv.utils;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.CachedImagePlusDataViewerWindow;
import org.hkijena.jipipe.plugins.opencv.datatypes.OpenCvImageData;

public class OpenCvImageDataImageViewerCustomLoader extends CachedImagePlusDataViewerWindow.CustomDataLoader {
    @Override
    public void load(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        setImagePlus(virtualData.getData(OpenCvImageData.class, progressInfo).toImagePlus());
        setRois(new ROI2DListData());
    }
}
