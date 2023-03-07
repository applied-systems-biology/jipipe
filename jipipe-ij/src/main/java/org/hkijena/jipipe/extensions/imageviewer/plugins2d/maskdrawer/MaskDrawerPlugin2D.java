package org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2D;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2DTool;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager.ROIManagerPlugin2D;
import org.hkijena.jipipe.extensions.parameters.library.ranges.*;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.ColorChooserButton;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.ribbon.*;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MaskDrawerPlugin2D extends ImageViewerPanelPlugin2D {

    public static final Stroke STROKE_GUIDE_LINE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{1}, 0);
    private final JPanel colorSelectionPanel = new JPanel();
    private final Map<MaskColor, JToggleButton> colorSelectionButtons = new HashMap<>();
    private final JCheckBox showGuidesToggle = new JCheckBox("Show guide lines", true);
    private final MouseMaskDrawer2DTool mouseTool = new MouseMaskDrawer2DTool(this);
    private final Ribbon ribbon = new Ribbon(3);
    private final List<MaskDrawer2DTool> registeredTools = new ArrayList<>();
    private final FormPanel toolSettingsPanel = new FormPanel(FormPanel.NONE);
    private ImagePlus mask;
    private ImageProcessor currentMaskSlice;
    private BufferedImage currentMaskSlicePreview;
    private MaskColor currentColor = MaskColor.Foreground;
    private MaskDrawer2DTool currentTool;
    private ColorChooserButton highlightColorButton;
    private ColorChooserButton maskColorButton;
    private Color highlightColor = new Color(255, 255, 0, 128);
    private Color maskColor = new Color(255, 0, 0, 128);
    private Function<ImagePlus, ImagePlus> maskGenerator;
    private SmallButtonAction exportToRoiManagerAction;
    private boolean drawCurrentMaskSlicePreview;

    public MaskDrawerPlugin2D(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
//        viewerPanel.setRotationEnabled(false);
        initialize();

        // Install tools
        this.currentTool = mouseTool;
        installTool(currentTool);
        installTool(new PencilMaskDrawer2DTool(this));
        installTool(new FloodFillMaskDrawer2DTool(this));
        installTool(new RectangleMaskDrawer2DTool(this));
        installTool(new EllipseMaskDrawer2DTool(this));
        installTool(new PolygonMaskDrawer2DTool(this));

        getViewerPanel2D().getCanvas().getEventBus().register(this);
        getViewerPanel2D().getCanvas().setTool(mouseTool);
        rebuildToolSettings();
    }

    public void installTool(MaskDrawer2DTool tool) {
        Ribbon.Task task = ribbon.getOrCreateTask("Draw");
        Ribbon.Band band = task.getOrCreateBand("Tools");
        if (tool instanceof MouseMaskDrawer2DTool) {
            LargeToggleButtonAction action = new LargeToggleButtonAction(tool.getName(), tool.getDescription(), tool.getIcon());
            action.addActionListener(e -> {
                if (action.getState()) {
                    getViewerPanel2D().getCanvas().setTool(tool);
                }
            });
            tool.addToggleButton(action.getButton(), getViewerPanel2D().getCanvas());
            band.add(action);
        } else {
            SmallToggleButtonAction action = new SmallToggleButtonAction(tool.getName(), tool.getDescription(), tool.getIcon());
            action.addActionListener(e -> {
                if (action.getState()) {
                    getViewerPanel2D().getCanvas().setTool(tool);
                }
            });
            tool.addToggleButton(action.getButton(), getViewerPanel2D().getCanvas());
            band.add(action);
        }

        // Register tool
        registeredTools.add(tool);
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

        // Style tool settings
        toolSettingsPanel.setBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 3));

        // Create ribbon
        initializeRibbon();
    }

    private void initializeRibbon() {
        // Pre-create the ribbon band
        ribbon.addTask("Draw").addBand("Tools");

        // View menu
        Ribbon.Task viewTask = ribbon.addTask("View");
        Ribbon.Band viewColorsBand = viewTask.addBand("Colors");
        Ribbon.Band viewTweaksBand = viewTask.addBand("Tweaks");

        maskColorButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        highlightColorButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        viewColorsBand.add(new Ribbon.Action(Arrays.asList(new JLabel("Mask color"), maskColorButton), 1, new Insets(2, 2, 2, 2)));
        viewColorsBand.add(new Ribbon.Action(Arrays.asList(new JLabel("Highlight color"), highlightColorButton), 1, new Insets(2, 2, 2, 2)));

        viewTweaksBand.add(new Ribbon.Action(showGuidesToggle, 1, new Insets(2, 2, 2, 2)));

        // Modify menu
        Ribbon.Task modifyTask = ribbon.addTask("Modify");
        Ribbon.Band modifyMaskBand = modifyTask.addBand("Mask");
        modifyMaskBand.add(new LargeButtonAction("Process ...", "Apply a morphological operation", UIUtils.getIcon32FromResources("actions/configure.png"),
                UIUtils.createMenuItem("Invert", "Inverts the mask", UIUtils.getIconFromResources("actions/object-inverse.png"), this::applyInvert),
                null,
                UIUtils.createMenuItem("Watershed", "Applies a distance transform watershed", UIUtils.getIconFromResources("actions/object-tweak-randomize.png"), this::applyWatershed),
                null,
                UIUtils.createMenuItem("Dilation", "Applies a 3x3 morphological dilation", UIUtils.getIconFromResources("actions/object-tweak-paint.png"), this::applyDilate),
                UIUtils.createMenuItem("Erosion", "Applies a 3x3 morphological erosion", UIUtils.getIconFromResources("actions/object-tweak-paint.png"), this::applyErode),
                UIUtils.createMenuItem("Opening", "Applies a 3x3 morphological opening", UIUtils.getIconFromResources("actions/object-tweak-paint.png"), this::applyOpen),
                UIUtils.createMenuItem("Closing", "Applies a 3x3 morphological closing", UIUtils.getIconFromResources("actions/object-tweak-paint.png"), this::applyClose)));
        modifyMaskBand.add(new SmallButtonAction("Copy to ...", "Copies the current slice to another position", UIUtils.getIconFromResources("actions/edit-duplicate.png"), this::copySlice));
        modifyMaskBand.add(new SmallButtonAction("Clear", "Sets the whole mask to zero", UIUtils.getIconFromResources("actions/tool_color_eraser.png"), this::clearCurrentMask));

        // Import/Export menu
        Ribbon.Task importExportTask = ribbon.addTask("Import/Export");
        Ribbon.Band fileImportExportBand = importExportTask.addBand("Mask");

        fileImportExportBand.add(new SmallButtonAction("Import mask", "Imports the mask slice from a *.tif file", UIUtils.getIconFromResources("actions/document-import.png"), this::importMask));
        fileImportExportBand.add(new SmallButtonAction("Export mask", "Exports the mask slice to a *.tif file", UIUtils.getIconFromResources("actions/document-export.png"), this::exportMask));

        exportToRoiManagerAction = new SmallButtonAction("To ROI", "Exports the mask to the ROI manager", UIUtils.getIconFromResources("data-types/roi.png"), this::addToROIManager);
        fileImportExportBand.add(exportToRoiManagerAction);
    }

    private void addToROIManager() {
        RoiManager manager = new RoiManager(true);
        ResultsTable table = new ResultsTable();
        ParticleAnalyzer.setRoiManager(manager);
        ParticleAnalyzer.setResultsTable(table);
        ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.INCLUDE_HOLES,
                0,
                table,
                0,
                Double.POSITIVE_INFINITY,
                0,
                Double.POSITIVE_INFINITY);
        analyzer.analyze(new ImagePlus("mask", getCurrentMaskSlice()));
        ROIListData rois = new ROIListData(Arrays.asList(manager.getRoisAsArray()));

        // Set slices
        if (getViewerPanel().getImage().getStackSize() > 1) {
            ImageSliceIndex index = getViewerPanel2D().getCurrentSliceIndex();
            for (Roi roi : rois) {
                roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
            }
        }

        ROIManagerPlugin2D roiManager = getViewerPanel().getPlugin(ROIManagerPlugin2D.class);
        roiManager.importROIs(rois, false);
        clearCurrentMask();
    }

    private void applyInvert() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getCurrentMaskSlice();
            processor.invert();
            recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void applyOpen() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getCurrentMaskSlice();
            // Dilate and erode are switched for some reason
            processor.dilate(); // Erode
            processor.erode(); // Dilate
            recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void applyClose() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getCurrentMaskSlice();
            // Dilate and erode are switched for some reason
            processor.erode(); // Dilate
            processor.dilate(); // Erode
            recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void applyDilate() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getCurrentMaskSlice();
            processor.erode(); // Dilate and erode are switched for some reason
            recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void applyErode() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            ByteProcessor processor = (ByteProcessor) getCurrentMaskSlice();
            processor.erode(); // Dilate and erode are switched for some reason
            recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void applyWatershed() {
        try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
            EDM edm = new EDM();
            edm.toWatershed(getCurrentMaskSlice());
            recalculateMaskPreview();
            postMaskChangedEvent();
        }
    }

    private void importMask() {
        Path selectedFile = FileChooserSettings.openFile(getViewerPanel(),
                FileChooserSettings.LastDirectoryKey.Data,
                "Import mask",
                UIUtils.EXTENSION_FILTER_TIFF);
        if (selectedFile != null) {
            try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                ImagePlus image = IJ.openImage(selectedFile.toString());
                ImageProcessor processor = getCurrentMaskSlice();
                if (image.getWidth() != processor.getWidth() || image.getHeight() != processor.getHeight()) {
                    JOptionPane.showMessageDialog(getViewerPanel(),
                            "The imported mask must have a size of " + processor.getWidth() + "x" + processor.getHeight(),
                            "Import mask",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (image.getBitDepth() != 8) {
                    ImageConverter ic = new ImageConverter(image);
                    ic.convertToGray8();
                }
                processor.setRoi((Roi) null);
                processor.copyBits(image.getProcessor(), 0, 0, Blitter.COPY);
                recalculateMaskPreview();
                postMaskChangedEvent();
            }
        }
    }

    private void postMaskChangedEvent() {
        getViewerPanel2D().getCanvas().getEventBus().post(new MaskChangedEvent(this));
    }

    private void exportMask() {
        Path selectedFile = FileChooserSettings.saveFile(getViewerPanel(),
                FileChooserSettings.LastDirectoryKey.Data,
                "Export mask",
                UIUtils.EXTENSION_FILTER_TIFF);
        if (selectedFile != null) {
            try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                ImagePlus image = new ImagePlus("Mask", getCurrentMaskSlice());
                IJ.saveAsTiff(image, selectedFile.toString());
            }
        }
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
        drawCurrentMaskSlicePreview = currentMaskSlice.getStats().max > 0;
        getViewerPanel2D().getCanvas().repaint(50);
    }

    private <T> void addSelectionButton(T value, JPanel target, Map<T, JToggleButton> targetMap, ButtonGroup targetGroup, String text, String toolTip, Icon icon, Supplier<T> getter, Consumer<T> setter) {
        JToggleButton button = new JToggleButton(text, icon, Objects.equals(getter.get(), value));
        button.setMinimumSize(new Dimension(72, 48));
        button.setPreferredSize(new Dimension(72, 48));
        button.setMaximumSize(new Dimension(72, 48));
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
        ImageSliceIndex index = getViewerPanel2D().getCurrentSliceIndex();
        int z = Math.min(index.getZ(), mask.getNSlices() - 1);
        int c = Math.min(index.getC(), mask.getNChannels() - 1);
        int t = Math.min(index.getT(), mask.getNFrames() - 1);
        ImageProcessor lastMaskSlice = currentMaskSlice;
        currentMaskSlice = ImageJUtils.getSliceZero(mask, c, z, t);
        if (lastMaskSlice != currentMaskSlice) {
            recalculateMaskPreview();
        }
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
    public void onSliceChanged(boolean deferUploadSlice) {
        updateCurrentMaskSlice();
        currentTool.onSliceChanged(true);
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        if (getCurrentImage() == null) {
            return;
        }

        formPanel.addWideToForm(ribbon);
        exportToRoiManagerAction.setVisible(getViewerPanel().getPlugin(ROIManagerPlugin2D.class) != null);
        SwingUtilities.invokeLater(ribbon::rebuildRibbon);

        formPanel.addWideToForm(toolSettingsPanel);
        currentTool.initializeSettingsPanel(formPanel);
    }

    public Ribbon getRibbon() {
        return ribbon;
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
        ImageSliceIndex currentIndex = getViewerPanel2D().getCurrentSliceIndex();

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
        getViewerPanel2D().refreshFormPanel();
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
            postMaskChangedEvent();
        }
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        final int renderX = renderArea.x;
        final int renderY = renderArea.y;
        final int renderW = renderArea.width;
        final int renderH = renderArea.height;
        final double zoom = getViewerPanel2D().getCanvas().getZoom();
        if(drawCurrentMaskSlicePreview) {
            AffineTransform transform = new AffineTransform();
            transform.scale(zoom, zoom);
            BufferedImageOp op = new AffineTransformOp(transform, zoom < 1 ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            graphics2D.drawImage(currentMaskSlicePreview, op, renderX, renderY);
        }
//        currentTool.postprocessDraw(graphics2D, renderArea, sliceIndex);
        if (showGuidesToggle.isSelected() && currentTool.showGuides()) {
            graphics2D.setStroke(STROKE_GUIDE_LINE);
            graphics2D.setColor(getHighlightColor());
            Point mousePosition = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, false);
            if (mousePosition != null) {
                int displayedX = (int) (renderX + zoom * mousePosition.x);
                int displayedY = (int) (renderY + zoom * mousePosition.y);
                graphics2D.drawLine(renderX, displayedY, renderX + renderW, displayedY);
                graphics2D.drawLine(displayedX, renderY, displayedX, renderY + renderH);
            }
        }
    }

    @Override
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex, double magnification) {
        if (mask == null)
            return;
        int z = Math.min(mask.getNSlices() - 1, sliceIndex.getZ());
        int c = Math.min(mask.getNChannels() - 1, sliceIndex.getC());
        int t = Math.min(mask.getNFrames() - 1, sliceIndex.getT());
        ImageProcessor selectedMaskSlice = ImageJUtils.getSliceZero(mask, c, z, t);
        BufferedImage renderedMask = new BufferedImage(mask.getWidth(), mask.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        if (magnification != 1.0) {
            renderedMask = BufferedImageUtils.toBufferedImage(renderedMask.getScaledInstance(image.getWidth(), image.getHeight(), Image.SCALE_DEFAULT), BufferedImage.TYPE_4BYTE_ABGR);
        }
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

    public MaskDrawer2DTool getCurrentTool() {
        return currentTool;
    }

    private void rebuildToolSettings() {
        toolSettingsPanel.clear();
        if (currentTool != null) {
            toolSettingsPanel.addToForm(colorSelectionPanel, new JLabel("Current color"));
            toolSettingsPanel.addWideToForm(new JSeparator(SwingConstants.HORIZONTAL));
            int count = toolSettingsPanel.getComponentCount();
            currentTool.initializeSettingsPanel(toolSettingsPanel);
            if (toolSettingsPanel.getComponentCount() == count) {
                toolSettingsPanel.removeLastRow();
            }
        }
        toolSettingsPanel.revalidate();
        toolSettingsPanel.repaint();
    }

    @Subscribe
    public void onToolChanged(ImageViewerPanelCanvas2D.ToolChangedEvent event) {
        ImageViewerPanelCanvas2DTool newTool = event.getNewTool();
        MaskDrawer2DTool localTool;
        if (newTool instanceof MaskDrawer2DTool) {
            if (registeredTools.contains(newTool)) {
                localTool = (MaskDrawer2DTool) newTool;
            } else {
                localTool = mouseTool;
            }
        } else {
            localTool = mouseTool;
        }
        this.currentTool = localTool;
        rebuildToolSettings();
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
        private final MaskDrawerPlugin2D plugin;

        public MaskChangedEvent(MaskDrawerPlugin2D plugin) {
            this.plugin = plugin;
        }

        public MaskDrawerPlugin2D getPlugin() {
            return plugin;
        }
    }
}
