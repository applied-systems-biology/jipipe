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

package org.hkijena.jipipe.extensions.parameters.library.roi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Editor for {@link FixedMargin}
 */
public class InnerMarginEditorUI extends JIPipeParameterEditorUI {
    private boolean skipNextReload = false;
    private ParameterPanel parameterPanel;

    /**
     * @param workbench       workbench
     * @param parameterTree the parameter tree
     * @param parameterAccess the parameter
     */
    public InnerMarginEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createControlBorder());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        toolBar.add(new JLabel(getParameterAccess().getName()));

        JPanel content = new JPanel(new BorderLayout());

        parameterPanel = new ParameterPanel(getWorkbench(), null, null, ParameterPanel.NO_EMPTY_GROUP_HEADERS);
        content.add(parameterPanel, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        InnerMargin roi = getParameter(InnerMargin.class);

        // Update the parameter panel
        JIPipeParameterTree traversedParameterCollection = new JIPipeParameterTree(roi);
        Set<String> relevantParameterKeys = Sets.newHashSet("left", "top", "right", "bottom");
        for (String s : ImmutableList.copyOf(traversedParameterCollection.getParameters().keySet())) {
            if (!relevantParameterKeys.contains(s)) {
                traversedParameterCollection.getParameters().remove(s);
            }
        }
        parameterPanel.setDisplayedParameters(traversedParameterCollection);

    }
}
