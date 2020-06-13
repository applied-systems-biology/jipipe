package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

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

        String keyLabel = "";
        String valueLabel = "";
        boolean singleRow = true;
        boolean singleRowChevron = true;

        if (getParameterAccess().getAnnotationOfType(PairParameterSettings.class) != null) {
            PairParameterSettings settings = getParameterAccess().getAnnotationOfType(PairParameterSettings.class);
            keyLabel = settings.keyLabel();
            valueLabel = settings.valueLabel();
            singleRow = settings.singleRow();
            singleRowChevron = settings.singleRowWithChevron();
        }

        ACAQParameterEditorUI keyEditor = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), keyAccess);
        ACAQParameterEditorUI valueEditor = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), valueAccess);

        if (singleRow) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(new JLabel(keyLabel));
            if (!StringUtils.isNullOrEmpty(keyLabel))
                add(Box.createHorizontalStrut(4));
            add(keyEditor);
            if (singleRowChevron)
                add(new JLabel(UIUtils.getIconFromResources("chevron-right.png")));
            add(new JLabel(valueLabel));
            if (!StringUtils.isNullOrEmpty(valueLabel))
                add(Box.createHorizontalStrut(4));
            add(valueEditor);
        } else {
            setLayout(new BorderLayout());
            FormPanel panel = new FormPanel(null, FormPanel.NONE);
            panel.addToForm(new JLabel(keyLabel), keyEditor, null);
            panel.addToForm(new JLabel(valueLabel), valueEditor, null);
            add(panel, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }
}
