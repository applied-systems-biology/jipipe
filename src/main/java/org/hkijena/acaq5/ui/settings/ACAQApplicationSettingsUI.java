package org.hkijena.acaq5.ui.settings;

import org.hkijena.acaq5.api.ACAQDefaultDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.registries.ACAQSettingsRegistry;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI for {@link org.hkijena.acaq5.api.registries.ACAQSettingsRegistry}
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
