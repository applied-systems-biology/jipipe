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
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeNodeInfoRef;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.JIPipeNodeInfoPicker;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Parameter for {@link JIPipeNodeInfoRef}
 */
public class JIPipeNodeInfoRefParameterEditorUI extends JIPipeParameterEditorUI {

    private JIPipeNodeInfoPicker picker;
    private JButton currentlyDisplayed;
    private JDialog pickerDialog;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public JIPipeNodeInfoRefParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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
        selectButton.setToolTipText("Select algorithm type");
        selectButton.addActionListener(e -> pickTrait());
        add(selectButton, BorderLayout.EAST);

        initializePicker();
        initializePickerDialog();
    }

    private void initializePickerDialog() {
        pickerDialog = new JDialog();
        pickerDialog.setTitle("Select algorithm type");
        pickerDialog.setContentPane(picker);
        pickerDialog.setModal(false);
    }

    @Override
    public void reload() {
        JIPipeNodeInfoRef infoRef = getParameter(JIPipeNodeInfoRef.class);
        JIPipeNodeInfo info = infoRef.getInfo();
        if (info != null) {
            currentlyDisplayed.setText(info.getName());
            currentlyDisplayed.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
            currentlyDisplayed.addActionListener(e -> pickTrait());
            currentlyDisplayed.setIcon(UIUtils.getIconFromColor(UIUtils.getFillColorFor(info)));
            picker.setSelectedInfos(Collections.singleton(info));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("error.png"));
            picker.setSelectedInfos(Collections.emptySet());
        }
    }

    private void initializePicker() {
        picker = new JIPipeNodeInfoPicker(JIPipeNodeInfoPicker.Mode.Single, JIPipeNodeRegistry.getInstance().getRegisteredNodeInfos().values().stream()
                .filter(d -> d.getCategory() != JIPipeNodeCategory.Internal).collect(Collectors.toSet()));
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
    public void onAlgorithmSelected(JIPipeNodeInfoPicker.NodeSelectedEvent event) {
        if (pickerDialog.isVisible()) {
            JIPipeNodeInfoRef infoRef = getParameter(JIPipeNodeInfoRef.class);
            infoRef.setInfo(event.getInfo());
            setParameter(infoRef, true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
