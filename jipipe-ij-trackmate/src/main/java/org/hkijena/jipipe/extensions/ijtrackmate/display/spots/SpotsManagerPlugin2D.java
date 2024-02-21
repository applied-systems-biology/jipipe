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
import org.hkijena.jipipe.extensions.ijtrackmate.TrackMateExtension;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots.MeasureSpotsNode;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeature;
import org.hkijena.jipipe.extensions.ijtrackmate.settings.ImageViewerUISpotsDisplaySettings;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotDrawer;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin2D;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.SmallToggleButtonAction;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class SpotsManagerPlugin2D extends JIPipeImageViewerPlugin2D {
    private final JList<Spot> spotsListControl = new JList<>();
    private final SmallToggleButtonAction displaySpotsViewMenuItem = new SmallToggleButtonAction("Display spots", "Determines whether spots are displayed", UIUtils.getIconFromResources("actions/eye.png"));
    private final SmallToggleButtonAction displayLabelsViewMenuItem = new SmallToggleButtonAction("Display labels", "Determines whether spot labels are displayed", UIUtils.getIconFromResources("actions/tag.png"));
    private final List<SelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private final Ribbon ribbon = new Ribbon(3);
    private SpotsCollectionData spotsCollection;
    private SpotDrawer spotDrawer = new SpotDrawer();
    private SpotListCellRenderer spotsListCellRenderer;
    private JPanel mainPanel;

    public SpotsManagerPlugin2D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
        initializeDefaults();
        initialize();
        addSelectionContextPanel(new SelectionInfoContextPanel(this));
    }

    private void initializeDefaults() {
        ImageViewerUISpotsDisplaySettings settings = ImageViewerUISpotsDisplaySettings.getInstance();
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

    private void initializeRibbon() {
        {
            displayLabelsViewMenuItem.addActionListener(e -> {
                spotDrawer.getLabelSettings().setDrawLabels(displayLabelsViewMenuItem.getState());
            });
            displayLabelsViewMenuItem.addActionListener(this::uploadSliceToCanvas);
            displaySpotsViewMenuItem.addActionListener(this::uploadSliceToCanvas);


            Ribbon.Task viewTask = ribbon.getOrCreateTask("View");
            Ribbon.Band generalBand = viewTask.addBand("General");
            Ribbon.Band visualizationBand = viewTask.addBand("Visualization");

            generalBand.add(displaySpotsViewMenuItem);
            generalBand.add(displayLabelsViewMenuItem);

            SmallButtonAction colorButton = new SmallButtonAction("Color by ...", "Allows to change how spots are colored", UIUtils.getIconFromResources("actions/colors-rgb.png"));
            visualizationBand.add(colorButton);
            {
                JPopupMenu colorByMenu = UIUtils.addPopupMenuToButton(colorButton.getButton());
                SpotFeature.VALUE_LABELS.keySet().stream().sorted(NaturalOrderComparator.INSTANCE).forEach(key -> {
                    String name = SpotFeature.VALUE_LABELS.get(key);
                    JMenuItem colorByMenuEntry = new JMenuItem(name);
                    colorByMenuEntry.setToolTipText("Colors the spots by their " + name.toLowerCase());
                    colorByMenuEntry.addActionListener(e -> {
                        spotDrawer.setStrokeColorFeature(new SpotFeature(key));
                        spotDrawer.setUniformStrokeColor(false);
                        spotsListCellRenderer.updateColorMaps();
                        uploadSliceToCanvas();
                    });
                    colorByMenu.add(colorByMenuEntry);
                });
            }

            SmallButtonAction labelButton = new SmallButtonAction("Set label to ...", "Allows to change how spots are labeled", UIUtils.getIconFromResources("actions/colors-rgb.png"));
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
                    setLabelMenuEntry.setToolTipText("Set the displayed label to " + name.toLowerCase());
                    setLabelMenuEntry.addActionListener(e -> {
                        spotDrawer.getLabelSettings().setDrawName(false);
                        spotDrawer.getLabelSettings().setDrawnFeature(new SpotFeature(key));
                        uploadSliceToCanvas();
                    });
                    setLabelMenu.add(setLabelMenuEntry);
                });
            }
            visualizationBand.add(new Ribbon.Action(new JPanel(), 1, new Insets(2, 2, 2, 2)));

            visualizationBand.add(new SmallButtonAction("More settings ...", "Opens a dialog where all available visualization settings can be changed", UIUtils.getIconFromResources("actions/configure.png"), this::openDrawingSettings));
            visualizationBand.add(new SmallButtonAction("Save settings", "Saves the current settings as default", UIUtils.getIconFromResources("actions/save.png"), this::saveDefaults));
        }
        {
            Ribbon.Task selectionTask = ribbon.addTask("Selection");
            Ribbon.Band generalBand = selectionTask.addBand("General");
            Ribbon.Band modifyBand = selectionTask.addBand("Modify");
            Ribbon.Band measureBand = selectionTask.addBand("Measure");

//            ROIPickerTool pickerTool = new ROIPickerTool(this);
//            LargeToggleButtonAction pickerToggle = new LargeToggleButtonAction("Pick", "Allows to select ROI via the mouse", UIUtils.getIcon32FromResources("actions/followmouse.png"));
//            pickerTool.addToggleButton(pickerToggle.getButton(), getViewerPanel().getCanvas());
//            generalBand.add(pickerToggle);

            generalBand.add(new SmallButtonAction("Select all", "Selects all spots", UIUtils.getIconFromResources("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new SmallButtonAction("Clear selection", "Deselects all spots", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new SmallButtonAction("Invert selection", "Inverts the current selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new SmallButtonAction("Delete", "Deletes the selected spots", UIUtils.getIconFromResources("actions/delete.png"), () -> removeSelectedSpots(false)));

            SmallButtonAction measureAction = new SmallButtonAction("Measure", "Measures the spots and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedSpots);
            measureBand.add(measureAction);
        }
        {
            Ribbon.Task importExportTask = ribbon.addTask("Import/Export");
            Ribbon.Band fileBand = importExportTask.addBand("File");

            fileBand.add(new SmallButtonAction("Import from file", "Imports spots from a file", UIUtils.getIconFromResources("actions/fileopen.png"), this::importSpotsFromFile));
            fileBand.add(new SmallButtonAction("Export to file", "Exports ROI to a file", UIUtils.getIconFromResources("actions/save.png"), this::exportSpotsToFile));
        }
    }

    private void measureSelectedSpots() {
        MeasureSpotsNode node = JIPipe.createNode(MeasureSpotsNode.class);

        SpotsCollectionData selected = getSelectedSpotsOrAll("Measure spots", "Please select which spots should be measured");
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        node.getFirstInputSlot().addData(selected, progressInfo);
        node.run(new JIPipeGraphNodeRunContext(), progressInfo);
        ResultsTableData measurements = node.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
        TableEditor.openWindow(getViewerPanel().getWorkbench(), measurements, "Measurements");
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
            ImageViewerUISpotsDisplaySettings settings = ImageViewerUISpotsDisplaySettings.getInstance();
            settings.getSpotDrawer().copyFrom(spotDrawer);
            settings.setShowSpots(displaySpotsViewMenuItem.getState());
            JIPipe.getSettings().save();
        }
    }

    private void openDrawingSettings() {
        ParameterPanel.showDialog(getWorkbench(), getViewerPanel(), spotDrawer, new MarkdownDocument("# Spots display settings\n\nPlease use the settings on the left to modify how spots are visualized."), "Spots display settings", ParameterPanel.DEFAULT_DIALOG_FLAGS);
        uploadSliceToCanvas();
    }

    private void importSpotsFromFile() {
        Path path = FileChooserSettings.openFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Import spots", UIUtils.EXTENSION_FILTER_ZIP);
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
        Path path = FileChooserSettings.saveFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Export spots", fileNameExtensionFilters);
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

        // Setup ribbon
        initializeRibbon();
        ribbon.rebuildRibbon();

        // Setup panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setMinimumSize(new Dimension(100, 300));
        mainPanel.setBorder(UIUtils.createControlBorder());

        mainPanel.add(ribbon, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(spotsListControl);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        selectionContentPanelUI.setLayout(new BoxLayout(selectionContentPanelUI, BoxLayout.Y_AXIS));
        mainPanel.add(selectionContentPanelUI, BorderLayout.SOUTH);
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        super.initializeSettingsPanel(formPanel);
        formPanel.addVerticalGlue(mainPanel, null);
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

        private final SpotsManagerPlugin2D spotsManagerPlugin;

        protected SelectionContextPanel(SpotsManagerPlugin2D spotsManagerPlugin) {
            this.spotsManagerPlugin = spotsManagerPlugin;
        }

        public SpotsManagerPlugin2D getSpotsManagerPlugin() {
            return spotsManagerPlugin;
        }

        public JIPipeImageViewer getViewerPanel() {
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
            roiInfoLabel.setIcon(TrackMateExtension.RESOURCES.getIconFromResources("trackmate-spots.png"));
            roiInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(roiInfoLabel);
            add(Box.createHorizontalGlue());
        }

        @Override
        public void selectionUpdated(SpotsCollectionData spotsCollectionData, List<Spot> selectedSpots) {
            if (selectedSpots.isEmpty())
                roiInfoLabel.setText(spotsCollectionData.getNSpots() + " spots");
            else
                roiInfoLabel.setText(selectedSpots.size() + "/" + spotsCollectionData.getNSpots() + " spots");
        }
    }

}
