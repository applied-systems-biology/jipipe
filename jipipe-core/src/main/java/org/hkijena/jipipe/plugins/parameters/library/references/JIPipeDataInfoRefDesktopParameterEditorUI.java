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

package org.hkijena.jipipe.plugins.parameters.library.references;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.pickers.JIPipeDesktopDataTypePicker;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.classfilters.AnyClassFilter;
import org.hkijena.jipipe.utils.classfilters.ClassFilter;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameter for {@link JIPipeDataInfoRef}
 */
public class JIPipeDataInfoRefDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI implements JIPipeDesktopDataTypePicker.SelectedDataTypesChangedEventListener {

    private JIPipeDesktopDataTypePicker picker;
    private JButton currentlyDisplayed;
    private JDialog pickerDialog;
    private boolean isReloading = false;

    public JIPipeDataInfoRefDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.addActionListener(e -> pickDataInfo());
        UIUtils.setStandardButtonBorder(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        UIUtils.setStandardButtonBorder(selectButton);
        selectButton.setToolTipText("Select data type");
        selectButton.addActionListener(e -> pickDataInfo());
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
        JIPipeDataInfo info;
        try {
            JIPipeDataInfoRef infoRef = getParameter(JIPipeDataInfoRef.class);
            info = infoRef.getInfo();
        } catch (NullPointerException ignored) {
            info = null;
        }
        if (info != null) {
            currentlyDisplayed.setText(info.getName());
            currentlyDisplayed.setToolTipText(TooltipUtils.getDataTooltip(info));
            currentlyDisplayed.setIcon(JIPipe.getDataTypes().getIconFor(info.getDataClass()));
            if (!pickerDialog.isVisible())
                picker.setSelectedDataTypes(Collections.singleton(info));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            if (!pickerDialog.isVisible())
                picker.setSelectedDataTypes(Collections.emptySet());
        }
        isReloading = false;
    }

    private void initializePicker() {
        Class<? extends JIPipeData> baseClass = JIPipeData.class;
        ClassFilter classFilter = new AnyClassFilter();
        boolean showHidden = false;
        JIPipeDataParameterSettings settings = getParameterAccess().getAnnotationOfType(JIPipeDataParameterSettings.class);
        if (settings != null) {
            baseClass = settings.dataBaseClass();
            showHidden = settings.showHidden();
            classFilter = (ClassFilter) ReflectionUtils.newInstance(settings.dataClassFilter());
        }

        Set<JIPipeDataInfo> availableInfos = new HashSet<>();
        for (Class<? extends JIPipeData> klass : JIPipe.getDataTypes().getRegisteredDataTypes().values()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(klass);
            if (info.isHidden() && !showHidden)
                continue;
            if (baseClass.isAssignableFrom(info.getDataClass()) && classFilter.test(info.getDataClass())) {
                availableInfos.add(info);
            }
        }

        picker = new JIPipeDesktopDataTypePicker(JIPipeDesktopDataTypePicker.Mode.Single, availableInfos);
        picker.getSelectedDataTypesChangedEventEmitter().subscribe(this);
    }

    private void pickDataInfo() {
        pickerDialog.pack();
        pickerDialog.setSize(new Dimension(500, 400));
        pickerDialog.setLocationRelativeTo(this);
        pickerDialog.setVisible(true);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void onDataTypePickerSelectedDataTypesChanged(JIPipeDesktopDataTypePicker.SelectedDataTypesChangedEvent event) {
        if (pickerDialog.isVisible()) {
            pickerDialog.setVisible(false);
            JIPipeDataInfoRef infoRef = new JIPipeDataInfoRef(picker.getSelectedDataTypes().isEmpty() ? null : picker.getSelectedDataTypes().iterator().next());
            setParameter(infoRef, true);
        }
    }
}
