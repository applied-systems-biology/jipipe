package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.parameters.api.enums.DefaultEnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.DynamicStringEnumParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.annotation.Annotation;
import java.util.Objects;

public class OMEAccessorParameterEditorUI extends JIPipeParameterEditorUI {

    private OMEAccessorTypeEnumParameter typeParameter = new OMEAccessorTypeEnumParameter();
    private JIPipeManualParameterAccess typeParameterAccess;
    private ParameterPanel parameterPanel;

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public OMEAccessorParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        FormPanel formPanel = new FormPanel(FormPanel.NONE);
        add(formPanel, BorderLayout.CENTER);

        OMEAccessorTypeEnumParameter typeParameter = new OMEAccessorTypeEnumParameter();
        typeParameterAccess = JIPipeManualParameterAccess.builder().setSource(new JIPipeDummyParameterCollection())
                .setGetter(() -> typeParameter)
                .setSetter(this::omeTypeChanged)
                .setFieldClass(OMEAccessorTypeEnumParameter.class)
                .setKey("type")
                .addAnnotation(new EnumParameterSettings() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return EnumParameterSettings.class;
                    }

                    @Override
                    public Class<? extends EnumItemInfo> itemInfo() {
                        return DefaultEnumItemInfo.class;
                    }

                    @Override
                    public boolean searchable() {
                        return true;
                    }
                })
                .build();
        JIPipeParameterEditorUI typeParameterUI = JIPipe.getParameterTypes().createEditorFor(getWorkbench(), typeParameterAccess);
        formPanel.addWideToForm(typeParameterUI);

        parameterPanel = new ParameterPanel(getWorkbench(), new JIPipeDummyParameterCollection(), new MarkdownDocument(),
                ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_NO_UI);
        formPanel.addWideToForm(parameterPanel);
    }

    private void omeTypeChanged(Object value) {
        if(value instanceof OMEAccessorTypeEnumParameter) {
            typeParameter = (OMEAccessorTypeEnumParameter) value;
            OMEAccessorTypeEnumParameter enumParameter = (OMEAccessorTypeEnumParameter) value;
            if(!StringUtils.isNullOrEmpty(enumParameter.getValue()) && ImageJAlgorithmsExtension.OME_ACCESSOR_STORAGE.getTemplateMap().containsKey(enumParameter.getValue())) {
                OMEAccessorParameter parameter = getParameter(OMEAccessorParameter.class);
                parameter.setAccessorId(enumParameter.getValue());
                parameter.resetParameters();
                reload();
            }
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        OMEAccessorParameter parameter = getParameter(OMEAccessorParameter.class);
        if(!Objects.equals(typeParameter.getValue(), parameter.getAccessorId())) {
            OMEAccessorTypeEnumParameter typeEnumParameter = new OMEAccessorTypeEnumParameter();
            typeEnumParameter.setValue(parameter.getAccessorId());
            typeParameterAccess.set(typeEnumParameter);
            return;
        }
        parameterPanel.setDisplayedParameters(parameter.getParameters());
    }
}
