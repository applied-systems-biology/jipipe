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

package org.hkijena.jipipe.extensions.ijtrackmate.display.spots;

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
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackMateUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiDrawer;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ImageViewerPanelPlugin;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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

    private final JCheckBoxMenuItem displayLabelsViewMenuItem = new JCheckBoxMenuItem("Display labels", UIUtils.getIconFromResources("actions/tag.png"));

    private String displayLabelsFeature = null;

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
        displayLabelsViewMenuItem.setState(true);
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
            toggle.addActionListener(e -> uploadSliceToCanvas());
            viewMenu.add(toggle);
        }
        {
            JMenu colorByMenu = new JMenu("Color by ...");
            SpotFeature.VALUE_LABELS.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(key -> {
                String name = SpotFeature.VALUE_LABELS.get(key);
                JMenuItem colorByMenuEntry = new JMenuItem(name);
                colorByMenuEntry.setToolTipText("Colors the spots by their " + name.toLowerCase());
                colorByMenuEntry.addActionListener(e -> {
                    displaySettings.setSpotColorBy(DisplaySettings.TrackMateObject.SPOTS, key);
                    double min = Double.POSITIVE_INFINITY;
                    double max = Double.NEGATIVE_INFINITY;
                    for (Spot spot : getSpotsCollection().getSpots().iterable(true)) {
                        double feature = spotsCollection.getSpotFeature(spot, key, Double.NaN);
                        if(Double.isNaN(feature))
                            continue;
                        min = Math.min(feature, min);
                        max = Math.max(feature, max);
                    }
                    if(Double.isFinite(min)) {
                        displaySettings.setSpotMinMax(min, max);
                    }
                    spotsListCellRenderer.updateColorMaps();
                    uploadSliceToCanvas();
                });
                colorByMenu.add(colorByMenuEntry);
            });
            viewMenu.add(colorByMenu);
        }
        viewMenu.addSeparator();
        {
            displayLabelsViewMenuItem.addActionListener(e->uploadSliceToCanvas());
            viewMenu.add(displayLabelsViewMenuItem);
        }
        {
            JMenu setLabelMenu = new JMenu("Set label to ...");
            {
                JMenuItem nameItem = new JMenuItem("Name");
                nameItem.addActionListener(e -> {
                    displayLabelsFeature = null;
                    uploadSliceToCanvas();
                });
                setLabelMenu.add(nameItem);
            }
            SpotFeature.VALUE_LABELS.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(key -> {
                String name = SpotFeature.VALUE_LABELS.get(key);
                JMenuItem setLabelMenuEntry = new JMenuItem(name);
                setLabelMenuEntry.setToolTipText("Set the displayed label to " + name.toLowerCase());
                setLabelMenuEntry.addActionListener(e -> {
                    displayLabelsFeature = key;
                    uploadSliceToCanvas();
                });
                setLabelMenu.add(setLabelMenuEntry);
            });
            viewMenu.add(setLabelMenu);
        }
    }

    private void importSpotsFromFile() {
        Path path = FileChooserSettings.openFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Import spots", UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null && displaySpotsViewMenuItem.getState()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try(JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, path)) {
                SpotsCollectionData spotsCollectionData = SpotsCollectionData.importData(storage, progressInfo);
                setSpotCollection(spotsCollectionData, false);
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
            if(displayLabelsViewMenuItem.getState()) {
                Font labelFont = new Font(Font.DIALOG, Font.PLAIN, 12);
                for (Spot spot : getSpotsCollection().getSpots().iterable(true)) {
                    int z = (int) spot.getDoublePosition(2);
                    int frame = spot.getFeature(Spot.FRAME).intValue();
                    if(z == sliceIndex.getZ() && frame == sliceIndex.getT()) {
                        String label;
                        if(StringUtils.isNullOrEmpty(displayLabelsFeature)) {
                            label = spot.getName();
                        }
                        else {
                            label = TrackMateUtils.FEATURE_DECIMAL_FORMAT.format(Optional.ofNullable(spot.getFeature(displayLabelsFeature)).orElse(0d));
                        }
                        RoiDrawer.drawLabelOnGraphics(label,
                                graphics2D,
                                spot.getDoublePosition(0),
                                spot.getDoublePosition(1),
                                getViewerPanel().getCanvas().getZoom(),
                                Color.WHITE,
                                Color.BLACK,
                                labelFont,
                                true);
                    }
                }
            }
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
            if(displayLabelsViewMenuItem.getState()) {
                Font labelFont = new Font(Font.DIALOG, Font.PLAIN, 12);
                for (Spot spot : getSpotsCollection().getSpots().iterable(true)) {
                    int z = (int) spot.getDoublePosition(2);
                    int frame = spot.getFeature(Spot.FRAME).intValue();
                    if(z == sliceIndex.getZ() && frame == sliceIndex.getT()) {
                        String label;
                        if(StringUtils.isNullOrEmpty(displayLabelsFeature)) {
                            label = spot.getName();
                        }
                        else {
                            label = TrackMateUtils.FEATURE_DECIMAL_FORMAT.format(Optional.ofNullable(spot.getFeature(displayLabelsFeature)).orElse(0d));
                        }
                        RoiDrawer.drawLabelOnGraphics(label,
                                graphics2D,
                                spot.getDoublePosition(0),
                                spot.getDoublePosition(1),
                                1.0,
                                Color.WHITE,
                                Color.BLACK,
                                labelFont,
                                true);
                    }
                }
            }
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

    public void setSpotCollection(SpotsCollectionData spots, boolean deferUploadSlice) {
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

        private final SpotsManagerPlugin spotsManagerPlugin;

        protected SelectionContextPanel(SpotsManagerPlugin spotsManagerPlugin) {
            this.spotsManagerPlugin = spotsManagerPlugin;
        }

        public SpotsManagerPlugin getSpotsManagerPlugin() {
            return spotsManagerPlugin;
        }

        public ImageViewerPanel getViewerPanel() {
            return spotsManagerPlugin.getViewerPanel();
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
            JList<Spot> roiJList = getSpotsManagerPlugin().getSpotsListControl();
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
            SpotsCollectionData selected = getSpotsManagerPlugin().getSelectedSpotsOrAll("Measure", "Please select which spots should be measured");
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
