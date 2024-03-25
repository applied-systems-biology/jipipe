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

package org.hkijena.jipipe.plugins.clij2.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.CachedImagePlusDataViewerWindow;

public class CLIJImageViewerCustomDataLoader extends CachedImagePlusDataViewerWindow.CustomDataLoader {
    @Override
    public void load(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        JIPipeData data = virtualData.getData(progressInfo);
        if (data instanceof ImagePlusData) {
            setImagePlus(((ImagePlusData) data).getImage());
        } else if (data instanceof CLIJImageData) {
            setImagePlus(((CLIJImageData) data).pull().getImage());
        }
        setRois(new ROIListData());
    }
}
