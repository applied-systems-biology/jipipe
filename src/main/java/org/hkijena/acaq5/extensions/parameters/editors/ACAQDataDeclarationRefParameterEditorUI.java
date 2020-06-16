package org.hkijena.acaq5.extensions.parameters.editors;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.parameters.references.ACAQDataDeclarationRef;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.ACAQDataTypePicker;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameter for {@link ACAQDataDeclarationRef}
 */
public class ACAQDataDeclarationRefParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQDataTypePicker picker;
    private JButton currentlyDisplayed;
    private JDialog pickerDialog;
    private boolean isReloading = false;

    /**
     * @param workbench        workbench
     * @param parameterAccess the parameter
     */
    public ACAQDataDeclarationRefParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
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
        ACAQDataDeclarationRef declarationRef = getParameter(ACAQDataDeclarationRef.class);
        ACAQDataDeclaration declaration = declarationRef.getDeclaration();
        if (declaration != null) {
            currentlyDisplayed.setText(declaration.getName());
            currentlyDisplayed.setToolTipText(TooltipUtils.getDataTooltip(declaration));
            currentlyDisplayed.setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(declaration.getDataClass()));
            if (!pickerDialog.isVisible())
                picker.setSelectedDataTypes(Collections.singleton(declaration));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("error.png"));
            if (!pickerDialog.isVisible())
                picker.setSelectedDataTypes(Collections.emptySet());
        }
        isReloading = false;
    }

    private void initializePicker() {
        Class<? extends ACAQData> baseClass = ACAQData.class;
        boolean showHidden = false;
        ACAQDataParameterSettings settings = getParameterAccess().getAnnotationOfType(ACAQDataParameterSettings.class);
        if (settings != null) {
            baseClass = settings.dataBaseClass();
            showHidden = settings.showHidden();
        }

        Set<ACAQDataDeclaration> availableTraits = new HashSet<>();
        for (Class<? extends ACAQData> klass : ACAQDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            ACAQDataDeclaration declaration = ACAQDataDeclaration.getInstance(klass);
            if (declaration.isHidden() && !showHidden)
                continue;
            if (baseClass.isAssignableFrom(declaration.getDataClass())) {
                availableTraits.add(declaration);
            }
        }

        picker = new ACAQDataTypePicker(ACAQDataTypePicker.Mode.Single, availableTraits);
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
    public void onTraitSelected(ACAQDataTypePicker.SelectedDataTypesChangedEvent event) {
        if (pickerDialog.isVisible()) {
            ACAQDataDeclarationRef declarationRef = getParameter(ACAQDataDeclarationRef.class);
            declarationRef.setDeclaration(picker.getSelectedDataTypes().isEmpty() ? null : picker.getSelectedDataTypes().iterator().next());
            getParameterAccess().set(declarationRef);
            reload();
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
