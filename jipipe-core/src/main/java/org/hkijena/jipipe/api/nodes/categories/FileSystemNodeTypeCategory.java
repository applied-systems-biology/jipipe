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

import javax.swing.*;
import java.awt.*;

public class FileSystemNodeTypeCategory implements JIPipeNodeTypeCategory {

    public static final Color FILL_COLOR = Color.getHSBColor(60.0f / 360.0f, 0.1f, 0.9f);
    public static final Color BORDER_COLOR = Color.getHSBColor(60.0f / 360.0f, 0.1f, 0.5f);
    public static final Color FILL_COLOR_DARK = Color.getHSBColor(60.0f / 360.0f, 0.5f, 0.3f);
    public static final Color BORDER_COLOR_DARK = Color.getHSBColor(60.0f / 360.0f, 0.5f, 0.9f);

    @Override
    public String getId() {
        return "org.hkijena.jipipe:file-system";
    }

    @Override
    public String getName() {
        return "File system";
    }

    @Override
    public String getDescription() {
        return "Nodes that are encapsulate file system operations";
    }

    @Override
    public int getUIOrder() {
        return 10;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("devices/drive-harddisk.png");
    }

    @Override
    public Color getFillColor() {
        return FILL_COLOR;
    }

    @Override
    public Color getBorderColor() {
        return BORDER_COLOR;
    }

    @Override
    public Color getDarkFillColor() {
        return FILL_COLOR_DARK;
    }

    @Override
    public Color getDarkBorderColor() {
        return BORDER_COLOR_DARK;
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
