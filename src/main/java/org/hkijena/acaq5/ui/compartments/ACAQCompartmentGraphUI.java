package org.hkijena.acaq5.ui.compartments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.compartments.ACAQExportedCompartment;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.DefaultUIActionRequestedEvent;
import org.hkijena.acaq5.ui.events.OpenSettingsUIRequestedEvent;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph.COMPARTMENT_DEFAULT;

public class ACAQCompartmentGraphUI extends ACAQUIPanel implements MouseListener, MouseMotionListener {

    protected JMenuBar menuBar = new JMenuBar();
    private ACAQAlgorithmGraphCanvasUI graphUI;
    private ACAQAlgorithmGraph compartmentGraph;

    private JSplitPane splitPane;
    private JScrollPane scrollPane;
    private ACAQCompartmentSettingsPanelUI currentSettings;

    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private JToggleButton switchPanningDirectionButton;

    private MarkdownReader documentationPanel;

    public ACAQCompartmentGraphUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        this.compartmentGraph = workbenchUI.getProject().getCompartmentGraph();
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
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/compartment-graph.md"));

        graphUI = new ACAQAlgorithmGraphCanvasUI(compartmentGraph, COMPARTMENT_DEFAULT);
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

        compartmentGraph.getEventBus().register(this);
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
        ACAQAlgorithmDeclaration declaration = ACAQAlgorithmRegistry.getInstance().getDefaultDeclarationFor(ACAQProjectCompartment.class);

        JButton addItem = new JButton("Add new compartment", UIUtils.getIconFromResources("add.png"));
        UIUtils.makeFlat(addItem);
        addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
        addItem.addActionListener(e -> addCompartment());
        menuBar.add(addItem);

        JButton importItem = new JButton("Import compartment", UIUtils.getIconFromResources("open.png"));
        UIUtils.makeFlat(importItem);
        importItem.setToolTipText("Imports a compartment from a *.json file");
        importItem.addActionListener(e -> importCompartment());
        menuBar.add(importItem);
    }

    private void importCompartment() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import compartment *.json");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ObjectMapper objectMapper = JsonUtils.getObjectMapper();
                ACAQExportedCompartment exportedCompartment = objectMapper.readerFor(ACAQExportedCompartment.class).readValue(fileChooser.getSelectedFile());

                String name = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the new compartment:",
                        exportedCompartment.getSuggestedName(), s -> getProject().getCompartments().containsKey(s));
                if (name != null && !name.isEmpty()) {
                    exportedCompartment.addTo(getProject(), name);
                }
            } catch (IOException e) {
            }
        }
    }

    private void addCompartment() {
        String compartmentName = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the compartment",
                "Compartment", s -> getProject().getCompartments().containsKey(s));
        if (compartmentName != null && !compartmentName.trim().isEmpty()) {
            getProject().addCompartment(compartmentName);
        }
    }

    public ACAQAlgorithmGraph getCompartmentGraph() {
        return compartmentGraph;
    }

    @Subscribe
    public void onOpenAlgorithmSettings(OpenSettingsUIRequestedEvent event) {
        if (currentSettings == null || currentSettings.getCompartment() != event.getUi().getAlgorithm()) {
            currentSettings = new ACAQCompartmentSettingsPanelUI(getWorkbenchUI(), (ACAQProjectCompartment) event.getUi().getAlgorithm());
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(currentSettings);
            splitPane.setDividerLocation(dividerLocation);
        }
    }

    @Subscribe
    public void onOpenCompartment(DefaultUIActionRequestedEvent event) {
        if (event.getUi() != null && event.getUi().getAlgorithm() instanceof ACAQProjectCompartment) {
            getWorkbenchUI().openCompartmentGraph((ACAQProjectCompartment) event.getUi().getAlgorithm(), true);
        }
    }

    @Subscribe
    public void onGraphChanged(AlgorithmGraphChangedEvent event) {
        if (currentSettings != null && !compartmentGraph.getAlgorithmNodes().containsValue(currentSettings.getCompartment())) {
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(documentationPanel);
            splitPane.setDividerLocation(dividerLocation);
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
