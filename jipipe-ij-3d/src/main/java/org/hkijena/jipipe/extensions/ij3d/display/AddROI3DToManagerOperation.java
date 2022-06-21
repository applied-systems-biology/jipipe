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

package org.hkijena.jipipe.extensions.ij3d.display;

import mcib_plugins.tools.RoiManager3D_2;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class AddROI3DToManagerOperation implements JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        Object obj = ReflectionUtils.getDeclaredStaticFieldValue("manager3d", RoiManager3D_2.class);
        RoiManager3D_2 manager3D;
        if(obj instanceof RoiManager3D_2) {
            manager3D = (RoiManager3D_2) obj;
        }
        else {
            manager3D = new RoiManager3D_2();
            manager3D.create3DManager();
        }
        ROI3DListData listData = (ROI3DListData) data;
        manager3D.addObjects3DPopulation(listData);
    }

    @Override
    public String getId() {
        return "add-roi3d-to-manager";
    }

    @Override
    public String getName() {
        return "Add to 3D ROI Manager";
    }

    @Override
    public String getDescription() {
        return "Adds the 3D ROI to the 3D ImageJ Suite ROI Manager";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/link.png");
    }
}
