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

package org.hkijena.jipipe.extensions.parameters.predicates;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link DoublePredicate}
 */
public class DoublePredicateParameterEditorUI extends JIPipeParameterEditorUI {

    private boolean isReloading;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public DoublePredicateParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
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

        isReloading = true;
        removeAll();
        DoublePredicate filter = getParameter(DoublePredicate.class);

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
