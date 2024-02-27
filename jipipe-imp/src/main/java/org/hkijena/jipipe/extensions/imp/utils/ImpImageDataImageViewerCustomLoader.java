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

package org.hkijena.jipipe.extensions.imp.utils;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageData;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.image.BufferedImage;

public class ImpImageDataImageViewerCustomLoader extends CachedImagePlusDataViewerWindow.CustomDataLoader {
    @Override
    public void load(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        setImagePlus(virtualData.getData(ImpImageData.class, progressInfo).toImagePlus(true, 10));
        setRois(new ROIListData());
    }
}
