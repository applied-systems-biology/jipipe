/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.library.jipipe;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class JIPipeDesktopArtifactQueryParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final JComboBox<String> comboBox = new JComboBox<>();

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterTree   the parameter tree that contains the access
     * @param parameterAccess the parameter access
     */
    public JIPipeDesktopArtifactQueryParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reloadArtifacts();
    }

    private void reloadArtifacts() {
        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
        String[] filters = { "*" };
        JIPipeArtifactQueryParameterSettings annotation = getParameterAccess().getAnnotationOfType(JIPipeArtifactQueryParameterSettings.class);
        if(annotation != null) {
            filters = annotation.getFilters();
        }
        for (JIPipeArtifact artifact : JIPipe.getArtifacts().queryCachedArtifacts(filters)) {
            comboBoxModel.addElement(artifact.getFullId());
        }
        comboBox.setModel(comboBoxModel);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        comboBox.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        comboBox.setEditable(true);
        add(comboBox, BorderLayout.CENTER);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {

    }
}
