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

public class InternalNodeTypeCategory implements JIPipeNodeTypeCategory {

    @Override
    public String getId() {
        return "org.hkijena.jipipe:internal";
    }

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
        return JIPipe.RESOURCES.getIcon16("actions/configure.png");
    }

    @Override
    public float getColorHue() {
        return -1;
    }

    @Override
    public boolean isVisibleInPipeline() {
        return false;
    }

    @Override
    public boolean isVisibleInCompartments() {
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

    @Override
    public boolean isRunnable() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return false;
    }
}
