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

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotDefinition;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.api.registries.JIPipeImageJAdapterRegistry;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Displays an {@link JIPipeDependency}
 */
public class JIPipeDependencyUI extends JPanel {
    private JIPipeDependency dependency;

    /**
     * Creates a new UI
     *
     * @param dependency The dependency
     */
    public JIPipeDependencyUI(JIPipeDependency dependency) {
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
        for (JIPipeAuthorMetadata author : dependency.getMetadata().getAuthors()) {
            JTextPane field = UIUtils.makeReadonlyTextPane(String.format("<html><strong>%s %s</strong><br/>%s</html>",
                    author.getFirstName(),
                    author.getLastName(),
                    StringUtils.nullToEmpty(author.getAffiliations()).replace("\n", "<br/>")));
            formPanel.addToForm(field, new JLabel("Author"), null);
        }
        if (!StringUtils.isNullOrEmpty(dependency.getMetadata().getWebsite()))
            formPanel.addToForm(UIUtils.makeURLLabel(dependency.getMetadata().getWebsite()), new JLabel("Website"), null);
        if (!StringUtils.isNullOrEmpty(dependency.getMetadata().getCitation()))
            formPanel.addToForm(UIUtils.makeReadonlyTextPane(StringUtils.wordWrappedHTML(dependency.getMetadata().getCitation(), 80)),
                    new JLabel("Citation"), null);
        for (String citation : dependency.getMetadata().getDependencyCitations()) {
            formPanel.addToForm(UIUtils.makeReadonlyTextPane(StringUtils.wordWrappedHTML(citation, 80)), new JLabel("Dependent work"), null);
        }
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
        if (dependency instanceof JIPipeJsonExtension) {
            JIPipeJsonExtension jsonExtension = (JIPipeJsonExtension) dependency;
            if (jsonExtension.getDependencies().isEmpty())
                return;
            DefaultTableModel model = new DefaultTableModel();
            model.setColumnIdentifiers(new Object[]{"Dependency"});
            for (JIPipeDependency dependency : jsonExtension.getDependencies()) {
                model.addRow(new Object[]{
                        "<html>" + JIPipeDependency.toHtmlElement(dependency) + "</html>"
                });
            }
            insertTable(formPanel, model, "Dependencies", UIUtils.getIconFromResources("module.png"));
        }
    }

    private void insertAddedDatatypes(FormPanel formPanel) {
        Set<JIPipeDataInfo> list = JIPipeDatatypeRegistry.getInstance().getDeclaredBy(dependency);
        if (list.isEmpty())
            return;
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new Object[]{"Name", "ID", "Description", "ImageJ support"});
        for (JIPipeDataInfo info : list) {
            boolean supportsImageJ = JIPipeImageJAdapterRegistry.getInstance().supportsJIPipeData(info.getDataClass());
            String supportsImageJEntry;
            if (supportsImageJ)
                supportsImageJEntry = StringUtils.createIconTextHTMLTable("Yes", ResourceUtils.getPluginResource("icons/check-circle-green.png"));
            else
                supportsImageJEntry = StringUtils.createIconTextHTMLTable("No", ResourceUtils.getPluginResource("icons/close-tab.png"));
            model.addRow(new Object[]{
                    StringUtils.createIconTextHTMLTable(info.getName(), JIPipeUIDatatypeRegistry.getInstance().getIconURLFor(info)),
                    info.getId(),
                    StringUtils.wordWrappedHTML(info.getDescription(), 50),
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
        List<JIPipeNodeInfo> list = new ArrayList<>(JIPipeNodeRegistry.getInstance().getDeclaredBy(dependency));
        if (list.isEmpty())
            return;
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new Object[]{"Name", "ID", "Description", "Input slots", "Output slots"});
        for (JIPipeNodeInfo info : list) {
            model.addRow(new Object[]{
                    info.getName(),
                    info.getId(),
                    StringUtils.wordWrappedHTML(info.getDescription(), 50),
                    TooltipUtils.getSlotTable(info.getInputSlots().stream().map(JIPipeSlotDefinition::new).collect(Collectors.toList())),
                    TooltipUtils.getSlotTable(info.getOutputSlots().stream().map(JIPipeSlotDefinition::new).collect(Collectors.toList()))
            });
        }
        insertTable(formPanel, model, "Algorithms", UIUtils.getIconFromResources("run.png"));
    }
}
