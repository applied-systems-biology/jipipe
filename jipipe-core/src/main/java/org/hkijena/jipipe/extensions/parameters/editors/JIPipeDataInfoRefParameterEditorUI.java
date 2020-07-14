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

package org.hkijena.jipipe.extensions.parameters.editors;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.JIPipeDataTypePicker;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameter for {@link JIPipeDataInfoRef}
 */
public class JIPipeDataInfoRefParameterEditorUI extends JIPipeParameterEditorUI {

    private JIPipeDataTypePicker picker;
    private JButton currentlyDisplayed;
    private JDialog pickerDialog;
    private boolean isReloading = false;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public JIPipeDataInfoRefParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.addActionListener(e -> pickTrait());
        UIUtils.makeFlat(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(UIUtils.getIconFromResources("edit.png"));
        UIUtils.makeFlat(selectButton);
        selectButton.setToolTipText("Select data type");
        selectButton.addActionListener(e -> pickTrait());
        add(selectButton, BorderLayout.EAST);

        initializePicker();
        initializePickerDialog();
    }

    private void initializePickerDialog() {
        pickerDialog = new JDialog();
        pickerDialog.setTitle("Select data type");
        pickerDialog.setContentPane(picker);
        pickerDialog.setModal(false);
    }

    @Override
    public void reload() {
        if (isReloading)
            return;
        isReloading = true;
        JIPipeDataInfoRef infoRef = getParameter(JIPipeDataInfoRef.class);
        JIPipeDataInfo info = infoRef.getInfo();
        if (info != null) {
            currentlyDisplayed.setText(info.getName());
            currentlyDisplayed.setToolTipText(TooltipUtils.getDataTooltip(info));
            currentlyDisplayed.setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(info.getDataClass()));
            if (!pickerDialog.isVisible())
                picker.setSelectedDataTypes(Collections.singleton(info));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("error.png"));
            if (!pickerDialog.isVisible())
                picker.setSelectedDataTypes(Collections.emptySet());
        }
        isReloading = false;
    }

    private void initializePicker() {
        Class<? extends JIPipeData> baseClass = JIPipeData.class;
        boolean showHidden = false;
        JIPipeDataParameterSettings settings = getParameterAccess().getAnnotationOfType(JIPipeDataParameterSettings.class);
        if (settings != null) {
            baseClass = settings.dataBaseClass();
            showHidden = settings.showHidden();
        }

        Set<JIPipeDataInfo> availableTraits = new HashSet<>();
        for (Class<? extends JIPipeData> klass : JIPipeDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(klass);
            if (info.isHidden() && !showHidden)
                continue;
            if (baseClass.isAssignableFrom(info.getDataClass())) {
                availableTraits.add(info);
            }
        }

        picker = new JIPipeDataTypePicker(JIPipeDataTypePicker.Mode.Single, availableTraits);
        picker.getEventBus().register(this);
    }

    private void pickTrait() {
        pickerDialog.pack();
        pickerDialog.setSize(new Dimension(500, 400));
        pickerDialog.setLocationRelativeTo(this);
        pickerDialog.setVisible(true);
    }

    /**
     * Triggered when a trait is selected
     *
     * @param event Generated event
     */
    @Subscribe
    public void onTraitSelected(JIPipeDataTypePicker.SelectedDataTypesChangedEvent event) {
        if (pickerDialog.isVisible()) {
            JIPipeDataInfoRef infoRef = getParameter(JIPipeDataInfoRef.class);
            infoRef.setInfo(picker.getSelectedDataTypes().isEmpty() ? null : picker.getSelectedDataTypes().iterator().next());
            setParameter(infoRef, true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
