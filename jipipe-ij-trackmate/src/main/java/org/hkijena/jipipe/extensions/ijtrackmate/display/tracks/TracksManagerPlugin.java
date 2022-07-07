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

package org.hkijena.jipipe.extensions.ijtrackmate.display.tracks;

import com.google.common.primitives.Ints;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.hyperstack.TrackOverlay;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.TrackMateExtension;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks.MeasureEdgesNode;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks.MeasureTracksNode;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.EdgeFeature;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ImageViewerPanelPlugin;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.UIUtils;
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TracksManagerPlugin extends ImageViewerPanelPlugin {
    private final JList<Integer> tracksListControl = new JList<>();
    private TrackCollectionData tracksCollection;
    private List<SelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private JPanel selectionContentPanelUI = new JPanel();
    private final JCheckBoxMenuItem displaySpotsViewMenuItem = new JCheckBoxMenuItem("Display tracks",  UIUtils.getIconFromResources("actions/eye.png"));

    private DisplaySettings displaySettings = new DisplaySettings();
    private TrackListCellRenderer tracksListCellRenderer;

    public TracksManagerPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        initializeDefaults();
        initialize();
        addSelectionContextPanel(new SelectionInfoContextPanel(this));
        addSelectionContextPanel(new MeasureContextPanel(this));
    }

    private void initializeDefaults() {
        displaySpotsViewMenuItem.setState(true);
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        if (getCurrentImage() == null)
            return;

        JPanel panel = new JPanel(new BorderLayout());
        panel.setMinimumSize(new Dimension(100, 300));
        panel.setBorder(BorderFactory.createEtchedBorder());

        JMenuBar listMenuBar = new JMenuBar();
        panel.add(listMenuBar, BorderLayout.NORTH);

        createViewMenu(listMenuBar);
//        createSelectionMenu(listMenuBar);
        createImportExportMenu(listMenuBar);

        listMenuBar.add(Box.createHorizontalGlue());
        createButtons(listMenuBar);

        JScrollPane scrollPane = new JScrollPane(tracksListControl);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        selectionContentPanelUI.setLayout(new BoxLayout(selectionContentPanelUI, BoxLayout.Y_AXIS));
        panel.add(selectionContentPanelUI, BorderLayout.SOUTH);

        formPanel.addVerticalGlue(panel, null);
    }

    private void createImportExportMenu(JMenuBar menuBar) {
        JMenu ioMenu = new JMenu("Import/Export");
        menuBar.add(ioMenu);
        // Import items
        {
            JMenuItem item = new JMenuItem("Import from file", UIUtils.getIconFromResources("actions/fileopen.png"));
            item.addActionListener(e -> importTracksFromFile());
            ioMenu.add(item);
        }
        // Separator
        ioMenu.addSeparator();
        // Export items
        {
            JMenuItem item = new JMenuItem("Export to file", UIUtils.getIconFromResources("actions/save.png"));
            item.addActionListener(e -> {
                if (tracksCollection.getSpots().getNSpots(true) <= 0) {
                    JOptionPane.showMessageDialog(getViewerPanel(), "No spots to export.", "Export spots", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                SpotsCollectionData result = getSelectedTracksOrAll("Export spots", "Do you want to export all ROI or only the selected ones?");
                if (result != null) {
                    exportTracksToFile(result);
                }
            });
            ioMenu.add(item);
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

    public DisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    private void createButtons(JMenuBar menuBar) {
        {
            JButton removeButton = new JButton("Delete", UIUtils.getIconFromResources("actions/delete.png"));
            removeButton.setToolTipText("Remove selected spots");
            removeButton.addActionListener(e -> {
                if (tracksListControl.getSelectedValuesList().isEmpty())
                    return;
                if (JOptionPane.showConfirmDialog(getViewerPanel(), "Do you really want to remove " + tracksListControl.getSelectedValuesList().size() + "spots?", "Delete spots", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    removeSelectedTracks(false);
                }
            });
            menuBar.add(removeButton);
        }
    }


    private void createViewMenu(JMenuBar menuBar) {
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        {
            JCheckBoxMenuItem toggle = displaySpotsViewMenuItem;
            toggle.addActionListener(e -> {
                uploadSliceToCanvas();
            });
            viewMenu.add(toggle);
        }
        JMenu colorByMenu = new JMenu("Color by ...");
        EdgeFeature.VALUE_LABELS.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(key -> {
            String name = EdgeFeature.VALUE_LABELS.get(key);
            JMenuItem colorByMenuEntry = new JMenuItem(name);
            colorByMenuEntry.setToolTipText("Colors the track edges by their " + name.toLowerCase());
            colorByMenuEntry.addActionListener(e -> {
                displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.EDGES, key);
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (DefaultWeightedEdge edge : getTracksCollection().getTracks().edgeSet()) {
                    double feature = tracksCollection.getEdgeFeature(edge, key, Double.NaN);
                    if(Double.isNaN(feature))
                        continue;
                    min = Math.min(feature, min);
                    max = Math.max(feature, max);
                }
                if(Double.isFinite(min)) {
                    displaySettings.setSpotMinMax(min, max);
                }
                tracksListCellRenderer.updateColorMaps();
                uploadSliceToCanvas();
            });
            colorByMenu.add(colorByMenuEntry);
        });
        viewMenu.add(colorByMenu);
    }

    private void importTracksFromFile() {
        Path path = FileChooserSettings.openFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Import tracks", UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null && displaySpotsViewMenuItem.getState()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try(JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, path)) {
                TrackCollectionData trackCollectionData = TrackCollectionData.importData(storage, progressInfo);
                setTrackCollection(trackCollectionData, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void exportTracksToFile(SpotsCollectionData rois) {
        FileNameExtensionFilter[] fileNameExtensionFilters = new FileNameExtensionFilter[] { UIUtils.EXTENSION_FILTER_ZIP };
        Path path = FileChooserSettings.saveFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Export tracks", fileNameExtensionFilters);
        if (path != null) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try(JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(progressInfo, path)) {
                rois.exportData(storage, "Tracks", false, progressInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (tracksCollection != null && displaySpotsViewMenuItem.getState()) {
            TrackOverlay trackOverlay = new TrackOverlay(tracksCollection.getModel(), tracksCollection.getImage(), displaySettings);
            ImageJUtils.setRoiCanvas(trackOverlay, getCurrentImage(), getViewerPanel().getZoomedDummyCanvas());
            List<Integer> selectedValuesList = tracksListControl.getSelectedValuesList();
            if (!selectedValuesList.isEmpty()) {
                Set<DefaultWeightedEdge> highlight = new HashSet<>();
                for (Integer trackId : selectedValuesList) {
                    highlight.addAll(tracksCollection.getTracks().trackEdges(trackId));
                }
                trackOverlay.setHighlight(highlight);
            }
            graphics2D.translate(renderArea.x, renderArea.y);
            trackOverlay.drawOverlay(graphics2D);
            graphics2D.translate(-renderArea.x, -renderArea.y);
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex) {
        if(tracksCollection != null) {
            ImagePlus imagePlus = tracksCollection.getImage();
            imagePlus.setSlice(sliceIndex.zeroSliceIndexToOneStackIndex(imagePlus));
            TrackOverlay trackOverlay = new TrackOverlay(tracksCollection.getModel(), imagePlus, displaySettings);
            ImageJUtils.setRoiCanvas(trackOverlay, getCurrentImage(), getViewerPanel().getExportDummyCanvas());
            List<Integer> selectedValuesList = tracksListControl.getSelectedValuesList();
            if (!selectedValuesList.isEmpty()) {
                Set<DefaultWeightedEdge> highlight = new HashSet<>();
                for (Integer trackId : selectedValuesList) {
                    highlight.addAll(tracksCollection.getTracks().trackEdges(trackId));
                }
                trackOverlay.setHighlight(highlight);
            }
            Graphics2D graphics2D = image.createGraphics();
            trackOverlay.drawOverlay(graphics2D);
            graphics2D.dispose();
            imagePlus.setSlice(1);
        }
    }

    @Override
    public void onSliceChanged(boolean deferUploadSlice) {
        updateTrackJList(deferUploadSlice);
    }

    @Override
    public String getCategory() {
        return "Tracks";
    }

    @Override
    public Icon getCategoryIcon() {
        return TrackMateExtension.RESOURCES.getIconFromResources("trackmate-tracker.png");
    }

    private void initialize() {
        // Setup ROI
        tracksListCellRenderer = new TrackListCellRenderer(this);
        tracksListControl.setCellRenderer(tracksListCellRenderer);
        tracksListControl.addListSelectionListener(e -> {
            updateContextPanels();
            uploadSliceToCanvas();
        });
    }

    public void setTrackCollection(TrackCollectionData tracksCollection, boolean deferUploadSlice) {
        this.tracksCollection = new TrackCollectionData(tracksCollection);
        tracksListCellRenderer.updateColorMaps();
        updateTrackJList(deferUploadSlice);
        uploadSliceToCanvas();
    }

    public void removeSelectedTracks(boolean deferUploadSlice) {
        for (Integer trackId : tracksListControl.getSelectedValuesList()) {
            tracksCollection.getModel().setTrackVisibility(trackId, false);
        }
        updateTrackJList(deferUploadSlice);
    }

    public TrackCollectionData getTracksCollection() {
        return tracksCollection;
    }

    private void updateTrackJList(boolean deferUploadSlice) {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        int[] selectedIndices = tracksListControl.getSelectedIndices();
        for (Integer trackID : tracksCollection.getTracks().trackIDs(true)) {
            model.addElement(trackID);
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

    public JList<Integer> getTracksListControl() {
        return tracksListControl;
    }

    public abstract static class SelectionContextPanel extends JPanel {

        private final TracksManagerPlugin tracksManagerPlugin;

        protected SelectionContextPanel(TracksManagerPlugin tracksManagerPlugin) {
            this.tracksManagerPlugin = tracksManagerPlugin;
        }

        public TracksManagerPlugin getTracksManagerPlugin() {
            return tracksManagerPlugin;
        }

        public ImageViewerPanel getViewerPanel() {
            return tracksManagerPlugin.getViewerPanel();
        }

        public abstract void selectionUpdated(TrackCollectionData trackCollectionData, List<Integer> selectedTrackIds);
    }

    public static class SelectionInfoContextPanel extends SelectionContextPanel {

        private final JLabel roiInfoLabel;

        public SelectionInfoContextPanel(TracksManagerPlugin parent) {
            super(parent);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(BorderFactory.createEmptyBorder(4,2,4,2));
            this.roiInfoLabel = new JLabel();
            roiInfoLabel.setIcon(TrackMateExtension.RESOURCES.getIconFromResources("trackmate-tracker.png"));
            roiInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(roiInfoLabel);
            add(Box.createHorizontalGlue());
            JList<Integer> roiJList = getTracksManagerPlugin().getTracksListControl();
            {
                JButton selectAllButton = new JButton("Select all", UIUtils.getIconFromResources("actions/edit-select-all.png"));
//                UIUtils.makeFlat25x25(selectAllButton);
                selectAllButton.setToolTipText("Select all");
                selectAllButton.addActionListener(e -> {
                    roiJList.setSelectionInterval(0, roiJList.getModel().getSize() - 1);
                });
                add(selectAllButton);
            }
            {
                JButton deselectAllButton = new JButton("Clear selection", UIUtils.getIconFromResources("actions/edit-select-none.png"));
//                UIUtils.makeFlat25x25(deselectAllButton);
                deselectAllButton.setToolTipText("Clear selection");
                deselectAllButton.addActionListener(e -> {
                    roiJList.clearSelection();
                });
                add(deselectAllButton);
            }
            {
                JButton invertSelectionButton = new JButton(UIUtils.getIconFromResources("actions/object-inverse.png"));
                UIUtils.makeFlat25x25(invertSelectionButton);
                invertSelectionButton.setToolTipText("Invert selection");
                invertSelectionButton.addActionListener(e -> {
                    Set<Integer> selectedIndices = Arrays.stream(roiJList.getSelectedIndices()).boxed().collect(Collectors.toSet());
                    roiJList.clearSelection();
                    Set<Integer> newSelectedIndices = new HashSet<>();
                    for (int i = 0; i < roiJList.getModel().getSize(); i++) {
                        if (!selectedIndices.contains(i))
                            newSelectedIndices.add(i);
                    }
                    roiJList.setSelectedIndices(Ints.toArray(newSelectedIndices));
                });
                add(invertSelectionButton);
            }
        }

        @Override
        public void selectionUpdated(TrackCollectionData trackCollectionData, List<Integer> selectedTrackIds) {
            if(selectedTrackIds.isEmpty())
                roiInfoLabel.setText(trackCollectionData.getNTracks() + " tracks");
            else
                roiInfoLabel.setText(selectedTrackIds.size() + "/" + trackCollectionData.getNTracks() + " tracks");
        }
    }

    public static class MeasureContextPanel extends SelectionContextPanel {

        private ImageStatisticsSetParameter statistics = new ImageStatisticsSetParameter();

        protected MeasureContextPanel(TracksManagerPlugin roiManagerPlugin) {
            super(roiManagerPlugin);
            statistics.setCollapsed(false);
            statistics.getValues().add(Measurement.PixelValueMean);

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(BorderFactory.createEmptyBorder(4,2,4,2));
//            this.roiInfoLabel = new JLabel("");
//            roiInfoLabel.setIcon(UIUtils.getIconFromResources("data-types/results-table.png"));
//            roiInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
//            add(roiInfoLabel);
            add(Box.createHorizontalGlue());

            JButton measureTracksButton = new JButton("Measure tracks", UIUtils.getIconFromResources("actions/statistics.png"));
            measureTracksButton.addActionListener(e -> measureTracks());
            add(measureTracksButton);

            JButton measureEdgesButton = new JButton("Measure edges", UIUtils.getIconFromResources("actions/statistics.png"));
            measureEdgesButton.addActionListener(e -> measureEdges());
            add(measureEdgesButton);

//            JButton settingsButton = new JButton( UIUtils.getIconFromResources("actions/configure.png"));
//            settingsButton.setToolTipText("Configure measurements");
//            UIUtils.makeFlat25x25(settingsButton);
//            settingsButton.addActionListener(e -> showSettings());
//            add(settingsButton);


        }

        private void measureEdges() {
            MeasureEdgesNode node = JIPipe.createNode(MeasureEdgesNode.class);
            TrackCollectionData selected = getTracksManagerPlugin().getSelectedTracksOrAll("Measure", "Please select which tracks should be measured");
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            node.getFirstInputSlot().addData(selected, progressInfo);
            node.run(progressInfo);
            ResultsTableData measurements = node.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
            TableEditor.openWindow(getViewerPanel().getWorkbench(), measurements, "Edge measurements");
        }

        private void measureTracks() {
            MeasureTracksNode node = JIPipe.createNode(MeasureTracksNode.class);
            TrackCollectionData selected = getTracksManagerPlugin().getSelectedTracksOrAll("Measure", "Please select which tracks should be measured");
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            node.getFirstInputSlot().addData(selected, progressInfo);
            node.run(progressInfo);
            ResultsTableData measurements = node.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
            TableEditor.openWindow(getViewerPanel().getWorkbench(), measurements, "Track measurements");
        }

        @Override
        public void selectionUpdated(TrackCollectionData trackCollectionData, List<Integer> selectedTrackIds) {
        }

//        private void showSettings() {
//            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getViewerPanel()));
//            dialog.setTitle("Measurement settings");
//            dialog.setContentPane(new ParameterPanel(new JIPipeDummyWorkbench(), this, null, FormPanel.WITH_SCROLLING));
//            UIUtils.addEscapeListener(dialog);
//            dialog.setSize(640, 480);
//            dialog.setLocationRelativeTo(getViewerPanel());
//            dialog.revalidate();
//            dialog.repaint();
//            dialog.setVisible(true);
//        }

        @JIPipeDocumentation(name = "Statistics", description = "The statistics to measure")
        @JIPipeParameter("statistics")
        public ImageStatisticsSetParameter getStatistics() {
            return statistics;
        }

        @JIPipeParameter("statistics")
        public void setStatistics(ImageStatisticsSetParameter statistics) {
            this.statistics = statistics;
        }
    }
}
