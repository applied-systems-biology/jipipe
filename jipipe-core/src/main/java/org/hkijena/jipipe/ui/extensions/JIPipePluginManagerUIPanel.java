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

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.extensions.jsonextensionloader.JsonExtensionLoaderExtension;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that lists all plugins
 */
public class JIPipePluginManagerUIPanel extends JIPipeProjectWorkbenchPanel {

    private JList<JIPipeDependency> dependencyJList;
    private JSplitPane splitPane;

    /**
     * @param ui The project UI
     */
    public JIPipePluginManagerUIPanel(JIPipeProjectWorkbench ui) {
        super(ui);
        initialize();
        reload();
        JIPipe.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolbar();

        dependencyJList = new JList<>();
        dependencyJList.setCellRenderer(new JIPipeDependencyListCellRenderer());
        dependencyJList.setModel(new DefaultListModel<>());
        dependencyJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dependencyJList.addListSelectionListener(e -> showDetails(dependencyJList.getSelectedValue()));

        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, AutoResizeSplitPane.RATIO_1_TO_3);
        JScrollPane scrollPane = new JScrollPane(dependencyJList);
        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(new JPanel());

        add(splitPane, BorderLayout.CENTER);
    }

    private void showDetails(JIPipeDependency dependency) {
        if (dependency == null) {
            splitPane.setRightComponent(new JPanel());
            return;
        }
        splitPane.setRightComponent(new JIPipeDependencyUI(dependency));
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton newExtensionButton = new JButton("New extension ...", UIUtils.getIconFromResources("actions/document-new.png"));
        newExtensionButton.addActionListener(e -> {
            JIPipeJsonExtensionWindow window = JIPipeJsonExtensionWindow.newWindow(getWorkbench().getContext(), new JIPipeJsonExtension(), true);
            window.setTitle("New project");
        });
        toolBar.add(newExtensionButton);

        JButton installButton = new JButton("Install ...", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        installButton.addActionListener(e -> JIPipeJsonExtensionWindow.installExtensions(getWorkbench()));
        toolBar.add(installButton);

        JButton openPluginDirectory = new JButton("Open plugin directory", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openPluginDirectory.addActionListener(e -> UIUtils.openFileInNative(JsonExtensionLoaderExtension.getPluginDirectory()));
        toolBar.add(openPluginDirectory);

        toolBar.add(Box.createHorizontalGlue());

        JIPipePluginValidityCheckerButton validityCheckerButton = new JIPipePluginValidityCheckerButton();
        validityCheckerButton.addActionListener(e -> getProjectWorkbench().getDocumentTabPane().selectSingletonTab("PLUGIN_VALIDITY_CHECK"));
        toolBar.add(validityCheckerButton);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * Reloads the UI
     */
    public void reload() {
        DefaultListModel<JIPipeDependency> model = (DefaultListModel<JIPipeDependency>) dependencyJList.getModel();
        model.clear();
        for (JIPipeDependency extension : JIPipe.getInstance().getRegisteredExtensions()) {
            model.addElement(extension);
        }
        if (!model.isEmpty())
            dependencyJList.setSelectedIndex(0);
    }

    /**
     * Triggered when an extension is registered
     *
     * @param event Generated event
     */
    @Subscribe
    public void onExtensionRegistered(JIPipe.ExtensionRegisteredEvent event) {
        reload();
    }
}
