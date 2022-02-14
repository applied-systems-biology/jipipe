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

package org.hkijena.jipipe.extensions.parameters.library.roi;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Editor for a any numeric parameter
 */
public class RectangleParameterEditorUI extends JIPipeParameterEditorUI {
    private JSpinner xSpinner;
    private JSpinner ySpinner;
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public RectangleParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        Rectangle rectangle = getParameter(Rectangle.class);
        xSpinner.getModel().setValue(rectangle.getX());
        ySpinner.getModel().setValue(rectangle.getY());
        widthSpinner.getModel().setValue(rectangle.getWidth());
        heightSpinner.getModel().setValue(rectangle.getHeight());
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        Rectangle rectangle = getParameter(Rectangle.class);
        xSpinner = addSpinner("X", rectangle.x, Integer.MIN_VALUE, i -> {
            rectangle.x = i;
            setParameter(rectangle, false);
        });
        add(Box.createHorizontalStrut(4));
        ySpinner = addSpinner("Y", rectangle.y, Integer.MIN_VALUE, i -> {
            rectangle.y = i;
            setParameter(rectangle, false);
        });
        add(Box.createHorizontalStrut(4));
        widthSpinner = addSpinner("W", rectangle.width, Integer.MIN_VALUE, i -> {
            rectangle.width = i;
            setParameter(rectangle, false);
        });
        add(Box.createHorizontalStrut(4));
        heightSpinner = addSpinner("H", rectangle.height, Integer.MIN_VALUE, i -> {
            rectangle.height = i;
            setParameter(rectangle, false);
        });
    }

    private JSpinner addSpinner(String label, int currentValue, int minValue, Consumer<Integer> setter) {
        SpinnerNumberModel model = new SpinnerNumberModel(currentValue, minValue, Integer.MAX_VALUE, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> setter.accept(model.getNumber().intValue()));
        spinner.setPreferredSize(new Dimension(40, spinner.getPreferredSize().height));
        add(new JLabel(label + ":"));
        add(Box.createHorizontalStrut(2));
        add(spinner);
        return spinner;
    }
}
