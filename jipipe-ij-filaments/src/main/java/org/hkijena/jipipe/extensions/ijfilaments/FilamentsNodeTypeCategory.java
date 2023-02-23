package org.hkijena.jipipe.extensions.ijfilaments;

import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class FilamentsNodeTypeCategory implements JIPipeNodeTypeCategory {
    public static final Color FILL_COLOR = Color.getHSBColor(0.05f, 0.1f, 0.9f);
    public static final Color BORDER_COLOR = Color.getHSBColor(0.05f, 0.1f, 0.5f);
    public static final Color FILL_COLOR_DARK = Color.getHSBColor(0.05f, 0.5f, 0.3f);
    public static final Color BORDER_COLOR_DARK = Color.getHSBColor(0.05f, 0.5f, 0.9f);

    @Override
    public String getId() {
        return "org.hkijena.jipipe:filaments";
    }

    @Override
    public String getName() {
        return "Filaments";
    }

    @Override
    public String getDescription() {
        return "Operations on filaments";
    }

    @Override
    public int getUIOrder() {
        return 45;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/path-mode-spiro.png");
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
}
