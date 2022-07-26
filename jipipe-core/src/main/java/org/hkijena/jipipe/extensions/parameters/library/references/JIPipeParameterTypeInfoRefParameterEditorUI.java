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

package org.hkijena.jipipe.extensions.parameters.library.references;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.pickers.JIPipeNodeInfoPicker;
import org.hkijena.jipipe.ui.components.pickers.JIPipeParameterTypeInfoPicker;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Parameter for {@link JIPipeParameterTypeInfoRef}
 */
public class JIPipeParameterTypeInfoRefParameterEditorUI extends JIPipeParameterEditorUI {
    private JButton currentlyDisplayed;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public JIPipeParameterTypeInfoRefParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.addActionListener(e -> pickNodeInfo());
        UIUtils.makeFlat(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        UIUtils.makeFlat(selectButton);
        selectButton.setToolTipText("Select parameter type");
        selectButton.addActionListener(e -> pickNodeInfo());
        add(selectButton, BorderLayout.EAST);
    }

    @Override
    public void reload() {
        JIPipeParameterTypeInfoRef infoRef = getParameter(JIPipeParameterTypeInfoRef.class);
        JIPipeParameterTypeInfo info = infoRef.getInfo();
        if (info != null) {
            currentlyDisplayed.setText(info.getName());
            currentlyDisplayed.setToolTipText(info.getDescription());
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("data-types/parameters.png"));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        }
    }


    private void pickNodeInfo() {
        JIPipeParameterTypeInfoPicker picker = new JIPipeParameterTypeInfoPicker(getWorkbench().getWindow());
        JIPipeParameterTypeInfoRef infoRef = getParameter(JIPipeParameterTypeInfoRef.class);
        JIPipeParameterTypeInfo info = infoRef.getInfo();
        if(info != null) {
            picker.setSelectedItem(info);
        }
        JIPipeParameterTypeInfo selection = picker.showDialog();
        if(selection != null) {
            setParameter(new JIPipeParameterTypeInfoRef(selection), true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
