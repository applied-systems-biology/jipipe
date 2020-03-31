package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.ui.components.ACAQTraitPicker;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameter for {@link ACAQTraitDeclarationRef}
 */
public class ACAQTraitDeclarationRefParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQTraitPicker picker;
    private JButton currentlyDisplayed;
    private JDialog pickerDialog;

    /**
     * @param context SciJava context
     * @param parameterAccess the parameter
     */
    public ACAQTraitDeclarationRefParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
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
        selectButton.setToolTipText("Select annotation");
        selectButton.addActionListener(e -> pickTrait());
        add(selectButton, BorderLayout.EAST);

        initializePicker();
        initializePickerDialog();
    }

    private void initializePickerDialog() {
        pickerDialog = new JDialog();
        pickerDialog.setTitle("Select annotation");
        pickerDialog.setContentPane(picker);
        pickerDialog.setModal(false);
    }

    @Override
    public void reload() {
        ACAQTraitDeclarationRef declarationRef = getParameterAccess().get();
        ACAQTraitDeclaration declaration = declarationRef.getDeclaration();
        if (declaration != null) {
            currentlyDisplayed.setText(declaration.getName());
            currentlyDisplayed.setToolTipText(TooltipUtils.getTraitTooltip(declaration));
            currentlyDisplayed.addActionListener(e -> pickTrait());
            currentlyDisplayed.setIcon(ACAQUITraitRegistry.getInstance().getIconFor(declaration));
            picker.setSelectedTraits(Collections.singleton(declaration));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("error.png"));
            picker.setSelectedTraits(Collections.emptySet());
        }
    }

    private void initializePicker() {
        Class<? extends ACAQTrait> baseClass = ACAQTrait.class;
        ACAQTraitDeclarationRefParameterSettings settings = getParameterAccess().getAnnotationOfType(ACAQTraitDeclarationRefParameterSettings.class);
        if (settings != null) {
            baseClass = settings.traitBaseClass();
        }

        Set<ACAQTraitDeclaration> availableTraits = new HashSet<>();
        for (ACAQTraitDeclaration traitDeclaration : ACAQTraitRegistry.getInstance().getRegisteredTraits().values()) {
            if (traitDeclaration.isHidden())
                continue;
            if (baseClass.isAssignableFrom(traitDeclaration.getTraitClass())) {
                availableTraits.add(traitDeclaration);
            }
        }

        picker = new ACAQTraitPicker(ACAQTraitPicker.Mode.Single, availableTraits);
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
     * @param event Generated event
     */
    @Subscribe
    public void onTraitSelected(ACAQTraitPicker.TraitSelectedEvent event) {
        if (pickerDialog.isVisible()) {
            ACAQTraitDeclarationRef declarationRef = getParameterAccess().get();
            declarationRef.setDeclaration(event.getTraitDeclaration());
            getParameterAccess().set(declarationRef);
            reload();
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
