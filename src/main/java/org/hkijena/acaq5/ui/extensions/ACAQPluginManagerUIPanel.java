package org.hkijena.acaq5.ui.extensions;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ACAQGUICommand;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWindow;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.ACAQProjectUIPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class ACAQPluginManagerUIPanel extends ACAQProjectUIPanel {

    private JList<ACAQDependency> dependencyJList;
    private JSplitPane splitPane;
    private ACAQGUICommand command;

    public ACAQPluginManagerUIPanel(ACAQProjectUI ui) {
        super(ui);
        this.command = ui.getCommand();
        initialize();
        reload();
        ACAQDefaultRegistry.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolbar();

        dependencyJList = new JList<>();
        dependencyJList.setCellRenderer(new ACAQDependencyListCellRenderer());
        dependencyJList.setModel(new DefaultListModel<>());
        dependencyJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dependencyJList.addListSelectionListener(e -> showDetails(dependencyJList.getSelectedValue()));

        splitPane = new JSplitPane();
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        splitPane.setLeftComponent(dependencyJList);
        splitPane.setRightComponent(new JPanel());

        add(splitPane, BorderLayout.CENTER);
    }

    private void showDetails(ACAQDependency dependency) {
        if (dependency == null) {
            splitPane.setRightComponent(new JPanel());
            return;
        }
        splitPane.setRightComponent(new ACAQDependencyUI(dependency));
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton newExtensionButton = new JButton("New extension ...", UIUtils.getIconFromResources("new.png"));
        newExtensionButton.addActionListener(e -> {
            ACAQJsonExtensionWindow window = ACAQJsonExtensionWindow.newWindow(command, new ACAQJsonExtension());
            window.setTitle("New project");
        });
        toolBar.add(newExtensionButton);

        JButton installButton = new JButton("Install ...", UIUtils.getIconFromResources("open.png"));
        installButton.addActionListener(e -> ACAQJsonExtensionWindow.installExtensions(this));
        toolBar.add(installButton);

        toolBar.add(Box.createHorizontalGlue());

        ACAQPluginValidityCheckerButton validityCheckerButton = new ACAQPluginValidityCheckerButton();
        validityCheckerButton.addActionListener(e -> getWorkbenchUI().getDocumentTabPane().selectSingletonTab("PLUGIN_VALIDITY_CHECK"));
        toolBar.add(validityCheckerButton);

        add(toolBar, BorderLayout.NORTH);
    }

    public void reload() {
        DefaultListModel<ACAQDependency> model = (DefaultListModel<ACAQDependency>) dependencyJList.getModel();
        model.clear();
        for (ACAQDependency extension : ACAQDefaultRegistry.getInstance().getRegisteredExtensions()) {
            model.addElement(extension);
        }
        if (!model.isEmpty())
            dependencyJList.setSelectedIndex(0);
    }

    @Subscribe
    public void onExtensionRegistered(ExtensionRegisteredEvent event) {
        reload();
    }
}
