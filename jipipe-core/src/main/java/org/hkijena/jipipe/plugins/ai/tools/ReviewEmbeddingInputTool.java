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

package org.hkijena.jipipe.plugins.ai.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;

import javax.swing.*;

/**
 * Menu tool that opens a compendium-style tab for reviewing the embedding input text
 * that is sent to the AI model for each node.
 */
public class ReviewEmbeddingInputTool extends JIPipeDesktopMenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ReviewEmbeddingInputTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Review embedding input");
        setToolTipText("Review the text that is sent to the AI embedding model for each node.");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/document-preview.png"));
        addActionListener(e -> runReviewTool());
    }

    private void runReviewTool() {
        // Check if AI is enabled
        if (!AIApplicationSettings.getInstance().isEnableAI()) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "AI functionality is disabled. Please enable it in the AI settings.",
                    getText(), JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Open the compendium as a tab
        getDesktopWorkbench().getDocumentTabPane().addTab("Review AI Embedding Input",
                JIPipe.RESOURCES.getIcon16("actions/document-preview.png"),
                new ReviewEmbeddingInputCompendiumUI(),
                JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                true);
        getDesktopWorkbench().getDocumentTabPane().switchToLastTab();
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "AI";
    }
}
