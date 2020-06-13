package org.hkijena.acaq5.extensions.parameters.roi;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Editor for a any numeric parameter
 */
public class RectangleParameterEditorUI extends ACAQParameterEditorUI {
    private JSpinner xSpinner;
    private JSpinner ySpinner;
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private boolean isReloading = false;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public RectangleParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (isReloading)
            return;
        Rectangle rectangle = getParameter(Rectangle.class);
        isReloading = true;
        xSpinner.getModel().setValue(rectangle.getX());
        ySpinner.getModel().setValue(rectangle.getY());
        widthSpinner.getModel().setValue(rectangle.getWidth());
        heightSpinner.getModel().setValue(rectangle.getHeight());
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        Rectangle rectangle = getParameter(Rectangle.class);
        xSpinner = addSpinner("X", rectangle.x, Integer.MIN_VALUE, i -> {
            rectangle.x = i;
            getParameterAccess().set(rectangle);
        });
        add(Box.createHorizontalStrut(4));
        ySpinner = addSpinner("Y", rectangle.y, Integer.MIN_VALUE, i -> {
            rectangle.y = i;
            getParameterAccess().set(rectangle);
        });
        add(Box.createHorizontalStrut(4));
        widthSpinner = addSpinner("W", rectangle.width, Integer.MIN_VALUE, i -> {
            rectangle.width = i;
            getParameterAccess().set(rectangle);
        });
        add(Box.createHorizontalStrut(4));
        heightSpinner = addSpinner("H", rectangle.height, Integer.MIN_VALUE, i -> {
            rectangle.height = i;
            getParameterAccess().set(rectangle);
        });
    }

    private JSpinner addSpinner(String label, int currentValue, int minValue, Consumer<Integer> setter) {
        SpinnerNumberModel model = new SpinnerNumberModel(currentValue, minValue, Integer.MAX_VALUE, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            if (!isReloading) {
                setter.accept(model.getNumber().intValue());
            }
        });
        spinner.setPreferredSize(new Dimension(40, spinner.getPreferredSize().height));
        add(new JLabel(label + ":"));
        add(Box.createHorizontalStrut(2));
        add(spinner);
        return spinner;
    }
}
