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

package org.hkijena.acaq5.ui.extensions;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.ACAQAuthorMetadata;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQImageJAdapterRegistry;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Displays an {@link ACAQDependency}
 */
public class ACAQDependencyUI extends JPanel {
    private ACAQDependency dependency;

    /**
     * Creates a new UI
     *
     * @param dependency The dependency
     */
    public ACAQDependencyUI(ACAQDependency dependency) {
        this.dependency = dependency;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        // Add general metadata
        formPanel.addGroupHeader("About", UIUtils.getIconFromResources("info.png"));
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getMetadata().getName()), new JLabel("Name"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getDependencyId()), new JLabel("ID"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getDependencyVersion()), new JLabel("Version"), null);
        for (ACAQAuthorMetadata author : dependency.getMetadata().getAuthors()) {
            JTextPane field = UIUtils.makeReadonlyTextPane(String.format("<html><strong>%s %s</strong><br/>%s</html>",
                    author.getFirstName(),
                    author.getLastName(),
                    StringUtils.nullToEmpty(author.getAffiliations()).replace("\n", "<br/>")));
            formPanel.addToForm(field, new JLabel("Author"), null);
        }
        if (!StringUtils.isNullOrEmpty(dependency.getMetadata().getWebsite()))
            formPanel.addToForm(UIUtils.makeURLLabel(dependency.getMetadata().getWebsite()), new JLabel("Website"), null);
        if (!StringUtils.isNullOrEmpty(dependency.getMetadata().getCitation()))
            formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getMetadata().getCitation()), new JLabel("Citation"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getMetadata().getLicense()), new JLabel("License"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField("" + dependency.getDependencyLocation()), new JLabel("Defining file"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextArea(dependency.getMetadata().getDescription()), new JLabel("Description"), null);

        insertDependencies(formPanel);
        insertAddedDatatypes(formPanel);
        insertAddedAlgorithms(formPanel);


        formPanel.addVerticalGlue();

        add(formPanel, BorderLayout.CENTER);
    }

    private void insertDependencies(FormPanel formPanel) {
        if (dependency instanceof ACAQJsonExtension) {
            ACAQJsonExtension jsonExtension = (ACAQJsonExtension) dependency;
            if (jsonExtension.getDependencies().isEmpty())
                return;
            DefaultTableModel model = new DefaultTableModel();
            model.setColumnIdentifiers(new Object[]{"Dependency"});
            for (ACAQDependency dependency : jsonExtension.getDependencies()) {
                model.addRow(new Object[]{
                        "<html>" + ACAQDependency.toHtmlElement(dependency) + "</html>"
                });
            }
            insertTable(formPanel, model, "Dependencies", UIUtils.getIconFromResources("module.png"));
        }
    }

    private void insertAddedDatatypes(FormPanel formPanel) {
        Set<ACAQDataDeclaration> list = ACAQDatatypeRegistry.getInstance().getDeclaredBy(dependency);
        if (list.isEmpty())
            return;
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new Object[]{"Name", "ID", "Description", "ImageJ support"});
        for (ACAQDataDeclaration declaration : list) {
            boolean supportsImageJ = ACAQImageJAdapterRegistry.getInstance().supportsACAQData(declaration.getDataClass());
            String supportsImageJEntry;
            if (supportsImageJ)
                supportsImageJEntry = StringUtils.createIconTextHTMLTable("Yes", ResourceUtils.getPluginResource("icons/check-circle-green.png"));
            else
                supportsImageJEntry = StringUtils.createIconTextHTMLTable("No", ResourceUtils.getPluginResource("icons/close-tab.png"));
            model.addRow(new Object[]{
                    StringUtils.createIconTextHTMLTable(declaration.getName(), ACAQUIDatatypeRegistry.getInstance().getIconURLFor(declaration)),
                    declaration.getId(),
                    StringUtils.wordWrappedHTML(declaration.getDescription(), 50),
                    supportsImageJEntry
            });
        }
        insertTable(formPanel, model, "Data types", UIUtils.getIconFromResources("data-types/data-type.png"));
    }

    private void insertTable(FormPanel formPanel, DefaultTableModel model, String categoryName, Icon categoryIcon) {
        formPanel.addGroupHeader(categoryName, categoryIcon);
        JXTable table = new JXTable(model);
        table.packAll();
        table.setSortOrder(0, SortOrder.ASCENDING);
        UIUtils.fitRowHeights(table);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(table, BorderLayout.CENTER);
        panel.add(table.getTableHeader(), BorderLayout.NORTH);
        formPanel.addWideToForm(panel, null);
    }

    private void insertAddedAlgorithms(FormPanel formPanel) {
        List<ACAQAlgorithmDeclaration> list = new ArrayList<>(ACAQAlgorithmRegistry.getInstance().getDeclaredBy(dependency));
        if (list.isEmpty())
            return;
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new Object[]{"Name", "ID", "Description", "Input slots", "Output slots"});
        for (ACAQAlgorithmDeclaration declaration : list) {
            model.addRow(new Object[]{
                    declaration.getName(),
                    declaration.getId(),
                    StringUtils.wordWrappedHTML(declaration.getDescription(), 50),
                    TooltipUtils.getSlotTable(declaration.getInputSlots().stream().map(ACAQSlotDefinition::new).collect(Collectors.toList())),
                    TooltipUtils.getSlotTable(declaration.getOutputSlots().stream().map(ACAQSlotDefinition::new).collect(Collectors.toList()))
            });
        }
        insertTable(formPanel, model, "Algorithms", UIUtils.getIconFromResources("run.png"));
    }
}
