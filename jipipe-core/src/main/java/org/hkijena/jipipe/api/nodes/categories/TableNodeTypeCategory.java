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

package org.hkijena.jipipe.api.nodes.categories;

import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.JIPipe;

import javax.swing.*;
import java.awt.*;

public class TableNodeTypeCategory implements JIPipeNodeTypeCategory {

    @Override
    public String getId() {
        return "org.hkijena.jipipe:tables";
    }

    @Override
    public String getName() {
        return "Tables";
    }

    @Override
    public String getDescription() {
        return "Operations on tables";
    }

    @Override
    public int getUIOrder() {
        return 50;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/table.png");
    }

    @Override
    public float getColorHue() {
        return 216.0f / 360.0f;
    }

    @Override
    public boolean isVisibleInPipeline() {
        return true;
    }

    @Override
    public boolean isVisibleInCompartments() {
        return false;
    }
}
