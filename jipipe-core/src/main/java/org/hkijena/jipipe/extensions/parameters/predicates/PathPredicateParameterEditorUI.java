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
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Editor for {@link PathPredicate}
 */
public class PathPredicateParameterEditorUI extends JIPipeParameterEditorUI {

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public PathPredicateParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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
        PathPredicate filter = getParameter(PathPredicate.class);

        JToggleButton invertButton = new JToggleButton(UIUtils.getIconFromResources("not.png"));
        UIUtils.makeFlat25x25(invertButton);
        invertButton.setToolTipText("Inverts the predicate");
        invertButton.setSelected(filter.isInvert());
        invertButton.addActionListener(e -> {
            filter.setInvert(invertButton.isSelected());
            setParameter(filter, false);
        });
        add(invertButton);
        add(Box.createHorizontalStrut(4));

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
                UIUtils.getIconFromResources("text2.png"),
                PathPredicate.Mode.Contains,
                "Filename contains filter text");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("glob.png"),
                PathPredicate.Mode.Glob,
                "Filename matches Glob-pattern (e.g. *.txt)");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("regex.png"),
                PathPredicate.Mode.Regex,
                "Filename matches Regex pattern (e.g. .*\\.txt)");
        revalidate();
        repaint();
    }

    private void addFilterModeSelection(PathPredicate filter, ButtonGroup group, Icon icon, PathPredicate.Mode mode, String description) {
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
