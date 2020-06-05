package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.IntModificationParameter;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link org.hkijena.acaq5.extensions.parameters.IntModificationParameter}
 */
public class IntModificationParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public IntModificationParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        removeAll();
        IntModificationParameter modification = getParameterAccess().get(IntModificationParameter.class);
        if (modification == null) {
            getParameterAccess().set(new IntModificationParameter());
            return;
        }
        if (modification.isUseExactValue()) {
            SpinnerNumberModel model = new SpinnerNumberModel(modification.getExactValue(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
            JSpinner spinner = new JSpinner(model);
            spinner.addChangeListener(e -> {
                if (!isReloading) {
                    skipNextReload = true;
                    modification.setExactValue((int) model.getNumber());
                    getParameterAccess().set(modification);
                }
            });
            spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
            add(spinner);
        } else {
            SpinnerNumberModel model = new SpinnerNumberModel(modification.getFactor(), 0.0, Double.MAX_VALUE, 0.1d);
            JSpinner spinner = new JSpinner(model);
            spinner.addChangeListener(e -> {
                if (!isReloading) {
                    skipNextReload = true;
                    modification.setFactor((double) model.getNumber());
                    getParameterAccess().set(modification);
                }
            });
            spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
            add(spinner);
        }

        ButtonGroup group = new ButtonGroup();
        addModeSelection(modification,
                group,
                UIUtils.getIconFromResources("equals.png"),
                true,
                "Set to exact value");
        addModeSelection(modification,
                group,
                UIUtils.getIconFromResources("percent.png"),
                false,
                "Set to relative value (1 equals identity)");
        revalidate();
        repaint();

        isReloading = false;
    }

    private void addModeSelection(IntModificationParameter modification, ButtonGroup group, Icon icon, boolean mode, String description) {
        JToggleButton toggleButton = new JToggleButton(icon);
        UIUtils.makeFlat25x25(toggleButton);
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected())
                modification.setUseExactValue(mode);
            reload();
        });
        toggleButton.setToolTipText(description);
        toggleButton.setSelected(modification.isUseExactValue() == mode);
        group.add(toggleButton);
        add(toggleButton);
    }
}
