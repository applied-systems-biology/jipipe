package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link Pair}
 */
public class PairParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public PairParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        removeAll();

        Pair<?, ?> renaming = getParameter(Pair.class);
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

        ACAQParameterEditorUI keyEditor = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), keyAccess);
        ACAQParameterEditorUI valueEditor = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), valueAccess);

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
            panel.setBorder(BorderFactory.createEtchedBorder());
            panel.addToForm(keyEditor, new JLabel(keyLabel), null);
            panel.addToForm(valueEditor, new JLabel(valueLabel), null);
            add(panel, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }
}
