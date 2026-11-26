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

package org.hkijena.jipipe.plugins.settings.application;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.database.NodeUsage;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;

import javax.swing.*;
import java.awt.*;

/**
 * The editor of the {@link NodeUsageResetParameter}: the "Reset usage
 * data" button of the "Node search" settings page. Pressing it clears the
 * recorded personal node usage counts
 * ({@link NodeUsage#resetUserCounts()}); the shipped community prior
 * stays active.
 * <p>
 * The parameter name is provided by the form panel's label column; the
 * button carries the action. The button is disabled while no JIPipe
 * instance is running: the reset persists through the settings registry,
 * which does not exist yet at that point.
 */
public class NodeUsageResetDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI<NodeUsageResetParameter> {

    private static final String ENABLED_TOOLTIP = "Clears the recorded personal node usage counts; the shipped community prior stays active";
    private static final String DISABLED_TOOLTIP = "JIPipe is not fully loaded: usage data cannot be reset";

    private final JButton resetButton = new JButton("Reset usage data", JIPipe.RESOURCES.getIcon16("actions/edit-reset.png"));

    public NodeUsageResetDesktopParameterEditorUI(InitializationParameters initializationParameters) {
        super(NodeUsageResetParameter.class, initializationParameters);
        setLayout(new BorderLayout());
        resetButton.setToolTipText(ENABLED_TOOLTIP);
        resetButton.addActionListener(e -> NodeUsage.resetUserCounts());
        add(resetButton, BorderLayout.CENTER);
        reload();
    }

    @Override
    public void reload() {
        boolean instantiated = JIPipe.isInstantiated();
        resetButton.setEnabled(instantiated);
        resetButton.setToolTipText(instantiated ? ENABLED_TOOLTIP : DISABLED_TOOLTIP);
    }
}
