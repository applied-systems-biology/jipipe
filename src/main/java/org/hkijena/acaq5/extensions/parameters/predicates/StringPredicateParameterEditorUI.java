package org.hkijena.acaq5.extensions.parameters.predicates;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;

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
