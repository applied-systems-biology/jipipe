package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import com.google.common.primitives.Ints;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.RoiListCellRenderer;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ROIManagerPlugin extends ImageViewerPanelPlugin {
    private final ImageViewerPanel viewerPanel;
    private ROIListData rois = new ROIListData();
    private ROIListData overlayRois = new ROIListData();
    private JList<Roi> roiJList = new JList<>();
    private JLabel roiInfoLabel = new JLabel();
    private boolean roiSeeThroughZ = false;
    private boolean roiSeeThroughC = false;
    private boolean roiSeeThroughT = false;
    private boolean roiDrawOutline = true;
    private boolean roiFillOutline = false;
    private boolean roiDrawLabels = false;
    private boolean roiFilterList = false;

    public ROIManagerPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        this.viewerPanel = viewerPanel;
        initialize();
    }



    @Override
    public void onImageChanged() {
        for (Roi roi : overlayRois) {
            rois.remove(roi);
        }
        if(getCurrentImage().getOverlay() != null) {
            if(getCurrentImage().getRoi() != null) {
                rois.add(getCurrentImage().getRoi());
            }
            for (Roi roi : getCurrentImage().getOverlay()) {
                rois.add(roi);
                overlayRois.add(roi);
            }
        }
        updateROIJList();
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        if (getCurrentImage() == null)
            return;
        FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("ROI", UIUtils.getIconFromResources("data-types/roi.png"));
        JButton importROIsButton = new JButton("Import", UIUtils.getIconFromResources("actions/document-import.png"));
        importROIsButton.setToolTipText("Imports ROIs from the ImageJ ROI manager");
        importROIsButton.addActionListener(e -> importROIs());
        headerPanel.addColumn(importROIsButton);

        JButton exportROIsButton = new JButton(UIUtils.getIconFromResources("actions/document-export.png"));
        exportROIsButton.setToolTipText("Exports ROIs to the ImageJ ROI manager");
        exportROIsButton.addActionListener(e -> exportROIs());
        headerPanel.addColumn(exportROIsButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        JToolBar listToolBar = new JToolBar();
        listToolBar.setFloatable(false);
        panel.add(listToolBar, BorderLayout.NORTH);

        listToolBar.add(roiInfoLabel);
        listToolBar.add(Box.createHorizontalGlue());

        {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/eye.png"));
            toggle.setToolTipText("Show only visible ROI");
            toggle.setSelected(roiFilterList);
            toggle.addActionListener(e -> {
                roiFilterList = toggle.isSelected();
                updateROIJList();
            });
            listToolBar.add(toggle);
        }
        listToolBar.addSeparator();

        JButton selectAllButton = new JButton(UIUtils.getIconFromResources("actions/edit-select-all.png"));
        selectAllButton.setToolTipText("Select all");
        selectAllButton.addActionListener(e -> {
            roiJList.setSelectionInterval(0, roiJList.getModel().getSize() - 1);
        });
        listToolBar.add(selectAllButton);

        JButton deselectAllButton = new JButton(UIUtils.getIconFromResources("actions/edit-select-none.png"));
        deselectAllButton.setToolTipText("Clear selection");
        deselectAllButton.addActionListener(e -> {
            roiJList.clearSelection();
        });
        listToolBar.add(deselectAllButton);

        JButton invertSelectionButton = new JButton(UIUtils.getIconFromResources("actions/object-inverse.png"));
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
        listToolBar.add(invertSelectionButton);

        listToolBar.addSeparator();

        JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.setToolTipText("Remove selected ROIs");
        removeButton.addActionListener(e -> removeSelectedROIs());
        listToolBar.add(removeButton);

        JScrollPane scrollPane = new JScrollPane(roiJList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JToolBar viewToolBar = new JToolBar();
        viewToolBar.setFloatable(false);

        {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/object-stroke.png"));
            toggle.setToolTipText("Draw outline");
            toggle.setSelected(roiDrawOutline);
            toggle.addActionListener(e -> {
                roiDrawOutline = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewToolBar.add(toggle);
        }
        {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/object-fill.png"));
            toggle.setToolTipText("Fill outline");
            toggle.setSelected(roiFillOutline);
            toggle.addActionListener(e -> {
                roiFillOutline = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewToolBar.add(toggle);
        }
        {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/edit-select-text.png"));
            toggle.setToolTipText("Draw labels");
            toggle.setSelected(roiDrawLabels);
            toggle.addActionListener(e -> {
                roiDrawLabels = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewToolBar.add(toggle);
        }

        viewToolBar.addSeparator();

        JButton editButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        JPopupMenu editMenu = new JPopupMenu();
        UIUtils.addReloadablePopupMenuToComponent(editButton, editMenu, () -> reloadEditRoiMenu(editMenu));
        viewToolBar.add(editButton);

        viewToolBar.add(Box.createHorizontalGlue());

        if (getCurrentImage().getNSlices() > 1) {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/layer-flatten-z.png"));
            toggle.setToolTipText("Show all ROIs regardless of Z axis.");
            toggle.setSelected(roiSeeThroughZ);
            toggle.addActionListener(e -> {
                roiSeeThroughZ = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewToolBar.add(toggle);
        }
        if (getCurrentImage().getNFrames() > 1) {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/layer-flatten-t.png"));
            toggle.setToolTipText("Show all ROIs regardless of time axis.");
            toggle.setSelected(roiSeeThroughT);
            toggle.addActionListener(e -> {
                roiSeeThroughT = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewToolBar.add(toggle);
        }
        if (getCurrentImage().getNChannels() > 1) {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/layer-flatten-c.png"));
            toggle.setToolTipText("Show all ROIs regardless of channel axis.");
            toggle.setSelected(roiSeeThroughC);
            toggle.addActionListener(e -> {
                roiSeeThroughC = toggle.isSelected();
                uploadSliceToCanvas();
            });
            viewToolBar.add(toggle);
        }

        panel.add(viewToolBar, BorderLayout.SOUTH);

        formPanel.addWideToForm(panel, null);
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
    public void onSliceChanged() {
        updateROIJList();
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
        JMenuItem setLineColorItem = new JMenuItem("Set line color ...", new ColorIcon(16, 16, currentStrokeColor));
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
        JMenuItem setFillColorItem = new JMenuItem("Set fill color ...", new ColorIcon(16, 16, currentFillColor));
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
            Integer value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set line width", "Please put the line width here:", currentStrokeThickness, 1, Integer.MAX_VALUE);
            if (value != null) {
                for (Roi roi : selectedRois) {
                    roi.setStrokeWidth(value);
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
            Integer value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set Z position", "The first index is 1. Set it to zero to make the ROI appear on all Z planes.", currentZPosition, 0, Integer.MAX_VALUE);
            if (value != null) {
                for (Roi roi : selectedRois) {
                    roi.setPosition(roi.getCPosition(), value, roi.getTPosition());
                }
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setZPositionItem);

        int currentCPosition = Math.max(0, selectedRois.stream().map(Roi::getZPosition).min(Comparator.naturalOrder()).get());
        JMenuItem setCPositionItem = new JMenuItem("Set C position ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setCPositionItem.addActionListener(e -> {
            Integer value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set C position", "The first index is 1. Set it to zero to make the ROI appear on all channel planes.", currentCPosition, 0, Integer.MAX_VALUE);
            if (value != null) {
                for (Roi roi : selectedRois) {
                    roi.setPosition(value, roi.getZPosition(), roi.getTPosition());
                }
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setCPositionItem);

        int currentTPosition = Math.max(0, selectedRois.stream().map(Roi::getZPosition).min(Comparator.naturalOrder()).get());
        JMenuItem setTPositionItem = new JMenuItem("Set T position ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setTPositionItem.addActionListener(e -> {
            Integer value = UIUtils.getIntegerByDialog(getViewerPanel(), "Set T position", "The first index is 1. Set it to zero to make the ROI appear on all frame planes.", currentTPosition, 0, Integer.MAX_VALUE);
            if (value != null) {
                for (Roi roi : selectedRois) {
                    roi.setPosition(roi.getCPosition(), roi.getZPosition(), value);
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

    public void importROIs(ROIListData rois) {
        for (Roi roi : rois) {
            this.rois.add((Roi) roi.clone());
        }
        updateROIJList();
        uploadSliceToCanvas();
    }

    public void removeSelectedROIs() {
        rois.removeAll(roiJList.getSelectedValuesList());
        updateROIJList();
    }

    public void clearROIs() {
        rois.clear();
        updateROIJList();
    }

    public void exportROIs() {
        rois.addToRoiManager(RoiManager.getRoiManager());
    }

    public void importROIs() {
        for (Roi roi : RoiManager.getRoiManager().getRoisAsArray()) {
            rois.add((Roi) roi.clone());
        }
        updateROIJList();
    }

    private void updateROIJList() {
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
        uploadSliceToCanvas();
    }
}
