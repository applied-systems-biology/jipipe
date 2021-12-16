package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ImageViewerPanelPlugin;
import org.hkijena.jipipe.extensions.parameters.ranges.DefaultTrackBackground;
import org.hkijena.jipipe.extensions.parameters.ranges.IntNumberRangeParameter;
import org.hkijena.jipipe.extensions.parameters.ranges.NumberRangeInvertedMode;
import org.hkijena.jipipe.extensions.parameters.ranges.NumberRangeParameterSettings;
import org.hkijena.jipipe.extensions.parameters.ranges.PaintGenerator;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.ColorChooserButton;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MaskDrawerPlugin extends ImageViewerPanelPlugin {

    public static final Stroke STROKE_GUIDE_LINE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{1}, 0);
    private final JPanel colorSelectionPanel = new JPanel();
    private final JPanel toolSelectionPanel = new JPanel();
    private final Map<MaskColor, JToggleButton> colorSelectionButtons = new HashMap<>();
    private final Map<MaskDrawerTool, JToggleButton> toolSelectionButtons = new HashMap<>();
    private ImagePlus mask;
    private ImageProcessor currentMaskSlice;
    private BufferedImage currentMaskSlicePreview;
    private MaskColor currentColor = MaskColor.Foreground;
    private MaskDrawerTool currentTool = new MouseMaskDrawerTool(this);
    private ColorChooserButton highlightColorButton;
    private ColorChooserButton maskColorButton;
    private Color highlightColor = new Color(255, 255, 0, 128);
    private Color maskColor = new Color(255, 0, 0, 128);
    private JCheckBox showGuidesToggle = new JCheckBox("Show guide lines", true);
    private ButtonGroup toolButtonGroup = new ButtonGroup();
    private Function<ImagePlus, ImagePlus> maskGenerator;
    private FormPanel.GroupHeaderPanel currentGroupHeader;

    public MaskDrawerPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        viewerPanel.setRotationEnabled(false);
        initialize();

        // Install tools
        installTool(currentTool);
        installTool(new PencilMaskDrawerTool(this));
        installTool(new FloodFillMaskDrawerTool(this));
        installTool(new RectangleMaskDrawerTool(this));
        installTool(new EllipseMaskDrawerTool(this));
        installTool(new PolygonMaskDrawerTool(this));

        viewerPanel.getCanvas().getEventBus().register(this);
        setCurrentTool(currentTool);
    }

    public void installTool(MaskDrawerTool tool) {
        addSelectionButton(tool,
                toolSelectionPanel,
                toolSelectionButtons,
                toolButtonGroup,
                "",
                "<html><strong>" + tool.getName() + "</strong><br/>" +
                        tool.getDescription() + "</html>",
                tool.getIcon(),
                this::getCurrentTool,
                this::setCurrentTool);
    }

    private void initialize() {
        // Create highlight color selection
        highlightColorButton = new ColorChooserButton("Change highlight color");
        highlightColorButton.setUpdateWithHexCode(true);
        highlightColorButton.setSelectedColor(highlightColor);
        highlightColorButton.getEventBus().register(new Object() {
            @Subscribe
            public void onColorChosen(ColorChooserButton.ColorChosenEvent event) {
                setHighlightColor(event.getColor());
            }
        });

        // Create mask color selection
        maskColorButton = new ColorChooserButton("Change mask color");
        maskColorButton.setUpdateWithHexCode(true);
        maskColorButton.setSelectedColor(maskColor);
        maskColorButton.getEventBus().register(new Object() {
            @Subscribe
            public void onColorChosen(ColorChooserButton.ColorChosenEvent event) {
                setMaskColor(event.getColor());
            }
        });

        // Create Color selection
        colorSelectionPanel.setLayout(new BoxLayout(colorSelectionPanel, BoxLayout.X_AXIS));
        ButtonGroup colorButtonGroup = new ButtonGroup();

        addSelectionButton(MaskColor.Foreground,
                colorSelectionPanel,
                colorSelectionButtons,
                colorButtonGroup,
                "255",
                "Sets the currently drawn color to foreground (255)",
                new SolidColorIcon(16, 16, Color.WHITE, Color.DARK_GRAY),
                this::getCurrentColor,
                this::setCurrentColor);
        addSelectionButton(MaskColor.Background,
                colorSelectionPanel,
                colorSelectionButtons,
                colorButtonGroup,
                "0",
                "Sets the currently drawn color to background (0)",
                new SolidColorIcon(16, 16, Color.BLACK, Color.DARK_GRAY),
                this::getCurrentColor,
                this::setCurrentColor);

        // Create tool selection
        toolSelectionPanel.setLayout(new BoxLayout(toolSelectionPanel, BoxLayout.X_AXIS));
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
        currentTool.onHighlightColorChanged();
    }

    public void recalculateMaskPreview() {
        if (currentMaskSlice == null || currentMaskSlicePreview == null)
            return;
        ImageJUtils.maskToBufferedImage(currentMaskSlice, currentMaskSlicePreview, maskColor, ColorUtils.WHITE_TRANSPARENT);
        getViewerPanel().getCanvas().repaint();
    }

    private <T> void addSelectionButton(T value, JPanel target, Map<T, JToggleButton> targetMap, ButtonGroup targetGroup, String text, String toolTip, Icon icon, Supplier<T> getter, Consumer<T> setter) {
        JToggleButton button = new JToggleButton(text, icon, Objects.equals(getter.get(), value));
        button.setToolTipText(toolTip);
        button.addActionListener(e -> {
            if (button.isSelected()) {
                setter.accept(value);
            }
        });
        targetGroup.add(button);
        target.add(button);
        targetMap.put(value, button);
    }

    private void updateCurrentMaskSlice() {
        if (getCurrentImage() == null)
            return;
        if (mask == null)
            return;
        ImageSliceIndex index = getViewerPanel().getCurrentSlicePosition();
        int z = Math.min(index.getZ(), mask.getNSlices() - 1);
        int c = Math.min(index.getC(), mask.getNChannels() - 1);
        int t = Math.min(index.getT(), mask.getNFrames() - 1);
        currentMaskSlice = ImageJUtils.getSliceZero(mask, c, z, t);
        recalculateMaskPreview();
    }

    @Override
    public void onImageChanged() {
        if (getCurrentImage() != null) {
            if (mask == null || !ImageJUtils.imagesHaveSameSize(mask, getCurrentImage())) {
                if (maskGenerator == null) {
                    mask = IJ.createHyperStack("Mask",
                            getCurrentImage().getWidth(),
                            getCurrentImage().getHeight(),
                            getCurrentImage().getNChannels(),
                            getCurrentImage().getNSlices(),
                            getCurrentImage().getNFrames(),
                            8);
                } else {
                    mask = maskGenerator.apply(getCurrentImage());
                }
                currentMaskSlicePreview = new BufferedImage(getCurrentImage().getWidth(),
                        getCurrentImage().getHeight(),
                        BufferedImage.TYPE_4BYTE_ABGR);
                currentMaskSlice = mask.getProcessor();
                setMask(mask);
            }
        } else {
            mask = null;
            currentMaskSlice = null;
            currentMaskSlicePreview = null;
        }
        currentTool.onImageChanged();
    }

    @Override
    public void onSliceChanged() {
        updateCurrentMaskSlice();
        currentTool.onSliceChanged();
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        if (getCurrentImage() == null) {
            return;
        }
        FormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("Draw mask", UIUtils.getIconFromResources("actions/draw-brush.png"));
        this.currentGroupHeader = groupHeader;
        if (mask != null && mask.getStackSize() > 1) {
            JButton copySliceButton = new JButton("Copy slice to ...", UIUtils.getIconFromResources("actions/edit-copy.png"));
            copySliceButton.addActionListener(e -> copySlice());
            groupHeader.addColumn(copySliceButton);
        }
        formPanel.addToForm(toolSelectionPanel, new JLabel("Tool"), null);
        currentTool.createPalettePanel(formPanel);
        formPanel.addToForm(showGuidesToggle, null);
        formPanel.addToForm(colorSelectionPanel, new JLabel("Color"), null);
        formPanel.addToForm(highlightColorButton, new JLabel("Highlight color"), null);
        formPanel.addToForm(maskColorButton, new JLabel("Mask color"), null);
    }

    /**
     * The current group header created by  createPalettePanel. Use this for adding your own buttons into createPalettePanel
     *
     * @return the current group header. can be null
     */
    public FormPanel.GroupHeaderPanel getCurrentGroupHeader() {
        return currentGroupHeader;
    }

    private void copySlice() {
        JIPipeDynamicParameterCollection parameterCollection = new JIPipeDynamicParameterCollection(false);
        parameterCollection.addParameter("z", IntNumberRangeParameter.class, "Target Z slices", "Determines to which Z slices the mask is copied to.",
                new NumberRangeParameterSettings() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return NumberRangeParameterSettings.class;
                    }

                    @Override
                    public double min() {
                        return 0;
                    }

                    @Override
                    public double max() {
                        return mask.getNSlices() - 1;
                    }

                    @Override
                    public Class<? extends PaintGenerator> trackBackground() {
                        return DefaultTrackBackground.class;
                    }

                    @Override
                    public NumberRangeInvertedMode invertedMode() {
                        return NumberRangeInvertedMode.OutsideMinMax;
                    }
                });
        parameterCollection.addParameter("c", IntNumberRangeParameter.class, "Target C slices", "Determines to which channel slices the mask is copied to.",
                new NumberRangeParameterSettings() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return NumberRangeParameterSettings.class;
                    }

                    @Override
                    public double min() {
                        return 0;
                    }

                    @Override
                    public double max() {
                        return mask.getNChannels() - 1;
                    }

                    @Override
                    public Class<? extends PaintGenerator> trackBackground() {
                        return DefaultTrackBackground.class;
                    }

                    @Override
                    public NumberRangeInvertedMode invertedMode() {
                        return NumberRangeInvertedMode.OutsideMinMax;
                    }
                });
        parameterCollection.addParameter("t", IntNumberRangeParameter.class, "Target T slices", "Determines to which frame slices the mask is copied to.",
                new NumberRangeParameterSettings() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return NumberRangeParameterSettings.class;
                    }

                    @Override
                    public double min() {
                        return 0;
                    }

                    @Override
                    public double max() {
                        return mask.getNFrames() - 1;
                    }

                    @Override
                    public Class<? extends PaintGenerator> trackBackground() {
                        return DefaultTrackBackground.class;
                    }

                    @Override
                    public NumberRangeInvertedMode invertedMode() {
                        return NumberRangeInvertedMode.OutsideMinMax;
                    }
                });
        if (mask.getNSlices() == 1) {
            ((JIPipeMutableParameterAccess) parameterCollection.get("z")).setHidden(true);
        }
        if (mask.getNChannels() == 1) {
            ((JIPipeMutableParameterAccess) parameterCollection.get("c")).setHidden(true);
        }
        if (mask.getNFrames() == 1) {
            ((JIPipeMutableParameterAccess) parameterCollection.get("t")).setHidden(true);
        }

        ParameterPanel parameterPanel = new ParameterPanel(new JIPipeDummyWorkbench(),
                parameterCollection,
                null,
                FormPanel.WITH_DOCUMENTATION | FormPanel.WITH_SCROLLING);

        AtomicBoolean canceled = new AtomicBoolean(true);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getViewerPanel()),
                "Copy current slice");
        dialog.setModal(true);
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(parameterPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            canceled.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("OK", UIUtils.getIconFromResources("actions/button_ok.png"));
        confirmButton.addActionListener(e -> {
            canceled.set(false);
            dialog.setVisible(false);
        });
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        UIUtils.addEscapeListener(dialog);

        dialog.setContentPane(contentPanel);
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(getViewerPanel());
        dialog.setVisible(true);

        if (canceled.get())
            return;

        IntNumberRangeParameter zRange = parameterCollection.get("z").get(IntNumberRangeParameter.class);
        IntNumberRangeParameter cRange = parameterCollection.get("c").get(IntNumberRangeParameter.class);
        IntNumberRangeParameter tRange = parameterCollection.get("t").get(IntNumberRangeParameter.class);
        ImageSliceIndex currentIndex = getViewerPanel().getCurrentSlicePosition();

        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ImageJUtils.forEachIndexedZCTSlice(mask, (ip, index) -> {
                if (index.equals(currentIndex))
                    return;
                if (index.getZ() < zRange.getMin() || index.getZ() > zRange.getMax())
                    return;
                if (index.getC() < cRange.getMin() || index.getC() > cRange.getMax())
                    return;
                if (index.getT() < tRange.getMin() || index.getT() > tRange.getMax())
                    return;
                ip.copyBits(getCurrentMaskSlice(), 0, 0, Blitter.COPY);
            }, new JIPipeProgressInfo());
        }
    }

    public ImagePlus getMask() {
        return mask;
    }

    public void setMask(ImagePlus mask) {
        this.mask = mask;
        currentMaskSlice = mask.getProcessor();
        getViewerPanel().refreshFormPanel();
        updateCurrentMaskSlice();
    }

    public ImageProcessor getCurrentMaskSlice() {
        return currentMaskSlice;
    }

    public void clearCurrentMask() {
        if (getCurrentMaskSlice() != null) {
            getCurrentMaskSlice().setColor(0);
            getCurrentMaskSlice().fillRect(0, 0, getCurrentMaskSlice().getWidth(), getCurrentMaskSlice().getHeight());
            recalculateMaskPreview();
            getViewerPanel().getCanvas().getEventBus().post(new MaskDrawerPlugin.MaskChangedEvent(this));
        }
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, int x, int y, int w, int h) {
        final double zoom = getViewerPanel().getCanvas().getZoom();
        AffineTransform transform = new AffineTransform();
        transform.scale(zoom, zoom);
        BufferedImageOp op = new AffineTransformOp(transform, zoom < 1 ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        graphics2D.drawImage(currentMaskSlicePreview, op, x, y);
        currentTool.postprocessDraw(graphics2D, x, y, w, h);
        if (showGuidesToggle.isSelected() && currentTool.showGuides()) {
            graphics2D.setStroke(STROKE_GUIDE_LINE);
            graphics2D.setColor(getHighlightColor());
            Point mousePosition = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
            if (mousePosition != null) {
                int displayedX = (int) (x + zoom * mousePosition.x);
                int displayedY = (int) (y + zoom * mousePosition.y);
                graphics2D.drawLine(x, displayedY, x + w, displayedY);
                graphics2D.drawLine(displayedX, y, displayedX, y + h);
            }
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex) {
        if (mask == null)
            return;
        int z = Math.min(mask.getNSlices() - 1, sliceIndex.getZ());
        int c = Math.min(mask.getNChannels() - 1, sliceIndex.getC());
        int t = Math.min(mask.getNFrames() - 1, sliceIndex.getT());
        ImageProcessor selectedMaskSlice = ImageJUtils.getSliceZero(mask, c, z, t);
        BufferedImage renderedMask = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        ImageJUtils.maskToBufferedImage(selectedMaskSlice, renderedMask, maskColor, ColorUtils.WHITE_TRANSPARENT);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(renderedMask, 0, 0, null);
        graphics.dispose();
    }

    @Override
    public String getCategory() {
        return "Draw mask";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("actions/draw-brush.png");
    }

    public MaskColor getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(MaskColor currentColor) {
        this.currentColor = currentColor;
        JToggleButton button = colorSelectionButtons.get(currentColor);
        if (!button.isSelected()) {
            button.doClick();
        }
    }

    public MaskDrawerTool getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(MaskDrawerTool currentTool) {
        if (this.currentTool != null) {
            this.currentTool.deactivate();
        }
        this.currentTool = currentTool;
        JToggleButton button = toolSelectionButtons.get(currentTool);
        if (!button.isSelected()) {
            button.doClick();
        }
        currentTool.activate();
        getViewerPanel().refreshFormPanel();
    }

    public Color getMaskColor() {
        return maskColor;
    }

    public void setMaskColor(Color maskColor) {
        this.maskColor = maskColor;
        recalculateMaskPreview();
    }

    public Function<ImagePlus, ImagePlus> getMaskGenerator() {
        return maskGenerator;
    }

    public void setMaskGenerator(Function<ImagePlus, ImagePlus> maskGenerator) {
        this.maskGenerator = maskGenerator;
    }

    public static void main(String[] args) {
//        ImagePlus img = IJ.openImage("E:\\Projects\\JIPipe\\testdata\\ATTC_IµL_3rdReplicate-Experiment-5516\\in\\data.tif");
        ImagePlus img = IJ.openImage("E:\\Projects\\Mitochondria\\data_21.12.20\\1 Deconv.lif - 1_Series047_5_cmle_converted.tif");
        JIPipeUITheme.ModernLight.install();
        JFrame frame = new JFrame();
        ImageViewerPanel panel = new ImageViewerPanel();
        MaskDrawerPlugin maskDrawerPlugin = new MaskDrawerPlugin(panel);
        panel.setPlugins(Collections.singletonList(maskDrawerPlugin));
        panel.setImage(img);
//        maskDrawerPlugin.setMask(IJ.createImage("Mask", img.getWidth(), img.getHeight(), 1, 8));
        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(1280, 1024);
        frame.setVisible(true);
    }

    public enum MaskColor {
        Foreground(255),
        Background(0);

        private final int value;

        MaskColor(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static class MaskChangedEvent {
        private final MaskDrawerPlugin plugin;

        public MaskChangedEvent(MaskDrawerPlugin plugin) {
            this.plugin = plugin;
        }

        public MaskDrawerPlugin getPlugin() {
            return plugin;
        }
    }
}
