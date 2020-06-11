package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;

/**
 * Editor for {@link Pair}
 */
public class PairParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public PairParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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

        Pair<?, ?> renaming = getParameterAccess().get(Pair.class);
        PairParameterKeyAccess<?, ?> keyAccess = new PairParameterKeyAccess<>(getParameterAccess(), renaming);
        PairParameterValueAccess<?, ?> valueAccess = new PairParameterValueAccess<>(getParameterAccess(), renaming);

        add(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), keyAccess));
        add(new JLabel(UIUtils.getIconFromResources("chevron-right.png")));
        add(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), valueAccess));

        revalidate();
        repaint();
    }
}
