package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQTraversedParameterCollection;
import org.hkijena.acaq5.extensions.parameters.filters.StringRenaming;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;

/**
 * Editor for {@link org.hkijena.acaq5.extensions.parameters.filters.StringRenaming}
 */
public class StringRenamingParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public StringRenamingParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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

        StringRenaming renaming = getParameterAccess().get();
        ACAQTraversedParameterCollection parameterCollection = new ACAQTraversedParameterCollection(renaming);

        add(new StringFilterParameterEditorUI(getContext(), parameterCollection.getParameters().get("filter")));
        add(new JLabel(UIUtils.getIconFromResources("chevron-right.png")));
        add(new StringParameterEditorUI(getContext(), parameterCollection.getParameters().get("target")));

        revalidate();
        repaint();
    }
}
