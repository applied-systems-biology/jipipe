package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanelCanvas;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ImageViewerPanelPlugin;
import org.hkijena.jipipe.ui.components.ColorChooserButton;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MaskDrawerPlugin extends ImageViewerPanelPlugin {

    private ImagePlus mask;
    private ImageProcessor currentMaskSlice;
    private BufferedImage currentMaskSlicePreview;
    private final JPanel colorSelectionPanel = new JPanel();
    private final JPanel toolSelectionPanel = new JPanel();
    private MaskColor currentColor = MaskColor.Foreground;
    private MaskDrawerTool currentTool = new MouseMaskDrawerTool(this);
    private final Map<MaskColor, JToggleButton> colorSelectionButtons = new HashMap<>();
    private final Map<MaskDrawerTool, JToggleButton> toolSelectionButtons = new HashMap<>();
    private ColorChooserButton highlightColorButton;
    private ColorChooserButton maskColorButton;
    private Color highlightColor = new Color(255,255,0,128);
    private Color maskColor = new Color(255,0,0,128);
    private ButtonGroup toolButtonGroup = new ButtonGroup();

    public MaskDrawerPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        initialize();

        // Install tools
        installTool(currentTool);
        installTool(new PencilMaskDrawerTool(this));
        installTool(new FloodFillMaskDrawerTool(this));
        installTool(new RectangleMaskDrawerTool(this));
        installTool(new EllipseMaskDrawerTool(this));

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
                new ColorIcon(16,16, Color.WHITE, Color.DARK_GRAY),
                this::getCurrentColor,
                this::setCurrentColor);
        addSelectionButton(MaskColor.Background,
                colorSelectionPanel,
                colorSelectionButtons,
                colorButtonGroup,
                "0",
                "Sets the currently drawn color to background (0)",
                new ColorIcon(16,16, Color.BLACK, Color.DARK_GRAY),
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
        if(currentMaskSlice == null || currentMaskSlicePreview == null)
            return;
        ImageJUtils.maskToBufferedImage(currentMaskSlice, currentMaskSlicePreview, maskColor, ColorUtils.WHITE_TRANSPARENT);
        getViewerPanel().getCanvas().repaint();
    }

    private <T> void addSelectionButton(T value, JPanel target, Map<T, JToggleButton> targetMap, ButtonGroup targetGroup, String text, String toolTip, Icon icon, Supplier<T> getter, Consumer<T> setter) {
        JToggleButton button = new JToggleButton(text,icon, Objects.equals(getter.get(), value));
        button.setToolTipText(toolTip);
        button.addActionListener(e -> {
            if(button.isSelected()) {
                setter.accept(value);
            }
        });
        targetGroup.add(button);
        target.add(button);
        targetMap.put(value, button);
    }

    @Override
    public void onImageChanged() {
        if(getCurrentImage() != null) {
            if(mask == null || !ImageJUtils.imagesHaveSameSize(mask, getCurrentImage())) {
                mask = IJ.createHyperStack("Mask",
                        getCurrentImage().getWidth(),
                        getCurrentImage().getHeight(),
                        getCurrentImage().getNChannels(),
                        getCurrentImage().getNSlices(),
                        getCurrentImage().getNFrames(),
                        8);
                currentMaskSlicePreview = new BufferedImage(getCurrentImage().getWidth(),
                        getCurrentImage().getHeight(),
                        BufferedImage.TYPE_4BYTE_ABGR);
                currentMaskSlice = mask.getProcessor();
            }
        }
        currentTool.onImageChanged();
    }

    @Override
    public void onSliceChanged() {
        if(getCurrentImage() != null && mask != null) {
            ImageSliceIndex index = getViewerPanel().getCurrentSlicePosition();
            currentMaskSlice = ImageJUtils.getSlice(mask, index.zeroToOne());
            getViewerPanel().getCanvas().repaint();
        }
        currentTool.onSliceChanged();
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        if (getCurrentImage() == null) {
            return;
        }
        formPanel.addGroupHeader("Draw mask", UIUtils.getIconFromResources("actions/draw-brush.png"));
        formPanel.addToForm(toolSelectionPanel, new JLabel("Tool"), null);
        currentTool.createPalettePanel(formPanel);
        formPanel.addToForm(colorSelectionPanel, new JLabel("Color"), null);
        formPanel.addToForm(highlightColorButton, new JLabel("Highlight color"), null);
        formPanel.addToForm(maskColorButton, new JLabel("Mask color"), null);

    }

    public ImagePlus getMask() {
        return mask;
    }

    public ImageProcessor getCurrentMaskSlice() {
        return currentMaskSlice;
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, int x, int y, int w, int h) {
        final double zoom = getViewerPanel().getCanvas().getZoom();
        AffineTransform transform = new AffineTransform();
        transform.scale(zoom, zoom);
        BufferedImageOp op = new AffineTransformOp(transform, zoom < 1 ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        graphics2D.drawImage(currentMaskSlicePreview, op, x, y);
        currentTool.postprocessDraw(graphics2D, x, y, w, h);
    }

    public static void main(String[] args) {
        ImagePlus img = IJ.openImage("E:\\Projects\\JIPipe\\testdata\\ATTC_IµL_3rdReplicate-Experiment-5516\\in\\data.tif");
        JIPipeUITheme.ModernLight.install();
        JFrame frame = new JFrame();
        ImageViewerPanel panel = new ImageViewerPanel();
        panel.setPlugins(Collections.singletonList(new MaskDrawerPlugin(panel)));
        panel.setImage(img);
        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(1280, 1024);
        frame.setVisible(true);
    }

    public MaskColor getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(MaskColor currentColor) {
        this.currentColor = currentColor;
        JToggleButton button = colorSelectionButtons.get(currentColor);
        if(!button.isSelected()) {
            button.doClick();
        }
    }

    public MaskDrawerTool getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(MaskDrawerTool currentTool) {
        if(this.currentTool != null) {
            this.currentTool.deactivate();
        }
        this.currentTool = currentTool;
        JToggleButton button = toolSelectionButtons.get(currentTool);
        if(!button.isSelected()) {
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
}
