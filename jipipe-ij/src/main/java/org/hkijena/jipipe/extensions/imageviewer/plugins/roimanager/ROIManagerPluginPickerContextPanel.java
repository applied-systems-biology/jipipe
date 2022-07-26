package org.hkijena.jipipe.extensions.imageviewer.plugins.roimanager;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelCanvas;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelCanvasTool;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.MouseClickedEvent;
import org.hkijena.jipipe.utils.ui.MouseDraggedEvent;
import org.hkijena.jipipe.utils.ui.MouseExitedEvent;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ROIManagerPluginPickerContextPanel extends ROIManagerPluginSelectionContextPanel implements JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();
    private final JToggleButton pickerToolButton = new JToggleButton("Pick", UIUtils.getIconFromResources("actions/followmouse.png"));

    private final ROIPickerTool roiPickerTool;

    public ROIManagerPluginPickerContextPanel(ROIManagerPlugin roiManagerPlugin) {
        super(roiManagerPlugin);
        this.roiPickerTool = new ROIPickerTool(this, roiManagerPlugin);
        initialize();
        roiManagerPlugin.getViewerPanel().getCanvas().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
//        add(Box.createHorizontalGlue());

        pickerToolButton.addActionListener(e->getRoiManagerPlugin().getViewerPanel().getCanvas().setTool(roiPickerTool));
        add(pickerToolButton);

        JList<Roi> roiJList = getRoiManagerPlugin().getRoiListControl();
        {
            JButton selectAllButton = new JButton("Select all", UIUtils.getIconFromResources("actions/edit-select-all.png"));
//                UIUtils.makeFlat25x25(selectAllButton);
            selectAllButton.setToolTipText("Select all");
            selectAllButton.addActionListener(e -> {
                roiJList.setSelectionInterval(0, roiJList.getModel().getSize() - 1);
            });
            add(selectAllButton);
        }
        {
            JButton deselectAllButton = new JButton("Clear selection", UIUtils.getIconFromResources("actions/edit-select-none.png"));
//                UIUtils.makeFlat25x25(deselectAllButton);
            deselectAllButton.setToolTipText("Clear selection");
            deselectAllButton.addActionListener(e -> {
                roiJList.clearSelection();
            });
            add(deselectAllButton);
        }

        JButton moreButton = new JButton("...");
        moreButton.setMaximumSize(new Dimension(Short.MAX_VALUE,
                Short.MAX_VALUE));
        JPopupMenu moreMenu = UIUtils.addPopupMenuToComponent(moreButton);

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
            moreMenu.add(invertSelectionButton);
        }
        add(moreButton);

        JButton settingsButton = new JButton(UIUtils.getIconFromResources("actions/configure.png"));
        settingsButton.setToolTipText("Configure ROI picker");
        UIUtils.makeFlat25x25(settingsButton);
        settingsButton.addActionListener(e -> showSettings());
//        add(settingsButton);
    }

    @Override
    public void selectionUpdated(ROIListData allROI, List<Roi> selectedROI) {
    }

    private void showSettings() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getViewerPanel()));
        dialog.setTitle("Measurement settings");
        dialog.setContentPane(new ParameterPanel(new JIPipeDummyWorkbench(), this, null, FormPanel.WITH_SCROLLING));
        UIUtils.addEscapeListener(dialog);
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(getViewerPanel());
        dialog.revalidate();
        dialog.repaint();
        dialog.setVisible(true);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Subscribe
    public void onToolChanged(ImageViewerPanelCanvas.ToolChangedEvent event) {
        pickerToolButton.setSelected(event.getNewTool() == roiPickerTool);
    }

    public static class ROIPickerTool implements ImageViewerPanelCanvasTool {

        private final ROIManagerPluginPickerContextPanel settings;
        private final ROIManagerPlugin roiManagerPlugin;

        private Point dragStart;

        public ROIPickerTool(ROIManagerPluginPickerContextPanel settings, ROIManagerPlugin roiManagerPlugin) {
            this.settings = settings;
            this.roiManagerPlugin = roiManagerPlugin;
            roiManagerPlugin.getViewerPanel().getCanvas().getEventBus().register(this);
        }

        public ImageViewerPanelCanvas getCanvas() {
            return roiManagerPlugin.getViewerPanel().getCanvas();
        }

        @Override
        public Cursor getToolCursor() {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }

        @Override
        public void onToolActivate(ImageViewerPanelCanvas canvas) {

        }

        @Override
        public void onToolDeactivate(ImageViewerPanelCanvas canvas) {

        }

        @Subscribe
        public void onMouseClick(MouseClickedEvent event) {
            if (!toolIsActive(roiManagerPlugin.getViewerPanel().getCanvas())) {
                return;
            }
            if (SwingUtilities.isLeftMouseButton(event)) {
                dragStart = getCanvas().getMouseModelPixelCoordinate(false);
               pickRoiFromCanvas();
            }
            cancelPicking();
        }

        private void pickRoiFromCanvas() {

        }

        @Subscribe
        public void onMouseExited(MouseExitedEvent event) {
            cancelPicking();
        }

        @Subscribe
        public void onMouseDrag(MouseDraggedEvent event) {

        }

        private void cancelPicking() {
            dragStart = null;
        }

        @Override
        public String getToolName() {
            return "Pick ROI";
        }
    }

}
