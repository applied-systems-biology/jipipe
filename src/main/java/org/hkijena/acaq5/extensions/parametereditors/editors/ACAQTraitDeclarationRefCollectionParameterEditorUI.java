package org.hkijena.acaq5.extensions.parametereditors.editors;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefCollection;
import org.hkijena.acaq5.ui.components.ACAQTraitPicker;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Editor for {@link ACAQTraitDeclarationRefCollection}
 */
public class ACAQTraitDeclarationRefCollectionParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQTraitPicker picker;
    private JPanel currentlyDisplayed;
    private JDialog pickerDialog;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public ACAQTraitDeclarationRefCollectionParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JPanel();
        currentlyDisplayed.setLayout(new BoxLayout(currentlyDisplayed, BoxLayout.X_AXIS));
        currentlyDisplayed.setBorder(BorderFactory.createEtchedBorder());
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
        ACAQTraitDeclarationRefCollection declarationRefs = getParameterAccess().get();
        if (declarationRefs == null) {
            declarationRefs = new ACAQTraitDeclarationRefCollection();
        }
        currentlyDisplayed.removeAll();
        if (declarationRefs.isEmpty()) {
            currentlyDisplayed.add(new JLabel("None selected"));
        } else if (declarationRefs.size() == 1) {
            ACAQTraitDeclaration declaration = declarationRefs.get(0).getDeclaration();
            JLabel label = new JLabel(declaration.getName(), ACAQUITraitRegistry.getInstance().getIconFor(declaration), JLabel.LEFT);
            label.setToolTipText(TooltipUtils.getTraitTooltip(declaration));
            currentlyDisplayed.add(label);
        } else {
            for (ACAQTraitDeclarationRef declarationRef : declarationRefs) {
                ACAQTraitDeclaration declaration = declarationRef.getDeclaration();
                JLabel label = new JLabel();
                label.setIcon(ACAQUITraitRegistry.getInstance().getIconFor(declaration));
                label.setToolTipText(TooltipUtils.getTraitTooltip(declaration));
                currentlyDisplayed.add(label);
            }
        }
        currentlyDisplayed.revalidate();
        currentlyDisplayed.repaint();
    }

    private void initializePicker() {
        Class<? extends ACAQTrait> baseClass = ACAQTrait.class;
        boolean showHidden = false;
        ACAQTraitParameterSettings settings = getParameterAccess().getAnnotationOfType(ACAQTraitParameterSettings.class);
        if (settings != null) {
            baseClass = settings.traitBaseClass();
            showHidden = settings.showHidden();
        }

        Set<ACAQTraitDeclaration> availableTraits = new HashSet<>();
        for (ACAQTraitDeclaration traitDeclaration : ACAQTraitRegistry.getInstance().getRegisteredTraits().values()) {
            if (traitDeclaration.isHidden() && !showHidden)
                continue;
            if (baseClass.isAssignableFrom(traitDeclaration.getTraitClass())) {
                availableTraits.add(traitDeclaration);
            }
        }

        picker = new ACAQTraitPicker(ACAQTraitPicker.Mode.Multiple, availableTraits);
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
    public void onTraitSelected(ACAQTraitPicker.SelectedTraitsChangedEvent event) {
        if (pickerDialog.isVisible()) {
            ACAQTraitDeclarationRefCollection refs = getParameterAccess().get();
            refs.clear();
            for (ACAQTraitDeclaration selectedTrait : event.getTraitPicker().getSelectedTraits()) {
                refs.add(new ACAQTraitDeclarationRef(selectedTrait));
            }

            getParameterAccess().set(refs);
            reload();
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

}
