package org.hkijena.acaq5.extensions.standardparametereditors.ui;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.PathFilter;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Editor for {@link PathFilter}
 */
public class PathFilterParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public PathFilterParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        PathFilter filter = getParameterAccess().get();
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
                PathFilter.Mode.Contains,
                "Filename contains filter text");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("glob.png"),
                PathFilter.Mode.Glob,
                "Filename matches Glob-pattern (e.g. *.txt)");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("regex.png"),
                PathFilter.Mode.Regex,
                "Filename matches Regex pattern (e.g. .*\\.txt)");
        revalidate();
        repaint();
    }

    private void addFilterModeSelection(PathFilter filter, ButtonGroup group, Icon icon, PathFilter.Mode mode, String description) {
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
