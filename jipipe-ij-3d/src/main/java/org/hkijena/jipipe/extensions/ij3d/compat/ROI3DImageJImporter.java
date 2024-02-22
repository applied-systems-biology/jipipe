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

import ij.WindowManager;
import mcib3d.geom.Objects3DPopulation;
import mcib_plugins.tools.RoiManager3D_2;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.List;
import java.util.Objects;

@SetJIPipeDocumentation(name = "Import 3D ROI from ImageJ")
public class ROI3DImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(ROI3DListData.class);
        if (!objects.isEmpty()) {
            for (Object object : objects) {
                if (object instanceof RoiManager3D_2) {
                    RoiManager3D_2 manager = (RoiManager3D_2) object;
                    Objects3DPopulation population = (Objects3DPopulation) ReflectionUtils.getDeclaredFieldValue("objects3DPopulation", manager);
                    ROI3DListData listData = new ROI3DListData();
                    listData.addFromPopulation(population, 0, 0);
                    if (parameters.isDuplicate())
                        dataTable.addData(listData.duplicate(progressInfo), progressInfo);
                    else
                        dataTable.addData(listData, progressInfo);
                }
            }
        } else {
            for (Window window : WindowManager.getAllNonImageWindows()) {
                if (window instanceof RoiManager3D_2) {
                    if (!StringUtils.isNullOrEmpty(parameters.getName()) && !Objects.equals(parameters.getName(), ((RoiManager3D_2) window).getTitle())) {
                        continue;
                    }
                    RoiManager3D_2 manager = (RoiManager3D_2) window;
                    Objects3DPopulation population = (Objects3DPopulation) ReflectionUtils.getDeclaredFieldValue("objects3DPopulation", manager);
                    ROI3DListData listData = new ROI3DListData();
                    listData.addFromPopulation(population, 0, 0);
                    if (parameters.isDuplicate())
                        dataTable.addData(listData.duplicate(progressInfo), progressInfo);
                    else
                        dataTable.addData(listData, progressInfo);
                }
            }
        }
        return dataTable;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return ROI3DListData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return RoiManager3D_2.class;
    }
}
