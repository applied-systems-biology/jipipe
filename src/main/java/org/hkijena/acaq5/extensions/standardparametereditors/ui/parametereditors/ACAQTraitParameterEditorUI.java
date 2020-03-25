package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.components.ACAQTraitPicker;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ACAQTraitParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQTraitPicker picker;
    private JXTextField currentValue;
    private JButton currentlyDisplayed;
    private JDialog pickerDialog;
    private boolean skipNextReload = false;
    private boolean isReloading = false;

    public ACAQTraitParameterEditorUI(ACAQProjectUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new GridBagLayout());

        currentValue = new JXTextField();
        currentValue.setPrompt("Value");
        currentValue.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                onCurrentValueChanged(currentValue.getText());
            }
        });
        add(currentValue, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridwidth = 1;
                anchor = GridBagConstraints.WEST;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
                insets = UIUtils.UI_PADDING;
            }
        });

        currentlyDisplayed = new JButton();
        UIUtils.makeFlatH25(currentlyDisplayed);
        currentlyDisplayed.addActionListener(e -> pickTrait());
        add(currentlyDisplayed, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.PAGE_START;
                gridx = 1;
                gridy = 0;
            }
        });

        JButton selectButton = new JButton(UIUtils.getIconFromResources("edit.png"));
        UIUtils.makeFlat25x25(selectButton);
        selectButton.setToolTipText("Select annotation");
        selectButton.addActionListener(e -> pickTrait());
        add(selectButton, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.PAGE_START;
                gridx = 2;
                gridy = 0;
            }
        });

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
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        ACAQTrait trait = getParameterAccess().get();
        if (trait != null) {
            ACAQTraitDeclaration declaration = trait.getDeclaration();
            currentlyDisplayed.setText(declaration.getName());
            currentlyDisplayed.setToolTipText(TooltipUtils.getTraitTooltip(declaration));
            currentlyDisplayed.addActionListener(e -> pickTrait());
            currentlyDisplayed.setIcon(ACAQUITraitRegistry.getInstance().getIconFor(declaration));
            picker.setSelectedTraits(Collections.singleton(declaration));

            if (trait instanceof ACAQDiscriminator) {
                currentValue.setText(((ACAQDiscriminator) trait).getValue());
                currentValue.setEnabled(true);
            } else {
                currentValue.setText("<No value>");
                currentValue.setEnabled(false);
            }
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("error.png"));
            currentValue.setText("<No value>");
            currentValue.setEnabled(false);
            picker.setSelectedTraits(Collections.emptySet());
        }
        isReloading = false;
    }

    private void initializePicker() {
        Class<? extends ACAQTrait> baseClass = ACAQTrait.class;
        ACAQTraitParameterSettings settings = getParameterAccess().getAnnotationOfType(ACAQTraitParameterSettings.class);
        if (settings != null) {
            baseClass = settings.traitBaseClass();
        }

        Set<ACAQTraitDeclaration> availableTraits = new HashSet<>();
        for (ACAQTraitDeclaration traitDeclaration : ACAQTraitRegistry.getInstance().getRegisteredTraits().values()) {
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

    @Subscribe
    public void onTraitSelected(ACAQTraitPicker.TraitSelectedEvent event) {
        if (pickerDialog.isVisible()) {
            ACAQTraitDeclaration declaration = event.getTraitDeclaration();
            getParameterAccess().set(declaration.newInstance());
            reload();
        }
    }

    private void onCurrentValueChanged(String text) {
        if (!isReloading) {
            ACAQTrait trait = getParameterAccess().get();
            if (trait instanceof ACAQDiscriminator) {
                if (text != null && !text.isEmpty() && !text.equals(((ACAQDiscriminator) trait).getValue())) {
                    skipNextReload = true;
                    getParameterAccess().set(trait.getDeclaration().newInstance(text));
                }
            }
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
