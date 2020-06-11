package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.predicates.DoublePredicate;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link DoublePredicate}
 */
public class DoubleFilterParameterEditorUI extends ACAQParameterEditorUI {

    private boolean isReloading;
    private boolean skipNextReload;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public DoubleFilterParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        if (isReloading)
            return;
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }

        isReloading = true;
        removeAll();
        DoublePredicate filter = getParameterAccess().get(DoublePredicate.class);
        if (filter == null) {
            getParameterAccess().set(new DoublePredicate());
            return;
        }

        ButtonGroup group = new ButtonGroup();
        addFilterModeSelection(filter,
                group,
                DoublePredicate.Mode.LessThan);
        addFilterModeSelection(filter,
                group,
                DoublePredicate.Mode.LessThanOrEquals);
        addFilterModeSelection(filter,
                group,
                DoublePredicate.Mode.Equals);
        addFilterModeSelection(filter,
                group,
                DoublePredicate.Mode.GreaterThanOrEquals);
        addFilterModeSelection(filter,
                group,
                DoublePredicate.Mode.GreaterThan);


        SpinnerNumberModel model = new SpinnerNumberModel(filter.getReference(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                filter.setReference(model.getNumber().doubleValue());
            }
        });
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner);

        revalidate();
        repaint();

        isReloading = false;
    }

    private void addFilterModeSelection(DoublePredicate filter, ButtonGroup group, DoublePredicate.Mode mode) {
        JToggleButton toggleButton = new JToggleButton(mode.getStringRepresentation());
        UIUtils.makeFlat25x25(toggleButton);
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected())
                filter.setMode(mode);
        });
        toggleButton.setSelected(filter.getMode() == mode);
        group.add(toggleButton);
        add(toggleButton);
    }
}
