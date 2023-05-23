package org.hkijena.jipipe.extensions.ijfilaments.display;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import customnode.CustomMesh;
import customnode.CustomMultiMesh;
import customnode.CustomTriangleMesh;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij3d.Content;
import ij3d.ContentCreator;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.imageviewer.*;
import org.hkijena.jipipe.extensions.ij3d.utils.Roi3DDrawer;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsExtension;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.settings.ImageViewerUIFilamentDisplaySettings;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentsDrawer;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel3D;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin3D;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.Image3DRenderType;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.SmallToggleButtonAction;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.scijava.vecmath.Color3f;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class FilamentsManagerPlugin3D extends JIPipeImageViewerPlugin3D implements JIPipeRunnable.FinishedEventListener {
    private final JList<Filaments3DData> filamentsListControl = new JList<>();
    private final SmallToggleButtonAction displayROIViewMenuItem = new SmallToggleButtonAction("Display filaments", "Determines whether filaments are displayed", UIUtils.getIconFromResources("actions/eye.png"));
    private final SmallToggleButtonAction displayROIAsVolumeItem = new SmallToggleButtonAction("Render as volume", "If enabled, render filaments as volume", UIUtils.getIconFromResources("actions/antivignetting.png"));
    private final List<SelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private final Ribbon ribbon = new Ribbon(3);
    private final Timer updateContentLaterTimer;
    private List<Filaments3DData> filamentsList = new ArrayList<>();
    private boolean filterListOnlySelected = false;
    private JPanel mainPanel;
    private Filament3DToContentConverterRun currentRendererRun;
    private Content currentRendereredContent;

    private FilamentsDrawer filamentsDrawer = new FilamentsDrawer();

    public FilamentsManagerPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
        loadDefaults();
        initialize();
        addSelectionContextPanel(new SelectionInfoContextPanel(this));

        this.updateContentLaterTimer = new Timer(1000, e -> rebuildRoiContentNow());
        updateContentLaterTimer.setRepeats(false);

        getViewerPanel3D().getViewerRunnerQueue().getFinishedEventEmitter().subscribeWeak(this);
    }

    private void loadDefaults() {
        ImageViewerUIFilamentDisplaySettings settings = ImageViewerUIFilamentDisplaySettings.getInstance();
        filamentsDrawer = new FilamentsDrawer(settings.getFilamentDrawer());
        displayROIViewMenuItem.setState(settings.isShowFilaments());
    }

    @Override
    public void onImageChanged() {
        cancelScheduledTasks();
        updateListModel(Collections.emptySet());

        // Load ROI3D content
        if (getCurrentImage() != null) {
            ROI3DListData data = new ROI3DListData();
            for (ROI3DListData listData : getCurrentImage().extractOverlaysOfType(ROI3DListData.class)) {
                data.addAll(listData);
            }
            if (!data.isEmpty()) {
                getViewerPanel().addOverlay(data);
            }
        }
    }

    @Override
    public void onOverlayAdded(Object overlay) {
        if (overlay instanceof Filaments3DData) {
            importFilaments((Filaments3DData) overlay);
        }
    }

    @Override
    public void onOverlayRemoved(Object overlay) {
        // Currently not possible (creates copies of the ROI)
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof Filament3DToContentConverterRun) {
            Filament3DToContentConverterRun run = (Filament3DToContentConverterRun) event.getRun();
            if (currentRendereredContent != null) {
                getViewerPanel3D().getUniverse().removeContent(currentRendereredContent.getName());
            }
            currentRendereredContent = run.getRenderedContent();
            getViewerPanel3D().getUniverse().addContent(run.getRenderedContent());
            updateContentVisibility();
        }
    }

    private void importFilaments(Filaments3DData overlay) {
        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = overlay.getConnectivityInspector();
        for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
            filamentsList.add(overlay.extractDeepCopy(connectedSet));
        }
        rebuildRoiContentNow();
        updateListModel(Collections.emptyList());
    }

    @Override
    public void dispose() {
        updateContentLaterTimer.stop();
        cancelScheduledTasks();
        currentRendereredContent = null;
        filamentsList.clear();
        filamentsListControl.setModel(new DefaultListModel<>());
    }

    @Override
    public void onOverlaysCleared() {
        cancelScheduledTasks();
        clearFilaments();
    }

    private void cancelScheduledTasks() {
        getViewerPanel3D().getViewerRunnerQueue().cancelIf(run -> run instanceof Filament3DToContentConverterRun);
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        if (getCurrentImagePlus() == null)
            return;
        formPanel.addVerticalGlue(mainPanel, null);
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
            }
            else {
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

    private void openDrawingSettings() {
        ParameterPanel.showDialog(getWorkbench(), getViewerPanel(), filamentsDrawer, new MarkdownDocument("# Filaments display settings\n\nPlease use the settings on the left to modify how filaments are visualized."), "Filaments display settings", ParameterPanel.DEFAULT_DIALOG_FLAGS);
        rebuildRoiContentLater();
    }

    private void initializeRibbon() {

        // Register necessary actions
        displayROIViewMenuItem.addActionListener(this::updateContentVisibility);
        displayROIAsVolumeItem.addActionListener(this::rebuildRoiContentLater);

        // View menu for general display
        {
            Ribbon.Task viewTask = ribbon.addTask("View");
            Ribbon.Band renderingBand = viewTask.addBand("Rendering");

            renderingBand.add(displayROIViewMenuItem);

            renderingBand.add(new SmallButtonAction("More settings ...", "Opens more rendering settings", UIUtils.getIconFromResources("actions/configure.png"), this::openDrawingSettings));
            renderingBand.add(displayROIAsVolumeItem);
            renderingBand.add(new SmallButtonAction("Save settings", "Saves the current settings as default", UIUtils.getIconFromResources("actions/save.png"), this::saveDefaults));
            renderingBand.add(new SmallButtonAction("Rebuild", "Re-renders the ROI", UIUtils.getIconFromResources("actions/run-build.png"), this::rebuildRoiContentNow));
        }

        // Filter task
        {
            Ribbon.Task filterTask = ribbon.addTask("Filter");
            Ribbon.Band listBand = filterTask.addBand("List");

            listBand.add(new SmallToggleButtonAction("Only selection", "Show only ROI that are selected", UIUtils.getIconFromResources("actions/edit-select-all.png"), filterListOnlySelected, (toggle) -> {
                filterListOnlySelected = toggle.isSelected();
                updateListModel();
            }));
        }

        // Select/Edit task
        {
            Ribbon.Task selectionTask = ribbon.addTask("Selection");
            Ribbon.Band generalBand = selectionTask.addBand("General");
            Ribbon.Band modifyBand = selectionTask.addBand("Modify");
            Ribbon.Band measureBand = selectionTask.addBand("Measure");

            generalBand.add(new SmallButtonAction("Select all", "Selects all filaments", UIUtils.getIconFromResources("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new SmallButtonAction("Clear selection", "Deselects all filaments", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new SmallButtonAction("Invert selection", "Inverts the current selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new SmallButtonAction("Delete", "Deletes the selected filaments", UIUtils.getIconFromResources("actions/delete.png"), this::removeSelectedFilaments));

            SmallButtonAction measureAction = new SmallButtonAction("Measure", "Measures the ROI and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedFilaments);
            measureBand.add(measureAction);
            measureBand.add(new SmallButtonAction("Settings ...", "Opens the measurement settings", UIUtils.getIconFromResources("actions/configure.png"), this::openMeasurementSettings));

        }

        // Import/Export task
        {
            Ribbon.Task importExportTask = ribbon.addTask("Import/Export");
            Ribbon.Band fileBand = importExportTask.addBand("File");

            fileBand.add(new SmallButtonAction("Import from file", "Imports filaments from a *.roi or *.zip file", UIUtils.getIconFromResources("actions/fileopen.png"), this::importFilamentsFromFile));
            fileBand.add(new SmallButtonAction("Export to file", "Exports filaments to a *.zip file", UIUtils.getIconFromResources("actions/save.png"), this::exportFilamentsToFile));
        }
    }

    private void updateContentVisibility() {
        if (currentRendereredContent != null) {
            currentRendereredContent.setVisible(displayROIViewMenuItem.isSelected());
        }
    }

    private void openMeasurementSettings() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getViewerPanel()));
        dialog.setTitle("Measurement settings");
        dialog.setContentPane(new ParameterPanel(new JIPipeDummyWorkbench(), Measurement3DSettings.INSTANCE, null, FormPanel.WITH_SCROLLING));
        UIUtils.addEscapeListener(dialog);
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(getViewerPanel());
        dialog.revalidate();
        dialog.repaint();
        dialog.setVisible(true);
    }

    private void measureSelectedFilaments() {
        Filaments3DData selected = getSelectedFilamentsOrAll("Measure filaments", "Please select which filaments should be measured");
        ResultsTableData measurements = selected.measureComponents();
        TableEditor.openWindow(getViewerPanel().getWorkbench(), measurements, "Measurements");
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

    private void saveDefaults() {
        if (JOptionPane.showConfirmDialog(getViewerPanel(),
                "Do you want to save the filament display settings as default?",
                "Save settings as default",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ImageViewerUIFilamentDisplaySettings settings = ImageViewerUIFilamentDisplaySettings.getInstance();
            settings.getFilamentDrawer().copyFrom(filamentsDrawer);
            settings.setShowFilaments(displayROIViewMenuItem.getState());
            JIPipe.getSettings().save();
        }
    }

    private void importFilamentsFromFile() {
        Path path = FileChooserSettings.openFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Import filaments", UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null && displayROIViewMenuItem.getState()) {
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            try (JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, path)) {
                Filaments3DData filaments3DData = Filaments3DData.importData(storage, progressInfo);
                ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = filaments3DData.getConnectivityInspector();
                for (Set<FilamentVertex> connectedSet : connectivityInspector.connectedSets()) {
                    filamentsList.add(filaments3DData.extractShallowCopy(connectedSet));
                }
                updateListModel();
                rebuildRoiContentLater();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void exportFilamentsToFile() {
        Filaments3DData result = getSelectedFilamentsOrAll("Export filaments", "Do you want to export all filaments or only the selected ones?");
        if (result != null) {
            exportFilamentsToFile(result);
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
    public String getCategory() {
        return "Filaments";
    }
    @Override
    public Icon getCategoryIcon() {
        return FilamentsExtension.RESOURCES.getIconFromResources("data-type-filaments.png");
    }

    private void initialize() {
        // Setup ROI list
        filamentsListControl.setCellRenderer(new FilamentListCellRenderer());
        filamentsListControl.addListSelectionListener(e -> {
            updateContextPanels();
            rebuildRoiContentLater();
        });

        // Setup ribbon
        initializeRibbon();
        ribbon.rebuildRibbon();

        // Setup panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setMinimumSize(new Dimension(100, 300));
        mainPanel.setBorder(BorderFactory.createEtchedBorder());

        mainPanel.add(ribbon, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(filamentsListControl);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        selectionContentPanelUI.setLayout(new BoxLayout(selectionContentPanelUI, BoxLayout.Y_AXIS));
        mainPanel.add(selectionContentPanelUI, BorderLayout.SOUTH);
    }

    private void rebuildRoiContentLater() {
        updateContentLaterTimer.restart();
    }

    private void rebuildRoiContentNow() {
        if (currentRendererRun != null) {
            getViewerPanel3D().getViewerRunnerQueue().cancel(currentRendererRun);
        }
        if (getViewerPanel3D().getCurrentImageContents() == null) {
            // Wait for ImageContentReady
            return;
        }
        currentRendererRun = new Filament3DToContentConverterRun(new ArrayList<>(filamentsList),
                filamentsDrawer,
                getCurrentImagePlus(),
                displayROIAsVolumeItem.isSelected(), getViewerPanel3D().getImage3DRendererSettings().getResamplingFactor(getCurrentImage().getImage()));
        getViewerPanel3D().getViewerRunnerQueue().enqueue(currentRendererRun);
    }

    @Override
    public void onImageContentReady(List<Content> content) {
        rebuildRoiContentNow();
    }

    public void removeSelectedFilaments() {
        ImmutableList<Filaments3DData> deleted = ImmutableList.copyOf(filamentsListControl.getSelectedValuesList());
        filamentsList.removeAll(filamentsListControl.getSelectedValuesList());
        updateListModel(Collections.emptySet());
        rebuildRoiContentNow();
    }

    public void clearFilaments() {
        filamentsList.clear();
        updateListModel(Collections.emptySet());
        if (currentRendereredContent != null && getViewerPanel3D().getUniverse() != null) {
            getViewerPanel3D().getUniverse().removeContent(currentRendereredContent.getName());
        }
    }

    public List<Filaments3DData> getFilamentsList() {
        return filamentsList;
    }

    public void setFilamentsList(List<Filaments3DData> filamentsList) {
        this.filamentsList = filamentsList;
        updateListModel(Collections.emptySet());
    }

    public boolean isFilterListOnlySelected() {
        return filterListOnlySelected;
    }

    public void setFilterListOnlySelected(boolean filterListOnlySelected) {
        this.filterListOnlySelected = filterListOnlySelected;
        updateListModel();
    }

    public void updateListModel() {
        updateListModel(Collections.emptySet());
    }

    public void updateListModel(Collection<Filaments3DData> excludeFromFilter) {
        DefaultListModel<Filaments3DData> model = new DefaultListModel<>();
        List<Filaments3DData> selectedValuesList = filamentsListControl.getSelectedValuesList();
        for (Filaments3DData roi : filamentsList) {
            boolean excluded = excludeFromFilter.contains(roi);
            if (!excluded && !selectedValuesList.isEmpty() && (filterListOnlySelected && !selectedValuesList.contains(roi)))
                continue;
            model.addElement(roi);
        }
        filamentsListControl.setModel(model);
        setSelectedFilaments(selectedValuesList, false);
        updateContextPanels();
    }

    private void updateContextPanels() {
        List<Filaments3DData> selectedValuesList = filamentsListControl.getSelectedValuesList();
        for (SelectionContextPanel selectionContextPanel : selectionContextPanels) {
            selectionContextPanel.selectionUpdated(filamentsList, selectedValuesList);
        }
    }

    private void selectAll() {
        filamentsListControl.setSelectionInterval(0, filamentsListControl.getModel().getSize() - 1);
    }

    public void setSelectedFilaments(List<Filaments3DData> select, boolean force) {
        TIntList indices = new TIntArrayList();
        DefaultListModel<Filaments3DData> model = (DefaultListModel<Filaments3DData>) filamentsListControl.getModel();

        if (force) {
            boolean rebuild = false;
            for (Filaments3DData roi : select) {
                if (filamentsList.contains(roi) && !model.contains(roi)) {
                    rebuild = true;
                    break;
                }
            }
            if (rebuild) {
                filamentsListControl.clearSelection();
                updateListModel(select);
                model = (DefaultListModel<Filaments3DData>) filamentsListControl.getModel();
            }
        }

        for (Filaments3DData roi : select) {
            int i = model.indexOf(roi);
            if (i >= 0) {
                indices.add(i);
            }
        }
        filamentsListControl.setSelectedIndices(indices.toArray());
    }

    public abstract static class SelectionContextPanel extends JPanel {

        private final FilamentsManagerPlugin3D filamentsManagerPlugin3D;

        protected SelectionContextPanel(FilamentsManagerPlugin3D filamentsManagerPlugin3D) {
            this.filamentsManagerPlugin3D = filamentsManagerPlugin3D;
        }

        public FilamentsManagerPlugin3D getFilamentsManagerPlugin3D() {
            return filamentsManagerPlugin3D;
        }

        public JIPipeImageViewer getViewerPanel() {
            return filamentsManagerPlugin3D.getViewerPanel();
        }

        public abstract void selectionUpdated(List<Filaments3DData> filaments, List<Filaments3DData> selectedFilaments);
    }

    public static class SelectionInfoContextPanel extends FilamentsManagerPlugin3D.SelectionContextPanel {

        private final JLabel roiInfoLabel;

        public SelectionInfoContextPanel(FilamentsManagerPlugin3D parent) {
            super(parent);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            this.roiInfoLabel = new JLabel();
            roiInfoLabel.setIcon(FilamentsExtension.RESOURCES.getIconFromResources("data-type-filaments.png"));
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

    private static class Filament3DToContentConverterRun extends AbstractJIPipeRunnable {

        private final List<Filaments3DData> filaments3DData;
        private final FilamentsDrawer filamentsDrawer;
        private final ImagePlus referenceImage;
        private final int resolutionFactor;
        private final boolean renderAsVolume;
        private Content renderedContent;

        public Filament3DToContentConverterRun(List<Filaments3DData> filaments3DData, FilamentsDrawer filamentsDrawer, ImagePlus referenceImage, boolean renderAsVolume, int resolutionFactor) {
            this.filaments3DData = filaments3DData;
            this.filamentsDrawer = filamentsDrawer;
            this.referenceImage = referenceImage;
            this.renderAsVolume = renderAsVolume;
            this.resolutionFactor = resolutionFactor;
        }

        @Override
        public String getTaskLabel() {
            return "Preprocess 3D filaments";
        }

        public Content getRenderedContent() {
            return renderedContent;
        }

        @Override
        public void run() {

            Filaments3DData merged = new Filaments3DData();
            for (Filaments3DData data : filaments3DData) {
                merged.mergeWith(data);
            }

            ImagePlus render = IJ.createHyperStack("render", referenceImage.getWidth(), referenceImage.getHeight(),
                    referenceImage.getNChannels(), referenceImage.getNSlices(), referenceImage.getNFrames(), 24);

            if(getProgressInfo().isCancelled())
                return;

            ImageJUtils.forEachIndexedZCTSlice(render, (ip, index) -> {
                filamentsDrawer.drawFilamentsOnProcessor(merged, (ColorProcessor) ip, index.getZ(), index.getC(), index.getT());
            }, getProgressInfo());

            renderedContent = ContentCreator.createContent("Filaments-" + UUID.randomUUID(),
                    render,
                    renderAsVolume ? Image3DRenderType.Volume.getNativeValue() : Image3DRenderType.Surface.getNativeValue(),
                    resolutionFactor,
                    0,
                    new Color3f(1, 1, 1),
                    0,
                    new boolean[]{true, true, true});
        }
    }
}
