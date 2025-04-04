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

package org.hkijena.jipipe.plugins.ijtrackmate.display.tracks;

import com.google.common.primitives.Ints;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
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
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.plugins.ijtrackmate.TrackMatePlugin;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.display.trackscheme.ShowTrackSchemeDataDisplayOperation;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.tracks.MeasureEdgesNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.tracks.MeasureTracksNode;
import org.hkijena.jipipe.plugins.ijtrackmate.parameters.EdgeFeature;
import org.hkijena.jipipe.plugins.ijtrackmate.parameters.TrackFeature;
import org.hkijena.jipipe.plugins.ijtrackmate.settings.ImageViewerUITracksDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.TrackDrawer;
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
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TracksManagerPlugin2D extends JIPipeDesktopLegacyImageViewerPlugin2D {
    private final JList<Integer> tracksListControl = new JList<>();
    private final JIPipeDesktopSmallToggleButtonRibbonAction displayTracksViewMenuItem = new JIPipeDesktopSmallToggleButtonRibbonAction("Display tracks", "Determines whether tracks are displayed", UIUtils.getIconFromResources("actions/eye.png"));
    private final List<SelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private TrackCollectionData tracksCollection;
    private TrackDrawer trackDrawer = new TrackDrawer();
    private TrackListCellRenderer tracksListCellRenderer;
    private JPanel mainPanel;

    public TracksManagerPlugin2D(JIPipeDesktopLegacyImageViewer viewerPanel) {
        super(viewerPanel);
        initializeDefaults();
        initialize();
        addSelectionContextPanel(new SelectionInfoContextPanel(this));
    }

    private void initializeDefaults() {
        ImageViewerUITracksDisplayApplicationSettings settings = ImageViewerUITracksDisplayApplicationSettings.getInstance();
        displayTracksViewMenuItem.setState(settings.isShowTracks());
        trackDrawer = new TrackDrawer(settings.getTrackDrawer());
    }

    @Override
    public boolean isBuildingCustomPanel() {
        return true;
    }

    @Override
    public JComponent buildCustomPanel() {
        return mainPanel;
    }

    private void openTrackScheme() {
        ShowTrackSchemeDataDisplayOperation operation = new ShowTrackSchemeDataDisplayOperation();
        operation.display(tracksCollection, "Track scheme", getDesktopWorkbench(), getDataSource());
    }

    private void exportTracksToFile() {
        if (tracksCollection.getNTracks() <= 0) {
            JOptionPane.showMessageDialog(getViewerPanel(), "No tracks to export.", "Export tracks", JOptionPane.ERROR_MESSAGE);
            return;
        }
        TrackCollectionData result = getSelectedTracksOrAll("Export tracks", "Do you want to export all ROI or only the selected ones?");
        if (result != null) {
            exportTracksToFile(result);
        }
    }

    public TrackCollectionData getSelectedTracksOrAll(String title, String message) {
        if (!tracksListControl.getSelectedValuesList().isEmpty()) {
            int result = JOptionPane.showOptionDialog(getViewerPanel(),
                    message,
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"All tracks (" + tracksCollection.getNTracks() + ")", "Selected tracks (" + tracksListControl.getSelectedValuesList().size() + ")", "Cancel"},
                    "All tracks (" + tracksCollection.getNTracks() + ")");
            if (result == JOptionPane.CANCEL_OPTION)
                return null;
            else if (result == JOptionPane.YES_OPTION)
                return tracksCollection;
            else {
                return tracksCollection.filterTracks(new HashSet<>(tracksListControl.getSelectedValuesList()));
            }
        }
        return tracksCollection;
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

    private void selectAll() {
        tracksListControl.setSelectionInterval(0, tracksListControl.getModel().getSize() - 1);
    }

    private void selectNone() {
        tracksListControl.clearSelection();
    }

    private void invertSelection() {
        Set<Integer> selectedIndices = Arrays.stream(tracksListControl.getSelectedIndices()).boxed().collect(Collectors.toSet());
        tracksListControl.clearSelection();
        Set<Integer> newSelectedIndices = new HashSet<>();
        for (int i = 0; i < tracksListControl.getModel().getSize(); i++) {
            if (!selectedIndices.contains(i))
                newSelectedIndices.add(i);
        }
        tracksListControl.setSelectedIndices(Ints.toArray(newSelectedIndices));
    }

    public TrackDrawer getTrackDrawer() {
        return trackDrawer;
    }

    private void importTracksFromFile() {
        Path path = JIPipeDesktop.openFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Import tracks", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null && displayTracksViewMenuItem.getState()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, path)) {
                TrackCollectionData trackCollectionData = TrackCollectionData.importData(storage, progressInfo);
                setTrackCollection(trackCollectionData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveDefaults() {
        if (JOptionPane.showConfirmDialog(getViewerPanel(),
                "Do you want to save the spot display settings as default?",
                "Save settings as default",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ImageViewerUITracksDisplayApplicationSettings settings = ImageViewerUITracksDisplayApplicationSettings.getInstance();
            settings.getTrackDrawer().copyFrom(trackDrawer);
            settings.setShowTracks(displayTracksViewMenuItem.getState());
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
        }
    }

    private void openDrawingSettings() {
        JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), getViewerPanel(), trackDrawer, new MarkdownText("# Track display settings\n\nPlease use the settings on the left to modify how tracks are visualized."), "Track display settings", JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
        uploadSliceToCanvas();
    }

    private void exportTracksToFile(TrackCollectionData rois) {
        FileNameExtensionFilter[] fileNameExtensionFilters = new FileNameExtensionFilter[]{UIUtils.EXTENSION_FILTER_ZIP};
        Path path = JIPipeDesktop.saveFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export tracks", HTMLText.EMPTY, fileNameExtensionFilters);
        if (path != null) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(progressInfo, path)) {
                rois.exportData(storage, "Tracks", false, progressInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (tracksCollection != null && displayTracksViewMenuItem.getState()) {
            List<Integer> selectedValuesList = tracksListControl.getSelectedValuesList();
            Set<DefaultWeightedEdge> highlight = new HashSet<>();
            for (Integer trackId : selectedValuesList) {
                highlight.addAll(tracksCollection.getTrackModel().trackEdges(trackId));
            }
            trackDrawer.drawOnGraphics(tracksCollection, graphics2D, renderArea, sliceIndex, highlight);
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex, double magnification) {
        if (tracksCollection != null) {
            ImagePlus imagePlus = tracksCollection.getImage();
            int oldSlice = imagePlus.getSlice();
            imagePlus.setSlice(sliceIndex.zeroSliceIndexToOneStackIndex(imagePlus));
            Graphics2D graphics2D = image.createGraphics();
            Set<DefaultWeightedEdge> highlight = new HashSet<>();
            List<Integer> selectedValuesList = tracksListControl.getSelectedValuesList();
            for (Integer trackId : selectedValuesList) {
                highlight.addAll(tracksCollection.getTrackModel().trackEdges(trackId));
            }
            trackDrawer.drawOnGraphics(tracksCollection, graphics2D, new Rectangle(0, 0, image.getWidth(), image.getHeight()), getCurrentSlicePosition(), highlight);
            graphics2D.dispose();
            imagePlus.setSlice(oldSlice);
        }
    }

    @Override
    public void onSliceChanged(boolean deferUploadSlice) {
        updateTrackJList(deferUploadSlice);
    }

    @Override
    public String getPanelName() {
        return "Tracks";
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
        tracksListCellRenderer = new TrackListCellRenderer(this);
        tracksListControl.setCellRenderer(tracksListCellRenderer);
        tracksListControl.addListSelectionListener(e -> {
            updateContextPanels();
            uploadSliceToCanvas();
        });

        // Setup panel
        mainPanel = new JPanel(new BorderLayout());
//        mainPanel.setMinimumSize(new Dimension(100, 300));
//        mainPanel.setBorder(UIUtils.createControlBorder());

        JScrollPane scrollPane = new JScrollPane(tracksListControl);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        selectionContentPanelUI.setLayout(new BoxLayout(selectionContentPanelUI, BoxLayout.Y_AXIS));
        mainPanel.add(selectionContentPanelUI, BorderLayout.SOUTH);

        // Ribbon actions
        displayTracksViewMenuItem.addActionListener(this::uploadSliceToCanvas);
    }

    public void setTrackCollection(TrackCollectionData tracksCollection) {
        this.tracksCollection = new TrackCollectionData(tracksCollection);
        tracksListCellRenderer.updateColorMaps();
        updateTrackJList(false);
        uploadSliceToCanvas();
    }

    @Override
    public boolean isActive() {
        return getViewerPanel().getOverlays().stream().anyMatch(TrackCollectionData.class::isInstance);
    }

    @Override
    public void onOverlayAdded(Object overlay) {
        if (overlay instanceof TrackCollectionData) {
            TrackCollectionData trackCollectionData = (TrackCollectionData) overlay;
            setTrackCollection(trackCollectionData);
        }
    }

    @Override
    public void onOverlayRemoved(Object overlay) {
        if (overlay instanceof TrackCollectionData) {
            setTrackCollection(new TrackCollectionData(new Model(), new Settings(), IJ.createImage("", 1, 1, 1, 8)));
        }
    }

    @Override
    public void buildRibbon(JIPipeDesktopRibbon ribbon) {

        JIPipeDesktopRibbon.Task tracksTask = ribbon.getOrCreateTask("Tracks");

        {
            JIPipeDesktopRibbon.Band generalBand = tracksTask.getOrCreateBand("Select");
            JIPipeDesktopRibbon.Band modifyBand = tracksTask.getOrCreateBand("Modify");
            JIPipeDesktopRibbon.Band measureBand = tracksTask.getOrCreateBand("Measure");

//            ROIPickerTool pickerTool = new ROIPickerTool(this);
//            LargeToggleButtonAction pickerToggle = new LargeToggleButtonAction("Pick", "Allows to select ROI via the mouse", UIUtils.getIcon32FromResources("actions/followmouse.png"));
//            pickerTool.addToggleButton(pickerToggle.getButton(), getViewerPanel().getCanvas());
//            generalBand.add(pickerToggle);

            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Select all", "Selects all tracks", UIUtils.getIconFromResources("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Clear selection", "Deselects all tracks", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Invert selection", "Inverts the current selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new JIPipeDesktopSmallButtonRibbonAction("Delete", "Deletes the selected tracks", UIUtils.getIconFromResources("actions/delete.png"), () -> removeSelectedTracks(false)));

            measureBand.add(new JIPipeDesktopSmallButtonRibbonAction("Measure edges", "Measures the tracks and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedEdges));
            measureBand.add(new JIPipeDesktopSmallButtonRibbonAction("Measure tracks", "Measures the tracks and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedTracks));
        }
        {
            JIPipeDesktopRibbon.Band generalBand = tracksTask.getOrCreateBand("View");
            JIPipeDesktopRibbon.Band visualizationBand = tracksTask.getOrCreateBand("View");

            generalBand.add(displayTracksViewMenuItem);

            JIPipeDesktopSmallButtonRibbonAction colorButton = new JIPipeDesktopSmallButtonRibbonAction("Color by ...", "Allows to change how tracks are colored", UIUtils.getIconFromResources("actions/colors-rgb.png"));
            visualizationBand.add(colorButton);
            {
                JPopupMenu colorByMenu = UIUtils.addPopupMenuToButton(colorButton.getButton());
                EdgeFeature.VALUE_LABELS.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(key -> {
                    String name = EdgeFeature.VALUE_LABELS.get(key);
                    JMenuItem colorByMenuEntry = new JMenuItem("Edge: " + name);
                    colorByMenuEntry.setToolTipText("Colors the track edges by their " + name.toLowerCase(Locale.ROOT));
                    colorByMenuEntry.addActionListener(e -> {
                        trackDrawer.setStrokeColorMode(TrackDrawer.StrokeColorMode.PerEdge);
                        trackDrawer.setStrokeColorEdgeFeature(new EdgeFeature(key));
                        tracksListCellRenderer.updateColorMaps();
                        uploadSliceToCanvas();
                    });
                    colorByMenu.add(colorByMenuEntry);
                });
                colorByMenu.addSeparator();
                TrackFeature.VALUE_LABELS.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(key -> {
                    String name = TrackFeature.VALUE_LABELS.get(key);
                    JMenuItem colorByMenuEntry = new JMenuItem("Track: " + name);
                    colorByMenuEntry.setToolTipText("Colors the tracks by their " + name.toLowerCase(Locale.ROOT));
                    colorByMenuEntry.addActionListener(e -> {
                        trackDrawer.setStrokeColorMode(TrackDrawer.StrokeColorMode.PerTrack);
                        trackDrawer.setStrokeColorTrackFeature(new TrackFeature(key));
                        tracksListCellRenderer.updateColorMaps();
                        uploadSliceToCanvas();
                    });
                    colorByMenu.add(colorByMenuEntry);
                });
            }

            JIPipeDesktopSmallButtonRibbonAction displayModeButton = new JIPipeDesktopSmallButtonRibbonAction("Display mode", "Sets the display mode", UIUtils.getIconFromResources("actions/distribute-graph-directed.png"));
            visualizationBand.add(displayModeButton);
            {
                JPopupMenu displayMenu = UIUtils.addPopupMenuToButton(displayModeButton.getButton());
                for (DisplaySettings.TrackDisplayMode displayMode : DisplaySettings.TrackDisplayMode.values()) {
                    JMenuItem item = new JMenuItem(displayMode.toString());
                    item.addActionListener(e -> {
                        trackDrawer.setTrackDisplayMode(displayMode);
                        uploadSliceToCanvas();
                    });
                    displayMenu.add(item);
                }
            }
            visualizationBand.add(new JIPipeDesktopRibbon.Action(new JPanel(), 1, new Insets(2, 2, 2, 2)));

            visualizationBand.add(new JIPipeDesktopSmallButtonRibbonAction("More settings ...", "Opens a dialog where all available visualization settings can be changed", UIUtils.getIconFromResources("actions/configure.png"), this::openDrawingSettings));
            visualizationBand.add(new JIPipeDesktopSmallButtonRibbonAction("Save settings", "Saves the current settings as default", UIUtils.getIconFromResources("actions/filesave.png"), this::saveDefaults));
        }
        {
            JIPipeDesktopRibbon.Band fileBand = tracksTask.getOrCreateBand("Import/Export");

            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Import from file", "Imports tracks from a file", UIUtils.getIconFromResources("actions/fileopen.png"), this::importTracksFromFile));
            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Export to file", "Exports ROI to a file", UIUtils.getIconFromResources("actions/filesave.png"), this::exportTracksToFile));
        }
        {
            JIPipeDesktopRibbon.Band generalBand = tracksTask.addBand("Tools");
            generalBand.add(new JIPipeDesktopLargeButtonRibbonAction("Track scheme", "Displays the track scheme", TrackMatePlugin.RESOURCES.getIcon32FromResources("trackscheme.png"), this::openTrackScheme));
        }
    }

    @Override
    public void buildDock(JIPipeDesktopDockPanel dockPanel) {

    }

    @Override
    public void buildStatusBar(JToolBar statusBar) {

    }

    public void removeSelectedTracks(boolean deferUploadSlice) {
        if (tracksListControl.getSelectedValuesList().isEmpty())
            return;
        if (JOptionPane.showConfirmDialog(getViewerPanel(), "Do you really want to remove " + tracksListControl.getSelectedValuesList().size() + "tracks?", "Delete tracks", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (Integer trackId : tracksListControl.getSelectedValuesList()) {
                tracksCollection.getModel().setTrackVisibility(trackId, false);
            }
            updateTrackJList(deferUploadSlice);
        }
    }

    public TrackCollectionData getTracksCollection() {
        return tracksCollection;
    }

    private void updateTrackJList(boolean deferUploadSlice) {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        int[] selectedIndices = tracksListControl.getSelectedIndices();
        if (tracksCollection != null) {
            for (Integer trackID : tracksCollection.getTrackModel().trackIDs(true)) {
                model.addElement(trackID);
            }
        }
        tracksListControl.setModel(model);
        tracksListControl.setSelectedIndices(selectedIndices);
        updateContextPanels();
        if (!deferUploadSlice)
            uploadSliceToCanvas();
    }

    private void updateContextPanels() {
        List<Integer> selectedValuesList = tracksListControl.getSelectedValuesList();
        for (SelectionContextPanel selectionContextPanel : selectionContextPanels) {
            selectionContextPanel.selectionUpdated(tracksCollection, selectedValuesList);
        }
    }

    private void measureSelectedEdges() {
        MeasureEdgesNode node = JIPipe.createNode(MeasureEdgesNode.class);
        TrackCollectionData selected = getSelectedTracksOrAll("Measure", "Please select which tracks should be measured");
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        node.getFirstInputSlot().addData(selected, progressInfo);
        node.run(new JIPipeGraphNodeRunContext(), progressInfo);
        ResultsTableData measurements = node.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
        JIPipeDesktopTableEditor.openWindow(getViewerPanel().getDesktopWorkbench(), measurements, "Edge measurements");
    }

    private void measureSelectedTracks() {
        MeasureTracksNode node = JIPipe.createNode(MeasureTracksNode.class);
        TrackCollectionData selected = getSelectedTracksOrAll("Measure", "Please select which tracks should be measured");
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        node.getFirstInputSlot().addData(selected, progressInfo);
        node.run(new JIPipeGraphNodeRunContext(), progressInfo);
        ResultsTableData measurements = node.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
        JIPipeDesktopTableEditor.openWindow(getViewerPanel().getDesktopWorkbench(), measurements, "Track measurements");
    }

    public JList<Integer> getTracksListControl() {
        return tracksListControl;
    }

    public abstract static class SelectionContextPanel extends JPanel {

        private final TracksManagerPlugin2D tracksManagerPlugin;

        protected SelectionContextPanel(TracksManagerPlugin2D tracksManagerPlugin) {
            this.tracksManagerPlugin = tracksManagerPlugin;
        }

        public TracksManagerPlugin2D getTracksManagerPlugin() {
            return tracksManagerPlugin;
        }

        public JIPipeDesktopLegacyImageViewer getViewerPanel() {
            return tracksManagerPlugin.getViewerPanel();
        }

        public abstract void selectionUpdated(TrackCollectionData trackCollectionData, List<Integer> selectedTrackIds);
    }

    public static class SelectionInfoContextPanel extends SelectionContextPanel {

        private final JLabel roiInfoLabel;

        public SelectionInfoContextPanel(TracksManagerPlugin2D parent) {
            super(parent);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            this.roiInfoLabel = new JLabel();
            roiInfoLabel.setIcon(TrackMatePlugin.RESOURCES.getIconFromResources("trackmate-tracker.png"));
            roiInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(roiInfoLabel);
            add(Box.createHorizontalGlue());
        }

        @Override
        public void selectionUpdated(TrackCollectionData trackCollectionData, List<Integer> selectedTrackIds) {
            if (trackCollectionData != null) {
                if (selectedTrackIds.isEmpty()) {
                    roiInfoLabel.setText(trackCollectionData.getNTracks() + " tracks");
                } else {
                    roiInfoLabel.setText(selectedTrackIds.size() + "/" + trackCollectionData.getNTracks() + " tracks");
                }
            } else {
                roiInfoLabel.setText("0 tracks");
            }

        }
    }

}
