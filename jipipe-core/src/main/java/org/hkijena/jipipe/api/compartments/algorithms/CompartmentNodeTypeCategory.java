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

package org.hkijena.jipipe.api.compartments.algorithms;

import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class CompartmentNodeTypeCategory implements JIPipeNodeTypeCategory {
    @Override
    public String getId() {
        return "org.hkijena.jipipe:compartment-node";
    }

    @Override
    public String getName() {
        return "Compartment management";
    }

    @Override
    public String getDescription() {
        return "Nodes that manage compartments";
    }

    @Override
    public int getUIOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("data-types/graph-compartment.png");
    }

    @Override
    public Color getFillColor() {
        return Color.WHITE;
    }

    @Override
    public Color getBorderColor() {
        return Color.LIGHT_GRAY;
    }

    @Override
    public Color getDarkFillColor() {
        return Color.DARK_GRAY;
    }

    @Override
    public Color getDarkBorderColor() {
        return Color.BLACK;
    }

    @Override
    public boolean isVisibleInPipeline() {
        return false;
    }

    @Override
    public boolean isVisibleInCompartments() {
        return true;
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
