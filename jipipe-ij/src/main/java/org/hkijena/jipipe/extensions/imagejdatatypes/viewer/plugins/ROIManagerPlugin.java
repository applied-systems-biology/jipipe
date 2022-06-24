package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import com.google.common.primitives.Ints;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ROIEditor;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.RoiListCellRenderer;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ROIManagerPlugin extends ImageViewerPanelPlugin {
    private final ROIListData overlayRois = new ROIListData();
    private final JList<Roi> roiJList = new JList<>();
    private final JLabel roiInfoLabel = new JLabel();
    private ROIListData rois = new ROIListData();
    private boolean roiSeeThroughZ = false;
    private boolean roiSeeThroughC = false;
    private boolean roiSeeThroughT = false;
    private boolean roiDrawOutline = true;
    private boolean roiFillOutline = false;
    private boolean roiDrawLabels = false;
    private boolean roiFilterList = false;

    public ROIManagerPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        initialize();
    }


    @Override
    public void onImageChanged() {
        for (Roi roi : overlayRois) {
            rois.remove(roi);
        }
        if (getCurrentImage() != null && getCurrentImage().getOverlay() != null) {
            if (getCurrentImage().getRoi() != null) {
                rois.add(getCurrentImage().getRoi());
            }
            for (Roi roi : getCurrentImage().getOverlay()) {
                rois.add(roi);
                overlayRois.add(roi);
            }
        }
        updateROIJList(true);
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
        createSelectionMenu(listMenuBar);
        createImportExportMenu(listMenuBar);

        listMenuBar.add(Box.createHorizontalGlue());
        createButtons(listMenuBar);

        JScrollPane scrollPane = new JScrollPane(roiJList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Info (bottom toolbar)
        createInfoToolbar(panel);

        formPanel.addVerticalGlue(panel, null);
    }

    private void createImportExportMenu(JMenuBar menuBar) {
        JMenu ioMenu = new JMenu("Import/Export");
        menuBar.add(ioMenu);
        // Import items
        {
            JMenuItem item = new JMenuItem("Import from file", UIUtils.getIconFromResources("actions/fileopen.png"));
            item.addActionListener(e -> importROIsFromFile());
            ioMenu.add(item);
        }
        // Separator
        ioMenu.addSeparator();
        // Export items
        {
            JMenuItem item = new JMenuItem("Export to ImageJ ROI Manager", UIUtils.getIconFromResources("apps/imagej.png"));
            item.addActionListener(e -> {
                if (rois.isEmpty()) {
                    JOptionPane.showMessageDialog(getViewerPanel(), "No ROI to export.", "Export ROI", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ROIListData result = getSelectedROIOrAll("Export ROI", "Do you want to export all ROI or only the selected ones?");
                if (result != null) {
                    exportROIsToManager(result);
                }
            });
            ioMenu.add(item);
        }
        {
            JMenuItem item = new JMenuItem("Export to file", UIUtils.getIconFromResources("actions/save.png"));
            item.addActionListener(e -> {
                if (rois.isEmpty()) {
                    JOptionPane.showMessageDialog(getViewerPanel(), "No ROI to export.", "Export ROI", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ROIListData result = getSelectedROIOrAll("Export ROI", "Do you want to export all ROI or only the selected ones?");
                if (result != null) {
                    exportROIsToFile(result);
                }
            });
            ioMenu.add(item);
        }
    }

    public ROIListData getSelectedROIOrAll(String title, String message) {
        if (!roiJList.getSelectedValuesList().isEmpty()) {
            int result = JOptionPane.showOptionDialog(getViewerPanel(),
                    message,
                    title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"All ROI (" + rois.size() + ")", "Selected ROI (" + roiJList.getSelectedValuesList().size() + ")", "Cancel"},
                    "All ROI (" + rois.size() + ")");
            if (result == JOptionPane.CANCEL_OPTION)
                return null;
            else if (result == JOptionPane.YES_OPTION)
                return rois;
            else
                return new ROIListData(roiJList.getSelectedValuesList());
        }
        return rois;
    }

    private void createSelectionMenu(JMenuBar menuBar) {
        JMenu selectionMenu = new JMenu("Selection");
        menuBar.add(selectionMenu);

        {
            JMenuItem selectAllButton = new JMenuItem("Select all", UIUtils.getIconFromResources("actions/edit-select-all.png"));
            selectAllButton.addActionListener(e -> {
                roiJList.setSelectionInterval(0, roiJList.getModel().getSize() - 1);
            });
            selectionMenu.add(selectAllButton);
        }
        {
            JMenuItem deselectAllButton = new JMenuItem("Clear selection", UIUtils.getIconFromResources("actions/edit-select-none.png"));
            deselectAllButton.addActionListener(e -> {
                roiJList.clearSelection();
            });
            selectionMenu.add(deselectAllButton);
        }
        {
            JMenuItem invertSelectionButton = new JMenuItem("Invert selection", UIUtils.getIconFromResources("actions/object-inverse.png"));
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
            selectionMenu.add(invertSelectionButton);
        }
    }

    private void createInfoToolbar(JPanel panel) {
        JToolBar infoToolBar = new JToolBar();
        infoToolBar.setFloatable(false);

        roiInfoLabel.setIcon(UIUtils.getIconFromResources("data-types/roi.png"));
        roiInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        infoToolBar.add(roiInfoLabel);
        infoToolBar.add(Box.createHorizontalGlue());

        panel.add(infoToolBar, BorderLayout.SOUTH);
    }

    private void createButtons(JMenuBar menuBar) {
        {
            JButton removeButton = new JButton("Delete", UIUtils.getIconFromResources("actions/delete.png"));
            removeButton.setToolTipText("Remove selected ROIs");
            removeButton.addActionListener(e -> {
                if (roiJList.getSelectedValuesList().isEmpty())
                    return;
                if (JOptionPane.showConfirmDialog(getViewerPanel(), "Do you really want to remove " + roiJList.getSelectedValuesList().size() + "ROI?", "Edit ROI", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    removeSelectedROIs(false);
                }
            });
            menuBar.add(removeButton);
        }
        {
            JButton editButton = new JButton("Edit", UIUtils.getIconFromResources("actions/edit.png"));
            JPopupMenu editMenu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToComponent(editButton, editMenu, () -> reloadEditRoiMenu(editMenu));
            menuBar.add(editButton);
        }
    }

    private void editROIInDialog() {
        List<Roi> selected = roiJList.getSelectedValuesList();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(getViewerPanel(), "You have not selected ROI to edit.", "Edit ROI", JOptionPane.ERROR_MESSAGE);
        }
        if (selected.size() > 5) {
            if (JOptionPane.showConfirmDialog(getViewerPanel(), "Do you really want to edit " + selected.size() + "ROI?", "Edit ROI", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        DocumentTabPane documentTabPane = new DocumentTabPane();
        List<ROIEditor> editors = new ArrayList<>();
        for (Roi roi : selected) {
            ROIEditor editor = new ROIEditor(roi);
            ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), editor, new MarkdownDocument("# Edit ROI"), ParameterPanel.WITH_SEARCH_BAR | FormPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION);
            documentTabPane.addTab(roi.getName() + "", UIUtils.getIconFromResources("data-types/roi.png"), parameterPanel, DocumentTabPane.CloseMode.withoutCloseButton, true);
            editors.add(editor);
        }
        if (UIUtils.showOKCancelDialog(getViewerPanel(), documentTabPane, "Edit ROI")) {
            for (int i = 0; i < selected.size(); i++) {
                Roi roi = editors.get(i).applyToRoi(selected.get(i));
                rois.set(i, roi);
            }
            updateROIJList(false);
        }
    }

    private void createViewMenu(JMenuBar menuBar) {
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        {
            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Show only visible ROI in list", UIUtils.getIconFromResources("actions/eye.png"));
            toggle.setSelected(roiFilterList);
            toggle.addActionListener(e -> {
                roiFilterList = toggle.isSelected();
                updateROIJList(false);
            });
            viewMenu.add(toggle);
        }
        viewMenu.addSeparator();
        {
            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Draw ROI outline", UIUtils.getIconFromResources("actions/object-stroke.png"));
            toggle.setSelected(roiDrawOutline);
            toggle.addActionListener(e -> {
                roiDrawOutline = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewMenu.add(toggle);
        }
        {
            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Fill ROI outline", UIUtils.getIconFromResources("actions/object-fill.png"));
            toggle.setSelected(roiFillOutline);
            toggle.addActionListener(e -> {
                roiFillOutline = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewMenu.add(toggle);
        }
        {
            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Draw ROI labels", UIUtils.getIconFromResources("actions/edit-select-text.png"));
            toggle.setSelected(roiDrawLabels);
            toggle.addActionListener(e -> {
                roiDrawLabels = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewMenu.add(toggle);
        }
        viewMenu.addSeparator();
        {
            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Draw ROI: Ignore Z axis", UIUtils.getIconFromResources("actions/layer-flatten-z.png"));
            toggle.setSelected(roiSeeThroughZ);
            toggle.addActionListener(e -> {
                roiSeeThroughZ = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewMenu.add(toggle);
        }
        {
            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Draw ROI: Ignore time/frame axis", UIUtils.getIconFromResources("actions/layer-flatten-t.png"));
            toggle.setSelected(roiSeeThroughT);
            toggle.addActionListener(e -> {
                roiSeeThroughT = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewMenu.add(toggle);
        }
        {
            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Draw ROI: Ignore channel axis", UIUtils.getIconFromResources("actions/layer-flatten-c.png"));
            toggle.setSelected(roiSeeThroughC);
            toggle.addActionListener(e -> {
                roiSeeThroughC = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewMenu.add(toggle);
        }
    }

    private void importROIsFromFile() {
        Path path = FileChooserSettings.openFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Import ROI", UIUtils.EXTENSION_FILTER_ROIS);
        if (path != null) {
            ROIListData importedROIs = ROIListData.loadRoiListFromFile(path);
            importROIs(importedROIs, false);
        }
    }

    private void exportROIsToFile(ROIListData rois) {
        FileNameExtensionFilter[] fileNameExtensionFilters;
        if (rois.size() == 1) {
            fileNameExtensionFilters = new FileNameExtensionFilter[]{UIUtils.EXTENSION_FILTER_ROI, UIUtils.EXTENSION_FILTER_ROI_ZIP};
        } else {
            fileNameExtensionFilters = new FileNameExtensionFilter[]{UIUtils.EXTENSION_FILTER_ROI_ZIP};
        }
        Path path = FileChooserSettings.saveFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Data, "Export ROI", fileNameExtensionFilters);
        if (path != null) {
            rois.save(path);
        }
    }

    @Override
    public ImageProcessor draw(int c, int z, int t, ImageProcessor processor) {
        if (!rois.isEmpty()) {
            processor = new ColorProcessor(processor.getBufferedImage());
            rois.draw(processor, new ImageSliceIndex(c, z, t),
                    roiSeeThroughZ,
                    roiSeeThroughC,
                    roiSeeThroughT,
                    roiDrawOutline,
                    roiFillOutline,
                    roiDrawLabels,
                    1,
                    Color.RED,
                    Color.YELLOW,
                    roiJList.getSelectedValuesList());
        }
        return processor;
    }

    @Override
    public void onSliceChanged(boolean deferUploadSlice) {
        updateROIJList(deferUploadSlice);
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
        List<Roi> selectedRois = roiJList.getSelectedValuesList();
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
                roiJList.repaint();
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
                roiJList.repaint();
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
                roiJList.repaint();
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
                roiJList.repaint();
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
                roiJList.repaint();
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
                roiJList.repaint();
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
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setTPositionItem);
    }

    private void initialize() {
        // Setup ROI
        roiJList.setCellRenderer(new RoiListCellRenderer());
        roiJList.addListSelectionListener(e -> uploadSliceToCanvas());
    }

    public void importROIs(ROIListData rois, boolean deferUploadSlice) {
        for (Roi roi : rois) {
            this.rois.add((Roi) roi.clone());
        }
        updateROIJList(deferUploadSlice);
        uploadSliceToCanvas();
    }

    public void removeSelectedROIs(boolean deferUploadSlice) {
        rois.removeAll(roiJList.getSelectedValuesList());
        updateROIJList(deferUploadSlice);
    }

    public void clearROIs(boolean deferUploadSlice) {
        rois.clear();
        updateROIJList(deferUploadSlice);
    }

    public ROIListData getRois() {
        return rois;
    }

    public void setRois(ROIListData rois, boolean deferUploadSlice) {
        this.rois = rois;
        updateROIJList(deferUploadSlice);
    }

    public void exportROIsToManager(ROIListData rois) {
        rois.addToRoiManager(RoiManager.getRoiManager());
    }

    public void importROIsFromManager(boolean deferUploadSlice) {
        for (Roi roi : RoiManager.getRoiManager().getRoisAsArray()) {
            rois.add((Roi) roi.clone());
        }
        updateROIJList(deferUploadSlice);
    }

    private void updateROIJList(boolean deferUploadSlice) {
        DefaultListModel<Roi> model = new DefaultListModel<>();
        int[] selectedIndices = roiJList.getSelectedIndices();
        ImageSliceIndex currentIndex = getCurrentSlicePosition();
        for (Roi roi : rois) {
            if (roiFilterList && !ROIListData.isVisibleIn(roi, currentIndex, roiSeeThroughZ, roiSeeThroughC, roiSeeThroughT))
                continue;
            model.addElement(roi);
        }
        roiJList.setModel(model);
        roiJList.setSelectedIndices(selectedIndices);
        roiInfoLabel.setText(rois.size() + " ROI");
        if (!deferUploadSlice)
            uploadSliceToCanvas();
    }
}
