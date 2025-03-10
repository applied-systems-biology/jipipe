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

package org.hkijena.jipipe.plugins.ij3d.imageviewer;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeToggleButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ROIElementDrawingMode;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiDrawer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin2D;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ROIManagerPlugin3D extends JIPipeDesktopLegacyImageViewerPlugin2D {
    private final JList<ROI3D> roiListControl = new JList<>();
    private final JIPipeDesktopLargeToggleButtonRibbonAction displayROIViewMenuItem = new JIPipeDesktopLargeToggleButtonRibbonAction("Display ROI", "Determines whether ROI are displayed", UIUtils.getIcon32FromResources("data-types/roi.png"));
    private final List<ROIManagerPlugin3DSelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private final RoiDrawer roiDrawer = new RoiDrawer();
    private final Map<ImageSliceIndex, ROI2DListData> renderedRois = new HashMap<>();
    private ROI3DListData rois = new ROI3DListData();
    private boolean filterListOnlySelected = false;
    private JPanel mainPanel;

    public ROIManagerPlugin3D(JIPipeDesktopLegacyImageViewer viewerPanel) {
        super(viewerPanel);
        roiDrawer.setDrawOutlineMode(ROIElementDrawingMode.IfAvailable);
        loadDefaults();
        initialize();
        addSelectionContextPanel(new ROIManagerPlugin3DInfoContextPanel(this));
    }

    public RoiDrawer getRoiDrawer() {
        return roiDrawer;
    }

    private void loadDefaults() {
        ImageViewerUIROI3DDisplayApplicationSettings settings = ImageViewerUIROI3DDisplayApplicationSettings.getInstance();
        displayROIViewMenuItem.setSelected(settings.isShowROI());
    }

    @Override
    public void onImageChanged() {
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
        if (overlay instanceof ROI3DListData) {
            importROIs((ROI3DListData) overlay);
        }
    }

    @Override
    public void onOverlayRemoved(Object overlay) {
        // Currently not possible (creates copies of the ROI)
    }

    private void importROIs(ROI3DListData overlay) {
        for (ROI3D roi3D : overlay) {
            ROI3D copy = new ROI3D();
            copy.setObject3D(roi3D.getObject3D());
            copy.copyMetadata(roi3D);
            rois.add(copy);
        }
        renderedRois.clear();
        updateListModel(Collections.emptyList());
        uploadSliceToCanvas();
    }

    @Override
    public void dispose() {
        rois.clear();
        roiListControl.setModel(new DefaultListModel<>());
    }

    @Override
    public void onOverlaysCleared() {
        clearROIs();
    }

    @Override
    public boolean isBuildingCustomPanel() {
        return true;
    }

    @Override
    public JComponent buildCustomPanel() {
        return mainPanel;
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

    @Override
    public void buildRibbon(JIPipeDesktopRibbon ribbon) {

        JIPipeDesktopRibbon.Task roiTask = ribbon.getOrCreateTask("3D ROI");

        // View menu for general display
        {
            JIPipeDesktopRibbon.Band renderingBand = roiTask.addBand("Rendering");

            renderingBand.add(displayROIViewMenuItem);

            renderingBand.add(new JIPipeDesktopSmallButtonRibbonAction("More settings ...", "Opens more rendering settings", UIUtils.getIconFromResources("actions/configure.png"), this::openRoiDrawingSettings));
//            renderingBand.add(displayROIAsVolumeItem);
            renderingBand.add(new JIPipeDesktopSmallButtonRibbonAction("Save settings", "Saves the current settings as default", UIUtils.getIconFromResources("actions/filesave.png"), this::saveDefaults));

        }

        // Filter task
        {
            JIPipeDesktopRibbon.Band listBand = roiTask.addBand("List");
//            Ribbon.Band roiBand = filterTask.addBand("ROI");

            // List band
//            listBand.add(new SmallToggleButtonAction("Hide invisible", "Show only visible ROI in list", UIUtils.getIconFromResources("actions/eye-slash.png"), filterListHideInvisible, (toggle) -> {
//                filterListHideInvisible = toggle.isSelected();
//                updateListModel();
//            }));
            listBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Only selection", "Show only ROI that are selected", UIUtils.getIconFromResources("actions/edit-select-all.png"), filterListOnlySelected, (toggle) -> {
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
            JIPipeDesktopRibbon.Band generalBand = roiTask.addBand("Selection");
            JIPipeDesktopRibbon.Band modifyBand = roiTask.addBand("Modify");
            JIPipeDesktopRibbon.Band measureBand = roiTask.addBand("Measure");

//            ROIPicker2DTool pickerTool = new ROIPicker2DTool(this);
//            LargeToggleButtonAction pickerToggle = new LargeToggleButtonAction("Pick", "Allows to select ROI via the mouse", UIUtils.getIcon32FromResources("actions/followmouse.png"));
//            pickerTool.addToggleButton(pickerToggle.getButton(), getViewerPanel2D().getCanvas());
//            generalBand.add(pickerToggle);

            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Select all", "Selects all ROI", UIUtils.getIconFromResources("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Clear selection", "Deselects all ROI", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Invert selection", "Inverts the current selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new JIPipeDesktopSmallButtonRibbonAction("Delete", "Deletes the selected ROI", UIUtils.getIconFromResources("actions/delete.png"), this::removeSelectedROIs));

            JIPipeDesktopSmallButtonRibbonAction modifyEditAction = new JIPipeDesktopSmallButtonRibbonAction("Modify", "Modifies the selected ROI", UIUtils.getIconFromResources("actions/edit.png"), () -> {
            });
            JPopupMenu modifyEditMenu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToButton(modifyEditAction.getButton(), modifyEditMenu, () -> reloadEditRoiMenu(modifyEditMenu));
            modifyBand.add(modifyEditAction);

//            measureBand.add(new SmallButtonAction("Metadata", "Shows the metadata of the selected ROI as table", UIUtils.getIconFromResources("actions/tag.png"), this::showSelectedROIMetadata));

            JIPipeDesktopSmallButtonRibbonAction measureAction = new JIPipeDesktopSmallButtonRibbonAction("Measure", "Measures the ROI and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedROI);
            measureBand.add(measureAction);
            measureBand.add(new JIPipeDesktopSmallButtonRibbonAction("Settings ...", "Opens the measurement settings", UIUtils.getIconFromResources("actions/configure.png"), this::openMeasurementSettings));

        }

        // Import/Export task
        {
            JIPipeDesktopRibbon.Band importExportBand = roiTask.getOrCreateBand("Import/Export");

            importExportBand.add(new JIPipeDesktopSmallButtonRibbonAction("Import from file", "Imports ROI from a *.roi or *.zip file", UIUtils.getIconFromResources("actions/fileopen.png"), this::importROIsFromFile));
            importExportBand.add(new JIPipeDesktopSmallButtonRibbonAction("Export to file", "Exports ROI to a *.zip file", UIUtils.getIconFromResources("actions/filesave.png"), this::exportROIsToFile));
        }
    }

    private void openRoiDrawingSettings() {
        JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), getViewerPanel(), roiDrawer, new MarkdownText("# ROI display settings\n\nPlease use the settings on the left to modify how ROI are visualized."), "ROI display settings", JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
        uploadSliceToCanvas();
    }


    @Override
    public void buildDock(JIPipeDesktopDockPanel dockPanel) {

    }

    @Override
    public void buildStatusBar(JToolBar statusBar) {

    }

    private void openMeasurementSettings() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getViewerPanel()));
        dialog.setTitle("Measurement settings");
        dialog.setContentPane(new JIPipeDesktopParameterFormPanel(new JIPipeDesktopDummyWorkbench(), Measurement3DSettings.INSTANCE, null, JIPipeDesktopFormPanel.WITH_SCROLLING));
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
        JIPipeDesktopTableEditor.openWindow(getViewerPanel().getDesktopWorkbench(), measurements, "Measurements");
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
            ImageViewerUIROI3DDisplayApplicationSettings settings = ImageViewerUIROI3DDisplayApplicationSettings.getInstance();
            settings.setShowROI(displayROIViewMenuItem.getState());
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
        }
    }

    private void importROIsFromFile() {
        Path path = JIPipeFileChooserApplicationSettings.openFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Import 3D ROI", UIUtils.EXTENSION_FILTER_ZIP);
        if (path != null) {
            ROI3DListData data = ROI3DListData.importData(path, JIPipeProgressInfo.SILENT);
            importROIs(data);
        }
    }

    private void exportROIsToFile() {
        ROI3DListData result = getSelectedROIOrAll("Export ROI", "Do you want to export all ROI or only the selected ones?");
        if (result != null) {
            exportROIsToFile(result);
        }
    }

    private void exportROIsToFile(ROI3DListData rois) {
        Path path = JIPipeFileChooserApplicationSettings.saveFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export ROI", UIUtils.EXTENSION_FILTER_ROI_ZIP);
        if (path != null) {
            rois.save(path);
        }
    }

    @Override
    public String getPanelName() {
        return "3D ROI";
    }

    @Override
    public JIPipeDesktopDockPanel.PanelLocation getPanelLocation() {
        return JIPipeDesktopDockPanel.PanelLocation.BottomRight;
    }

    @Override
    public Icon getPanelIcon() {
        return UIUtils.getIcon32FromResources("actions/cube.png");
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (displayROIViewMenuItem.getState()) {
            ROI2DListData rendered = renderedRois.getOrDefault(sliceIndex, null);
            if (rendered == null) {
                rendered = rois.toRoi2D(sliceIndex.add(1), JIPipeProgressInfo.SILENT);
                renderedRois.put(sliceIndex, rendered);
            }
            for (Roi roi : rendered) {
                ImageJUtils.setRoiCanvas(roi, getCurrentImagePlus(), getViewerPanel2D().getZoomedDummyCanvas());
            }
            roiDrawer.drawOverlayOnGraphics(rendered, graphics2D, renderArea, sliceIndex, Collections.emptySet(), getViewerPanel2D().getCanvas().getZoom());
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex, double magnification) {
        if (displayROIViewMenuItem.getState()) {
            ROI2DListData rendered = renderedRois.getOrDefault(sliceIndex, null);
            if (rendered == null) {
                rendered = rois.toRoi2D(sliceIndex.add(1), JIPipeProgressInfo.SILENT);
                renderedRois.put(sliceIndex, rendered);
            }
            Graphics2D graphics = image.createGraphics();
            ROI2DListData copy = new ROI2DListData();
            ImageCanvas canvas = ImageJUtils.createZoomedDummyCanvas(getCurrentImagePlus(), magnification);
            for (Roi roi : rendered) {
                Roi clone = (Roi) roi.clone();
                ImageJUtils.setRoiCanvas(clone, getCurrentImagePlus(), canvas);
                copy.add(clone);
            }
            roiDrawer.drawOverlayOnGraphics(copy, graphics, new Rectangle(0, 0, image.getWidth(), image.getHeight()), sliceIndex, Collections.emptySet(), magnification);
            graphics.dispose();
        }
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
                uploadSliceToCanvas();
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
                uploadSliceToCanvas();
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
                uploadSliceToCanvas();
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
                uploadSliceToCanvas();
            }
        });
        menu.add(setTPositionItem);
    }

    private void initialize() {
        // Setup ROI list
        roiListControl.setCellRenderer(new ROI3DListCellRenderer());
        roiListControl.addListSelectionListener(e -> {
            updateContextPanels();
        });

        // Setup panel
        mainPanel = new JPanel(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(roiListControl);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        selectionContentPanelUI.setLayout(new BoxLayout(selectionContentPanelUI, BoxLayout.Y_AXIS));
        mainPanel.add(selectionContentPanelUI, BorderLayout.SOUTH);
    }

    public void removeSelectedROIs() {
        ImmutableList<ROI3D> deleted = ImmutableList.copyOf(roiListControl.getSelectedValuesList());
        rois.removeAll(roiListControl.getSelectedValuesList());
        renderedRois.clear();
        updateListModel(Collections.emptySet());
    }

    public void clearROIs() {
        rois.clear();
        renderedRois.clear();
        updateListModel(Collections.emptySet());
    }

    public ROI3DListData getRois() {
        return rois;
    }

    public void setRois(ROI3DListData rois) {
        this.rois = rois;
        updateListModel(Collections.emptySet());
    }

    public void exportROIsToManager(ROI2DListData rois) {
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

}
