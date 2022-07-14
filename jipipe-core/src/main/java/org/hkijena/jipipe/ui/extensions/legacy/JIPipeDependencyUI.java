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
 *
 */

package org.hkijena.jipipe.ui.extensions.legacy;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.components.FormPanel;
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
        formPanel.addGroupHeader("About", UIUtils.getIconFromResources("actions/help-info.png"));
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getMetadata().getName()), new JLabel("Name"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getDependencyId()), new JLabel("ID"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getDependencyVersion()), new JLabel("Version"), null);
        if (!dependency.getMetadata().getAuthors().isEmpty()) {
            JPanel authorPanel = new JPanel();
            authorPanel.setLayout(new BoxLayout(authorPanel, BoxLayout.X_AXIS));
            for (JIPipeAuthorMetadata author : dependency.getMetadata().getAuthors()) {
                JButton button = new JButton(author.toString(), UIUtils.getIconFromResources("actions/im-user.png"));
                button.addActionListener(e -> JIPipeAuthorMetadata.openAuthorInfoWindow(SwingUtilities.getWindowAncestor(this), dependency.getMetadata().getAuthors(), author));
                authorPanel.add(button);
            }
            formPanel.addToForm(authorPanel, new JLabel("Authors"), null);
        }

        if (!StringUtils.isNullOrEmpty(dependency.getMetadata().getWebsite()))
            formPanel.addToForm(UIUtils.makeURLLabel(dependency.getMetadata().getWebsite()), new JLabel("Website"), null);
        if (!StringUtils.isNullOrEmpty(dependency.getMetadata().getCitation()))
            formPanel.addToForm(UIUtils.makeReadonlyTextPane(StringUtils.wordWrappedHTML(dependency.getMetadata().getCitation(), 80)),
                    new JLabel("Citation"), null);
        for (String citation : dependency.getMetadata().getDependencyCitations()) {
            formPanel.addToForm(UIUtils.makeReadonlyTextPane(StringUtils.wordWrappedHTML(citation, 80)), new JLabel("Also cite"), null);
        }
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getMetadata().getLicense()), new JLabel("License"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField("" + dependency.getDependencyLocation()), new JLabel("Defining file"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextPane(dependency.getMetadata().getDescription().getHtml()), new JLabel("Description"), null);

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
            insertTable(formPanel, model, "Dependencies", UIUtils.getIconFromResources("actions/plugins.png"));
        }
    }

    private void insertAddedDatatypes(FormPanel formPanel) {
        Set<JIPipeDataInfo> list = JIPipe.getDataTypes().getDeclaredBy(dependency);
        if (list.isEmpty())
            return;
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new Object[]{"Name", "ID", "Description", "ImageJ support"});
        for (JIPipeDataInfo info : list) {
            String supportsImageJEntry;
            supportsImageJEntry = StringUtils.createIconTextHTMLTable("Yes", ResourceUtils.getPluginResource("icons/emblems/vcs-normal.png"));
            model.addRow(new Object[]{
                    StringUtils.createIconTextHTMLTable(info.getName(), JIPipe.getDataTypes().getIconURLFor(info)),
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
        table.setRowHeight(64);
        table.packAll();
        table.setSortOrder(0, SortOrder.ASCENDING);
        UIUtils.fitRowHeights(table);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(table, BorderLayout.CENTER);
        panel.add(table.getTableHeader(), BorderLayout.NORTH);
        formPanel.addWideToForm(panel, null);
    }

    private void insertAddedAlgorithms(FormPanel formPanel) {
        List<JIPipeNodeInfo> list = new ArrayList<>(JIPipe.getNodes().getDeclaredBy(dependency));
        if (list.isEmpty())
            return;
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new Object[]{"Name", "ID", "Description", "Input slots", "Output slots", "ImageJ support"});
        for (JIPipeNodeInfo info : list) {
            String supportsImageJEntry;
            supportsImageJEntry = StringUtils.createIconTextHTMLTable("Yes", ResourceUtils.getPluginResource("icons/emblems/vcs-normal.png"));
            model.addRow(new Object[]{
                    info.getName(),
                    info.getId(),
                    info.getDescription(),
                    TooltipUtils.getSlotTable(info.getInputSlots().stream().map(JIPipeDataSlotInfo::new).collect(Collectors.toList())),
                    TooltipUtils.getSlotTable(info.getOutputSlots().stream().map(JIPipeDataSlotInfo::new).collect(Collectors.toList())),
                    supportsImageJEntry
            });
        }
        insertTable(formPanel, model, "Algorithms", UIUtils.getIconFromResources("actions/run-build.png"));
    }
}