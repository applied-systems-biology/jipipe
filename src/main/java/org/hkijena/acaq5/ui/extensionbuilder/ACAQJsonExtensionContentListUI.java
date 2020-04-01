package org.hkijena.acaq5.ui.extensionbuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.events.ExtensionContentAddedEvent;
import org.hkijena.acaq5.api.events.ExtensionContentRemovedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbenchPanel;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.extensionbuilder.grapheditor.ACAQJsonExtensionAlgorithmGraphUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Lists the content of an {@link org.hkijena.acaq5.ACAQJsonExtension}
 */
public class ACAQJsonExtensionContentListUI extends ACAQJsonExtensionWorkbenchPanel {

    private JList<Object> list;
    private JSplitPane splitPane;
    private Object currentlySelectedValue;

    /**
     * Creates new instance
     *
     * @param workbenchUI The workbench UI
     */
    public ACAQJsonExtensionContentListUI(ACAQJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        reload();
        getProject().getEventBus().register(this);
    }

    private void reload() {
        Object selectedValue = list.getSelectedValue();
        DefaultListModel<Object> model = (DefaultListModel<Object>) list.getModel();
        model.clear();
        for (ACAQAlgorithmDeclaration declaration : getProject().getAlgorithmDeclarations().stream()
                .sorted(Comparator.comparing(ACAQAlgorithmDeclaration::getName, Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.toList())) {
            model.addElement(declaration);
        }
        for (ACAQTraitDeclaration declaration : getProject().getTraitDeclarations().stream()
                .sorted(Comparator.comparing(ACAQTraitDeclaration::getName, Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.toList())) {
            model.addElement(declaration);
        }
        if (model.contains(selectedValue)) {
            list.setSelectedValue(selectedValue, true);
        } else if (!model.isEmpty()) {
            list.setSelectedIndex(0);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JPanel listPanel = new JPanel(new BorderLayout());

        list = new JList<>(new DefaultListModel<>());
        list.setCellRenderer(new JsonExtensionContentListCellRenderer());
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.addListSelectionListener(e -> {
            setCurrentlySelectedValue(list.getSelectedValue());
        });
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
        toolBar.add(removeButton);
    }

    private void removeSelection() {
        for (Object item : ImmutableList.copyOf(list.getSelectedValuesList())) {
            if (item instanceof ACAQJsonTraitDeclaration) {
                getProject().removeAnnotation((ACAQJsonTraitDeclaration) item);
            } else if (item instanceof GraphWrapperAlgorithmDeclaration) {
                getProject().removeAlgorithm((GraphWrapperAlgorithmDeclaration) item);
            }
        }
    }

    private void addTrait() {
        ACAQJsonTraitDeclaration declaration = new ACAQJsonTraitDeclaration();
        getProject().addTrait(declaration);
        declaration.getEventBus().register(this);
    }

    private void addAlgorithm() {
        GraphWrapperAlgorithmDeclaration declaration = new GraphWrapperAlgorithmDeclaration();
        declaration.getMetadata().setName("");
        declaration.setGraph(new ACAQAlgorithmGraph());
        getProject().addAlgorithm(declaration);
        declaration.getMetadata().getEventBus().register(this);
    }

    /**
     * Triggered when a name was changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onParameterChanged(ParameterChangedEvent event) {
        if ("name".equals(event.getKey())) {
            list.repaint();
        }
    }

    /**
     * Triggered when content was added
     *
     * @param event Generated event
     */
    @Subscribe
    public void onContentAddedEvent(ExtensionContentAddedEvent event) {
        reload();
    }

    /**
     * Triggered when content was removed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onContentRemovedEvent(ExtensionContentRemovedEvent event) {
        reload();
    }

    /**
     * @return The currently selected value
     */
    public Object getCurrentlySelectedValue() {
        return currentlySelectedValue;
    }

    /**
     * Sets the selected value
     *
     * @param currentlySelectedValue The value
     */
    public void setCurrentlySelectedValue(Object currentlySelectedValue) {
        if (currentlySelectedValue != this.currentlySelectedValue) {
            this.currentlySelectedValue = currentlySelectedValue;
            if (currentlySelectedValue != null) {
                if (currentlySelectedValue instanceof GraphWrapperAlgorithmDeclaration) {
                    splitPane.setRightComponent(new GraphWrapperAlgorithmDeclarationUI(getExtensionWorkbenchUI(), (GraphWrapperAlgorithmDeclaration) currentlySelectedValue));
                } else if (currentlySelectedValue instanceof ACAQJsonTraitDeclaration) {
                    splitPane.setRightComponent(new ACAQJsonTraitDeclarationUI(getExtensionWorkbenchUI(), (ACAQJsonTraitDeclaration) currentlySelectedValue));
                }
            } else {
                splitPane.setRightComponent(new JPanel());
            }
        }
    }
}
