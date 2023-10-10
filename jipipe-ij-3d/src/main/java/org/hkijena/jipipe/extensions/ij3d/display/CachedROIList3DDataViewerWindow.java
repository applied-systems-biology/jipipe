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

package org.hkijena.jipipe.extensions.ij3d.display;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedImagePlusDataViewerWindow;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

public class CachedROIList3DDataViewerWindow extends CachedImagePlusDataViewerWindow {

    public CachedROIList3DDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        ROI3DListData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), ROI3DListData.class, progressInfo);
        ImagePlus image = data.createBlankCanvas("Empty", 8);
        getImageViewer().clearOverlays();
        getImageViewer().setImageData(new ImagePlusData(image));
        getImageViewer().addOverlay(data);
    }

}
