package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.Font;

/**
 * Editor for {@link StringFilter}
 */
public class StringFilterParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public StringFilterParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        removeAll();
        StringFilter filter = getParameterAccess().get(StringFilter.class);
        if (filter == null) {
            getParameterAccess().set(new StringFilter());
            return;
        }
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
                StringFilter.Mode.Equals,
                "String equals filter text");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("text2.png"),
                StringFilter.Mode.Contains,
                "String contains filter text");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("regex.png"),
                StringFilter.Mode.Regex,
                "String matches Regex pattern (e.g. .*\\.txt)");
        revalidate();
        repaint();
    }

    private void addFilterModeSelection(StringFilter filter, ButtonGroup group, Icon icon, StringFilter.Mode mode, String description) {
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
