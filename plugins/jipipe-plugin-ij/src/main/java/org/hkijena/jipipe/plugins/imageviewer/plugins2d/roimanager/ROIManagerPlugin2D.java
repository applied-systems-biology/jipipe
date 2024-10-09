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

package org.hkijena.jipipe.plugins.imageviewer.plugins2d.roimanager;

import com.google.common.primitives.Ints;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.*;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.settings.ImageViewerUIROI2DDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ROIEditor;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiDrawer;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeLegacyImageViewerPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.utils.RoiListCellRenderer;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ROIManagerPlugin2D extends JIPipeLegacyImageViewerPlugin2D {
    private final ROI2DListData overlayRois = new ROI2DListData();
    private final JList<Roi> roiListControl = new JList<>();
    private final RoiDrawer roiDrawer = new RoiDrawer();
    private final JIPipeDesktopLargeToggleButtonRibbonAction displayROIViewMenuItem = new JIPipeDesktopLargeToggleButtonRibbonAction("Display ROI", "Determines whether ROI are displayed", UIUtils.getIcon32FromResources("data-types/roi.png"));
    private final JIPipeDesktopSmallToggleButtonRibbonAction renderROIAsOverlayViewMenuItem = new JIPipeDesktopSmallToggleButtonRibbonAction("Draw ROI as overlay", "If disabled, ROI are drawn as pixels directly into the displayed image.", UIUtils.getIconFromResources("actions/path-break-apart.png"));
    private final List<ROIManagerPlugin2DSelectionContextPanel> selectionContextPanels = new ArrayList<>();
    private final JPanel selectionContentPanelUI = new JPanel();
    private final JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon(3);
    private ROI2DListData rois = new ROI2DListData();
    private boolean filterListHideInvisible = false;
    private boolean filterListOnlySelected = false;
    private JPanel mainPanel;

    public ROIManagerPlugin2D(JIPipeLegacyImageViewer viewerPanel) {
        super(viewerPanel);
        loadDefaults();
        initialize();
        addSelectionContextPanel(new ROIManagerPlugin2DInfoContextPanel(this));
    }

    private void loadDefaults() {
        ImageViewerUIROI2DDisplayApplicationSettings settings = ImageViewerUIROI2DDisplayApplicationSettings.getInstance();
        roiDrawer.copyFrom(settings.getRoiDrawer());
        displayROIViewMenuItem.setSelected(settings.isShowROI());
        renderROIAsOverlayViewMenuItem.setSelected(settings.isRenderROIAsOverlay());
    }

    @Override
    public void onImageChanged() {
        for (Roi roi : overlayRois) {
            rois.remove(roi);
        }
        if (getCurrentImagePlus() != null && getCurrentImagePlus().getOverlay() != null) {
            if (getCurrentImagePlus().getRoi() != null) {
                rois.add((Roi) getCurrentImagePlus().getRoi().clone());
            }
            for (Roi roi : getCurrentImagePlus().getOverlay()) {
                rois.add((Roi) roi.clone());
                overlayRois.add((Roi) roi.clone());
            }
        }
        for (Roi roi : rois) {
            ImageJUtils.setRoiCanvas(roi, getCurrentImagePlus(), getViewerPanel2D().getZoomedDummyCanvas());
        }
        updateListModel(true, Collections.emptySet());
    }

    @Override
    public void onOverlayAdded(Object overlay) {
        if (overlay instanceof ROI2DListData) {
            importROIs((ROI2DListData) overlay, false);
        }
    }

    @Override
    public void onOverlayRemoved(Object overlay) {
        // Currently not possible (creates copies of the ROI)
    }

    @Override
    public void onOverlaysCleared() {
        clearROIs(false);
    }

    @Override
    public void initializeSettingsPanel(JIPipeDesktopFormPanel formPanel) {
        if (getCurrentImagePlus() == null)
            return;
        formPanel.addVerticalGlue(mainPanel, null);
    }

    public ROI2DListData getSelectedROIOrAll(String title, String message) {
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
            else
                return new ROI2DListData(roiListControl.getSelectedValuesList());
        }
        return rois;
    }

    public List<ROIManagerPlugin2DSelectionContextPanel> getSelectionContextPanels() {
        return Collections.unmodifiableList(selectionContextPanels);
    }

    public void addSelectionContextPanel(ROIManagerPlugin2DSelectionContextPanel panel) {
        selectionContextPanels.add(panel);
        selectionContentPanelUI.add(panel);
        selectionContentPanelUI.revalidate();
        selectionContentPanelUI.repaint();
    }

    public void removeSelectionContextPanel(ROIManagerPlugin2DSelectionContextPanel panel) {
        selectionContextPanels.remove(panel);
        selectionContentPanelUI.remove(panel);
        selectionContentPanelUI.revalidate();
        selectionContentPanelUI.repaint();
    }

    private void editROIInDialog() {
        List<Roi> selected = roiListControl.getSelectedValuesList();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(getViewerPanel(), "You have not selected ROI to edit.", "Edit ROI", JOptionPane.ERROR_MESSAGE);
        }
        if (selected.size() > 5) {
            if (JOptionPane.showConfirmDialog(getViewerPanel(), "Do you really want to edit " + selected.size() + "ROI?", "Edit ROI", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        JIPipeDesktopTabPane documentTabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
        List<ROIEditor> editors = new ArrayList<>();
        for (Roi roi : selected) {
            ROIEditor editor = new ROIEditor(roi);
            JIPipeDesktopParameterFormPanel parameterPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), editor, new MarkdownText("# Edit ROI"), JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION);
            documentTabPane.addTab(roi.getName() + "", UIUtils.getIconFromResources("data-types/roi.png"), parameterPanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton, true);
            editors.add(editor);
        }
        if (UIUtils.showOKCancelDialog(getViewerPanel(), documentTabPane, "Edit ROI")) {
            for (int i = 0; i < selected.size(); i++) {
                Roi roi = editors.get(i).applyToRoi(selected.get(i));
                rois.set(i, roi);
            }
            updateListModel();
        }
    }

    private void initializeRibbon() {

        // Register necessary actions
        displayROIViewMenuItem.addActionListener(this::uploadSliceToCanvas);
        renderROIAsOverlayViewMenuItem.addActionListener(this::uploadSliceToCanvas);

        // View menu for general display
        {
            JIPipeDesktopRibbon.Task viewTask = ribbon.addTask("View");
            JIPipeDesktopRibbon.Band renderingBand = viewTask.addBand("Rendering");

            renderingBand.add(displayROIViewMenuItem);

            renderingBand.add(renderROIAsOverlayViewMenuItem);
            renderingBand.add(new JIPipeDesktopSmallButtonRibbonAction("More settings ...", "Opens more rendering settings", UIUtils.getIconFromResources("actions/configure.png"), this::openRoiDrawingSettings));
            renderingBand.add(new JIPipeDesktopSmallButtonRibbonAction("Save settings", "Saves the current settings as default", UIUtils.getIconFromResources("actions/filesave.png"), this::saveDefaults));
        }

        // Filter task
        {
            JIPipeDesktopRibbon.Task filterTask = ribbon.addTask("Filter");
            JIPipeDesktopRibbon.Band listBand = filterTask.addBand("List");
            JIPipeDesktopRibbon.Band roiBand = filterTask.addBand("ROI");

            // List band
            listBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Hide invisible", "Show only visible ROI in list", UIUtils.getIconFromResources("actions/eye-slash.png"), filterListHideInvisible, (toggle) -> {
                filterListHideInvisible = toggle.isSelected();
                updateListModel();
            }));
            listBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Only selection", "Show only ROI that are selected", UIUtils.getIconFromResources("actions/edit-select-all.png"), filterListOnlySelected, (toggle) -> {
                filterListOnlySelected = toggle.isSelected();
                updateListModel();
            }));

            // ROI band
            roiBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Ignore Z", "If enabled, ROI ignore the Z axis", UIUtils.getIconFromResources("actions/layer-flatten-z.png"), roiDrawer.isIgnoreZ(), (toggle) -> {
                roiDrawer.setIgnoreZ(toggle.isSelected());
                uploadSliceToCanvas();
            }));
            roiBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Ignore C", "If enabled, ROI ignore the channel axis", UIUtils.getIconFromResources("actions/layer-flatten-c.png"), roiDrawer.isIgnoreC(), (toggle) -> {
                roiDrawer.setIgnoreC(toggle.isSelected());
                uploadSliceToCanvas();
            }));
            roiBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Ignore T", "If enabled, ROI ignore the time/frame axis", UIUtils.getIconFromResources("actions/layer-flatten-t.png"), roiDrawer.isIgnoreT(), (toggle) -> {
                roiDrawer.setIgnoreT(toggle.isSelected());
                uploadSliceToCanvas();
            }));
        }

        // Select/Edit task
        {
            JIPipeDesktopRibbon.Task selectionTask = ribbon.addTask("Selection");
            JIPipeDesktopRibbon.Band generalBand = selectionTask.addBand("General");
            JIPipeDesktopRibbon.Band modifyBand = selectionTask.addBand("Modify");
            JIPipeDesktopRibbon.Band measureBand = selectionTask.addBand("Measure");

            ROIPicker2DTool pickerTool = new ROIPicker2DTool(this);
            JIPipeDesktopLargeToggleButtonRibbonAction pickerToggle = new JIPipeDesktopLargeToggleButtonRibbonAction("Pick", "Allows to select ROI via the mouse", UIUtils.getIcon32FromResources("actions/followmouse.png"));
            pickerTool.addToggleButton(pickerToggle.getButton(), getViewerPanel2D().getCanvas());
            generalBand.add(pickerToggle);

            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Select all", "Selects all ROI", UIUtils.getIconFromResources("actions/edit-select-all.png"), this::selectAll));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Clear selection", "Deselects all ROI", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::selectNone));
            generalBand.add(new JIPipeDesktopSmallButtonRibbonAction("Invert selection", "Inverts the current selection", UIUtils.getIconFromResources("actions/edit-select-none.png"), this::invertSelection));

            modifyBand.add(new JIPipeDesktopSmallButtonRibbonAction("Delete", "Deletes the selected ROI", UIUtils.getIconFromResources("actions/delete.png"), () -> removeSelectedROIs(false)));

            JIPipeDesktopSmallButtonRibbonAction modifyEditAction = new JIPipeDesktopSmallButtonRibbonAction("Modify", "Modifies the selected ROI", UIUtils.getIconFromResources("actions/edit.png"), () -> {
            });
            JPopupMenu modifyEditMenu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToButton(modifyEditAction.getButton(), modifyEditMenu, () -> reloadEditRoiMenu(modifyEditMenu));
            modifyBand.add(modifyEditAction);

            measureBand.add(new JIPipeDesktopSmallButtonRibbonAction("Metadata", "Shows the metadata of the selected ROI as table", UIUtils.getIconFromResources("actions/tag.png"), this::showSelectedROIMetadata));

            JIPipeDesktopSmallButtonRibbonAction measureAction = new JIPipeDesktopSmallButtonRibbonAction("Measure", "Measures the ROI and displays the results as table", UIUtils.getIconFromResources("actions/statistics.png"), this::measureSelectedROI);
            measureBand.add(measureAction);
            measureBand.add(new JIPipeDesktopSmallButtonRibbonAction("Settings ...", "Opens the measurement settings", UIUtils.getIconFromResources("actions/configure.png"), this::openMeasurementSettings));

        }

        // Import/Export task
        {
            JIPipeDesktopRibbon.Task importExportTask = ribbon.addTask("Import/Export");
            JIPipeDesktopRibbon.Band imageJBand = importExportTask.addBand("ImageJ");
            JIPipeDesktopRibbon.Band fileBand = importExportTask.addBand("File");

            imageJBand.add(new JIPipeDesktopLargeButtonRibbonAction("To ROI Manager", "Exports the ROI into the ImageJ ROI manager", UIUtils.getIcon32FromResources("apps/imagej2.png"), this::exportROIsToManager));

            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Import from file", "Imports ROI from a *.roi or *.zip file", UIUtils.getIconFromResources("actions/fileopen.png"), this::importROIsFromFile));
            fileBand.add(new JIPipeDesktopSmallButtonRibbonAction("Export to file", "Exports ROI to a *.zip file", UIUtils.getIconFromResources("actions/filesave.png"), this::exportROIsToFile));
        }
    }

    private void openMeasurementSettings() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getViewerPanel()));
        dialog.setTitle("Measurement settings");
        dialog.setContentPane(new JIPipeDesktopParameterFormPanel(new JIPipeDesktopDummyWorkbench(), Measurement2DSettings.INSTANCE, null, JIPipeDesktopFormPanel.WITH_SCROLLING));
        UIUtils.addEscapeListener(dialog);
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(getViewerPanel());
        dialog.revalidate();
        dialog.repaint();
        dialog.setVisible(true);
    }

    private void showSelectedROIMetadata() {
        ROI2DListData rois = getSelectedROIOrAll("Show metadata", "Please select which ROI metadata you want displayed");
        ResultsTableData table = new ResultsTableData();
        table.addStringColumn("ROI Name");
        table.addStringColumn("ROI Index");
        for (int i = 0; i < rois.size(); i++) {
            Roi roi = rois.get(i);
            Map<String, String> map = ImageJUtils.getRoiProperties(roi);
            int row = table.addRow();
            table.setValueAt(StringUtils.orElse(roi.getName(), "Unnamed"), row, "ROI Name");
            table.setValueAt(i, row, "ROI Index");
            for (Map.Entry<String, String> entry : map.entrySet()) {
                table.setValueAt(entry.getValue(), row, entry.getKey());
            }
        }
        JIPipeDesktopTableEditor.openWindow(getViewerPanel().getDesktopWorkbench(), table, "ROI metadata");
    }

    private void measureSelectedROI() {
        ROI2DListData data = getSelectedROIOrAll("Measure", "Please select which ROI you want to measure");
        Measurement2DSettings settings = Measurement2DSettings.INSTANCE;
        ResultsTableData measurements = data.measure(ImageJUtils.duplicate(getViewerPanel().getImagePlus()),
                settings.getStatistics(), true, settings.isMeasureInPhysicalUnits());
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
            ImageViewerUIROI2DDisplayApplicationSettings settings = ImageViewerUIROI2DDisplayApplicationSettings.getInstance();
            settings.getRoiDrawer().copyFrom(roiDrawer);
            settings.setRenderROIAsOverlay(renderROIAsOverlayViewMenuItem.getState());
            settings.setShowROI(displayROIViewMenuItem.getState());
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
        }
    }

    private void openRoiDrawingSettings() {
        JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), getViewerPanel(), roiDrawer, new MarkdownText("# ROI display settings\n\nPlease use the settings on the left to modify how ROI are visualized."), "ROI display settings", JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
        uploadSliceToCanvas();
    }

    private void importROIsFromFile() {
        Path path = JIPipeFileChooserApplicationSettings.openFile(getViewerPanel(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Import ROI", UIUtils.EXTENSION_FILTER_ROIS);
        if (path != null) {
            ROI2DListData importedROIs = ROI2DListData.loadRoiListFromFile(path);
            importROIs(importedROIs, false);
        }
    }

    private void exportROIsToFile() {
        ROI2DListData result = getSelectedROIOrAll("Export ROI", "Do you want to export all ROI or only the selected ones?");
        if (result != null) {
            exportROIsToFile(result);
        }
    }

    private void exportROIsToFile(ROI2DListData rois) {
        FileNameExtensionFilter[] fileNameExtensionFilters;
        if (rois.size() == 1) {
            fileNameExtensionFilters = new FileNameExtensionFilter[]{UIUtils.EXTENSION_FILTER_ROI, UIUtils.EXTENSION_FILTER_ROI_ZIP};
        } else {
            fileNameExtensionFilters = new FileNameExtensionFilter[]{UIUtils.EXTENSION_FILTER_ROI_ZIP};
        }
        Path path = JIPipeFileChooserApplicationSettings.saveFile(getViewerPanel(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export ROI", fileNameExtensionFilters);
        if (path != null) {
            rois.save(path);
        }
    }

    @Override
    public ImageProcessor draw(int c, int z, int t, ImageProcessor processor) {
        if (displayROIViewMenuItem.getState() && !renderROIAsOverlayViewMenuItem.getState()) {
            if (!rois.isEmpty()) {
                processor = new ColorProcessor(processor.getBufferedImage());
                roiDrawer.drawOnProcessor(rois, (ColorProcessor) processor, new ImageSliceIndex(c, z, t), new HashSet<>(roiListControl.getSelectedValuesList()));
            }
        }
        return processor;
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (displayROIViewMenuItem.getState() && renderROIAsOverlayViewMenuItem.getState()) {
            for (int i = 0; i < rois.size(); i++) {
                Roi roi = rois.get(i);
                ImageJUtils.setRoiCanvas(roi, getCurrentImagePlus(), getViewerPanel2D().getZoomedDummyCanvas());
            }
            roiDrawer.drawOverlayOnGraphics(rois, graphics2D, renderArea, sliceIndex, new HashSet<>(roiListControl.getSelectedValuesList()), getViewerPanel2D().getCanvas().getZoom());
        }
    }

    public RoiDrawer getRoiDrawer() {
        return roiDrawer;
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex, double magnification) {
        if (displayROIViewMenuItem.getState() && renderROIAsOverlayViewMenuItem.getState()) {
            Graphics2D graphics = image.createGraphics();
            ROI2DListData copy = new ROI2DListData();
            ImageCanvas canvas = ImageJUtils.createZoomedDummyCanvas(getCurrentImagePlus(), magnification);
            for (Roi roi : rois) {
                Roi clone = (Roi) roi.clone();
                ImageJUtils.setRoiCanvas(clone, getCurrentImagePlus(), canvas);
                copy.add(clone);
            }
            roiDrawer.drawOverlayOnGraphics(copy, graphics, new Rectangle(0, 0, image.getWidth(), image.getHeight()), sliceIndex, new HashSet<>(roiListControl.getSelectedValuesList()), magnification);
            graphics.dispose();
        }
    }

    @Override
    public void onSliceChanged(boolean deferUploadSlice) {
        updateListModel(deferUploadSlice, Collections.emptySet());
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
        List<Roi> selectedRois = roiListControl.getSelectedValuesList();
        menu.removeAll();
        if (selectedRois.isEmpty()) {
            JMenuItem noSelection = new JMenuItem("No ROI selected");
            noSelection.setEnabled(false);
            menu.add(noSelection);
            return;
        }

        Color currentStrokeColor = selectedRois.stream().map(Roi::getStrokeColor).filter(Objects::nonNull).findAny().orElse(Color.YELLOW);
        JMenuItem setLineColorItem = new JMenuItem("Set line color ...", new SolidColorIcon(16, 16, currentStrokeColor));
        setLineColorItem.addActionListener(e -> {
            Color value = JColorChooser.showDialog(getViewerPanel(), "Set line color", currentStrokeColor);
            if (value != null) {
                for (Roi roi : selectedRois) {
                    roi.setStrokeColor(value);
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setLineColorItem);

        Color currentFillColor = selectedRois.stream().map(Roi::getFillColor).filter(Objects::nonNull).findAny().orElse(Color.RED);
        JMenuItem setFillColorItem = new JMenuItem("Set fill color ...", new SolidColorIcon(16, 16, currentFillColor));
        setFillColorItem.addActionListener(e -> {
            Color value = JColorChooser.showDialog(getViewerPanel(), "Set fill color", currentFillColor);
            if (value != null) {
                for (Roi roi : selectedRois) {
                    roi.setFillColor(value);
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setFillColorItem);

        int currentStrokeThickness = Math.max(1, selectedRois.stream().map(Roi::getStrokeWidth).min(Comparator.naturalOrder()).get().intValue());
        JMenuItem setStrokeThicknessItem = new JMenuItem("Set line width ...", UIUtils.getIconFromResources("actions/transform-affect-stroke.png"));
        setStrokeThicknessItem.addActionListener(e -> {
            Optional<Integer> value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set line width", "Please put the line width here:", currentStrokeThickness, 1, Integer.MAX_VALUE);
            if (value.isPresent()) {
                for (Roi roi : selectedRois) {
                    roi.setStrokeWidth(value.get());
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setStrokeThicknessItem);

        String currentName = selectedRois.stream().map(Roi::getName).filter(Objects::nonNull).findAny().orElse("");
        JMenuItem setNameItem = new JMenuItem("Set name ...", UIUtils.getIconFromResources("actions/tag.png"));
        setNameItem.addActionListener(e -> {
            String value = JOptionPane.showInputDialog(getViewerPanel(), "Please set the name of the ROIs:", currentName);
            if (value != null) {
                for (Roi roi : selectedRois) {
                    roi.setName(value);
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setNameItem);

        menu.addSeparator();

        int currentZPosition = Math.max(0, selectedRois.stream().map(Roi::getZPosition).min(Comparator.naturalOrder()).get());
        JMenuItem setZPositionItem = new JMenuItem("Set Z position ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setZPositionItem.addActionListener(e -> {
            Optional<Integer> value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set Z position", "The first index is 1. Set it to zero to make the ROI appear on all Z planes.", currentZPosition, 0, Integer.MAX_VALUE);
            if (value.isPresent()) {
                for (Roi roi : selectedRois) {
                    roi.setPosition(roi.getCPosition(), value.get(), roi.getTPosition());
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setZPositionItem);

        int currentCPosition = Math.max(0, selectedRois.stream().map(Roi::getZPosition).min(Comparator.naturalOrder()).get());
        JMenuItem setCPositionItem = new JMenuItem("Set C position ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setCPositionItem.addActionListener(e -> {
            Optional<Integer> value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set C position", "The first index is 1. Set it to zero to make the ROI appear on all channel planes.", currentCPosition, 0, Integer.MAX_VALUE);
            if (value.isPresent()) {
                for (Roi roi : selectedRois) {
                    roi.setPosition(value.get(), roi.getZPosition(), roi.getTPosition());
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setCPositionItem);

        int currentTPosition = Math.max(0, selectedRois.stream().map(Roi::getZPosition).min(Comparator.naturalOrder()).get());
        JMenuItem setTPositionItem = new JMenuItem("Set T position ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setTPositionItem.addActionListener(e -> {
            Optional<Integer> value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set T position", "The first index is 1. Set it to zero to make the ROI appear on all frame planes.", currentTPosition, 0, Integer.MAX_VALUE);
            if (value.isPresent()) {
                for (Roi roi : selectedRois) {
                    roi.setPosition(roi.getCPosition(), roi.getZPosition(), value.get());
                }
                roiListControl.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setTPositionItem);
    }

    private void initialize() {
        // Setup ROI list
        roiListControl.setCellRenderer(new RoiListCellRenderer());
        roiListControl.addListSelectionListener(e -> {
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

    public void importROIs(ROI2DListData rois, boolean deferUploadSlice) {
        for (Roi roi : rois) {
            Roi clone = (Roi) roi.clone();
            ImageJUtils.setRoiCanvas(clone, getCurrentImagePlus(), getViewerPanel2D().getZoomedDummyCanvas());
            this.rois.add(clone);
        }
        updateListModel(deferUploadSlice, Collections.emptySet());
        uploadSliceToCanvas();
    }

    public void removeSelectedROIs(boolean deferUploadSlice) {
        rois.removeAll(roiListControl.getSelectedValuesList());
        updateListModel(deferUploadSlice, Collections.emptySet());
    }

    public void clearROIs(boolean deferUploadSlice) {
        rois.clear();
        updateListModel(deferUploadSlice, Collections.emptySet());
    }

    public ROI2DListData getRois() {
        return rois;
    }

    public void setRois(ROI2DListData rois, boolean deferUploadSlice) {
        this.rois = rois;
        updateListModel(deferUploadSlice, Collections.emptySet());
    }

    public void exportROIsToManager() {
        ROI2DListData rois = getSelectedROIOrAll("Export ROI to ImageJ", "Please select which ROI should be exported.");
        if (rois != null) {
            exportROIsToManager(rois);
        }
    }

    public void exportROIsToManager(ROI2DListData rois) {
        rois.addToRoiManager(RoiManager.getRoiManager());
    }

    public void importROIsFromManager(boolean deferUploadSlice) {
        for (Roi roi : RoiManager.getRoiManager().getRoisAsArray()) {
            rois.add((Roi) roi.clone());
        }
        updateListModel(deferUploadSlice, Collections.emptySet());
    }

    public boolean isFilterListHideInvisible() {
        return filterListHideInvisible;
    }

    public void setFilterListHideInvisible(boolean filterListHideInvisible) {
        this.filterListHideInvisible = filterListHideInvisible;
        updateListModel();
    }

    public boolean isFilterListOnlySelected() {
        return filterListOnlySelected;
    }

    public void setFilterListOnlySelected(boolean filterListOnlySelected) {
        this.filterListOnlySelected = filterListOnlySelected;
        updateListModel();
    }

    public void updateListModel() {
        updateListModel(false, Collections.emptySet());
    }

    public void updateListModel(boolean deferUploadSlice, Collection<Roi> excludeFromFilter) {
        DefaultListModel<Roi> model = new DefaultListModel<>();
        List<Roi> selectedValuesList = roiListControl.getSelectedValuesList();
        ImageSliceIndex currentIndex = getCurrentSlicePosition();
        for (Roi roi : rois) {
            boolean excluded = excludeFromFilter.contains(roi);
            if (!excluded && (filterListHideInvisible && !ROI2DListData.isVisibleIn(roi, currentIndex, roiDrawer.isIgnoreZ(), roiDrawer.isIgnoreC(), roiDrawer.isIgnoreT())))
                continue;
            if (!excluded && !selectedValuesList.isEmpty() && (filterListOnlySelected && !selectedValuesList.contains(roi)))
                continue;
            model.addElement(roi);
        }
        roiListControl.setModel(model);
        setSelectedROI(selectedValuesList, false);
        updateContextPanels();
        if (!deferUploadSlice)
            uploadSliceToCanvas();
    }

    private void updateContextPanels() {
        List<Roi> selectedValuesList = roiListControl.getSelectedValuesList();
        for (ROIManagerPlugin2DSelectionContextPanel selectionContextPanel : selectionContextPanels) {
            selectionContextPanel.selectionUpdated(rois, selectedValuesList);
        }
    }

    public JList<Roi> getRoiListControl() {
        return roiListControl;
    }

    private void selectAll() {
        roiListControl.setSelectionInterval(0, roiListControl.getModel().getSize() - 1);
    }

    public void setSelectedROI(Collection<Roi> select, boolean force) {
        TIntList indices = new TIntArrayList();
        DefaultListModel<Roi> model = (DefaultListModel<Roi>) roiListControl.getModel();

        if (force) {
            boolean rebuild = false;
            for (Roi roi : select) {
                if (rois.contains(roi) && !model.contains(roi)) {
                    rebuild = true;
                    break;
                }
            }
            if (rebuild) {
                roiListControl.clearSelection();
                updateListModel(false, select);
                model = (DefaultListModel<Roi>) roiListControl.getModel();
            }
        }

        for (Roi roi : select) {
            int i = model.indexOf(roi);
            if (i >= 0) {
                indices.add(i);
            }
        }
        roiListControl.setSelectedIndices(indices.toArray());
    }
}
