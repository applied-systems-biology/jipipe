package org.hkijena.jipipe.extensions.imageviewer.plugins3d;

import ij3d.AxisConstants;
import ij3d.Content;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin3D;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.Image3DRenderType;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import orthoslice.MultiOrthoGroup;
import orthoslice.OrthoGroup;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlicerControlsPlugin3D extends JIPipeImageViewerPlugin3D implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final FormPanel orthoSliceEditor = new FormPanel(FormPanel.NONE);

    private final ParameterPanel multiOrthoSliceEditor;

    private final MultiOrthoSlicerSettings multiOrthoSlicerSettings = new MultiOrthoSlicerSettings();
    private FormPanel.GroupHeaderPanel groupHeaderPanel;

    private OrthoSliderPanel[] orthoSliderPanels;
    private boolean isUpdatingSliders;

    private JIPipeRunnable currentUpdateRun;

    public SlicerControlsPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
        this.multiOrthoSliceEditor = new ParameterPanel(viewerPanel.getWorkbench(), multiOrthoSlicerSettings, null, ParameterPanel.NO_GROUP_HEADERS);
        initializeOrthoSliceEditor();
        initializeMultiOrthoSliceEditor();
    }

    private void initializeMultiOrthoSliceEditor() {
        multiOrthoSlicerSettings.getParameterChangedEventEmitter().subscribe(this);
    }

    private void initializeOrthoSliceEditor() {

        orthoSliderPanels = new OrthoSliderPanel[]{
                new OrthoSliderPanel("X", this, UIUtils.getIconFromResources("actions/3d-plane-yz.png")),
                new OrthoSliderPanel("Y", this, UIUtils.getIconFromResources("actions/3d-plane-xz.png")),
                new OrthoSliderPanel("Z", this, UIUtils.getIconFromResources("actions/3d-plane-xy.png"))
        };

        for (OrthoSliderPanel orthoSliderPanel : orthoSliderPanels) {
            orthoSliderPanel.addToForm(orthoSliceEditor);
            orthoSliderPanel.slider.addChangeListener(e -> {
                if (!isUpdatingSliders)
                    updateOrthoSlice();
            });
            orthoSliderPanel.visibleButton.addActionListener(e -> {
                if (!isUpdatingSliders)
                    updateOrthoSlice();
            });
        }
    }

    private void updateMultiOrthoSlice() {
        ImagePlusData image = getCurrentImage();
        if (image != null && getViewerPanel3D().getUniverse() != null && getViewerPanel3D().getCurrentImageContents() != null && getViewerPanel3D().getImage3DRendererSettings().getRenderType() == Image3DRenderType.MultiOrthoSlices) {
            List<Integer> xSlices = multiOrthoSlicerSettings.x.tryGetIntegers(1, image.getWidth(), new JIPipeExpressionVariablesMap());
            List<Integer> ySlices = multiOrthoSlicerSettings.y.tryGetIntegers(1, image.getHeight(), new JIPipeExpressionVariablesMap());
            List<Integer> zSlices = multiOrthoSlicerSettings.z.tryGetIntegers(1, image.getNSlices(), new JIPipeExpressionVariablesMap());
            if (xSlices != null) {
                xSlices.removeIf(value -> value < 1 || value > image.getWidth());
            } else {
                xSlices = new ArrayList<>();
            }
            if (ySlices != null) {
                ySlices.removeIf(value -> value < 1 || value > image.getHeight());
            } else {
                ySlices = new ArrayList<>();
            }
            if (zSlices != null) {
                zSlices.removeIf(value -> value < 1 || value > image.getNSlices());
            } else {
                zSlices = new ArrayList<>();
            }
            if (xSlices.isEmpty() && ySlices.isEmpty() && zSlices.isEmpty()) {
                xSlices = Collections.singletonList(image.getWidth() / 2 + 1);
                ySlices = Collections.singletonList(image.getHeight() / 2 + 1);
                zSlices = Collections.singletonList(image.getNSlices() / 2 + 1);
            }

            if (currentUpdateRun != null) {
                getViewerPanel3D().getViewerRunnerQueue().cancel(currentUpdateRun);
                currentUpdateRun = null;
            }

            currentUpdateRun = new ApplyMultiOrthoSlicerRun(getViewerPanel3D().getCurrentImageContents(),
                    getViewerPanel3D().getCurrentImageContentsResamplingFactor(),
                    xSlices,
                    ySlices,
                    zSlices,
                    multiOrthoSlicerSettings.opaqueTextures);
            getViewerPanel3D().getViewerRunnerQueue().enqueue(currentUpdateRun);
        }
    }

    private void updateOrthoSlice() {
        if (getCurrentImage() != null && getViewerPanel3D().getUniverse() != null && getViewerPanel3D().getCurrentImageContents() != null && getViewerPanel3D().getImage3DRendererSettings().getRenderType() == Image3DRenderType.OrthoSlice) {
            int x = Math.max(1, Math.min(getCurrentImage().getWidth(), orthoSliderPanels[0].getValue())) - 1;
            int y = Math.max(1, Math.min(getCurrentImage().getHeight(), orthoSliderPanels[1].getValue())) - 1;
            int z = Math.max(1, Math.min(getCurrentImage().getNSlices(), orthoSliderPanels[2].getValue())) - 1;

            if (currentUpdateRun != null) {
                getViewerPanel3D().getViewerRunnerQueue().cancel(currentUpdateRun);
                currentUpdateRun = null;
            }

            currentUpdateRun = new ApplyOrthoSlicerRun(getViewerPanel3D().getCurrentImageContents(),
                    getViewerPanel3D().getCurrentImageContentsResamplingFactor(),
                    x,
                    y,
                    z,
                    orthoSliderPanels[0].isPlaneVisible(),
                    orthoSliderPanels[1].isPlaneVisible(),
                    orthoSliderPanels[2].isPlaneVisible());
            getViewerPanel3D().getViewerRunnerQueue().enqueue(currentUpdateRun);
        }
    }

    @Override
    public String getCategory() {
        return "Display";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("devices/video-display.png");
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        groupHeaderPanel = formPanel.addGroupHeader("Ortho-slicer", UIUtils.getIconFromResources("actions/layer-visible-off.png"));

        formPanel.addWideToForm(orthoSliceEditor);
        formPanel.addWideToForm(multiOrthoSliceEditor);

        updateUIVisibility();
    }

    private void updateUIVisibility() {
        switch (getViewerPanel3D().getImage3DRendererSettings().getRenderType()) {
            case OrthoSlice: {
                groupHeaderPanel.setVisible(true);
                orthoSliceEditor.setVisible(true);
                multiOrthoSliceEditor.setVisible(false);
            }
            break;
            case MultiOrthoSlices: {
                groupHeaderPanel.setVisible(true);
                orthoSliceEditor.setVisible(false);
                multiOrthoSliceEditor.setVisible(true);
            }
            break;
            default: {
                groupHeaderPanel.setVisible(false);
                orthoSliceEditor.setVisible(false);
                multiOrthoSliceEditor.setVisible(false);
            }
        }
    }

    @Override
    public void onImageContentReady(List<Content> content) {
        updateUIVisibility();
        updateOrthoSlice();
        updateMultiOrthoSlice();
    }

    private void updateSlidersMinMax() {
        if (getCurrentImage() != null) {
            try {
                isUpdatingSliders = true;
                int max = Math.max(getCurrentImage().getWidth(), Math.max(getCurrentImage().getHeight(), getCurrentImage().getNSlices()));
                orthoSliderPanels[0].setMaximum(getCurrentImage().getWidth());
                orthoSliderPanels[1].setMaximum(getCurrentImage().getHeight());
                orthoSliderPanels[2].setMaximum(getCurrentImage().getNSlices());
                for (OrthoSliderPanel orthoSliderPanel : orthoSliderPanels) {
                    orthoSliderPanel.updateLabelSizing(max);
                }
                orthoSliceEditor.revalidate();
                orthoSliceEditor.repaint();
            } finally {
                isUpdatingSliders = false;
            }
        }
    }

    @Override
    public void onImageChanged() {
        super.onImageChanged();
        updateSlidersMinMax();
        if (getCurrentImage() != null) {
            try {
                isUpdatingSliders = true;
                orthoSliderPanels[0].setValue(getCurrentImage().getWidth() / 2 + 1);
                orthoSliderPanels[1].setValue(getCurrentImage().getHeight() / 2 + 1);
                orthoSliderPanels[2].setValue(getCurrentImage().getNSlices() / 2 + 1);
            } finally {
                isUpdatingSliders = false;
            }
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        updateMultiOrthoSlice();
    }

    public static class OrthoSliderPanel extends JPanel {
        private final JToggleButton visibleButton = new JToggleButton(UIUtils.getIconFromResources("actions/eye.png"), true);

        private final String sliderName;

        private final JSlider slider = new JSlider(1, 10, 1);
        private final SlicerControlsPlugin3D plugin;
        private final JLabel label;

        public OrthoSliderPanel(String sliderName, SlicerControlsPlugin3D plugin, Icon icon) {
            this.sliderName = sliderName;
            this.plugin = plugin;
            this.label = new JLabel(icon, JLabel.LEFT);
            this.slider.setMinimum(1);

            slider.addChangeListener(e -> updateLabel());
            slider.addMouseWheelListener(e -> {
                if (e.getWheelRotation() < 0) {
                    increment();
                } else {
                    decrement();
                }
            });
        }

        public void updateLabel() {
            label.setText(sliderName + " " + slider.getValue() + "/" + slider.getMaximum());
        }

        public void setMaximum(int maximum) {
            slider.setMaximum(maximum);
        }

        public void addToForm(FormPanel target) {

            // configure slider
            slider.setMajorTickSpacing(10);
            slider.setMinorTickSpacing(1);
            slider.setPaintTicks(false);

            JPanel descriptionPanel = new JPanel();
            descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.X_AXIS));

            JButton editButton = new JButton(UIUtils.getIconFromResources("actions/go-jump.png"));
            editButton.setToolTipText("Jump to slice");
            UIUtils.makeFlat25x25(editButton);
            editButton.addActionListener(e -> {
                String input = JOptionPane.showInputDialog(plugin.getViewerPanel3D(),
                        "Please input a new value for " + sliderName + " (" + slider.getMinimum() + "-" + slider.getMaximum() + ")",
                        slider.getValue());
                if (!StringUtils.isNullOrEmpty(input)) {
                    Integer index = NumberUtils.createInteger(input);
                    index = Math.min(slider.getMaximum(), Math.max(slider.getMinimum(), index));
                    slider.setValue(index);
                }
            });
            descriptionPanel.add(visibleButton);
            descriptionPanel.add(Box.createHorizontalStrut(4));
            descriptionPanel.add(label);
            descriptionPanel.add(Box.createHorizontalGlue());
            descriptionPanel.add(editButton);

            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setOpaque(false);
            contentPanel.add(slider, BorderLayout.CENTER);

            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
            contentPanel.add(rightPanel, BorderLayout.EAST);

            JButton lastFrame = new JButton(UIUtils.getIconFromResources("actions/caret-left.png"));
            UIUtils.makeFlat25x25(lastFrame);
            lastFrame.setToolTipText("Go one slice back");
            lastFrame.addActionListener(e -> {
                decrement();
            });
            rightPanel.add(lastFrame);

            JButton nextFrame = new JButton(UIUtils.getIconFromResources("actions/caret-right.png"));
            UIUtils.makeFlat25x25(nextFrame);
            nextFrame.setToolTipText("Go one slice forward");
            nextFrame.addActionListener(e -> {
                increment();
            });
            rightPanel.add(nextFrame);

            target.addToForm(contentPanel, descriptionPanel, null);
        }

        private void increment() {
            int value = slider.getValue();
            int maximum = slider.getMaximum();
            int newIndex = ((value) % maximum) + 1;
            slider.setValue(newIndex);
        }

        private void decrement() {
            int value = slider.getValue();
            int maximum = slider.getMaximum();
            int newIndex = value - 1;
            if (newIndex < 1)
                newIndex += maximum;
            slider.setValue(newIndex);
        }

        public int getValue() {
            return slider.getValue();
        }

        public void setValue(int value) {
            slider.setValue(value);
        }

        public boolean isPlaneVisible() {
            return visibleButton.isSelected();
        }

        public void updateLabelSizing(int max) {
            // fix label glitch
            {
                String maxFormat = String.format(sliderName + " %d/%d", max, max);
                int stringWidth = label.getFontMetrics(label.getFont()).stringWidth(maxFormat);
                int bufferedSw = stringWidth + 22;
                label.setMinimumSize(new Dimension(bufferedSw, 16));
                label.setPreferredSize(new Dimension(bufferedSw, 16));
            }
        }
    }

    public static class MultiOrthoSlicerSettings extends AbstractJIPipeParameterCollection {
        private IntegerRange x = new IntegerRange();
        private IntegerRange y = new IntegerRange();
        private IntegerRange z = new IntegerRange();

        private boolean opaqueTextures = false;

        @SetJIPipeDocumentation(name = "X slices")
        @JIPipeParameter("x")
        public IntegerRange getX() {
            return x;
        }

        @JIPipeParameter("x")
        public void setX(IntegerRange x) {
            this.x = x;
        }

        @SetJIPipeDocumentation(name = "Y slices")
        @JIPipeParameter("y")
        public IntegerRange getY() {
            return y;
        }

        @JIPipeParameter("y")
        public void setY(IntegerRange y) {
            this.y = y;
        }

        @SetJIPipeDocumentation(name = "Z slices")
        @JIPipeParameter("z")
        public IntegerRange getZ() {
            return z;
        }

        @JIPipeParameter("z")
        public void setZ(IntegerRange z) {
            this.z = z;
        }

        @JIPipeParameter(value = "opaque-textures", uiOrder = 100)
        public boolean isOpaqueTextures() {
            return opaqueTextures;
        }

        @JIPipeParameter("opaque-textures")
        public void setOpaqueTextures(boolean opaqueTextures) {
            this.opaqueTextures = opaqueTextures;
        }
    }

    public static class ApplyOrthoSlicerRun extends AbstractJIPipeRunnable {

        private final List<Content> contentList;

        private final int resamplingFactor;

        private final int x;

        private final int y;

        private final int z;

        private final boolean showX;

        private final boolean showY;

        private final boolean showZ;

        public ApplyOrthoSlicerRun(List<Content> contentList, int resamplingFactor, int x, int y, int z, boolean showX, boolean showY, boolean showZ) {
            this.contentList = contentList;
            this.resamplingFactor = resamplingFactor;
            this.x = x;
            this.y = y;
            this.z = z;
            this.showX = showX;
            this.showY = showY;
            this.showZ = showZ;
        }

        @Override
        public String getTaskLabel() {
            return "Update ortho-slice";
        }

        @Override
        public void run() {
            for (Content content : contentList) {
                if (content.getContent() instanceof OrthoGroup) {
                    OrthoGroup orthoGroup = (OrthoGroup) content.getContent();
                    orthoGroup.setVisible(AxisConstants.X_AXIS, showX);
                    orthoGroup.setVisible(AxisConstants.Y_AXIS, showY);
                    orthoGroup.setVisible(AxisConstants.Z_AXIS, showZ);
                    orthoGroup.setSlice(AxisConstants.X_AXIS, x / resamplingFactor);
                    orthoGroup.setSlice(AxisConstants.Y_AXIS, y / resamplingFactor);
                    orthoGroup.setSlice(AxisConstants.Z_AXIS, z / resamplingFactor);
                }
            }
        }
    }

    public static class ApplyMultiOrthoSlicerRun extends AbstractJIPipeRunnable {

        private final List<Content> contentList;

        private final int resamplingFactor;
        private final List<Integer> xSlices;
        private final List<Integer> ySlices;
        private final List<Integer> zSlices;
        private final boolean opaqueTextures;

        public ApplyMultiOrthoSlicerRun(List<Content> contentList, int resamplingFactor, List<Integer> xSlices, List<Integer> ySlices, List<Integer> zSlices, boolean opaqueTextures) {
            this.contentList = contentList;
            this.resamplingFactor = resamplingFactor;
            this.xSlices = xSlices;
            this.ySlices = ySlices;
            this.zSlices = zSlices;
            this.opaqueTextures = opaqueTextures;
        }

        @Override
        public String getTaskLabel() {
            return "Update ortho-slice (advanced)";
        }

        @Override
        public void run() {
            for (Content content : contentList) {
                if (content.getContent() instanceof MultiOrthoGroup) {
                    MultiOrthoGroup multiOrthoGroup = (MultiOrthoGroup) content.getContent();
                    final int X = AxisConstants.X_AXIS;
                    final int Y = AxisConstants.Y_AXIS;
                    final int Z = AxisConstants.Z_AXIS;

                    final boolean[] xAxis = new boolean[multiOrthoGroup.getSliceCount(X)];
                    final boolean[] yAxis = new boolean[multiOrthoGroup.getSliceCount(Y)];
                    final boolean[] zAxis = new boolean[multiOrthoGroup.getSliceCount(Z)];

                    for (int x : xSlices) {
                        int rx = (x - 1) / resamplingFactor;
                        if (rx >= 0 && rx < xAxis.length) {
                            xAxis[rx] = true;
                        }
                    }
                    for (int y : ySlices) {
                        int ry = (y - 1) / resamplingFactor;
                        if (ry >= 0 && ry < yAxis.length) {
                            yAxis[ry] = true;
                        }
                    }
                    for (int z : zSlices) {
                        int rz = (z - 1) / resamplingFactor;
                        if (rz >= 0 && rz < yAxis.length) {
                            zAxis[rz] = true;
                        }
                    }

                    multiOrthoGroup.setVisible(X, xAxis);
                    multiOrthoGroup.setVisible(Y, yAxis);
                    multiOrthoGroup.setVisible(Z, zAxis);
                    multiOrthoGroup.setTexturesOpaque(opaqueTextures);
                }
            }
        }
    }
}
