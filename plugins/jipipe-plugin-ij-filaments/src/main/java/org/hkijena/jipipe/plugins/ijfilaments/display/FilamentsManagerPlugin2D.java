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
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsPlugin;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.settings.ImageViewerUIFilamentDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentsDrawer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin2D;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.jgrapht.alg.connectivity.ConnectivityInspector;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FilamentsManagerPlugin2D extends JIPipeDesktopLegacyImageViewerPlugin2D {
    private final JList<Filaments3DGraphData> filamentsListControl = new JList<>();
    private final JIPipeDesktopSmallToggleButtonRibbonAction displayFilamentsViewMenuItem = new JIPipeDesktopSmallToggleButtonRibbonAction("Display filaments", "Determines whether filaments are displayed", UIUtils.getIconFromResources("actions/eye.png"));
    private final List<SelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private final List<Filaments3DGraphData> filamentsList = new ArrayList<>();
    private FilamentsDrawer filamentsDrawer = new FilamentsDrawer();
    private JPanel mainPanel;

    public FilamentsManagerPlugin2D(JIPipeDesktopLegacyImageViewer viewerPanel) {
        super(viewerPanel);
        initializeDefaults();
        initialize();
        addSelectionContextPanel(new SelectionInfoContextPanel(this));
    }

    private void initializeDefaults() {
        ImageViewerUIFilamentDisplayApplicationSettings settings = ImageViewerUIFilamentDisplayApplicationSettings.getInstance();
        filamentsDrawer = new FilamentsDrawer(settings.getFilamentDrawer());
        displayFilamentsViewMenuItem.setState(settings.isShowFilaments());
    }

    @Override
    public void onOverlayAdded(Object overlay) {
        if (overlay instanceof Filaments3DGraphData) {
            Filaments3DGraphData filaments3DGraphData = (Filaments3DGraphData) overlay;
            ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = filaments3DGraphData.getConnectivityInspector();
            for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
                filamentsList.add(filaments3DGraphData.extractDeepCopy(connectedSet));
            }
            updateFilamentsJList(false);
        }
    }

    @Override
    public void onOverlayRemoved(Object overlay) {
        if (overlay instanceof Filaments3DGraphData) {
            filamentsList.clear();
            updateFilamentsJList(false);
        }
    }

    @Override
    public void buildRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task filamentsTask = ribbon.getOrCreateTask("Filaments");
        {
            JIPipeDesktopRibbon.Band generalBand = filamentsTask.getOrCreateBand("General");
            JIPipeDesktopRibbon.Band modifyBand = filamentsTask.getOrCreateBand("Modify");
            JIPipeDesktopRibbon.Band measureBand = filamentsTask.getOrCreateBand("Measure");

            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Select all", "Selects all filaments", UIUtils.getIconFromResources("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Clear selection", "Deselects all filaments", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Invert selection", "Inverts the current selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new JIPipeDesktopSmallButtonRibbonAction("Delete", "Deletes the selected filaments", UIUtils.getIconFromResources("actions/delete.png"), () -> removeSelectedSpots(false)));

            JIPipeDesktopSmallButtonRibbonAction measureAction = new JIPipeDesktopSmallButtonRibbonAction("Measure", "Measures the filaments and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedFilaments);
            measureBand.add(measureAction);
        }
        {
            JIPipeDesktopRibbon.Band generalBand = filamentsTask.getOrCreateBand("View");
            JIPipeDesktopRibbon.Band visualizationBand = filamentsTask.getOrCreateBand("View");

            generalBand.add(displayFilamentsViewMenuItem);

            visualizationBand.add(new JIPipeDesktopSmallButtonRibbonAction("More settings ...", "Opens a dialog where all available visualization settings can be changed", UIUtils.getIconFromResources("actions/configure.png"), this::openDrawingSettings));
            visualizationBand.add(new JIPipeDesktopSmallButtonRibbonAction("Save settings", "Saves the current settings as default", UIUtils.getIconFromResources("actions/filesave.png"), this::saveDefaults));
        }
        {
            JIPipeDesktopRibbon.Band fileBand = filamentsTask.getOrCreateBand("Import/Export");

            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Import from file", "Imports filaments from a file", UIUtils.getIconFromResources("actions/fileopen.png"), this::importFilamentsFromFile));
            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Export to file", "Exports filaments to a file", UIUtils.getIconFromResources("actions/filesave.png"), this::exportFilamentsToFile));
        }
    }

    @Override
    public void buildDock(JIPipeDesktopDockPanel dockPanel) {

    }

    @Override
    public void buildStatusBar(JToolBar statusBar) {

    }

    private void measureSelectedFilaments() {
        Filaments3DGraphData selected = getSelectedFilamentsOrAll("Measure filaments", "Please select which filaments should be measured");
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
        Filaments3DGraphData result = getSelectedFilamentsOrAll("Export filaments", "Do you want to export all filaments or only the selected ones?");
        if (result != null) {
            exportFilamentsToFile(result);
        }
    }

    public Filaments3DGraphData getSelectedFilamentsOrAll(String title, String message) {
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
                Filaments3DGraphData copy = new Filaments3DGraphData();
                for (Filaments3DGraphData data : filamentsList) {
                    copy.mergeWith(data);
                }
                return copy;
            } else {
                Filaments3DGraphData copy = new Filaments3DGraphData();
                for (Filaments3DGraphData data : filamentsListControl.getSelectedValuesList()) {
                    copy.mergeWith(data);
                }
                return copy;
            }
        }
        {
            Filaments3DGraphData copy = new Filaments3DGraphData();
            for (Filaments3DGraphData data : filamentsList) {
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
            ImageViewerUIFilamentDisplayApplicationSettings settings = ImageViewerUIFilamentDisplayApplicationSettings.getInstance();
            settings.getFilamentDrawer().copyFrom(filamentsDrawer);
            settings.setShowFilaments(displayFilamentsViewMenuItem.getState());
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
        }
    }

    private void openDrawingSettings() {
        JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), getViewerPanel(), filamentsDrawer, new MarkdownText("# Filaments display settings\n\nPlease use the settings on the left to modify how filaments are visualized."), "Filaments display settings", JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
        uploadSliceToCanvas();
    }

    private void importFilamentsFromFile() {
        Path path = JIPipeDesktop.openFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Import filaments", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null && displayFilamentsViewMenuItem.getState()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, path)) {
                Filaments3DGraphData filaments3DGraphData = Filaments3DGraphData.importData(storage, progressInfo);
                ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = filaments3DGraphData.getConnectivityInspector();
                for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
                    filamentsList.add(filaments3DGraphData.extractShallowCopy(connectedSet));
                }
                updateFilamentsJList(false);
                uploadSliceToCanvas();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void exportFilamentsToFile(Filaments3DGraphData filaments) {
        FileNameExtensionFilter[] fileNameExtensionFilters = new FileNameExtensionFilter[]{UIUtils.EXTENSION_FILTER_ZIP};
        Path path = JIPipeDesktop.saveFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export filaments", HTMLText.EMPTY, fileNameExtensionFilters);
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
            ImmutableList<Filaments3DGraphData> copy = ImmutableList.copyOf(filamentsList);
            List<Filaments3DGraphData> selectedValuesList = filamentsListControl.getSelectedValuesList();
            if (selectedValuesList.isEmpty()) {
                for (Filaments3DGraphData data : copy) {
                    filamentsDrawer.drawFilamentsOnGraphics(data, graphics2D, renderArea,
                            getViewerPanel2D().getCanvas().getZoom(), sliceIndex.getZ(), sliceIndex.getC(), sliceIndex.getT(), false);
                }
            } else {
                for (Filaments3DGraphData data : copy) {
                    if (!selectedValuesList.contains(data)) {
                        filamentsDrawer.drawFilamentsOnGraphics(data, graphics2D, renderArea,
                                getViewerPanel2D().getCanvas().getZoom(), sliceIndex.getZ(), sliceIndex.getC(), sliceIndex.getT(), true);
                    }
                }
                for (Filaments3DGraphData data : selectedValuesList) {
                    filamentsDrawer.drawFilamentsOnGraphics(data, graphics2D, renderArea,
                            getViewerPanel2D().getCanvas().getZoom(), sliceIndex.getZ(), sliceIndex.getC(), sliceIndex.getT(), false);
                }
            }
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex, double magnification) {
        if (displayFilamentsViewMenuItem.getState()) {
            for (Filaments3DGraphData data : filamentsList) {
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
    public String getPanelName() {
        return "Filaments";
    }

    @Override
    public JIPipeDesktopDockPanel.PanelLocation getPanelLocation() {
        return JIPipeDesktopDockPanel.PanelLocation.BottomRight;
    }

    @Override
    public void dispose() {
        super.dispose();
        filamentsList.clear();
        filamentsListControl.setModel(new DefaultListModel<>());
    }

    @Override
    public Icon getPanelIcon() {
        return UIUtils.getIcon32FromResources("actions/curve-connector.png");
    }

    private void initialize() {
        // Setup ROI
        FilamentListCellRenderer filamentListCellRenderer = new FilamentListCellRenderer();
        filamentsListControl.setCellRenderer(filamentListCellRenderer);
        filamentsListControl.addListSelectionListener(e -> {
            updateContextPanels();
            uploadSliceToCanvas();
        });

        // Setup panel
        mainPanel = new JPanel(new BorderLayout());
//        mainPanel.setMinimumSize(new Dimension(100, 300));
//        mainPanel.setBorder(UIUtils.createControlBorder());

        JScrollPane scrollPane = new JScrollPane(filamentsListControl);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        selectionContentPanelUI.setLayout(new BoxLayout(selectionContentPanelUI, BoxLayout.Y_AXIS));
        mainPanel.add(selectionContentPanelUI, BorderLayout.SOUTH);

        // Ribbon actions
        displayFilamentsViewMenuItem.addActionListener(this::uploadSliceToCanvas);
    }

    @Override
    public boolean isBuildingCustomPanel() {
        return true;
    }

    @Override
    public JComponent buildCustomPanel() {
        return mainPanel;
    }

    @Override
    public boolean isActive() {
        return getViewerPanel().getOverlays().stream().anyMatch(Filaments3DGraphData.class::isInstance);
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
        DefaultListModel<Filaments3DGraphData> model = new DefaultListModel<>();
        int[] selectedIndices = filamentsListControl.getSelectedIndices();

        for (Filaments3DGraphData filaments3DGraphData : filamentsList) {
            model.addElement(filaments3DGraphData);
        }

        filamentsListControl.setModel(model);
        filamentsListControl.setSelectedIndices(selectedIndices);
        updateContextPanels();
        if (!deferUploadSlice)
            uploadSliceToCanvas();
    }

    private void updateContextPanels() {
        List<Filaments3DGraphData> selectedValuesList = filamentsListControl.getSelectedValuesList();
        for (SelectionContextPanel selectionContextPanel : selectionContextPanels) {
            selectionContextPanel.selectionUpdated(filamentsList, selectedValuesList);
        }
    }

    public JList<Filaments3DGraphData> getFilamentsListControl() {
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

        public JIPipeDesktopLegacyImageViewer getViewerPanel() {
            return filamentsManagerPlugin2D.getViewerPanel();
        }

        public abstract void selectionUpdated(List<Filaments3DGraphData> filaments, List<Filaments3DGraphData> selectedFilaments);
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
        public void selectionUpdated(List<Filaments3DGraphData> filaments, List<Filaments3DGraphData> selectedFilaments) {
            if (selectedFilaments.isEmpty())
                roiInfoLabel.setText(filaments.size() + " filaments");
            else
                roiInfoLabel.setText(selectedFilaments.size() + "/" + filaments.size() + " filaments");
        }
    }

}
