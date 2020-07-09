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

package org.hkijena.pipelinej.ui.settings;

import org.hkijena.pipelinej.api.ACAQDefaultDocumentation;
import org.hkijena.pipelinej.api.parameters.ACAQParameterTree;
import org.hkijena.pipelinej.api.registries.ACAQSettingsRegistry;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.ACAQWorkbenchPanel;
import org.hkijena.pipelinej.ui.components.DocumentTabPane;
import org.hkijena.pipelinej.ui.components.MarkdownDocument;
import org.hkijena.pipelinej.ui.parameters.ParameterPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI for {@link org.hkijena.pipelinej.api.registries.ACAQSettingsRegistry}
 */
public class ACAQApplicationSettingsUI extends ACAQWorkbenchPanel {

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     */
    public ACAQApplicationSettingsUI(ACAQWorkbench workbench) {
        super(workbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane documentTabPane = new DocumentTabPane();
        add(documentTabPane, BorderLayout.CENTER);
        Map<String, List<ACAQSettingsRegistry.Sheet>> byCategory =
                ACAQSettingsRegistry.getInstance().getRegisteredSheets().values().stream().collect(Collectors.groupingBy(ACAQSettingsRegistry.Sheet::getCategory));
        for (String category : byCategory.keySet().stream().sorted().collect(Collectors.toList())) {
            Icon categoryIcon = null;
            ACAQParameterTree traversedParameterCollection = new ACAQParameterTree();
            for (ACAQSettingsRegistry.Sheet sheet : byCategory.get(category)) {
                categoryIcon = sheet.getCategoryIcon();
                traversedParameterCollection.add(sheet.getParameterCollection(), sheet.getName(), null);
                traversedParameterCollection.setSourceDocumentation(sheet.getParameterCollection(), new ACAQDefaultDocumentation(sheet.getName(), null));
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
