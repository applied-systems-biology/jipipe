package org.hkijena.jipipe.extensions.ij3d.imageviewer;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import customnode.CustomMesh;
import customnode.CustomMultiMesh;
import customnode.CustomTriangleMesh;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij3d.Content;
import ij3d.ContentCreator;
import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.Roi3DDrawer;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin3D;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.Image3DRenderType;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.ribbon.LargeToggleButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.SmallToggleButtonAction;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.vecmath.Color3f;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ROIManagerPlugin3D extends JIPipeImageViewerPlugin3D implements JIPipeRunnable.FinishedEventListener {
    private final JList<ROI3D> roiListControl = new JList<>();
    private final LargeToggleButtonAction displayROIViewMenuItem = new LargeToggleButtonAction("Display ROI", "Determines whether ROI are displayed", UIUtils.getIcon32FromResources("data-types/roi.png"));
    private final SmallToggleButtonAction displayROIAsVolumeItem = new SmallToggleButtonAction("Render as volume", "If enabled, render ROI as volume", UIUtils.getIconFromResources("actions/antivignetting.png"));
    private final List<ROIManagerPlugin3DSelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private final Ribbon ribbon = new Ribbon(3);
    private final Timer updateContentLaterTimer;
    private final ROIListData scheduledRoi2D = new ROIListData();
    private ROI3DListData rois = new ROI3DListData();
    private boolean filterListOnlySelected = false;
    private JPanel mainPanel;
    private ROI3DToContentConverterRun currentRendererRun;
    private Content currentRendereredContent;

    public ROIManagerPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
        loadDefaults();
        initialize();
        addSelectionContextPanel(new ROIManagerPlugin3DInfoContextPanel(this));

        this.updateContentLaterTimer = new Timer(1000, e -> rebuildRoiContentNow());
        updateContentLaterTimer.setRepeats(false);

        getViewerPanel3D().getViewerRunnerQueue().getFinishedEventEmitter().subscribeWeak(this);
    }

    private void loadDefaults() {
        ImageViewerUIROI3DDisplaySettings settings = ImageViewerUIROI3DDisplaySettings.getInstance();
        displayROIViewMenuItem.setSelected(settings.isShowROI());
        displayROIAsVolumeItem.setSelected(settings.isRenderROIAsVolume());
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
        if (overlay instanceof ROIListData) {
            importROIs((ROIListData) overlay);
        } else if (overlay instanceof ROI3DListData) {
            importROIs((ROI3DListData) overlay);
        }
    }

    @Override
    public void onOverlayRemoved(Object overlay) {
        // Currently not possible (creates copies of the ROI)
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof ROI2DTo3DConverterRun) {
            ROI3DListData roi3DListData = new ROI3DListData();
            roi3DListData.addAll(((ROI2DTo3DConverterRun) event.getRun()).converted);
            importROIs(roi3DListData);
        } else if (event.getRun() instanceof ROI3DToContentConverterRun) {
            ROI3DToContentConverterRun run = (ROI3DToContentConverterRun) event.getRun();
            if (currentRendereredContent != null) {
                getViewerPanel3D().getUniverse().removeContent(currentRendereredContent.getName());
            }
            currentRendereredContent = run.getRenderedContent();
            getViewerPanel3D().getUniverse().addContent(run.getRenderedContent());
            updateContentVisibility();
        }
    }

    private void importROIs(ROI3DListData overlay) {
        for (ROI3D roi3D : overlay) {
            ROI3D copy = new ROI3D();
            copy.setObject3D(roi3D.getObject3D());
            copy.copyMetadata(roi3D);
            rois.add(copy);
        }
        rebuildRoiContentNow();
        updateListModel(Collections.emptyList());
    }

    @Override
    public void dispose() {
        updateContentLaterTimer.stop();
        cancelScheduledTasks();
        currentRendereredContent = null;
        rois.clear();
        roiListControl.setModel(new DefaultListModel<>());
    }

    @Override
    public void onOverlaysCleared() {
        cancelScheduledTasks();
        clearROIs();
    }

    private void cancelScheduledTasks() {
        scheduledRoi2D.clear();
        getViewerPanel3D().getViewerRunnerQueue().cancelIf(run -> run instanceof ROI2DTo3DConverterRun || run instanceof ROI3DToContentConverterRun);
        scheduledRoi2D.clear();
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        if (getCurrentImagePlus() == null)
            return;
        formPanel.addVerticalGlue(mainPanel, null);
    }

    public ROI3DListData getSelectedROIOrAll(String title, String message) {
        if (rois.isEmpty()) {
            JOptionPane.showMessageDialog(getViewerPanel(), "There are no ROI in the list", title, JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (!roiListControl.getSelectedValuesList().isEmpty()) {
            int result = JOptionPane.showOptionDialog(getViewerPanel(),
                    message,
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"All ROI (" + rois.size() + ")", "Selected ROI (" + roiListControl.getSelectedValuesList().size() + ")", "Cancel"},
                    "All ROI (" + rois.size() + ")");
            if (result == JOptionPane.CANCEL_OPTION)
                return null;
            else if (result == JOptionPane.YES_OPTION)
                return rois;
            else {
                ROI3DListData roi3DListData = new ROI3DListData();
                roi3DListData.addAll(roiListControl.getSelectedValuesList());
                return roi3DListData;
            }
        }
        return rois;
    }


    public List<ROIManagerPlugin3DSelectionContextPanel> getSelectionContextPanels() {
        return Collections.unmodifiableList(selectionContextPanels);
    }

    public void addSelectionContextPanel(ROIManagerPlugin3DSelectionContextPanel panel) {
        selectionContextPanels.add(panel);
        selectionContentPanelUI.add(panel);
        selectionContentPanelUI.revalidate();
        selectionContentPanelUI.repaint();
    }

    public void removeSelectionContextPanel(ROIManagerPlugin3DSelectionContextPanel panel) {
        selectionContextPanels.remove(panel);
        selectionContentPanelUI.remove(panel);
        selectionContentPanelUI.revalidate();
        selectionContentPanelUI.repaint();
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

//            renderingBand.add(new SmallButtonAction("More settings ...", "Opens more rendering settings", UIUtils.getIconFromResources("actions/configure.png"), this::openRoiDrawingSettings));
            renderingBand.add(displayROIAsVolumeItem);
            renderingBand.add(new SmallButtonAction("Save settings", "Saves the current settings as default", UIUtils.getIconFromResources("actions/save.png"), this::saveDefaults));
            renderingBand.add(new SmallButtonAction("Rebuild", "Re-renders the ROI", UIUtils.getIconFromResources("actions/run-build.png"), this::rebuildRoiContentNow));
        }

        // Filter task
        {
            Ribbon.Task filterTask = ribbon.addTask("Filter");
            Ribbon.Band listBand = filterTask.addBand("List");
//            Ribbon.Band roiBand = filterTask.addBand("ROI");

            // List band
//            listBand.add(new SmallToggleButtonAction("Hide invisible", "Show only visible ROI in list", UIUtils.getIconFromResources("actions/eye-slash.png"), filterListHideInvisible, (toggle) -> {
//                filterListHideInvisible = toggle.isSelected();
//                updateListModel();
//            }));
            listBand.add(new SmallToggleButtonAction("Only selection", "Show only ROI that are selected", UIUtils.getIconFromResources("actions/edit-select-all.png"), filterListOnlySelected, (toggle) -> {
                filterListOnlySelected = toggle.isSelected();
                updateListModel();
            }));

//            // ROI band
//            roiBand.add(new SmallToggleButtonAction("Ignore Z", "If enabled, ROI ignore the Z axis", UIUtils.getIconFromResources("actions/layer-flatten-z.png"), roiDrawer.isIgnoreZ(), (toggle) -> {
//                roiDrawer.setIgnoreZ(toggle.isSelected());
//                uploadSliceToCanvas();
//            }));
//            roiBand.add(new SmallToggleButtonAction("Ignore C", "If enabled, ROI ignore the channel axis", UIUtils.getIconFromResources("actions/layer-flatten-c.png"), roiDrawer.isIgnoreC(), (toggle) -> {
//                roiDrawer.setIgnoreC(toggle.isSelected());
//                uploadSliceToCanvas();
//            }));
//            roiBand.add(new SmallToggleButtonAction("Ignore T", "If enabled, ROI ignore the time/frame axis", UIUtils.getIconFromResources("actions/layer-flatten-t.png"), roiDrawer.isIgnoreT(), (toggle) -> {
//                roiDrawer.setIgnoreT(toggle.isSelected());
//                uploadSliceToCanvas();
//            }));
        }

        // Select/Edit task
        {
            Ribbon.Task selectionTask = ribbon.addTask("Selection");
            Ribbon.Band generalBand = selectionTask.addBand("General");
            Ribbon.Band modifyBand = selectionTask.addBand("Modify");
            Ribbon.Band measureBand = selectionTask.addBand("Measure");

//            ROIPicker2DTool pickerTool = new ROIPicker2DTool(this);
//            LargeToggleButtonAction pickerToggle = new LargeToggleButtonAction("Pick", "Allows to select ROI via the mouse", UIUtils.getIcon32FromResources("actions/followmouse.png"));
//            pickerTool.addToggleButton(pickerToggle.getButton(), getViewerPanel2D().getCanvas());
//            generalBand.add(pickerToggle);

            generalBand.add(new SmallButtonAction("Select all", "Selects all ROI", UIUtils.getIconFromResources("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new SmallButtonAction("Clear selection", "Deselects all ROI", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new SmallButtonAction("Invert selection", "Inverts the current selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new SmallButtonAction("Delete", "Deletes the selected ROI", UIUtils.getIconFromResources("actions/delete.png"), this::removeSelectedROIs));

            SmallButtonAction modifyEditAction = new SmallButtonAction("Modify", "Modifies the selected ROI", UIUtils.getIconFromResources("actions/edit.png"), () -> {
            });
            JPopupMenu modifyEditMenu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToButton(modifyEditAction.getButton(), modifyEditMenu, () -> reloadEditRoiMenu(modifyEditMenu));
            modifyBand.add(modifyEditAction);

//            measureBand.add(new SmallButtonAction("Metadata", "Shows the metadata of the selected ROI as table", UIUtils.getIconFromResources("actions/tag.png"), this::showSelectedROIMetadata));

            SmallButtonAction measureAction = new SmallButtonAction("Measure", "Measures the ROI and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedROI);
            measureBand.add(measureAction);
            measureBand.add(new SmallButtonAction("Settings ...", "Opens the measurement settings", UIUtils.getIconFromResources("actions/configure.png"), this::openMeasurementSettings));

        }

        // Import/Export task
        {
            Ribbon.Task importExportTask = ribbon.addTask("Import/Export");
            Ribbon.Band fileBand = importExportTask.addBand("File");

            fileBand.add(new SmallButtonAction("Import from file", "Imports ROI from a *.roi or *.zip file", UIUtils.getIconFromResources("actions/fileopen.png"), this::importROIsFromFile));
            fileBand.add(new SmallButtonAction("Export to file", "Exports ROI to a *.zip file", UIUtils.getIconFromResources("actions/save.png"), this::exportROIsToFile));
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

//    private void showSelectedROIMetadata() {
//        ROIListData rois = getSelectedROIOrAll("Show metadata", "Please select which ROI metadata you want displayed");
//        ResultsTableData table = new ResultsTableData();
//        table.addStringColumn("ROI Name");
//        table.addStringColumn("ROI Index");
//        for (int i = 0; i < rois.size(); i++) {
//            Roi roi = rois.get(i);
//            Map<String, String> map = ImageJUtils.getRoiProperties(roi);
//            int row = table.addRow();
//            table.setValueAt(StringUtils.orElse(roi.getName(), "Unnamed"), row, "ROI Name");
//            table.setValueAt(i, row, "ROI Index");
//            for (Map.Entry<String, String> entry : map.entrySet()) {
//                table.setValueAt(entry.getValue(), row, entry.getKey());
//            }
//        }
//        TableEditor.openWindow(getViewerPanel().getWorkbench(), table, "ROI metadata");
//    }

    private void measureSelectedROI() {
        ROI3DListData data = getSelectedROIOrAll("Measure", "Please select which ROI you want to measure");
        Measurement3DSettings settings = Measurement3DSettings.INSTANCE;
        ResultsTableData measurements = data.measure(ImageHandler.wrap(getCurrentImagePlus()), settings.getStatistics().getNativeValue(),
                settings.isMeasureInPhysicalUnits(), "", new JIPipeProgressInfo());
        TableEditor.openWindow(getViewerPanel().getWorkbench(), measurements, "Measurements");
    }

    private void selectNone() {
        roiListControl.clearSelection();
    }

    private void invertSelection() {
        Set<Integer> selectedIndices = Arrays.stream(roiListControl.getSelectedIndices()).boxed().collect(Collectors.toSet());
        roiListControl.clearSelection();
        Set<Integer> newSelectedIndices = new HashSet<>();
        for (int i = 0; i < roiListControl.getModel().getSize(); i++) {
            if (!selectedIndices.contains(i))
                newSelectedIndices.add(i);
        }
        roiListControl.setSelectedIndices(Ints.toArray(newSelectedIndices));
    }

    //    private void createButtons(JMenuBar menuBar) {
//        {
//            JButton removeButton = new JButton("Delete", UIUtils.getIconFromResources("actions/delete.png"));
//            removeButton.setToolTipText("Remove selected ROIs");
//            removeButton.addActionListener(e -> {
//                if (roiListControl.getSelectedValuesList().isEmpty())
//                    return;
//                if (JOptionPane.showConfirmDialog(getViewerPanel(), "Do you really want to remove " + roiListControl.getSelectedValuesList().size() + "ROI?", "Edit ROI", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
//                    removeSelectedROIs(false);
//                }
//            });
//            menuBar.add(removeButton);
//        }
//        {
//            JButton editButton = new JButton("Edit", UIUtils.getIconFromResources("actions/edit.png"));
//            JPopupMenu editMenu = new JPopupMenu();
//            UIUtils.addReloadablePopupMenuToComponent(editButton, editMenu, () -> reloadEditRoiMenu(editMenu));
//            menuBar.add(editButton);
//        }
//    }

    private void saveDefaults() {
        if (JOptionPane.showConfirmDialog(getViewerPanel(),
                "Do you want to save the ROI display settings as default?",
                "Save settings as default",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ImageViewerUIROI3DDisplaySettings settings = ImageViewerUIROI3DDisplaySettings.getInstance();
            settings.setShowROI(displayROIViewMenuItem.getState());
            settings.setRenderROIAsVolume(displayROIAsVolumeItem.getState());
            JIPipe.getSettings().save();
        }
    }

    private void importROIsFromFile() {
        Path path = FileChooserSettings.openFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Import ROI", UIUtils.EXTENSION_FILTER_ROIS);
        if (path != null) {
            ROIListData importedROIs = ROIListData.loadRoiListFromFile(path);
            importROIs(importedROIs);
        }
    }

    private void exportROIsToFile() {
        ROI3DListData result = getSelectedROIOrAll("Export ROI", "Do you want to export all ROI or only the selected ones?");
        if (result != null) {
            exportROIsToFile(result);
        }
    }

    private void exportROIsToFile(ROI3DListData rois) {
        Path path = FileChooserSettings.saveFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Export ROI", UIUtils.EXTENSION_FILTER_ROI_ZIP);
        if (path != null) {
            rois.save(path);
        }
    }

    @Override
    public String getCategory() {
        return "ROI";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("actions/roi.png");
    }

    private void reloadEditRoiMenu(JPopupMenu menu) {
        List<ROI3D> selectedRois = roiListControl.getSelectedValuesList();
        menu.removeAll();
        if (selectedRois.isEmpty()) {
            JMenuItem noSelection = new JMenuItem("No ROI selected");
            noSelection.setEnabled(false);
            menu.add(noSelection);
            return;
        }

        Color currentFillColor = selectedRois.stream().map(ROI3D::getFillColor).filter(Objects::nonNull).findAny().orElse(Color.RED);
        JMenuItem setFillColorItem = new JMenuItem("Set fill color ...", new SolidColorIcon(16, 16, currentFillColor));
        setFillColorItem.addActionListener(e -> {
            Color value = JColorChooser.showDialog(getViewerPanel(), "Set fill color", currentFillColor);
            if (value != null) {
                for (ROI3D roi : selectedRois) {
                    roi.setFillColor(value);
                }
                roiListControl.repaint();
                rebuildRoiContentLater();
            }
        });
        menu.add(setFillColorItem);

        String currentName = selectedRois.stream().map(ROI3D::getName).filter(Objects::nonNull).findAny().orElse("");
        JMenuItem setNameItem = new JMenuItem("Set name ...", UIUtils.getIconFromResources("actions/tag.png"));
        setNameItem.addActionListener(e -> {
            String value = JOptionPane.showInputDialog(getViewerPanel(), "Please set the name of the ROIs:", currentName);
            if (value != null) {
                for (ROI3D roi : selectedRois) {
                    roi.setName(value);
                }
                roiListControl.repaint();
            }
        });
        menu.add(setNameItem);

        menu.addSeparator();

        int currentCPosition = Math.max(0, selectedRois.stream().map(ROI3D::getChannel).min(Comparator.naturalOrder()).get());
        JMenuItem setCPositionItem = new JMenuItem("Set channel ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setCPositionItem.addActionListener(e -> {
            Optional<Integer> value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set channel", "The first index is 1. Set it to zero to make the ROI appear on all channel planes.", currentCPosition, 0, Integer.MAX_VALUE);
            if (value.isPresent()) {
                for (ROI3D roi : selectedRois) {
                    roi.setChannel(value.get());
                }
                roiListControl.repaint();
            }
        });
        menu.add(setCPositionItem);

        int currentTPosition = Math.max(0, selectedRois.stream().map(ROI3D::getFrame).min(Comparator.naturalOrder()).get());
        JMenuItem setTPositionItem = new JMenuItem("Set T position ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setTPositionItem.addActionListener(e -> {
            Optional<Integer> value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set T position", "The first index is 1. Set it to zero to make the ROI appear on all frame planes.", currentTPosition, 0, Integer.MAX_VALUE);
            if (value.isPresent()) {
                for (ROI3D roi : selectedRois) {
                    roi.setFrame(value.get());
                }
                roiListControl.repaint();
                rebuildRoiContentLater();
            }
        });
        menu.add(setTPositionItem);
    }

    private void initialize() {
        // Setup ROI list
        roiListControl.setCellRenderer(new ROI3DListCellRenderer());
        roiListControl.addListSelectionListener(e -> {
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

//        JMenuBar listMenuBar = new JMenuBar();
//        mainPanel.add(listMenuBar, BorderLayout.NORTH);
//
//        createViewMenu(listMenuBar);
////        createSelectionMenu(listMenuBar);
//
//        listMenuBar.add(Box.createHorizontalGlue());
//        createButtons(listMenuBar);

        JScrollPane scrollPane = new JScrollPane(roiListControl);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        selectionContentPanelUI.setLayout(new BoxLayout(selectionContentPanelUI, BoxLayout.Y_AXIS));
        mainPanel.add(selectionContentPanelUI, BorderLayout.SOUTH);
    }

    private void rebuildRoiContentLater() {
        updateContentLaterTimer.restart();
    }

    public void importROIs(ROIListData rois) {
        scheduledRoi2D.addAll(rois);
        rebuildRoiContentNow();
    }

    private void rebuildRoiContentNow() {
        if (currentRendererRun != null) {
            getViewerPanel3D().getViewerRunnerQueue().cancel(currentRendererRun);
        }
        if (getViewerPanel3D().getCurrentImageContents() == null) {
            // Wait for ImageContentReady
            return;
        }
        currentRendererRun = new ROI3DToContentConverterRun(new ArrayList<>(rois),
                new Roi3DDrawer(),
                getCurrentImagePlus(),
                displayROIAsVolumeItem.isSelected(), getViewerPanel3D().getImage3DRendererSettings().getResamplingFactor(getCurrentImage().getImage()));
        getViewerPanel3D().getViewerRunnerQueue().enqueue(currentRendererRun);
    }

    @Override
    public void onImageContentReady(List<Content> content) {
        rebuildRoiContentNow();
    }

    public void removeSelectedROIs() {
        ImmutableList<ROI3D> deleted = ImmutableList.copyOf(roiListControl.getSelectedValuesList());
        rois.removeAll(roiListControl.getSelectedValuesList());
        updateListModel(Collections.emptySet());
        removeContent(deleted);
    }

    private void removeContent(Collection<ROI3D> rois) {
        this.rois.removeAll(rois);
        rebuildRoiContentNow();
    }

    private void removeContent(ROI3D roi3D) {
        rois.remove(roi3D);
        rebuildRoiContentNow();
    }

    public void clearROIs() {
        rois.clear();
        updateListModel(Collections.emptySet());
        if (currentRendereredContent != null && getViewerPanel3D().getUniverse() != null) {
            getViewerPanel3D().getUniverse().removeContent(currentRendereredContent.getName());
        }
    }

    public ROI3DListData getRois() {
        return rois;
    }

    public void setRois(ROI3DListData rois) {
        this.rois = rois;
        updateListModel(Collections.emptySet());
    }

    public void exportROIsToManager(ROIListData rois) {
        rois.addToRoiManager(RoiManager.getRoiManager());
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

    public void updateListModel(Collection<ROI3D> excludeFromFilter) {
        DefaultListModel<ROI3D> model = new DefaultListModel<>();
        List<ROI3D> selectedValuesList = roiListControl.getSelectedValuesList();
        for (ROI3D roi : rois) {
            boolean excluded = excludeFromFilter.contains(roi);
            if (!excluded && !selectedValuesList.isEmpty() && (filterListOnlySelected && !selectedValuesList.contains(roi)))
                continue;
            model.addElement(roi);
        }
        roiListControl.setModel(model);
        setSelectedROI(selectedValuesList, false);
        updateContextPanels();
    }

    private void updateContextPanels() {
        List<ROI3D> selectedValuesList = roiListControl.getSelectedValuesList();
        for (ROIManagerPlugin3DSelectionContextPanel selectionContextPanel : selectionContextPanels) {
            selectionContextPanel.selectionUpdated(rois, selectedValuesList);
        }
    }

    public JList<ROI3D> getRoiListControl() {
        return roiListControl;
    }

    private void selectAll() {
        roiListControl.setSelectionInterval(0, roiListControl.getModel().getSize() - 1);
    }

    public void setSelectedROI(Collection<ROI3D> select, boolean force) {
        TIntList indices = new TIntArrayList();
        DefaultListModel<ROI3D> model = (DefaultListModel<ROI3D>) roiListControl.getModel();

        if (force) {
            boolean rebuild = false;
            for (ROI3D roi : select) {
                if (rois.contains(roi) && !model.contains(roi)) {
                    rebuild = true;
                    break;
                }
            }
            if (rebuild) {
                roiListControl.clearSelection();
                updateListModel(select);
                model = (DefaultListModel<ROI3D>) roiListControl.getModel();
            }
        }

        for (ROI3D roi : select) {
            int i = model.indexOf(roi);
            if (i >= 0) {
                indices.add(i);
            }
        }
        roiListControl.setSelectedIndices(indices.toArray());
    }

    private static class ROI2DTo3DConverterRun extends AbstractJIPipeRunnable {

        private final List<Roi> rois;

        private final List<ROI3D> converted = new ArrayList<>();

        public ROI2DTo3DConverterRun(List<Roi> rois) {
            this.rois = rois;
        }

        @Override
        public String getTaskLabel() {
            return "Convert 2D ROI to 3D ROI";
        }

        @Override
        public void run() {
            ROIListData inputData = new ROIListData();
            inputData.addAll(rois);
            ROI3DListData outputData = IJ3DUtils.roi2DtoRoi3D(inputData, true, false, Neighborhood3D.TwentySixConnected, getProgressInfo());
            converted.addAll(outputData);
        }
    }

    private static class ROI3DToContentConverterRun extends AbstractJIPipeRunnable {

        private final List<ROI3D> rois;

        private final Roi3DDrawer drawer;

        private final ImagePlus referenceImage;
        private final int resolutionFactor;
        private final boolean renderAsVolume;
        private Content renderedContent;

        public ROI3DToContentConverterRun(List<ROI3D> rois, Roi3DDrawer drawer, ImagePlus referenceImage, boolean renderAsVolume, int resolutionFactor) {
            this.rois = rois;
            this.drawer = drawer;
            this.referenceImage = referenceImage;
            this.renderAsVolume = renderAsVolume;
            this.resolutionFactor = resolutionFactor;
        }

        @Override
        public String getTaskLabel() {
            return "Preprocess 3D ROI";
        }

        public Content getRenderedContent() {
            return renderedContent;
        }

        public List<ROI3D> getRois() {
            return rois;
        }

        @Override
        public void run() {

            if (renderAsVolume) {
                ROI3DListData roi3DListData = new ROI3DListData();
                roi3DListData.addAll(rois);
                Roi3DDrawer copyDrawer = new Roi3DDrawer(drawer);
                copyDrawer.setDrawOver(false);
                ImagePlus render = copyDrawer.draw(roi3DListData, referenceImage, getProgressInfo().resolve("Render ROI to RGB"));
                getProgressInfo().log("Converting RGB to ");
                renderedContent = ContentCreator.createContent("ROI3D-" + UUID.randomUUID(),
                        render,
                        Image3DRenderType.Volume.getNativeValue(),
                        resolutionFactor,
                        0,
                        new Color3f(1, 1, 1),
                        0,
                        new boolean[]{true, true, true});
            } else {
                List<CustomMesh> meshList = new ArrayList<>();
                getProgressInfo().setProgress(0, rois.size());
                for (int i = 0; i < rois.size(); i++) {
                    getProgressInfo().resolveAndLog("ROI to 3D mesh", i, rois.size());
                    ROI3D roi3D = rois.get(i);
                    CustomTriangleMesh mesh = new CustomTriangleMesh(roi3D.getObject3D().getObject3DSurface().getSurfaceTrianglesPixels(true),
                            new Color3f(roi3D.getFillColor().getRed() / 255.0f, roi3D.getFillColor().getGreen() / 255.0f, roi3D.getFillColor().getBlue() / 255.0f),
                            0f);
                    meshList.add(mesh);
                    getProgressInfo().incrementProgress();
                }
                CustomMultiMesh customMultiMesh = new CustomMultiMesh(meshList);
                renderedContent = ContentCreator.createContent(customMultiMesh, "ROI3D-" + UUID.randomUUID());
            }
        }
    }
}
