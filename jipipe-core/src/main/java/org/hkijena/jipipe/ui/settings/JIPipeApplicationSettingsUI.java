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

package org.hkijena.jipipe.ui.settings;

import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.JIPipeSettingsRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI for {@link org.hkijena.jipipe.api.registries.JIPipeSettingsRegistry}
 */
public class JIPipeApplicationSettingsUI extends JIPipeWorkbenchPanel {

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     */
    public JIPipeApplicationSettingsUI(JIPipeWorkbench workbench) {
        super(workbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane documentTabPane = new DocumentTabPane();
        add(documentTabPane, BorderLayout.CENTER);
        Map<String, List<JIPipeSettingsRegistry.Sheet>> byCategory =
                JIPipeSettingsRegistry.getInstance().getRegisteredSheets().values().stream().collect(Collectors.groupingBy(JIPipeSettingsRegistry.Sheet::getCategory));
        for (String category : byCategory.keySet().stream().sorted().collect(Collectors.toList())) {
            Icon categoryIcon = null;
            JIPipeParameterTree traversedParameterCollection = new JIPipeParameterTree();
            for (JIPipeSettingsRegistry.Sheet sheet : byCategory.get(category)) {
                categoryIcon = sheet.getCategoryIcon();
                traversedParameterCollection.add(sheet.getParameterCollection(), sheet.getName(), null);
                traversedParameterCollection.setSourceDocumentation(sheet.getParameterCollection(), new JIPipeDefaultDocumentation(sheet.getName(), null));
            }

            ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                    traversedParameterCollection,
                    MarkdownDocument.fromPluginResource("documentation/application-settings.md"),
                    ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SEARCH_BAR);
            documentTabPane.addTab(category,
                    categoryIcon,
                    parameterPanel,
                    DocumentTabPane.CloseMode.withoutCloseButton,
                    false);
        }
    }
}
