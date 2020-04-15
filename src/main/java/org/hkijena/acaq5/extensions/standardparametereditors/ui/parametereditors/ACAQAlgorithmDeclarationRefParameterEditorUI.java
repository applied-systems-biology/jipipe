package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclarationRef;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.components.ACAQAlgorithmPicker;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Parameter for {@link ACAQAlgorithmDeclarationRef}
 */
public class ACAQAlgorithmDeclarationRefParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQAlgorithmPicker picker;
    private JButton currentlyDisplayed;
    private JDialog pickerDialog;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public ACAQAlgorithmDeclarationRefParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        ACAQAlgorithmDeclarationRef declarationRef = getParameterAccess().get();
        if (declarationRef == null) {
            declarationRef = new ACAQAlgorithmDeclarationRef();
        }
        ACAQAlgorithmDeclaration declaration = declarationRef.getDeclaration();
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
        picker = new ACAQAlgorithmPicker(ACAQAlgorithmPicker.Mode.Single, ACAQAlgorithmRegistry.getInstance().getRegisteredAlgorithms().values().stream()
                .filter(d -> d.getCategory() != ACAQAlgorithmCategory.Internal).collect(Collectors.toSet()));
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
    public void onAlgorithmSelected(ACAQAlgorithmPicker.AlgorithmSelectedEvent event) {
        if (pickerDialog.isVisible()) {
            ACAQAlgorithmDeclarationRef declarationRef = getParameterAccess().get();
            declarationRef.setDeclaration(event.getDeclaration());
            getParameterAccess().set(declarationRef);
            reload();
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
