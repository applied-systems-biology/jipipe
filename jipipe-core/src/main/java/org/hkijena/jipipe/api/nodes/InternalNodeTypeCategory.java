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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;

public class InternalNodeTypeCategory implements JIPipeNodeTypeCategory {
    @Override
    public String getName() {
        return "Internal";
    }

    @Override
    public String getDescription() {
        return "Nodes that the user should not modify";
    }

    @Override
    public int getUIOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/configure.png");
    }

    @Override
    public Color getFillColor() {
        return new Color(0xFFBFB7);
    }

    @Override
    public Color getBorderColor() {
        return new Color(0xEE9084);
    }

    @Override
    public boolean isVisibleInGraphCompartment() {
        return false;
    }

    @Override
    public boolean isVisibleInCompartmentGraph() {
        return false;
    }

    @Override
    public boolean userCanCreate() {
        return false;
    }

    @Override
    public boolean userCanDelete() {
        return false;
    }
}
