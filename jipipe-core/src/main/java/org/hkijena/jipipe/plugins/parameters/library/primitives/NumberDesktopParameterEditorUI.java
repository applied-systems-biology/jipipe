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

package org.hkijena.jipipe.plugins.parameters.library.primitives;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Locale;
import java.util.Objects;

/**
 * Editor for a any numeric parameter
 */
public class NumberDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {
    private JTextField numberField;

    public NumberDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
        reload();
    }

    public static String formatNumber(double number) {
        if (number % 1 == 0) {
            return "" + (long) number;
        } else {
            return "" + number;
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        String s = formatNumber(getCurrentValue());
        if (!Objects.equals(s, numberField.getText()) && !numberField.hasFocus())
            numberField.setText(s);
    }

    private double getCurrentValue() {
        Object value = getParameterAccess().get(Object.class);
        Number asNumber = 0;
        if (value instanceof Number)
            asNumber = (Number) value;
        if (getParameterAccess().getFieldClass() == byte.class || getParameterAccess().getFieldClass() == Byte.class) {
            return asNumber.byteValue();
        } else if (getParameterAccess().getFieldClass() == short.class || getParameterAccess().getFieldClass() == Short.class) {
            return asNumber.shortValue();
        } else if (getParameterAccess().getFieldClass() == int.class || getParameterAccess().getFieldClass() == Integer.class) {
            return asNumber.intValue();
        } else if (getParameterAccess().getFieldClass() == long.class || getParameterAccess().getFieldClass() == Long.class) {
            return asNumber.longValue();
        } else if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class) {
            return asNumber.floatValue();
        } else if (getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            return asNumber.doubleValue();
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + value);
        }
    }

    private double getMinimumValue() {
        NumberParameterSettings settings = getParameterAccess().getAnnotationOfType(NumberParameterSettings.class);
        if (settings != null) {
            return settings.min();
        }

        if (getParameterAccess().getFieldClass() == byte.class || getParameterAccess().getFieldClass() == Byte.class) {
            return Byte.MIN_VALUE;
        } else if (getParameterAccess().getFieldClass() == short.class || getParameterAccess().getFieldClass() == Short.class) {
            return Short.MIN_VALUE;
        } else if (getParameterAccess().getFieldClass() == int.class || getParameterAccess().getFieldClass() == Integer.class) {
            return Integer.MIN_VALUE;
        } else if (getParameterAccess().getFieldClass() == long.class || getParameterAccess().getFieldClass() == Long.class) {
            return Long.MIN_VALUE;
        } else if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class) {
            return Float.NEGATIVE_INFINITY;
        } else if (getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            return Double.NEGATIVE_INFINITY;
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + getParameterAccess().getFieldClass());
        }
    }

    private double getMaximumValue() {
        NumberParameterSettings settings = getParameterAccess().getAnnotationOfType(NumberParameterSettings.class);
        if (settings != null) {
            return settings.max();
        }

        if (getParameterAccess().getFieldClass() == byte.class || getParameterAccess().getFieldClass() == Byte.class) {
            return Byte.MAX_VALUE;
        } else if (getParameterAccess().getFieldClass() == short.class || getParameterAccess().getFieldClass() == Short.class) {
            return Short.MAX_VALUE;
        } else if (getParameterAccess().getFieldClass() == int.class || getParameterAccess().getFieldClass() == Integer.class) {
            return Integer.MAX_VALUE;
        } else if (getParameterAccess().getFieldClass() == long.class || getParameterAccess().getFieldClass() == Long.class) {
            return Long.MAX_VALUE;
        } else if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class) {
            return Float.POSITIVE_INFINITY;
        } else if (getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            return Double.POSITIVE_INFINITY;
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + getParameterAccess().getFieldClass());
        }
    }

    private double getStep() {
        NumberParameterSettings settings = getParameterAccess().getAnnotationOfType(NumberParameterSettings.class);
        if (settings != null) {
            return settings.step();
        }
        return 1;
    }

    private boolean setCurrentValue(Number number) {
        if (getParameterAccess().getFieldClass() == byte.class || getParameterAccess().getFieldClass() == Byte.class) {
            return setParameter(number.byteValue(), false);
        } else if (getParameterAccess().getFieldClass() == short.class || getParameterAccess().getFieldClass() == Short.class) {
            return setParameter(number.shortValue(), false);
        } else if (getParameterAccess().getFieldClass() == int.class || getParameterAccess().getFieldClass() == Integer.class) {
            return setParameter(number.intValue(), false);
        } else if (getParameterAccess().getFieldClass() == long.class || getParameterAccess().getFieldClass() == Long.class) {
            return setParameter(number.longValue(), false);
        } else if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class) {
            return setParameter(number.floatValue(), false);
        } else if (getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            return setParameter(number.doubleValue(), false);
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + getParameterAccess().getFieldClass());
        }
    }

    private boolean isValidNumber(String text) {
        if (NumberUtils.isCreatable(text))
            return true;
        if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class ||
                getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            if (StringUtils.isNullOrEmpty(text))
                return false;
            return text.toLowerCase(Locale.ROOT).startsWith("-inf") || text.toLowerCase(Locale.ROOT).startsWith("inf") || text.equalsIgnoreCase("nan") || text.equalsIgnoreCase("na");
        }
        return false;
    }

    private double createNumber(String text) {
        if (NumberUtils.isCreatable(text))
            return NumberUtils.createDouble(text);
        if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class ||
                getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            if (StringUtils.isNullOrEmpty(text))
                return 0;
            if (text.toLowerCase(Locale.ROOT).startsWith("-inf"))
                return Double.NEGATIVE_INFINITY;
            if (text.toLowerCase(Locale.ROOT).startsWith("inf"))
                return Double.POSITIVE_INFINITY;
            if (text.equalsIgnoreCase("nan") || text.equalsIgnoreCase("na"))
                return Double.NaN;
        }
        return 0;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(UIUtils.createControlBorder());
        numberField = new JTextField();
        numberField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        numberField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        numberField.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                String text = StringUtils.orElse(numberField.getText(), "");
                text = text.replace(',', '.').replace(" ", ""); // Allow usage of comma as separator
                if (isValidNumber(text)) {
                    pushValue(text);
                } else {
                    setBorder(UIUtils.createControlErrorBorder());
                }
            }
        });
        numberField.addActionListener(e -> {
            // Try using an expression
            try {
                Object result = JIPipeExpressionParameter.getEvaluatorInstance().evaluate(numberField.getText());
                if (result instanceof Number) {
                    numberField.setText(formatNumber(((Number) result).doubleValue()));
                }
            } catch (Exception e1) {
            }
        });
        add(numberField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(UIManager.getColor("TextField.background"));
        JButton buttonUp = new JButton(UIUtils.getIconFromResources("actions/caret-up.png"));
        buttonUp.setBackground(UIManager.getColor("TextField.background"));
        buttonUp.setPreferredSize(new Dimension(21, 14));
        buttonUp.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        buttonUp.addActionListener(e -> increase());
        buttonPanel.add(buttonUp, BorderLayout.NORTH);

        JButton buttonDown = new JButton(UIUtils.getIconFromResources("actions/caret-down.png"));
        buttonDown.setBackground(UIManager.getColor("TextField.background"));
        buttonDown.setPreferredSize(new Dimension(21, 14));
        buttonDown.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        buttonDown.addActionListener(e -> decrease());
        buttonPanel.add(buttonDown, BorderLayout.SOUTH);
        add(buttonPanel, BorderLayout.EAST);
    }

    private void decrease() {
        double step = getStep();
        double value = getCurrentValue();
        double newValue = Math.max(getMinimumValue(), value - step);
        numberField.setText(formatNumber(newValue));
    }

    private void increase() {
        double step = getStep();
        double value = getCurrentValue();
        double newValue = Math.min(getMaximumValue(), value + step);
        numberField.setText(formatNumber(newValue));
    }

    private void pushValue(String text) {
        setBorder(UIUtils.createControlBorder());
        double newValue = createNumber(text);
        double currentValue = getCurrentValue();
        if (newValue != currentValue) {
            if (setCurrentValue(newValue)) {
                setBorder(UIUtils.createControlBorder());
            } else {
                setBorder(UIUtils.createControlErrorBorder());
            }
        }
    }
}
