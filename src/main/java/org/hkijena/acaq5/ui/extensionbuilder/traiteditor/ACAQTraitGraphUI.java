package org.hkijena.acaq5.ui.extensionbuilder.traiteditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUIPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitGraph;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitNode;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph.COMPARTMENT_DEFAULT;

public class ACAQTraitGraphUI extends ACAQJsonExtensionUIPanel implements MouseListener, MouseMotionListener {

    protected JMenuBar menuBar = new JMenuBar();
    private ACAQAlgorithmGraphCanvasUI graphUI;

    private JSplitPane splitPane;
    private JScrollPane scrollPane;

    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private JToggleButton switchPanningDirectionButton;

    private MarkdownReader documentationPanel;

    private Set<ACAQAlgorithmUI> selection = new HashSet<>();

    private ACAQTraitGraph graph;

    public ACAQTraitGraphUI(ACAQJsonExtensionUI workbenchUI) {
        super(workbenchUI);
        this.graph = new ACAQTraitGraph(getProject());
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        splitPane = new JSplitPane();
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });

        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/trait-graph.md"));

        graphUI = new ACAQAlgorithmGraphCanvasUI(graph, COMPARTMENT_DEFAULT);
        graphUI.getEventBus().register(this);
        graphUI.addMouseListener(this);
        graphUI.addMouseMotionListener(this);
        scrollPane = new JScrollPane(graphUI);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
            graphUI.setNewEntryLocationX(scrollPane.getHorizontalScrollBar().getValue());
        });
        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(documentationPanel);
        add(splitPane, BorderLayout.CENTER);

        add(menuBar, BorderLayout.NORTH);
        initializeToolbar();

        graph.getEventBus().register(this);
    }

    protected void initializeToolbar() {
        initializeAddNodesMenus();

        menuBar.add(Box.createHorizontalGlue());

        JButton autoLayoutButton = new JButton(UIUtils.getIconFromResources("auto-layout-all.png"));
        autoLayoutButton.setToolTipText("Auto-layout all nodes");
        UIUtils.makeFlat25x25(autoLayoutButton);
        autoLayoutButton.addActionListener(e -> graphUI.autoLayoutAll());
        menuBar.add(autoLayoutButton);

        switchPanningDirectionButton = new JToggleButton(UIUtils.getIconFromResources("cursor-arrow.png"));
        switchPanningDirectionButton.setToolTipText("Reverse panning direction");
        UIUtils.makeFlat25x25(switchPanningDirectionButton);
        switchPanningDirectionButton.setToolTipText("Changes the direction how panning (middle mouse button) affects the view.");
        menuBar.add(switchPanningDirectionButton);

        JToggleButton layoutHelperButton;
        layoutHelperButton = new JToggleButton(UIUtils.getIconFromResources("auto-layout-connections.png"), true);
        UIUtils.makeFlat25x25(layoutHelperButton);
        layoutHelperButton.setToolTipText("Auto-layout layout on making data slot connections");
        graphUI.setLayoutHelperEnabled(true);
        layoutHelperButton.addActionListener(e -> graphUI.setLayoutHelperEnabled(layoutHelperButton.isSelected()));
        menuBar.add(layoutHelperButton);

        JButton createScreenshotButton = new JButton(UIUtils.getIconFromResources("filetype-image.png"));
        createScreenshotButton.setToolTipText("Export graph as *.png");
        UIUtils.makeFlat25x25(createScreenshotButton);
        createScreenshotButton.addActionListener(e -> createScreenshot());
        menuBar.add(createScreenshotButton);
    }

    private void createScreenshot() {
        BufferedImage screenshot = graphUI.createScreenshot();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export graph as *.png");
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(screenshot, "PNG", fileChooser.getSelectedFile());
                getWorkbenchUI().sendStatusBarText("Exported graph as " + fileChooser.getSelectedFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void initializeAddNodesMenus() {
        ACAQAlgorithmDeclaration declaration = ACAQAlgorithmRegistry.getInstance().getDeclarationById("acaq:project-compartment");

        JButton addItem = new JButton("New annotation", UIUtils.getIconFromResources("new.png"));
        UIUtils.makeFlat(addItem);
        addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
        addItem.addActionListener(e -> addNewAnnotation());
        menuBar.add(addItem);

        JButton importItem = new JButton("Add existing annotation", UIUtils.getIconFromResources("add.png"));
        UIUtils.makeFlat(importItem);
        importItem.addActionListener(e -> addExistingAnnotation());
        menuBar.add(importItem);
    }

    private void addExistingAnnotation() {

    }

    private void addNewAnnotation() {
        getProject().addTrait(new ACAQJsonTraitDeclaration());
    }

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    @Subscribe
    public void onAlgorithmSelected(AlgorithmSelectedEvent event) {
        if (event.getUi() != null) {
            if (event.isAddToSelection()) {
                if (selection.contains(event.getUi())) {
                    removeFromSelection(event.getUi());
                } else {
                    addToSelection(event.getUi());
                }
            } else {
                selectOnly(event.getUi());
            }
        } else {
            clearSelection();
        }
    }

    public void clearSelection() {
        for (ACAQAlgorithmUI ui : selection) {
            ui.setSelected(false);
        }
        selection.clear();
        int dividerLocation = splitPane.getDividerLocation();
        splitPane.setRightComponent(documentationPanel);
        splitPane.setDividerLocation(dividerLocation);
    }

    public void selectOnly(ACAQAlgorithmUI ui) {
        if (selection.isEmpty()) {
            addToSelection(ui);
        } else if (selection.size() == 1) {
            if (selection.iterator().next() != ui) {
                clearSelection();
                addToSelection(ui);
            }
        } else {
            clearSelection();
            addToSelection(ui);
        }
    }

    public void addToSelection(ACAQAlgorithmUI ui) {
        selection.add(ui);
        ui.setSelected(true);
        if (selection.size() == 1) {
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(new ACAQSingleTraitSelectionPanelUI(getWorkbenchUI(), (ACAQTraitNode) ui.getAlgorithm(), graph));
            splitPane.setDividerLocation(dividerLocation);
        } else {
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(new ACAQMultiTraitSelectionPanelUI(getWorkbenchUI(),
                    graph, selection.stream().map(a -> (ACAQTraitNode) a.getAlgorithm()).collect(Collectors.toSet())));
            splitPane.setDividerLocation(dividerLocation);
        }
    }

    public void removeFromSelection(ACAQAlgorithmUI ui) {
        if (selection.contains(ui)) {
            selection.remove(ui);
            ui.setSelected(false);

            int dividerLocation = splitPane.getDividerLocation();
            if (selection.isEmpty()) {
                splitPane.setRightComponent(documentationPanel);
            } else if (selection.size() == 1) {
                splitPane.setRightComponent(new ACAQSingleTraitSelectionPanelUI(getWorkbenchUI(), (ACAQTraitNode) ui.getAlgorithm(), graph));
            } else {
                splitPane.setRightComponent(new ACAQMultiTraitSelectionPanelUI(getWorkbenchUI(),
                        graph, selection.stream().map(a -> (ACAQTraitNode) a.getAlgorithm()).collect(Collectors.toSet())));
            }
            splitPane.setDividerLocation(dividerLocation);
        }
    }

    @Subscribe
    public void onGraphChanged(AlgorithmGraphChangedEvent event) {
        if (selection.stream().anyMatch(ui -> !graph.getAlgorithmNodes().containsValue(ui.getAlgorithm()))) {
            clearSelection();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e)) {
            isPanning = true;
            int x = e.getX() - scrollPane.getHorizontalScrollBar().getValue();
            int y = e.getY() - scrollPane.getVerticalScrollBar().getValue();
            panningOffset = new Point(x, y);
            panningScrollbarOffset = new Point(scrollPane.getHorizontalScrollBar().getValue(),
                    scrollPane.getVerticalScrollBar().getValue());
            graphUI.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isPanning) {
            graphUI.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        isPanning = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (isPanning && panningOffset != null && panningScrollbarOffset != null) {
            int x = e.getX() - scrollPane.getHorizontalScrollBar().getValue();
            int y = e.getY() - scrollPane.getVerticalScrollBar().getValue();
            int dx = x - panningOffset.x;
            int dy = y - panningOffset.y;
            if (!switchPanningDirectionButton.isSelected()) {
                dx = -dx;
                dy = -dy;
            }
            int nx = panningScrollbarOffset.x + dx;
            int ny = panningScrollbarOffset.y + dy;
            scrollPane.getHorizontalScrollBar().setValue(nx);
            scrollPane.getVerticalScrollBar().setValue(ny);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
}
