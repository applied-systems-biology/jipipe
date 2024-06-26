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

package org.hkijena.jipipe.plugins.parameters.library.primitives.vectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.plugins.parameters.library.quantities.QuantityParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class VectorDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final JXTextField xEditor = new JXTextField();
    private final JXTextField yEditor = new JXTextField();
    private final JXTextField zEditor = new JXTextField();
    private boolean isUpdatingTextBoxes = false;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public VectorDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(UIUtils.createControlBorder());

        String xLabel = "X";
        String yLabel = "Y";
        String zLabel = "Z";
        VectorParameterSettings settings = getParameterAccess().getAnnotationOfType(VectorParameterSettings.class);
        if (settings != null) {
            xLabel = settings.xLabel();
            yLabel = settings.yLabel();
            zLabel = settings.zLabel();
        }

        List<JXTextField> editors;
        List<String> labels;

        if (Vector2Parameter.class.isAssignableFrom(getParameterFieldClass())) {
            editors = Arrays.asList(xEditor, yEditor);
            labels = Arrays.asList(xLabel, yLabel);
        } else if (Vector3Parameter.class.isAssignableFrom(getParameterFieldClass())) {
            editors = Arrays.asList(xEditor, yEditor, zEditor);
            labels = Arrays.asList(xLabel, yLabel, zLabel);
        } else {
            throw new UnsupportedOperationException("Unsupported parameter type!");
        }

        for (int i = 0; i < editors.size(); i++) {
            JXTextField editor = editors.get(i);
            editor.setText("0");
            editor.setBorder(UIUtils.createControlBorder());
            editor.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
                @Override
                public void changed(DocumentEvent documentEvent) {
                    if(!isUpdatingTextBoxes) {
                        writeToParameter();
                    }
                }
            });
            editor.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
                @Override
                public void changed(DocumentEvent documentEvent) {
                    if (!isUpdatingTextBoxes) {
                        writeToParameter();
                    }
                }
            });

            JLabel label = new JLabel(labels.get(i));

            if (i > 0) {
                label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY),
                        BorderFactory.createEmptyBorder(0, 16, 0, 4)));
            }

            add(label, new GridBagConstraints(i * 2, 0, 1, 1, 0, 0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
            add(editor, new GridBagConstraints(i * 2 + 1, 0, 1, 1, 1, 0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        }
    }

    private void writeToParameter() {

        String sx = StringUtils.nullToEmpty(xEditor.getText()).replace(',', '.').replace(" ", "");
        String sy = StringUtils.nullToEmpty(yEditor.getText()).replace(',', '.').replace(" ", "");
        String sz = StringUtils.nullToEmpty(zEditor.getText()).replace(',', '.').replace(" ", "");

        boolean success = true;
        if(!NumberUtils.isCreatable(sx)) {
            success = false;
            xEditor.setBorder(UIUtils.createControlErrorBorder());
        }
        else {
            xEditor.setBorder(UIUtils.createControlBorder());
        }
        if(!NumberUtils.isCreatable(sy)) {
            success = false;
            yEditor.setBorder(UIUtils.createControlErrorBorder());
        }
        else {
            yEditor.setBorder(UIUtils.createControlBorder());
        }
        if(!NumberUtils.isCreatable(sz)) {
            success = false;
            zEditor.setBorder(UIUtils.createControlErrorBorder());
        }
        else {
            zEditor.setBorder(UIUtils.createControlBorder());
        }

        if (success) {
            if (getParameterFieldClass() == Vector2dParameter.class) {
                setParameter(new Vector2dParameter(NumberUtils.createDouble(sx), NumberUtils.createDouble(sy)), false);
            } else if (getParameterFieldClass() == Vector2iParameter.class) {
                setParameter(new Vector2iParameter(NumberUtils.createDouble(sx).intValue(), NumberUtils.createDouble(sy).intValue()), false);
            } else if (getParameterFieldClass() == Vector3dParameter.class) {
                setParameter(new Vector3dParameter(NumberUtils.createDouble(sx), NumberUtils.createDouble(sy), NumberUtils.createDouble(sz)), false);
            } else if (getParameterFieldClass() == Vector3iParameter.class) {
                setParameter(new Vector3iParameter(NumberUtils.createDouble(sx).intValue(), NumberUtils.createDouble(sy).intValue(), NumberUtils.createDouble(sz).intValue()), false);
            }
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        updateTextFields();
        revalidate();
    }

    private void updateTextFields() {
        try {
            isUpdatingTextBoxes = true;
            Object obj = getParameter(Object.class);
            if (obj instanceof Vector2dParameter) {
                xEditor.setText(String.valueOf(((Vector2dParameter) obj).getX()));
                yEditor.setText(String.valueOf(((Vector2dParameter) obj).getY()));
                zEditor.setText("0");
            } else if (obj instanceof Vector2iParameter) {
                xEditor.setText(String.valueOf(((Vector2iParameter) obj).getX()));
                yEditor.setText(String.valueOf(((Vector2iParameter) obj).getY()));
                zEditor.setText("0");
            } else if (obj instanceof Vector3dParameter) {
                xEditor.setText(String.valueOf(((Vector3dParameter) obj).getX()));
                yEditor.setText(String.valueOf(((Vector3dParameter) obj).getY()));
                zEditor.setText(String.valueOf(((Vector3dParameter) obj).getZ()));
            } else if (obj instanceof Vector3iParameter) {
                xEditor.setText(String.valueOf(((Vector3iParameter) obj).getX()));
                yEditor.setText(String.valueOf(((Vector3iParameter) obj).getY()));
                zEditor.setText(String.valueOf(((Vector3iParameter) obj).getZ()));
            }
        } finally {
            isUpdatingTextBoxes = false;
        }
    }
}
