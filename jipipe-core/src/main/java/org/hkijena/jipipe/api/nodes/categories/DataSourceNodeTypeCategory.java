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

package org.hkijena.jipipe.api.nodes.categories;

import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;

public class DataSourceNodeTypeCategory implements JIPipeNodeTypeCategory {
    @Override
    public String getName() {
        return "Add data";
    }

    @Override
    public String getDescription() {
        return "Nodes that produce data";
    }

    @Override
    public int getUIOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/database.png");
    }

    @Override
    public Color getFillColor() {
        return Color.WHITE;
    }

    @Override
    public Color getBorderColor() {
        return Color.DARK_GRAY;
    }

    @Override
    public boolean isVisibleInGraphCompartment() {
        return true;
    }

    @Override
    public boolean isVisibleInCompartmentGraph() {
        return false;
    }

    @Override
    public boolean userCanCreate() {
        return true;
    }

    @Override
    public boolean userCanDelete() {
        return true;
    }
}
