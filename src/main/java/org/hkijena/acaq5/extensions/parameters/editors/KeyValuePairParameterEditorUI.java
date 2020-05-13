package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.collections.KeyValuePairParameter;
import org.hkijena.acaq5.extensions.parameters.collections.KeyValueParameterKeyAccess;
import org.hkijena.acaq5.extensions.parameters.collections.KeyValueParameterValueAccess;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;

/**
 * Editor for {@link org.hkijena.acaq5.extensions.parameters.collections.KeyValuePairParameter}
 */
public class KeyValuePairParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public KeyValuePairParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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

        KeyValuePairParameter<?, ?> renaming = getParameterAccess().get(KeyValuePairParameter.class);
        KeyValueParameterKeyAccess<?, ?> keyAccess = new KeyValueParameterKeyAccess<>(getParameterAccess(), renaming);
        KeyValueParameterValueAccess<?, ?> valueAccess = new KeyValueParameterValueAccess<>(getParameterAccess(), renaming);

        add(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), keyAccess));
        add(new JLabel(UIUtils.getIconFromResources("chevron-right.png")));
        add(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), valueAccess));

        revalidate();
        repaint();
    }
}
