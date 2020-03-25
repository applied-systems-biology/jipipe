package org.hkijena.acaq5.ui.extensionbuilder;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.events.ExtensionContentAddedEvent;
import org.hkijena.acaq5.api.events.ExtensionContentRemovedEvent;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUIPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ACAQJsonExtensionContentListUI extends ACAQJsonExtensionUIPanel {

    private JList<Object> list;
    private JSplitPane splitPane;

    public ACAQJsonExtensionContentListUI(ACAQJsonExtensionUI workbenchUI) {
        super(workbenchUI);
        initialize();
        reload();
        getProject().getEventBus().register(this);
    }

    private void reload() {
        DefaultListModel<Object> model = (DefaultListModel<Object>) list.getModel();
        model.clear();
        for (ACAQAlgorithmDeclaration declaration : getProject().getAlgorithmDeclarations().stream()
                .sorted(Comparator.comparing(ACAQAlgorithmDeclaration::getName)).collect(Collectors.toList())) {
            model.addElement(declaration);
        }
        for (ACAQTraitDeclaration declaration : getProject().getTraitDeclarations().stream()
                .sorted(Comparator.comparing(ACAQTraitDeclaration::getName)).collect(Collectors.toList())) {
            model.addElement(declaration);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JPanel listPanel = new JPanel(new BorderLayout());

        list = new JList<>(new DefaultListModel<>());
        list.setCellRenderer(new JsonExtensionContentListCellRenderer());
        listPanel.add(list, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        initializeToolbar(toolBar);
        listPanel.add(toolBar, BorderLayout.NORTH);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, new JPanel());
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        add(splitPane, BorderLayout.CENTER);
    }

    private void initializeToolbar(JToolBar toolBar) {
        JButton addAlgorithmButton = new JButton("Add algorithm", UIUtils.getIconFromResources("add.png"));
        addAlgorithmButton.addActionListener(e -> addAlgorithm());
        toolBar.add(addAlgorithmButton);

        JButton addTraitButton = new JButton("Add annotation", UIUtils.getIconFromResources("add.png"));
        addTraitButton.addActionListener(e -> addTrait());
        toolBar.add(addTraitButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton removeButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        removeButton.addActionListener(e -> removeSelection());
    }

    private void removeSelection() {

    }

    private void addTrait() {

    }

    private void addAlgorithm() {
        GraphWrapperAlgorithmDeclaration declaration = new GraphWrapperAlgorithmDeclaration();
        declaration.setGraph(new ACAQAlgorithmGraph());
        getProject().addAlgorithm(declaration);
    }

    @Subscribe
    public void onContentAddedEvent(ExtensionContentAddedEvent event) {
        reload();
    }

    @Subscribe
    public void onContentRemovedEvent(ExtensionContentRemovedEvent event) {
        reload();
    }
}
