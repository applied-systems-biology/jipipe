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
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeToggleButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallToggleButtonRibbonAction;
import org.hkijena.jipipe.plugins.ij3d.datatypes.IJ3DROI;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJROIUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ROIElementDrawingMode;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiDrawer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin2D;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PathUtils;
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
    private final JList<IJ3DROI> roiListControl = new JList<>();
    private final JIPipeDesktopLargeToggleButtonRibbonAction displayROIViewMenuItem = new JIPipeDesktopLargeToggleButtonRibbonAction("Display ROI", "Determines whether ROI are displayed", JIPipe.RESOURCES.getIcon32("data-types/roi.png"));
    private final List<ROIManagerPlugin3DSelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private final RoiDrawer roiDrawer = new RoiDrawer();
    private final Map<ImageSliceIndex, Roi2dListData> renderedRois = new HashMap<>();
    private Ij3dSuiteRoiListData rois = new Ij3dSuiteRoiListData();
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
        ImageViewerUIRoi3DDisplayApplicationSettings settings = ImageViewerUIRoi3DDisplayApplicationSettings.getInstance();
        displayROIViewMenuItem.setSelected(settings.isShowROI());
    }

    @Override
    public void onImageChanged() {
        updateListModel(Collections.emptySet());

        // Load Roi3D content
        if (getCurrentImage() != null) {
            Ij3dSuiteRoiListData data = new Ij3dSuiteRoiListData();
            for (Ij3dSuiteRoiListData listData : getCurrentImage().extractOverlaysOfType(Ij3dSuiteRoiListData.class)) {
                data.addAll(listData);
            }
            if (!data.isEmpty()) {
                getViewerPanel().addOverlay(data);
            }
        }
    }

    @Override
    public void onOverlayAdded(Object overlay) {
        if (overlay instanceof Ij3dSuiteRoiListData) {
            importROIs((Ij3dSuiteRoiListData) overlay);
        }
    }

    @Override
    public void onOverlayRemoved(Object overlay) {
        // Currently not possible (creates copies of the ROI)
    }

    private void importROIs(Ij3dSuiteRoiListData overlay) {
        for (IJ3DROI roi3D : overlay) {
            IJ3DROI copy = new IJ3DROI();
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

    public Ij3dSuiteRoiListData getSelectedROIOrAll(String title, String message) {
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
                Ij3dSuiteRoiListData roi3DListData = new Ij3dSuiteRoiListData();
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

            renderingBand.add(new JIPipeDesktopSmallButtonRibbonAction("More settings ...", "Opens more rendering settings", JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openRoiDrawingSettings));
//            renderingBand.add(displayROIAsVolumeItem);
            renderingBand.add(new JIPipeDesktopSmallButtonRibbonAction("Save settings", "Saves the current settings as default", JIPipe.RESOURCES.getIcon16("actions/filesave.png"), this::saveDefaults));

        }

        // Filter task
        {
            JIPipeDesktopRibbon.Band listBand = roiTask.addBand("List");
//            Ribbon.Band roiBand = filterTask.addBand("ROI");

            // List band
//            listBand.add(new SmallToggleButtonAction("Hide invisible", "Show only visible ROI in list", JIPipe.RESOURCES.getIcon16("actions/eye-slash.png"), filterListHideInvisible, (toggle) -> {
//                filterListHideInvisible = toggle.isSelected();
//                updateListModel();
//            }));
            listBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Only selection", "Show only ROI that are selected", JIPipe.RESOURCES.getIcon16("actions/edit-select-all.png"), filterListOnlySelected, (toggle) -> {
                filterListOnlySelected = toggle.isSelected();
                updateListModel();
            }));

//            // ROI band
//            roiBand.add(new SmallToggleButtonAction("Ignore Z", "If enabled, ROI ignore the Z axis", JIPipe.RESOURCES.getIcon16("actions/layer-flatten-z.png"), roiDrawer.isIgnoreZ(), (toggle) -> {
//                roiDrawer.setIgnoreZ(toggle.isSelected());
//                uploadSliceToCanvas();
//            }));
//            roiBand.add(new SmallToggleButtonAction("Ignore C", "If enabled, ROI ignore the channel axis", JIPipe.RESOURCES.getIcon16("actions/layer-flatten-c.png"), roiDrawer.isIgnoreC(), (toggle) -> {
//                roiDrawer.setIgnoreC(toggle.isSelected());
//                uploadSliceToCanvas();
//            }));
//            roiBand.add(new SmallToggleButtonAction("Ignore T", "If enabled, ROI ignore the time/frame axis", JIPipe.RESOURCES.getIcon16("actions/layer-flatten-t.png"), roiDrawer.isIgnoreT(), (toggle) -> {
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
//            LargeToggleButtonAction pickerToggle = new LargeToggleButtonAction("Pick", "Allows to select ROI via the mouse", JIPipe.RESOURCES.getIcon32("actions/followmouse.png"));
//            pickerTool.addToggleButton(pickerToggle.getButton(), getViewerPanel2D().getCanvas());
//            generalBand.add(pickerToggle);

            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Select all", "Selects all ROI", JIPipe.RESOURCES.getIcon16("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Clear selection", "Deselects all ROI", JIPipe.RESOURCES.getIcon16("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Invert selection", "Inverts the current selection", JIPipe.RESOURCES.getIcon16("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new JIPipeDesktopSmallButtonRibbonAction("Delete", "Deletes the selected ROI", JIPipe.RESOURCES.getIcon16("actions/delete.png"), this::removeSelectedROIs));

            JIPipeDesktopSmallButtonRibbonAction modifyEditAction = new JIPipeDesktopSmallButtonRibbonAction("Modify", "Modifies the selected ROI", JIPipe.RESOURCES.getIcon16("actions/edit.png"), () -> {
            });
            JPopupMenu modifyEditMenu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToButton(modifyEditAction.getButton(), modifyEditMenu, () -> reloadEditRoiMenu(modifyEditMenu));
            modifyBand.add(modifyEditAction);

//            measureBand.add(new SmallButtonAction("Metadata", "Shows the metadata of the selected ROI as table", JIPipe.RESOURCES.getIcon16("actions/tag.png"), this::showSelectedROIMetadata));

            JIPipeDesktopSmallButtonRibbonAction measureAction = new JIPipeDesktopSmallButtonRibbonAction("Measure", "Measures the ROI and displays the results as table", JIPipe.RESOURCES.getIcon16("actions/statistics.png"), this::measureSelectedROI);
            measureBand.add(measureAction);
            measureBand.add(new JIPipeDesktopSmallButtonRibbonAction("Settings ...", "Opens the measurement settings", JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openMeasurementSettings));

        }

        // Import/Export task
        {
            JIPipeDesktopRibbon.Band importExportBand = roiTask.getOrCreateBand("Import/Export");

            importExportBand.add(new JIPipeDesktopSmallButtonRibbonAction("Import from file", "Imports ROI from a *.roi or *.zip file", JIPipe.RESOURCES.getIcon16("actions/fileopen.png"), this::importROIsFromFile));
            importExportBand.add(new JIPipeDesktopSmallButtonRibbonAction("Export to file", "Exports ROI to a *.zip file", JIPipe.RESOURCES.getIcon16("actions/filesave.png"), this::exportROIsToFile));
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
        Ij3dSuiteRoiListData data = getSelectedROIOrAll("Measure", "Please select which ROI you want to measure");
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
//            JButton removeButton = new JButton("Delete", JIPipe.RESOURCES.getIcon16("actions/delete.png"));
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
//            JButton editButton = new JButton("Edit", JIPipe.RESOURCES.getIcon16("actions/edit.png"));
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
            ImageViewerUIRoi3DDisplayApplicationSettings settings = ImageViewerUIRoi3DDisplayApplicationSettings.getInstance();
            settings.setShowROI(displayROIViewMenuItem.getState());
            JIPipe.autoSaveSettings();
        }
    }

    private void importROIsFromFile() {
        Path path = JIPipeDesktop.openFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Import 3D ROI", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_ZIP);
        if (path != null) {
            Ij3dSuiteRoiListData data = Ij3dSuiteRoiListData.importData(path, JIPipeProgressInfo.SILENT);
            importROIs(data);
        }
    }

    private void exportROIsToFile() {
        Ij3dSuiteRoiListData result = getSelectedROIOrAll("Export ROI", "Do you want to export all ROI or only the selected ones?");
        if (result != null) {
            exportROIsToFile(result);
        }
    }

    private void exportROIsToFile(Ij3dSuiteRoiListData rois) {
        Path path = JIPipeDesktop.saveFile(getViewerPanel(), getWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export ROI", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_ROI_ZIP);
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
        return JIPipe.RESOURCES.getIcon24("actions/cube.png");
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (displayROIViewMenuItem.getState()) {
            Roi2dListData rendered = renderedRois.getOrDefault(sliceIndex, null);
            if (rendered == null) {
                rendered = rois.toRoi2d(sliceIndex.add(1), JIPipeProgressInfo.SILENT);
                renderedRois.put(sliceIndex, rendered);
            }
            for (Roi roi : rendered) {
                ImageJROIUtils.setRoiCanvas(roi, getCurrentImagePlus(), getViewerPanel2D().getZoomedDummyCanvas());
            }
            roiDrawer.drawOverlayOnGraphics(rendered, graphics2D, renderArea, sliceIndex, Collections.emptySet(), getViewerPanel2D().getCanvas().getZoom());
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex, double magnification) {
        if (displayROIViewMenuItem.getState()) {
            Roi2dListData rendered = renderedRois.getOrDefault(sliceIndex, null);
            if (rendered == null) {
                rendered = rois.toRoi2d(sliceIndex.add(1), JIPipeProgressInfo.SILENT);
                renderedRois.put(sliceIndex, rendered);
            }
            Graphics2D graphics = image.createGraphics();
            Roi2dListData copy = new Roi2dListData();
            ImageCanvas canvas = ImageJUtils.createZoomedDummyCanvas(getCurrentImagePlus(), magnification);
            for (Roi roi : rendered) {
                Roi clone = (Roi) roi.clone();
                ImageJROIUtils.setRoiCanvas(clone, getCurrentImagePlus(), canvas);
                copy.add(clone);
            }
            roiDrawer.drawOverlayOnGraphics(copy, graphics, new Rectangle(0, 0, image.getWidth(), image.getHeight()), sliceIndex, Collections.emptySet(), magnification);
            graphics.dispose();
        }
    }

    private void reloadEditRoiMenu(JPopupMenu menu) {
        List<IJ3DROI> selectedRois = roiListControl.getSelectedValuesList();
        menu.removeAll();
        if (selectedRois.isEmpty()) {
            JMenuItem noSelection = new JMenuItem("No ROI selected");
            noSelection.setEnabled(false);
            menu.add(noSelection);
            return;
        }

        Color currentFillColor = selectedRois.stream().map(IJ3DROI::getFillColor).filter(Objects::nonNull).findAny().orElse(Color.RED);
        JMenuItem setFillColorItem = new JMenuItem("Set fill color ...", new SolidColorIcon(16, 16, currentFillColor));
        setFillColorItem.addActionListener(e -> {
            Color value = JColorChooser.showDialog(getViewerPanel(), "Set fill color", currentFillColor);
            if (value != null) {
                for (IJ3DROI roi : selectedRois) {
                    roi.setFillColor(value);
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setFillColorItem);

        String currentName = selectedRois.stream().map(IJ3DROI::getName).filter(Objects::nonNull).findAny().orElse("");
        JMenuItem setNameItem = new JMenuItem("Set name ...", JIPipe.RESOURCES.getIcon16("actions/tag.png"));
        setNameItem.addActionListener(e -> {
            String value = JOptionPane.showInputDialog(getViewerPanel(), "Please set the name of the ROIs:", currentName);
            if (value != null) {
                for (IJ3DROI roi : selectedRois) {
                    roi.setName(value);
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setNameItem);

        menu.addSeparator();

        int currentCPosition = Math.max(0, selectedRois.stream().map(IJ3DROI::getChannel).min(Comparator.naturalOrder()).get());
        JMenuItem setCPositionItem = new JMenuItem("Set channel ...", JIPipe.RESOURCES.getIcon16("actions/mark-location.png"));
        setCPositionItem.addActionListener(e -> {
            Optional<Integer> value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set channel", "The first index is 1. Set it to zero to make the ROI appear on all channel planes.", currentCPosition, 0, Integer.MAX_VALUE);
            if (value.isPresent()) {
                for (IJ3DROI roi : selectedRois) {
                    roi.setChannel(value.get());
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setCPositionItem);

        int currentTPosition = Math.max(0, selectedRois.stream().map(IJ3DROI::getFrame).min(Comparator.naturalOrder()).get());
        JMenuItem setTPositionItem = new JMenuItem("Set T position ...", JIPipe.RESOURCES.getIcon16("actions/mark-location.png"));
        setTPositionItem.addActionListener(e -> {
            Optional<Integer> value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set T position", "The first index is 1. Set it to zero to make the ROI appear on all frame planes.", currentTPosition, 0, Integer.MAX_VALUE);
            if (value.isPresent()) {
                for (IJ3DROI roi : selectedRois) {
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
        roiListControl.setCellRenderer(new Roi3DListCellRenderer());
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
        ImmutableList<IJ3DROI> deleted = ImmutableList.copyOf(roiListControl.getSelectedValuesList());
        rois.removeAll(roiListControl.getSelectedValuesList());
        renderedRois.clear();
        updateListModel(Collections.emptySet());
    }

    public void clearROIs() {
        rois.clear();
        renderedRois.clear();
        updateListModel(Collections.emptySet());
    }

    public Ij3dSuiteRoiListData getRois() {
        return rois;
    }

    public void setRois(Ij3dSuiteRoiListData rois) {
        this.rois = rois;
        updateListModel(Collections.emptySet());
    }

    public void exportROIsToManager(Roi2dListData rois) {
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

    public void updateListModel(Collection<IJ3DROI> excludeFromFilter) {
        DefaultListModel<IJ3DROI> model = new DefaultListModel<>();
        List<IJ3DROI> selectedValuesList = roiListControl.getSelectedValuesList();
        for (IJ3DROI roi : rois) {
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
        List<IJ3DROI> selectedValuesList = roiListControl.getSelectedValuesList();
        for (ROIManagerPlugin3DSelectionContextPanel selectionContextPanel : selectionContextPanels) {
            selectionContextPanel.selectionUpdated(rois, selectedValuesList);
        }
    }

    public JList<IJ3DROI> getRoiListControl() {
        return roiListControl;
    }

    private void selectAll() {
        roiListControl.setSelectionInterval(0, roiListControl.getModel().getSize() - 1);
    }

    public void setSelectedROI(Collection<IJ3DROI> select, boolean force) {
        TIntList indices = new TIntArrayList();
        DefaultListModel<IJ3DROI> model = (DefaultListModel<IJ3DROI>) roiListControl.getModel();

        if (force) {
            boolean rebuild = false;
            for (IJ3DROI roi : select) {
                if (rois.contains(roi) && !model.contains(roi)) {
                    rebuild = true;
                    break;
                }
            }
            if (rebuild) {
                roiListControl.clearSelection();
                updateListModel(select);
                model = (DefaultListModel<IJ3DROI>) roiListControl.getModel();
            }
        }

        for (IJ3DROI roi : select) {
            int i = model.indexOf(roi);
            if (i >= 0) {
                indices.add(i);
            }
        }
        roiListControl.setSelectedIndices(indices.toArray());
    }

}
