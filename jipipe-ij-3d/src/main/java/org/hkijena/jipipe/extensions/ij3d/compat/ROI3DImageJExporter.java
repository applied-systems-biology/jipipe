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
 *
 */

package org.hkijena.jipipe.extensions.ij3d.compat;

import ij.Prefs;
import mcib_plugins.tools.RoiManager3D_2;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Export 3D ROI to ImageJ")
public class ROI3DImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        boolean multiple = Prefs.get("RoiManager3D-Options_UseMultiple.boolean", false);
        if (parameters.isAppend())
            multiple = false;
        List<Object> result = new ArrayList<>();
        for (int row = 0; row < dataTable.getRowCount(); row++) {
            ROI3DListData data = dataTable.getData(row, ROI3DListData.class, progressInfo);
            RoiManager3D_2 manager3D = null;
            if (!multiple) {
                Object obj = ReflectionUtils.getDeclaredStaticFieldValue("manager3d", RoiManager3D_2.class);
                if (obj instanceof RoiManager3D_2) {
                    manager3D = (RoiManager3D_2) obj;
                }
            }
            if (manager3D == null) {
                manager3D = new RoiManager3D_2();
                manager3D.create3DManager();
                if (!StringUtils.isNullOrEmpty(parameters.getName()))
                    manager3D.setTitle(parameters.getName());
            }
            manager3D.addObjects3DPopulation(data.toPopulation());
            result.add(manager3D);
        }
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return ROI3DListData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return RoiManager3D_2.class;
    }
}
