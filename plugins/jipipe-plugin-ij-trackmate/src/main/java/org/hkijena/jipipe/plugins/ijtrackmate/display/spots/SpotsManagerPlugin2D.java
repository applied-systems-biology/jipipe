/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ijtrackmate.display.spots;

import com.google.common.primitives.Ints;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.plugins.ijtrackmate.TrackMatePlugin;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.spots.MeasureSpotsNode;
import org.hkijena.jipipe.plugins.ijtrackmate.parameters.SpotFeature;
import org.hkijena.jipipe.plugins.ijtrackmate.settings.ImageViewerUISpotsDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.SpotDrawer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin2D;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SpotsManagerPlugin2D extends JIPipeDesktopLegacyImageViewerPlugin2D {
    private final JList<Spot> spotsListControl = new JList<>();
    private final JIPipeDesktopSmallToggleButtonRibbonAction displaySpotsViewMenuItem = new JIPipeDesktopSmallToggleButtonRibbonAction("Display spots", "Determines whether spots are displayed", UIUtils.getIconFromResources("actions/eye.png"));
    private final JIPipeDesktopSmallToggleButtonRibbonAction displayLabelsViewMenuItem = new JIPipeDesktopSmallToggleButtonRibbonAction("Display labels", "Determines whether spot labels are displayed", UIUtils.getIconFromResources("actions/tag.png"));
    private final List<SelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private SpotsCollectionData spotsCollection;
    private SpotDrawer spotDrawer = new SpotDrawer();
    private SpotListCellRenderer spotsListCellRenderer;
    private JPanel mainPanel;

    public SpotsManagerPlugin2D(JIPipeDesktopLegacyImageViewer viewerPanel) {
        super(viewerPanel);
        initializeDefaults();
        initialize();
        addSelectionContextPanel(new SelectionInfoContextPanel(this));
    }

    private void initializeDefaults() {
        ImageViewerUISpotsDisplayApplicationSettings settings = ImageViewerUISpotsDisplayApplicationSettings.getInstance();
        spotDrawer = new SpotDrawer(settings.getSpotDrawer());
        displaySpotsViewMenuItem.setState(settings.isShowSpots());
        displayLabelsViewMenuItem.setState(spotDrawer.getLabelSettings().isDrawLabels());
    }

    @Override
    public void onOverlayAdded(Object overlay) {
        if (overlay instanceof SpotsCollectionData) {
            SpotsCollectionData trackCollectionData = (SpotsCollectionData) overlay;
            setSpotCollection(trackCollectionData, false);
        }
    }

    @Override
    public void onOverlayRemoved(Object overlay) {
        if (overlay instanceof SpotsCollectionData) {
            setSpotCollection(new SpotsCollectionData(new Model(), new Settings(), IJ.createImage("", 1, 1, 1, 8)), false);
        }
    }

    @Override
    public void buildRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task spotsTask = ribbon.getOrCreateTask("Spots");
        {
            JIPipeDesktopRibbon.Band generalBand = spotsTask.getOrCreateBand("General");
            JIPipeDesktopRibbon.Band modifyBand = spotsTask.getOrCreateBand("Modify");
            JIPipeDesktopRibbon.Band measureBand = spotsTask.getOrCreateBand("Measure");

//            ROIPickerTool pickerTool = new ROIPickerTool(this);
//            LargeToggleButtonAction pickerToggle = new LargeToggleButtonAction("Pick", "Allows to select ROI via the mouse", UIUtils.getIcon32FromResources("actions/followmouse.png"));
//            pickerTool.addToggleButton(pickerToggle.getButton(), getViewerPanel().getCanvas());
//            generalBand.add(pickerToggle);

            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Select all", "Selects all spots", UIUtils.getIconFromResources("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Clear selection", "Deselects all spots", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Invert selection", "Inverts the current selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new JIPipeDesktopSmallButtonRibbonAction("Delete", "Deletes the selected spots", UIUtils.getIconFromResources("actions/delete.png"), () -> removeSelectedSpots(false)));

            JIPipeDesktopSmallButtonRibbonAction measureAction = new JIPipeDesktopSmallButtonRibbonAction("Measure", "Measures the spots and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedSpots);
            measureBand.add(measureAction);
        }
        {
            JIPipeDesktopRibbon.Band generalBand = spotsTask.getOrCreateBand("View");
            JIPipeDesktopRibbon.Band visualizationBand = spotsTask.getOrCreateBand("View");

            generalBand.add(displaySpotsViewMenuItem);
            generalBand.add(displayLabelsViewMenuItem);

            JIPipeDesktopSmallButtonRibbonAction colorButton = new JIPipeDesktopSmallButtonRibbonAction("Color by ...", "Allows to change how spots are colored", UIUtils.getIconFromResources("actions/colors-rgb.png"));
            visualizationBand.add(colorButton);
            {
                JPopupMenu colorByMenu = UIUtils.addPopupMenuToButton(colorButton.getButton());
                SpotFeature.VALUE_LABELS.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(key -> {
                    String name = SpotFeature.VALUE_LABELS.get(key);
                    JMenuItem colorByMenuEntry = new JMenuItem(name);
                    colorByMenuEntry.setToolTipText("Colors the spots by their " + name.toLowerCase(Locale.ROOT));
                    colorByMenuEntry.addActionListener(e -> {
                        spotDrawer.setStrokeColorFeature(new SpotFeature(key));
                        spotDrawer.setUniformStrokeColor(false);
                        spotsListCellRenderer.updateColorMaps();
                        uploadSliceToCanvas();
                    });
                    colorByMenu.add(colorByMenuEntry);
                });
            }

            JIPipeDesktopSmallButtonRibbonAction labelButton = new JIPipeDesktopSmallButtonRibbonAction("Set label to ...", "Allows to change how spots are labeled", UIUtils.getIconFromResources("actions/colors-rgb.png"));
            visualizationBand.add(labelButton);
            {
                JPopupMenu setLabelMenu = UIUtils.addPopupMenuToButton(labelButton.getButton());
                {
                    JMenuItem nameItem = new JMenuItem("Name");
                    nameItem.addActionListener(e -> {
                        spotDrawer.getLabelSettings().setDrawName(true);
                        uploadSliceToCanvas();
                    });
                    setLabelMenu.add(nameItem);
                }
                SpotFeature.VALUE_LABELS.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(key -> {
                    String name = SpotFeature.VALUE_LABELS.get(key);
                    JMenuItem setLabelMenuEntry = new JMenuItem(name);
                    setLabelMenuEntry.setToolTipText("Set the displayed label to " + name.toLowerCase(Locale.ROOT));
                    setLabelMenuEntry.addActionListener(e -> {
                        spotDrawer.getLabelSettings().setDrawName(false);
                        spotDrawer.getLabelSettings().setDrawnFeature(new SpotFeature(key));
                        uploadSliceToCanvas();
                    });
                    setLabelMenu.add(setLabelMenuEntry);
                });
            }
            visualizationBand.add(new JIPipeDesktopRibbon.Action(new JPanel(), 1, new Insets(2, 2, 2, 2)));

            visualizationBand.add(new JIPipeDesktopSmallButtonRibbonAction("More settings ...", "Opens a dialog where all available visualization settings can be changed", UIUtils.getIconFromResources("actions/configure.png"), this::openDrawingSettings));
            visualizationBand.add(new JIPipeDesktopSmallButtonRibbonAction("Save settings", "Saves the current settings as default", UIUtils.getIconFromResources("actions/filesave.png"), this::saveDefaults));
        }
        {
            JIPipeDesktopRibbon.Band fileBand = spotsTask.getOrCreateBand("Import/Export");

            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Import from file", "Imports spots from a file", UIUtils.getIconFromResources("actions/fileopen.png"), this::importSpotsFromFile));
            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Export to file", "Exports ROI to a file", UIUtils.getIconFromResources("actions/filesave.png"), this::exportSpotsToFile));
        }
    }

    @Override
    public void buildDock(JIPipeDesktopDockPanel dockPanel) {

    }

    @Override
    public void buildStatusBar(JToolBar statusBar) {

    }

    private void measureSelectedSpots() {
        MeasureSpotsNode node = JIPipe.createNode(MeasureSpotsNode.class);

        SpotsCollectionData selected = getSelectedSpotsOrAll("Measure spots", "Please select which spots should be measured");
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        node.getFirstInputSlot().addData(selected, progressInfo);
        node.run(new JIPipeGraphNodeRunContext(), progressInfo);
        ResultsTableData measurements = node.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
        JIPipeDesktopTableEditor.openWindow(getViewerPanel().getDesktopWorkbench(), measurements, "Measurements");
    }

    private void selectAll() {
        spotsListControl.setSelectionInterval(0, spotsListControl.getModel().getSize() - 1);
    }

    private void selectNone() {
        spotsListControl.clearSelection();
    }

    private void invertSelection() {
        Set<Integer> selectedIndices = Arrays.stream(spotsListControl.getSelectedIndices()).boxed().collect(Collectors.toSet());
        spotsListControl.clearSelection();
        Set<Integer> newSelectedIndices = new HashSet<>();
        for (int i = 0; i < spotsListControl.getModel().getSize(); i++) {
            if (!selectedIndices.contains(i))
                newSelectedIndices.add(i);
        }
        spotsListControl.setSelectedIndices(Ints.toArray(newSelectedIndices));
    }

    private void exportSpotsToFile() {
        if (spotsCollection.getSpots().getNSpots(true) <= 0) {
            JOptionPane.showMessageDialog(getViewerPanel(), "No spots to export.", "Export spots", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SpotsCollectionData result = getSelectedSpotsOrAll("Export spots", "Do you want to export all spots or only the selected ones?");
        if (result != null) {
            exportSpotsToFile(result);
        }
    }

    public SpotsCollectionData getSelectedSpotsOrAll(String title, String message) {
        if (!spotsListControl.getSelectedValuesList().isEmpty()) {
            int result = JOptionPane.showOptionDialog(getViewerPanel(),
                    message,
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"All spots (" + spotsCollection.getNSpots() + ")", "Selected spots (" + spotsListControl.getSelectedValuesList().size() + ")", "Cancel"},
                    "All spots (" + spotsCollection.getNSpots() + ")");
            if (result == JOptionPane.CANCEL_OPTION)
                return null;
            else if (result == JOptionPane.YES_OPTION)
                return spotsCollection;
            else {
                SpotsCollectionData newCollectionData = new SpotsCollectionData(spotsCollection);
                SpotCollection newCollection = new SpotCollection();
                for (Spot spot : spotsListControl.getSelectedValuesList()) {
                    newCollection.add(spot, spot.getFeature(Spot.FRAME).intValue());
                }
                newCollectionData.getModel().setSpots(newCollection, true);
                return newCollectionData;
            }
        }
        return spotsCollection;
    }

    public List<SelectionContextPanel> getSelectionContextPanels() {
        return Collections.unmodifiableList(selectionContextPanels);
    }

    public void addSelectionContextPanel(SelectionContextPanel panel) {
        selectionContextPanels.add(panel);
        selectionContentPanelUI.add(panel);
        selectionContentPanelUI.revalidate();
        selectionContentPanelUI.repaint();
    }

    public void removeSelectionContextPanel(SelectionContextPanel panel) {
        selectionContextPanels.remove(panel);
        selectionContentPanelUI.remove(panel);
        selectionContentPanelUI.revalidate();
        selectionContentPanelUI.repaint();
    }

    public SpotDrawer getSpotDrawer() {
        return spotDrawer;
    }


    private void saveDefaults() {
        if (JOptionPane.showConfirmDialog(getViewerPanel(),
                "Do you want to save the spot display settings as default?",
                "Save settings as default",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ImageViewerUISpotsDisplayApplicationSettings settings = ImageViewerUISpotsDisplayApplicationSettings.getInstance();
            settings.getSpotDrawer().copyFrom(spotDrawer);
            settings.setShowSpots(displaySpotsViewMenuItem.getState());
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
        }
    }

    private void openDrawingSettings() {
        JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), getViewerPanel(), spotDrawer, new MarkdownText("# Spots display settings\n\nPlease use the settings on the left to modify how spots are visualized."), "Spots display settings", JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
        uploadSliceToCanvas();
    }

    private void importSpotsFromFile() {
        Path path = JIPipeDesktop.openFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Import spots", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null && displaySpotsViewMenuItem.getState()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, path)) {
                SpotsCollectionData spotsCollectionData = SpotsCollectionData.importData(storage, progressInfo);
                setSpotCollection(spotsCollectionData, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void exportSpotsToFile(SpotsCollectionData rois) {
        FileNameExtensionFilter[] fileNameExtensionFilters = new FileNameExtensionFilter[]{UIUtils.EXTENSION_FILTER_ZIP};
        Path path = JIPipeDesktop.saveFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export spots", HTMLText.EMPTY, fileNameExtensionFilters);
        if (path != null) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(progressInfo, path)) {
                rois.exportData(storage, "Spots", false, progressInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (spotsCollection != null && displaySpotsViewMenuItem.getState()) {
            spotDrawer.drawOnGraphics(spotsCollection, graphics2D, renderArea, getCurrentSlicePosition(), spotsListControl.getSelectedValuesList());
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex, double magnification) {
        if (spotsCollection != null) {
            ImagePlus imagePlus = spotsCollection.getImage();
            int oldSlice = imagePlus.getSlice();
            imagePlus.setSlice(sliceIndex.zeroSliceIndexToOneStackIndex(imagePlus));
            Graphics2D graphics2D = image.createGraphics();
            spotDrawer.drawOnGraphics(spotsCollection, graphics2D, new Rectangle(0, 0, image.getWidth(), image.getHeight()), getCurrentSlicePosition(), spotsListControl.getSelectedValuesList());
            graphics2D.dispose();
            imagePlus.setSlice(oldSlice);
        }
    }

    @Override
    public void onSliceChanged(boolean deferUploadSlice) {
        updateSpotJList(deferUploadSlice);
    }

    @Override
    public String getPanelName() {
        return "Spots";
    }

    @Override
    public JIPipeDesktopDockPanel.PanelLocation getPanelLocation() {
        return JIPipeDesktopDockPanel.PanelLocation.BottomRight;
    }

    @Override
    public Icon getPanelIcon() {
        return TrackMatePlugin.RESOURCES.getIcon32FromResources("trackmate.png");
    }

    private void initialize() {
        // Setup ROI
        spotsListCellRenderer = new SpotListCellRenderer(this);
        spotsListControl.setCellRenderer(spotsListCellRenderer);
        spotsListControl.addListSelectionListener(e -> {
            updateContextPanels();
            uploadSliceToCanvas();
        });

        // Setup panel
        mainPanel = new JPanel(new BorderLayout());
//        mainPanel.setMinimumSize(new Dimension(100, 300));
//        mainPanel.setBorder(UIUtils.createControlBorder());

        JScrollPane scrollPane = new JScrollPane(spotsListControl);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        selectionContentPanelUI.setLayout(new BoxLayout(selectionContentPanelUI, BoxLayout.Y_AXIS));
        mainPanel.add(selectionContentPanelUI, BorderLayout.SOUTH);

        // Ribbon actions
        displayLabelsViewMenuItem.addActionListener(e -> {
            spotDrawer.getLabelSettings().setDrawLabels(displayLabelsViewMenuItem.getState());
        });
        displayLabelsViewMenuItem.addActionListener(this::uploadSliceToCanvas);
        displaySpotsViewMenuItem.addActionListener(this::uploadSliceToCanvas);
    }

    @Override
    public boolean isActive() {
        return getViewerPanel().getOverlays().stream().anyMatch(SpotsCollectionData.class::isInstance);
    }

    @Override
    public boolean isBuildingCustomPanel() {
        return true;
    }

    @Override
    public JComponent buildCustomPanel() {
        return mainPanel;
    }

    public void setSpotCollection(SpotsCollectionData spots, boolean deferUploadSlice) {
        spotsCollection = new SpotsCollectionData(spots);
        spotsListCellRenderer.updateColorMaps();
        updateSpotJList(deferUploadSlice);
        uploadSliceToCanvas();
    }

    public void removeSelectedSpots(boolean deferUploadSlice) {
        if (spotsListControl.getSelectedValuesList().isEmpty())
            return;
        if (JOptionPane.showConfirmDialog(getViewerPanel(), "Do you really want to remove " + spotsListControl.getSelectedValuesList().size() + "spots?", "Delete spots", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (Spot spot : spotsListControl.getSelectedValuesList()) {
                spotsCollection.getSpots().remove(spot, spot.getFeature(Spot.FRAME).intValue());
            }
            updateSpotJList(deferUploadSlice);
        }
    }

    public SpotsCollectionData getSpotsCollection() {
        return spotsCollection;
    }

    private void updateSpotJList(boolean deferUploadSlice) {
        DefaultListModel<Spot> model = new DefaultListModel<>();
        int[] selectedIndices = spotsListControl.getSelectedIndices();
        if (spotsCollection != null) {
            for (Spot spot : spotsCollection.getSpots().iterable(true)) {
                model.addElement(spot);
            }
        }
        spotsListControl.setModel(model);
        spotsListControl.setSelectedIndices(selectedIndices);
        updateContextPanels();
        if (!deferUploadSlice)
            uploadSliceToCanvas();
    }

    private void updateContextPanels() {
        List<Spot> selectedValuesList = spotsListControl.getSelectedValuesList();
        for (SelectionContextPanel selectionContextPanel : selectionContextPanels) {
            selectionContextPanel.selectionUpdated(spotsCollection, selectedValuesList);
        }
    }

    public JList<Spot> getSpotsListControl() {
        return spotsListControl;
    }

    public abstract static class SelectionContextPanel extends JPanel {

        private final SpotsManagerPlugin2D spotsManagerPlugin;

        protected SelectionContextPanel(SpotsManagerPlugin2D spotsManagerPlugin) {
            this.spotsManagerPlugin = spotsManagerPlugin;
        }

        public SpotsManagerPlugin2D getSpotsManagerPlugin() {
            return spotsManagerPlugin;
        }

        public JIPipeDesktopLegacyImageViewer getViewerPanel() {
            return spotsManagerPlugin.getViewerPanel();
        }

        public abstract void selectionUpdated(SpotsCollectionData spotsCollectionData, List<Spot> selectedSpots);
    }

    public static class SelectionInfoContextPanel extends SelectionContextPanel {

        private final JLabel roiInfoLabel;

        public SelectionInfoContextPanel(SpotsManagerPlugin2D parent) {
            super(parent);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            this.roiInfoLabel = new JLabel();
            roiInfoLabel.setIcon(TrackMatePlugin.RESOURCES.getIconFromResources("trackmate-spots.png"));
            roiInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(roiInfoLabel);
            add(Box.createHorizontalGlue());
        }

        @Override
        public void selectionUpdated(SpotsCollectionData spotsCollectionData, List<Spot> selectedSpots) {
            if (spotsCollectionData != null) {
                if (selectedSpots.isEmpty()) {
                    roiInfoLabel.setText(spotsCollectionData.getNSpots() + " spots");
                } else {
                    roiInfoLabel.setText(selectedSpots.size() + "/" + spotsCollectionData.getNSpots() + " spots");
                }
            } else {
                roiInfoLabel.setText("0 spots");
            }
        }
    }

}
