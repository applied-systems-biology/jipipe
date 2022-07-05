package org.hkijena.jipipe.extensions.ijtrackmate.display;

import com.google.common.primitives.Ints;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;
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
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots.MeasureSpotsNode;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeature;
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
import java.util.*;
import java.util.stream.Collectors;

public class SpotsManagerPlugin extends ImageViewerPanelPlugin {
    private final JList<Spot> spotsListControl = new JList<>();
    private SpotsCollectionData spotsCollection;
    private List<SelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private JPanel selectionContentPanelUI = new JPanel();
    private final JCheckBoxMenuItem displaySpotsViewMenuItem = new JCheckBoxMenuItem("Display spots",  UIUtils.getIconFromResources("actions/eye.png"));

    private DisplaySettings displaySettings = new DisplaySettings();
    private SpotListCellRenderer spotsListCellRenderer;

    public SpotsManagerPlugin(ImageViewerPanel viewerPanel) {
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

        JScrollPane scrollPane = new JScrollPane(spotsListControl);
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
            item.addActionListener(e -> importSpotsFromFile());
            ioMenu.add(item);
        }
        // Separator
        ioMenu.addSeparator();
        // Export items
//        {
//            JMenuItem item = new JMenuItem("Export to ImageJ ROI Manager", UIUtils.getIconFromResources("apps/imagej.png"));
//            item.addActionListener(e -> {
//                if (spots.isEmpty()) {
//                    JOptionPane.showMessageDialog(getViewerPanel(), "No ROI to export.", "Export ROI", JOptionPane.ERROR_MESSAGE);
//                    return;
//                }
//                ROIListData result = getSelectedROIOrAll("Export ROI", "Do you want to export all ROI or only the selected ones?");
//                if (result != null) {
//                    exportROIsToManager(result);
//                }
//            });
//            ioMenu.add(item);
//        }
        {
            JMenuItem item = new JMenuItem("Export to file", UIUtils.getIconFromResources("actions/save.png"));
            item.addActionListener(e -> {
                if (spotsCollection.getSpots().getNSpots(true) <= 0) {
                    JOptionPane.showMessageDialog(getViewerPanel(), "No spots to export.", "Export spots", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                SpotsCollectionData result = getSelectedSpotsOrAll("Export spots", "Do you want to export all ROI or only the selected ones?");
                if (result != null) {
                    exportSpotsToFile(result);
                }
            });
            ioMenu.add(item);
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

    public DisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    private void createButtons(JMenuBar menuBar) {
        {
            JButton removeButton = new JButton("Delete", UIUtils.getIconFromResources("actions/delete.png"));
            removeButton.setToolTipText("Remove selected spots");
            removeButton.addActionListener(e -> {
                if (spotsListControl.getSelectedValuesList().isEmpty())
                    return;
                if (JOptionPane.showConfirmDialog(getViewerPanel(), "Do you really want to remove " + spotsListControl.getSelectedValuesList().size() + "spots?", "Delete spots", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    removeSelectedSpots(false);
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
        SpotFeature.VALUE_LABELS.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(key -> {
            String name = SpotFeature.VALUE_LABELS.get(key);
            JMenuItem colorByMenuEntry = new JMenuItem(name);
            colorByMenuEntry.setToolTipText("Colors the spots by their " + name.toLowerCase());
            colorByMenuEntry.addActionListener(e -> {
                displaySettings.setSpotColorBy(DisplaySettings.TrackMateObject.SPOTS, key);
                displaySettings.setSpotMinMax(0, getSpotsCollection().getNSpots());
                spotsListCellRenderer.updateColorMaps();
                uploadSliceToCanvas();
            });
            colorByMenu.add(colorByMenuEntry);
        });
        viewMenu.add(colorByMenu);
    }

    private void importSpotsFromFile() {
        Path path = FileChooserSettings.openFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Import ROI", UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null && displaySpotsViewMenuItem.getState()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try(JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, path)) {
                SpotsCollectionData spotsCollectionData = SpotsCollectionData.importData(storage, progressInfo);
                setSpots(spotsCollectionData, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void exportSpotsToFile(SpotsCollectionData rois) {
        FileNameExtensionFilter[] fileNameExtensionFilters = new FileNameExtensionFilter[] { UIUtils.EXTENSION_FILTER_ZIP };
        Path path = FileChooserSettings.saveFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Export spots", fileNameExtensionFilters);
        if (path != null) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try(JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(progressInfo, path)) {
                rois.exportData(storage, "Spots", false, progressInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if(spotsCollection != null && displaySpotsViewMenuItem.getState()) {
            SpotOverlay spotOverlay = new SpotOverlay(spotsCollection.getModel(), spotsCollection.getImage(), displaySettings);
            updateRoiCanvas(spotOverlay, getViewerPanel().getZoomedDummyCanvas());
            spotOverlay.setSpotSelection(spotsListControl.getSelectedValuesList());
            graphics2D.translate(renderArea.x, renderArea.y);
            spotOverlay.drawOverlay(graphics2D);
            graphics2D.translate(-renderArea.x, -renderArea.y);
        }
    }

    private void updateRoiCanvas(Roi roi, ImageCanvas canvas) {
        // First set the image
        roi.setImage(getCurrentImage());
        // We have to set the canvas or overlay rendering will fail
        try {
            Field field = Roi.class.getDeclaredField("ic");
            field.setAccessible(true);
            field.set(roi, canvas);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex) {
        if(spotsCollection != null) {
            ImagePlus imagePlus = spotsCollection.getImage();
            imagePlus.setSlice(sliceIndex.zeroSliceIndexToOneStackIndex(imagePlus));
            SpotOverlay spotOverlay = new SpotOverlay(spotsCollection.getModel(), imagePlus, displaySettings);
            updateRoiCanvas(spotOverlay, getViewerPanel().getExportDummyCanvas());
            spotOverlay.setSpotSelection(spotsListControl.getSelectedValuesList());
            Graphics2D graphics2D = image.createGraphics();
            spotOverlay.drawOverlay(graphics2D);
            graphics2D.dispose();
            imagePlus.setSlice(1);
        }
    }

    @Override
    public void onSliceChanged(boolean deferUploadSlice) {
        updateSpotJList(deferUploadSlice);
    }

    @Override
    public String getCategory() {
        return "Spots";
    }

    @Override
    public Icon getCategoryIcon() {
        return TrackMateExtension.RESOURCES.getIconFromResources("trackmate-spots.png");
    }

    private void initialize() {
        // Setup ROI
        spotsListCellRenderer = new SpotListCellRenderer(this);
        spotsListControl.setCellRenderer(spotsListCellRenderer);
        spotsListControl.addListSelectionListener(e -> {
            updateContextPanels();
            uploadSliceToCanvas();
        });
    }

    public void setSpots(SpotsCollectionData spots, boolean deferUploadSlice) {
        spotsCollection = new SpotsCollectionData(spots);
        spotsListCellRenderer.updateColorMaps();
        updateSpotJList(deferUploadSlice);
        uploadSliceToCanvas();
    }

    public void removeSelectedSpots(boolean deferUploadSlice) {
        for (Spot spot : spotsListControl.getSelectedValuesList()) {
            spotsCollection.getSpots().remove(spot, spot.getFeature(Spot.FRAME).intValue());
        }
        updateSpotJList(deferUploadSlice);
    }

    public SpotsCollectionData getSpotsCollection() {
        return spotsCollection;
    }

    private void updateSpotJList(boolean deferUploadSlice) {
        DefaultListModel<Spot> model = new DefaultListModel<>();
        int[] selectedIndices = spotsListControl.getSelectedIndices();
        for (Spot spot : spotsCollection.getSpots().iterable(true)) {
            model.addElement(spot);
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

        private final SpotsManagerPlugin roiManagerPlugin;

        protected SelectionContextPanel(SpotsManagerPlugin roiManagerPlugin) {
            this.roiManagerPlugin = roiManagerPlugin;
        }

        public SpotsManagerPlugin getRoiManagerPlugin() {
            return roiManagerPlugin;
        }

        public ImageViewerPanel getViewerPanel() {
            return roiManagerPlugin.getViewerPanel();
        }

        public abstract void selectionUpdated(SpotsCollectionData spotsCollectionData, List<Spot> selectedSpots);
    }

    public static class SelectionInfoContextPanel extends SelectionContextPanel {

        private final JLabel roiInfoLabel;

        public SelectionInfoContextPanel(SpotsManagerPlugin parent) {
            super(parent);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(BorderFactory.createEmptyBorder(4,2,4,2));
            this.roiInfoLabel = new JLabel();
            roiInfoLabel.setIcon(TrackMateExtension.RESOURCES.getIconFromResources("trackmate-spots.png"));
            roiInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(roiInfoLabel);
            add(Box.createHorizontalGlue());
            JList<Spot> roiJList = getRoiManagerPlugin().getSpotsListControl();
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
        public void selectionUpdated(SpotsCollectionData spotsCollectionData, List<Spot> selectedSpots) {
            if(selectedSpots.isEmpty())
                roiInfoLabel.setText(spotsCollectionData.getNSpots() + " spots");
            else
                roiInfoLabel.setText(selectedSpots.size() + "/" + spotsCollectionData.getNSpots() + " spots");
        }
    }

    public static class MeasureContextPanel extends SelectionContextPanel {

        private ImageStatisticsSetParameter statistics = new ImageStatisticsSetParameter();

        protected MeasureContextPanel(SpotsManagerPlugin roiManagerPlugin) {
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

            JButton measureButton = new JButton("Measure", UIUtils.getIconFromResources("actions/statistics.png"));
            measureButton.addActionListener(e -> measure());
            add(measureButton);

//            JButton settingsButton = new JButton( UIUtils.getIconFromResources("actions/configure.png"));
//            settingsButton.setToolTipText("Configure measurements");
//            UIUtils.makeFlat25x25(settingsButton);
//            settingsButton.addActionListener(e -> showSettings());
//            add(settingsButton);


        }

        private void measure() {
            MeasureSpotsNode node = JIPipe.createNode(MeasureSpotsNode.class);
            SpotsCollectionData selected = getRoiManagerPlugin().getSelectedSpotsOrAll("Measure", "Please select which spots should be measured");
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            node.getFirstInputSlot().addData(selected, progressInfo);
            node.run(progressInfo);
            ResultsTableData measurements = node.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
            TableEditor.openWindow(getViewerPanel().getWorkbench(), measurements, "Measurements");
        }

        @Override
        public void selectionUpdated(SpotsCollectionData spotsCollectionData, List<Spot> selectedSpots) {
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
