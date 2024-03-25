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

package org.hkijena.jipipe.plugins.ijfilaments.display;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsPlugin;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.settings.ImageViewerUIFilamentDisplaySettings;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentsDrawer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewerPlugin2D;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;
import org.jgrapht.alg.connectivity.ConnectivityInspector;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class FilamentsManagerPlugin2D extends JIPipeImageViewerPlugin2D {
    private final JList<Filaments3DData> filamentsListControl = new JList<>();
    private final JIPipeDesktopSmallToggleButtonRibbonAction displayFilamentsViewMenuItem = new JIPipeDesktopSmallToggleButtonRibbonAction("Display filaments", "Determines whether filaments are displayed", UIUtils.getIconFromResources("actions/eye.png"));
    private final List<SelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private final JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon(3);
    private List<Filaments3DData> filamentsList = new ArrayList<>();
    private FilamentsDrawer filamentsDrawer = new FilamentsDrawer();
    private FilamentListCellRenderer filamentListCellRenderer;
    private JPanel mainPanel;

    public FilamentsManagerPlugin2D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
        initializeDefaults();
        initialize();
        addSelectionContextPanel(new SelectionInfoContextPanel(this));
    }

    private void initializeDefaults() {
        ImageViewerUIFilamentDisplaySettings settings = ImageViewerUIFilamentDisplaySettings.getInstance();
        filamentsDrawer = new FilamentsDrawer(settings.getFilamentDrawer());
        displayFilamentsViewMenuItem.setState(settings.isShowFilaments());
    }

    @Override
    public void onOverlayAdded(Object overlay) {
        if (overlay instanceof Filaments3DData) {
            Filaments3DData filaments3DData = (Filaments3DData) overlay;
            ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = filaments3DData.getConnectivityInspector();
            for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
                filamentsList.add(filaments3DData.extractDeepCopy(connectedSet));
            }
            updateFilamentsJList(false);
        }
    }

    @Override
    public void onOverlayRemoved(Object overlay) {
        if (overlay instanceof Filaments3DData) {
            filamentsList.clear();
            updateFilamentsJList(false);
        }
    }

    private void initializeRibbon() {
        {
            displayFilamentsViewMenuItem.addActionListener(this::uploadSliceToCanvas);

            JIPipeDesktopRibbon.Task viewTask = ribbon.getOrCreateTask("View");
            JIPipeDesktopRibbon.Band generalBand = viewTask.addBand("General");
            JIPipeDesktopRibbon.Band visualizationBand = viewTask.addBand("Visualization");

            generalBand.add(displayFilamentsViewMenuItem);

            visualizationBand.add(new JIPipeDesktopSmallButtonRibbonAction("More settings ...", "Opens a dialog where all available visualization settings can be changed", UIUtils.getIconFromResources("actions/configure.png"), this::openDrawingSettings));
            visualizationBand.add(new JIPipeDesktopSmallButtonRibbonAction("Save settings", "Saves the current settings as default", UIUtils.getIconFromResources("actions/save.png"), this::saveDefaults));
        }
        {
            JIPipeDesktopRibbon.Task selectionTask = ribbon.addTask("Selection");
            JIPipeDesktopRibbon.Band generalBand = selectionTask.addBand("General");
            JIPipeDesktopRibbon.Band modifyBand = selectionTask.addBand("Modify");
            JIPipeDesktopRibbon.Band measureBand = selectionTask.addBand("Measure");

            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Select all", "Selects all filaments", UIUtils.getIconFromResources("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Clear selection", "Deselects all filaments", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Invert selection", "Inverts the current selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new JIPipeDesktopSmallButtonRibbonAction("Delete", "Deletes the selected filaments", UIUtils.getIconFromResources("actions/delete.png"), () -> removeSelectedSpots(false)));

            JIPipeDesktopSmallButtonRibbonAction measureAction = new JIPipeDesktopSmallButtonRibbonAction("Measure", "Measures the filaments and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedFilaments);
            measureBand.add(measureAction);
        }
        {
            JIPipeDesktopRibbon.Task importExportTask = ribbon.addTask("Import/Export");
            JIPipeDesktopRibbon.Band fileBand = importExportTask.addBand("File");

            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Import from file", "Imports filaments from a file", UIUtils.getIconFromResources("actions/fileopen.png"), this::importFilamentsFromFile));
            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Export to file", "Exports filaments to a file", UIUtils.getIconFromResources("actions/save.png"), this::exportFilamentsToFile));
        }
    }

    private void measureSelectedFilaments() {
        Filaments3DData selected = getSelectedFilamentsOrAll("Measure filaments", "Please select which filaments should be measured");
        ResultsTableData measurements = selected.measureComponents();
        JIPipeDesktopTableEditor.openWindow(getViewerPanel().getDesktopWorkbench(), measurements, "Measurements");
    }

    private void selectAll() {
        filamentsListControl.setSelectionInterval(0, filamentsListControl.getModel().getSize() - 1);
    }

    private void selectNone() {
        filamentsListControl.clearSelection();
    }

    private void invertSelection() {
        Set<Integer> selectedIndices = Arrays.stream(filamentsListControl.getSelectedIndices()).boxed().collect(Collectors.toSet());
        filamentsListControl.clearSelection();
        Set<Integer> newSelectedIndices = new HashSet<>();
        for (int i = 0; i < filamentsListControl.getModel().getSize(); i++) {
            if (!selectedIndices.contains(i))
                newSelectedIndices.add(i);
        }
        filamentsListControl.setSelectedIndices(Ints.toArray(newSelectedIndices));
    }

    private void exportFilamentsToFile() {
        Filaments3DData result = getSelectedFilamentsOrAll("Export filaments", "Do you want to export all filaments or only the selected ones?");
        if (result != null) {
            exportFilamentsToFile(result);
        }
    }

    public Filaments3DData getSelectedFilamentsOrAll(String title, String message) {
        if (!filamentsListControl.getSelectedValuesList().isEmpty()) {
            int result = JOptionPane.showOptionDialog(getViewerPanel(),
                    message,
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"All filaments (" + filamentsList.size() + ")", "Selected filaments (" + filamentsListControl.getSelectedValuesList().size() + ")", "Cancel"},
                    "All filaments (" + filamentsList.size() + ")");
            if (result == JOptionPane.CANCEL_OPTION)
                return null;
            else if (result == JOptionPane.YES_OPTION) {
                Filaments3DData copy = new Filaments3DData();
                for (Filaments3DData data : filamentsList) {
                    copy.mergeWith(data);
                }
                return copy;
            } else {
                Filaments3DData copy = new Filaments3DData();
                for (Filaments3DData data : filamentsListControl.getSelectedValuesList()) {
                    copy.mergeWith(data);
                }
                return copy;
            }
        }
        {
            Filaments3DData copy = new Filaments3DData();
            for (Filaments3DData data : filamentsList) {
                copy.mergeWith(data);
            }
            return copy;
        }
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

    public FilamentsDrawer getFilamentsDrawer() {
        return filamentsDrawer;
    }

    private void saveDefaults() {
        if (JOptionPane.showConfirmDialog(getViewerPanel(),
                "Do you want to save the filament display settings as default?",
                "Save settings as default",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ImageViewerUIFilamentDisplaySettings settings = ImageViewerUIFilamentDisplaySettings.getInstance();
            settings.getFilamentDrawer().copyFrom(filamentsDrawer);
            settings.setShowFilaments(displayFilamentsViewMenuItem.getState());
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
        }
    }

    private void openDrawingSettings() {
        JIPipeDesktopParameterPanel.showDialog(getDesktopWorkbench(), getViewerPanel(), filamentsDrawer, new MarkdownText("# Filaments display settings\n\nPlease use the settings on the left to modify how filaments are visualized."), "Filaments display settings", JIPipeDesktopParameterPanel.DEFAULT_DIALOG_FLAGS);
        uploadSliceToCanvas();
    }

    private void importFilamentsFromFile() {
        Path path = FileChooserSettings.openFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Import filaments", UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null && displayFilamentsViewMenuItem.getState()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, path)) {
                Filaments3DData filaments3DData = Filaments3DData.importData(storage, progressInfo);
                ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = filaments3DData.getConnectivityInspector();
                for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
                    filamentsList.add(filaments3DData.extractShallowCopy(connectedSet));
                }
                updateFilamentsJList(false);
                uploadSliceToCanvas();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void exportFilamentsToFile(Filaments3DData filaments) {
        FileNameExtensionFilter[] fileNameExtensionFilters = new FileNameExtensionFilter[]{UIUtils.EXTENSION_FILTER_ZIP};
        Path path = FileChooserSettings.saveFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Export filaments", fileNameExtensionFilters);
        if (path != null) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(progressInfo, path)) {
                filaments.exportData(storage, "Filaments", false, progressInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (displayFilamentsViewMenuItem.getState()) {
            ImmutableList<Filaments3DData> copy = ImmutableList.copyOf(filamentsList);
            List<Filaments3DData> selectedValuesList = filamentsListControl.getSelectedValuesList();
            if (selectedValuesList.isEmpty()) {
                for (Filaments3DData data : copy) {
                    filamentsDrawer.drawFilamentsOnGraphics(data, graphics2D, renderArea,
                            getViewerPanel2D().getCanvas().getZoom(), sliceIndex.getZ(), sliceIndex.getC(), sliceIndex.getT(), false);
                }
            } else {
                for (Filaments3DData data : copy) {
                    if (!selectedValuesList.contains(data)) {
                        filamentsDrawer.drawFilamentsOnGraphics(data, graphics2D, renderArea,
                                getViewerPanel2D().getCanvas().getZoom(), sliceIndex.getZ(), sliceIndex.getC(), sliceIndex.getT(), true);
                    }
                }
                for (Filaments3DData data : selectedValuesList) {
                    filamentsDrawer.drawFilamentsOnGraphics(data, graphics2D, renderArea,
                            getViewerPanel2D().getCanvas().getZoom(), sliceIndex.getZ(), sliceIndex.getC(), sliceIndex.getT(), false);
                }
            }
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex, double magnification) {
        if (displayFilamentsViewMenuItem.getState()) {
            for (Filaments3DData data : filamentsList) {
                Graphics2D graphics2D = image.createGraphics();
                filamentsDrawer.drawFilamentsOnGraphics(data, graphics2D, new Rectangle(0, 0, (int) (image.getWidth() * magnification), (int) (image.getHeight() * magnification)),
                        magnification, sliceIndex.getZ(), sliceIndex.getC(), sliceIndex.getT(), false);
            }
        }
    }

    @Override
    public void onSliceChanged(boolean deferUploadSlice) {
        updateFilamentsJList(deferUploadSlice);
    }

    @Override
    public String getCategory() {
        return "Filaments";
    }

    @Override
    public void dispose() {
        super.dispose();
        filamentsList.clear();
        filamentsListControl.setModel(new DefaultListModel<>());
    }

    @Override
    public Icon getCategoryIcon() {
        return FilamentsPlugin.RESOURCES.getIconFromResources("data-type-filaments.png");
    }

    private void initialize() {
        // Setup ROI
        filamentListCellRenderer = new FilamentListCellRenderer();
        filamentsListControl.setCellRenderer(filamentListCellRenderer);
        filamentsListControl.addListSelectionListener(e -> {
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

        JScrollPane scrollPane = new JScrollPane(filamentsListControl);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        selectionContentPanelUI.setLayout(new BoxLayout(selectionContentPanelUI, BoxLayout.Y_AXIS));
        mainPanel.add(selectionContentPanelUI, BorderLayout.SOUTH);
    }

    @Override
    public void initializeSettingsPanel(JIPipeDesktopFormPanel formPanel) {
        super.initializeSettingsPanel(formPanel);
        formPanel.addVerticalGlue(mainPanel, null);
    }

    public void removeSelectedSpots(boolean deferUploadSlice) {
        if (filamentsListControl.getSelectedValuesList().isEmpty())
            return;
        if (JOptionPane.showConfirmDialog(getViewerPanel(), "Do you really want to remove " + filamentsListControl.getSelectedValuesList().size() + "filaments?", "Delete filaments", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            filamentsList.removeAll(filamentsListControl.getSelectedValuesList());
            updateFilamentsJList(deferUploadSlice);
        }
    }

    private void updateFilamentsJList(boolean deferUploadSlice) {
        DefaultListModel<Filaments3DData> model = new DefaultListModel<>();
        int[] selectedIndices = filamentsListControl.getSelectedIndices();

        for (Filaments3DData filaments3DData : filamentsList) {
            model.addElement(filaments3DData);
        }

        filamentsListControl.setModel(model);
        filamentsListControl.setSelectedIndices(selectedIndices);
        updateContextPanels();
        if (!deferUploadSlice)
            uploadSliceToCanvas();
    }

    private void updateContextPanels() {
        List<Filaments3DData> selectedValuesList = filamentsListControl.getSelectedValuesList();
        for (SelectionContextPanel selectionContextPanel : selectionContextPanels) {
            selectionContextPanel.selectionUpdated(filamentsList, selectedValuesList);
        }
    }

    public JList<Filaments3DData> getFilamentsListControl() {
        return filamentsListControl;
    }

    public abstract static class SelectionContextPanel extends JPanel {

        private final FilamentsManagerPlugin2D filamentsManagerPlugin2D;

        protected SelectionContextPanel(FilamentsManagerPlugin2D filamentsManagerPlugin2D) {
            this.filamentsManagerPlugin2D = filamentsManagerPlugin2D;
        }

        public FilamentsManagerPlugin2D getFilamentsManagerPlugin2D() {
            return filamentsManagerPlugin2D;
        }

        public JIPipeImageViewer getViewerPanel() {
            return filamentsManagerPlugin2D.getViewerPanel();
        }

        public abstract void selectionUpdated(List<Filaments3DData> filaments, List<Filaments3DData> selectedFilaments);
    }

    public static class SelectionInfoContextPanel extends SelectionContextPanel {

        private final JLabel roiInfoLabel;

        public SelectionInfoContextPanel(FilamentsManagerPlugin2D parent) {
            super(parent);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            this.roiInfoLabel = new JLabel();
            roiInfoLabel.setIcon(FilamentsPlugin.RESOURCES.getIconFromResources("data-type-filaments.png"));
            roiInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(roiInfoLabel);
            add(Box.createHorizontalGlue());
        }

        @Override
        public void selectionUpdated(List<Filaments3DData> filaments, List<Filaments3DData> selectedFilaments) {
            if (selectedFilaments.isEmpty())
                roiInfoLabel.setText(filaments.size() + " filaments");
            else
                roiInfoLabel.setText(selectedFilaments.size() + "/" + filaments.size() + " filaments");
        }
    }

}
