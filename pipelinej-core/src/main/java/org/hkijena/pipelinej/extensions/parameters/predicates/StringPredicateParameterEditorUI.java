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

package org.hkijena.pipelinej.extensions.parameters.predicates;

import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.components.DocumentChangeListener;
import org.hkijena.pipelinej.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Editor for {@link StringPredicate}
 */
public class StringPredicateParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public StringPredicateParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
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
        removeAll();
        StringPredicate filter = getParameter(StringPredicate.class);
        JTextField filterStringEditor = new JTextField(filter.getFilterString());
        filterStringEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        filterStringEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                filter.setFilterString(filterStringEditor.getText());
            }
        });
        add(filterStringEditor);

        ButtonGroup group = new ButtonGroup();
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("equals.png"),
                StringPredicate.Mode.Equals,
                "String equals filter text");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("text2.png"),
                StringPredicate.Mode.Contains,
                "String contains filter text");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("regex.png"),
                StringPredicate.Mode.Regex,
                "String matches Regex pattern (e.g. .*\\.txt)");
        revalidate();
        repaint();
    }

    private void addFilterModeSelection(StringPredicate filter, ButtonGroup group, Icon icon, StringPredicate.Mode mode, String description) {
        JToggleButton toggleButton = new JToggleButton(icon);
        UIUtils.makeFlat25x25(toggleButton);
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected())
                filter.setMode(mode);
        });
        toggleButton.setToolTipText(description);
        toggleButton.setSelected(filter.getMode() == mode);
        group.add(toggleButton);
        add(toggleButton);
    }
}
