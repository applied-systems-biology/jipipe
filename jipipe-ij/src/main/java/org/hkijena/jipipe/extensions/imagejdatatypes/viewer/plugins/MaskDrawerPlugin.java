package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanelCanvas;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MaskDrawerPlugin extends ImageViewerPanelPlugin {

    private ImagePlus mask;
    private final JPanel colorSelectionPanel = new JPanel();
    private final JPanel toolSelectionPanel = new JPanel();
    private MaskColor currentColor = MaskColor.Foreground;
    private Tool currentTool = Tool.Mouse;
    private final Map<MaskColor, JToggleButton> colorSelectionButtons = new HashMap<>();
    private final Map<Tool, JToggleButton> toolSelectionButtons = new HashMap<>();
    private final List<Point> referencePoints = new ArrayList<>();
    private JSpinner stampSizeXSpinner;
    private JSpinner stampSizeYSpinner;

    public MaskDrawerPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        initialize();
        viewerPanel.getCanvas().getEventBus().register(this);
    }

    private void initialize() {
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
        ButtonGroup toolButtonGroup = new ButtonGroup();

        addSelectionButton(Tool.Mouse,
                toolSelectionPanel,
                toolSelectionButtons,
                toolButtonGroup,
                "",
                "Switches to the selection tool.",
                UIUtils.getIconFromResources("actions/followmouse.png"),
                this::getCurrentTool,
                this::setCurrentTool);
        addSelectionButton(Tool.Rectangle,
                toolSelectionPanel,
                toolSelectionButtons,
                toolButtonGroup,
                "",
                "Switches to the rectangle tool. Click the two opposite corners to create a rectangle.\n " +
                        "Press Esc or right click to cancel drawing.",
                UIUtils.getIconFromResources("actions/draw-rectangle.png"),
                this::getCurrentTool,
                this::setCurrentTool);
        addSelectionButton(Tool.Stamp,
                toolSelectionPanel,
                toolSelectionButtons,
                toolButtonGroup,
                "",
                "Switches to the stamp tool that allows to easily create simple geometric shapes of the same size.",
                UIUtils.getIconFromResources("actions/stamp.png"),
                this::getCurrentTool,
                this::setCurrentTool);

        initializeStampTool();
    }

    private void initializeStampTool() {
        SpinnerNumberModel stampSizeXModel = new SpinnerNumberModel(5,1,Integer.MAX_VALUE,1);
        stampSizeXSpinner = new JSpinner(stampSizeXModel);
        SpinnerNumberModel stampSizeYModel = new SpinnerNumberModel(5,1,Integer.MAX_VALUE,1);
        stampSizeYSpinner = new JSpinner(stampSizeYModel);
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
            }
        }
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        if (getCurrentImage() == null) {
            return;
        }
        formPanel.addGroupHeader("Draw mask", UIUtils.getIconFromResources("actions/draw-brush.png"));
        formPanel.addToForm(toolSelectionPanel, new JLabel("Tool"), null);
        if(currentTool == Tool.Stamp) {
            formPanel.addToForm(stampSizeXSpinner, new JLabel("Stamp width"), null);
            formPanel.addToForm(stampSizeYSpinner, new JLabel("Stamp height"), null);
        }
        formPanel.addToForm(colorSelectionPanel, new JLabel("Color"), null);
    }

    @Subscribe
    public void onPixelHover(ImageViewerPanelCanvas.PixelHoverEvent event) {
        getViewerPanel().getCanvas().repaint();
    }

    @Subscribe
    public void onCanvasClick(ImageViewerPanelCanvas.MouseClickedEvent event) {
        if(SwingUtilities.isLeftMouseButton(event)) {
            switch (currentTool) {
                case Rectangle: {

                }
            }
        }
        else if(SwingUtilities.isRightMouseButton(event)) {
            resetCurrentTool(false);
        }
    }

    public void resetCurrentTool(boolean fullReset) {
        switch (currentTool) {
            case Rectangle: {
                referencePoints.clear();
            }
            break;
        }
        getViewerPanel().getCanvas().repaint();
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, int x, int y, int w, int h) {
        Point mousePosition = getViewerPanel().getCanvas().getMousePosition();
        if(mousePosition == null)
            return;
        graphics2D.setColor(Color.YELLOW);
//        graphics2D.drawOval(mousePosition.x, mousePosition.y, 8, 8);
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

    public Tool getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(Tool currentTool) {
        if(this.currentTool == currentTool)
            return;
        this.currentTool = currentTool;
        JToggleButton button = toolSelectionButtons.get(currentTool);
        if(!button.isSelected()) {
            button.doClick();
        }
        getViewerPanel().getCanvas().setDragWithLeftMouse(currentTool == Tool.Mouse);
        if(currentTool == Tool.Mouse) {
            getViewerPanel().getCanvas().setStandardCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        else {
            getViewerPanel().getCanvas().setStandardCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
        getViewerPanel().refreshFormPanel();
    }

    public enum StampShape {
        Rectangle,
        Ellipse
    }

    public enum MaskColor {
        Foreground,
        Background
    }

    public enum Tool {
        Mouse,
        Rectangle,
        Stamp
    }
}
