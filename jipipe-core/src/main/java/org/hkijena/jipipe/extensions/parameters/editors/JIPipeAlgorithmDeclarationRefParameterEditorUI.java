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
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.registries.JIPipeAlgorithmRegistry;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeAlgorithmDeclarationRef;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.JIPipeAlgorithmDeclarationPicker;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Parameter for {@link JIPipeAlgorithmDeclarationRef}
 */
public class JIPipeAlgorithmDeclarationRefParameterEditorUI extends JIPipeParameterEditorUI {

    private JIPipeAlgorithmDeclarationPicker picker;
    private JButton currentlyDisplayed;
    private JDialog pickerDialog;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public JIPipeAlgorithmDeclarationRefParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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
        JIPipeAlgorithmDeclarationRef declarationRef = getParameter(JIPipeAlgorithmDeclarationRef.class);
        JIPipeAlgorithmDeclaration declaration = declarationRef.getDeclaration();
        if (declaration != null) {
            currentlyDisplayed.setText(declaration.getName());
            currentlyDisplayed.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
            currentlyDisplayed.addActionListener(e -> pickTrait());
            currentlyDisplayed.setIcon(UIUtils.getIconFromColor(UIUtils.getFillColorFor(declaration)));
            picker.setSelectedDeclarations(Collections.singleton(declaration));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("error.png"));
            picker.setSelectedDeclarations(Collections.emptySet());
        }
    }

    private void initializePicker() {
        picker = new JIPipeAlgorithmDeclarationPicker(JIPipeAlgorithmDeclarationPicker.Mode.Single, JIPipeAlgorithmRegistry.getInstance().getRegisteredAlgorithms().values().stream()
                .filter(d -> d.getCategory() != JIPipeAlgorithmCategory.Internal).collect(Collectors.toSet()));
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
    public void onAlgorithmSelected(JIPipeAlgorithmDeclarationPicker.AlgorithmSelectedEvent event) {
        if (pickerDialog.isVisible()) {
            JIPipeAlgorithmDeclarationRef declarationRef = getParameter(JIPipeAlgorithmDeclarationRef.class);
            declarationRef.setDeclaration(event.getDeclaration());
            setParameter(declarationRef, true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
