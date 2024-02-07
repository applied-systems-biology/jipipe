package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.util.Java2;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform.ScaleMode;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform.TransformScale2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.InterpolationMethod;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontFamilyParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom algorithm for creating montages
 */
public class MontageCreator extends AbstractJIPipeParameterCollection {

    private JIPipeExpressionParameter labelExpression = new JIPipeExpressionParameter("default_label");
    private OptionalJIPipeExpressionParameter sortingLabelExpression = new OptionalJIPipeExpressionParameter(false, "default_label");
    private OptionalIntegerParameter forceNumRows = new OptionalIntegerParameter();
    private OptionalIntegerParameter forceNumColumns = new OptionalIntegerParameter();
    private boolean forceRGB = false;
    private boolean rgbUseRender = true;
    private JIPipeExpressionParameter tileWidth = new JIPipeExpressionParameter("MAX(max_image_width, max_label_width)");
    private JIPipeExpressionParameter tileHeight = new JIPipeExpressionParameter("max_image_height");
    private final CanvasParameters canvasParameters;
    private final LabelParameters labelParameters;
    private final ImageParameters imageParameters;

    public MontageCreator() {
        this.canvasParameters = new CanvasParameters();
        this.labelParameters = new LabelParameters();
        this.imageParameters = new ImageParameters();
        registerSubParameters(canvasParameters, labelParameters, imageParameters);
    }

    public MontageCreator(MontageCreator other) {
        this.labelExpression = new JIPipeExpressionParameter(other.labelExpression);
        this.sortingLabelExpression = new OptionalJIPipeExpressionParameter(other.sortingLabelExpression);
        this.forceNumRows = new OptionalIntegerParameter(other.forceNumRows);
        this.forceNumColumns = new OptionalIntegerParameter(other.forceNumColumns);
        this.tileWidth = new JIPipeExpressionParameter(other.tileWidth);
        this.tileHeight = new JIPipeExpressionParameter(other.tileHeight);
        this.canvasParameters = new CanvasParameters(other.canvasParameters);
        this.labelParameters = new LabelParameters(other.labelParameters);
        this.imageParameters = new ImageParameters(other.imageParameters);
        this.forceRGB = other.forceRGB;
        this.rgbUseRender = other.rgbUseRender;
        registerSubParameters(canvasParameters, labelParameters, imageParameters);
    }

    public ImagePlus createMontage(List<InputEntry> inputEntries, List<JIPipeTextAnnotation> annotations, ExpressionVariables additionalVariables, JIPipeProgressInfo progressInfo) {
        List<LabelledImage> labelledImages = new ArrayList<>();

        for (InputEntry inputEntry : inputEntries) {
            ExpressionVariables variables = new ExpressionVariables();
            variables.putAll(additionalVariables);
            variables.putAnnotations(inputEntry.annotationList);
            variables.putAll(inputEntry.additionalVariables);

            String autoName = inputEntry.annotationList.stream().sorted(Comparator.comparing(JIPipeTextAnnotation::getName))
                    .map(JIPipeTextAnnotation::getValue).collect(Collectors.joining("_"));
            variables.set("default_label", autoName);
            Map<ImageSliceIndex, String> labels = new HashMap<>();

            ImagePlus img = inputEntry.getImagePlus();
            variables.putIfAbsent("width", img.getWidth());
            variables.putIfAbsent("height", img.getHeight());
            variables.putIfAbsent("size_d", img.getNDimensions());
            variables.putIfAbsent("size_c", img.getNChannels());
            variables.putIfAbsent("size_z", img.getNSlices());
            variables.putIfAbsent("size_t", img.getNFrames());
            boolean overrideC = !variables.containsKey("c");
            boolean overrideZ = !variables.containsKey("z");
            boolean overrideT = !variables.containsKey("t");
            for (int c = 0; c < img.getNChannels(); c++) {
                if(overrideC) {
                    variables.set("c", c);
                }
                for (int z = 0; z < img.getNSlices(); z++) {
                    if(overrideZ) {
                        variables.set("z", z);
                    }
                    for (int t = 0; t < img.getNFrames(); t++) {
                        if(overrideT) {
                            variables.set("t", t);
                        }
                        labels.put(new ImageSliceIndex(c,z,t), StringUtils.nullToEmpty(labelExpression.evaluateToString(variables)));
                    }
                }
            }

            String sortLabel = getSortingLabelExpression().isEnabled() ? StringUtils.nullToEmpty(getSortingLabelExpression().getContent().evaluateToString(variables)) :
                    labels.getOrDefault(new ImageSliceIndex(0,0,0), "");

            LabelledImage labelledImage = new LabelledImage(inputEntry.imagePlus, labels, sortLabel);
            labelledImages.add(labelledImage);
        }

        // Sort labelled images
        labelledImages = labelledImages.stream().sorted(Comparator.comparing(LabelledImage::getSortLabel, NaturalOrderComparator.INSTANCE)).collect(Collectors.toList());

        // Determine number of rows & columns
        final int nColumns;
        final int nRows;
        if (!this.forceNumColumns.isEnabled() && !this.forceNumRows.isEnabled()) {
            // No constraints
            nColumns = Math.max(1, (int) Math.sqrt(labelledImages.size()));
            nRows = (int) Math.ceil(1.0 * labelledImages.size() / nColumns);
        } else if (this.forceNumColumns.isEnabled() && !this.forceNumRows.isEnabled()) {
            // Columns constraints
            nColumns = this.forceNumColumns.getContent();
            nRows = (int) Math.ceil(1.0 * labelledImages.size() / nColumns);
        } else if (this.forceNumRows.isEnabled() && !this.forceNumColumns.isEnabled()) {
            // Rows constraints
            nRows = this.forceNumRows.getContent();
            nColumns = (int) Math.ceil(1.0 * labelledImages.size() / nRows);
        } else {
            nColumns = Math.max(1, this.forceNumColumns.getContent());
            nRows = Math.max(1, this.forceNumRows.getContent());

            // Restrict images
            if(labelledImages.size() > nColumns * nRows) {
                progressInfo.log("WARNING: Available spaces are " + (nColumns * nRows) + ", but there are " + labelledImages.size() + " images. Removing last images." );
                while(labelledImages.size() > nColumns * nRows) {
                    labelledImages.remove(labelledImages.size() - 1);
                }
            }
        }

        // Setup label font
        Font labelFont = null;
        FontMetrics labelFontMetrics = null;
        if(labelParameters.drawLabel){
            labelFont = labelParameters.getLabelFontFamily().toFont(Font.PLAIN, labelParameters.labelSize);

            BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = (Graphics2D)tmp.getGraphics();
            g.setFont(labelFont);
            Java2.setAntialiasedText(g, true);
            labelFontMetrics = g.getFontMetrics(labelFont);
        }

        // Determine target image size
        boolean topLabel = false;
        int labelBorder = 0;
        int imageWidth;
        int imageHeight;
        int gridWidth;
        int gridHeight;
        {
            int maxImageWidth = 0;
            int maxImageHeight = 0;
            int maxLabelWidth = 0;
            for(LabelledImage labelledImage : labelledImages) {
                maxImageWidth = Math.max(labelledImage.getImagePlus().getWidth(), maxImageWidth);
                maxImageHeight = Math.max(labelledImage.getImagePlus().getHeight(), maxImageHeight);
                if(labelParameters.drawLabel) {
                    assert labelFontMetrics != null;
                    for (Map.Entry<ImageSliceIndex, String> entry : labelledImage.labels.entrySet()) {
                        maxLabelWidth = Math.max(labelFontMetrics.stringWidth(entry.getValue()), maxLabelWidth);
                    }
                }
            }
            ExpressionVariables variables = new ExpressionVariables();
            variables.putAnnotations(annotations);
            variables.put("max_image_width", maxImageWidth);
            variables.put("max_image_height", maxImageHeight);
            variables.put("max_label_width", maxLabelWidth);

            imageWidth = this.tileWidth.evaluateToInteger(variables);
            imageHeight = this.tileHeight.evaluateToInteger(variables);

            gridWidth = imageWidth;
            gridHeight = imageHeight;

            // Top/Bottom label border
            if(labelParameters.drawLabel && labelParameters.avoidLabelOnImage) {
                switch (labelParameters.labelLocation) {
                    case TopLeft:
                    case TopRight:
                    case TopCenter:
                        topLabel = true;
                        // fallthrough
                    case BottomLeft:
                    case BottomCenter:
                    case BottomRight:
                        labelBorder = Objects.requireNonNull(labelFontMetrics).getHeight();
                        gridHeight += labelFont.getSize();
                        break;
                }
            }

            // Border
            if(canvasParameters.borderSize > 0) {
                gridWidth += canvasParameters.borderSize * 2;
                gridHeight += canvasParameters.borderSize * 2;
            }
        }

        // Create target image
        final boolean requestColorImage = forceRGB || !ColorUtils.isGreyscale(canvasParameters.getCanvasBackgroundColor()) || (canvasParameters.borderSize > 0 && !ColorUtils.isGreyscale(canvasParameters.borderColor));
        if(requestColorImage) {
            progressInfo.log("INFO: Canvas or borders are colored. Forcing RGB color output!");
        }
        final int consensusBitDepth = requestColorImage ? 24 : ImageJUtils.getConsensusBitDepth(labelledImages.stream().map(LabelledImage::getImagePlus).collect(Collectors.toList()));
        final int outputWidth = gridWidth * nColumns;
        final int outputHeight = gridHeight * nRows;
        final int numZ = labelledImages.stream().map(img -> img.getImagePlus().getNSlices()).max(Comparator.naturalOrder()).orElse(1);
        final int numC = labelledImages.stream().map(img -> img.getImagePlus().getNChannels()).max(Comparator.naturalOrder()).orElse(1);
        final int numT = labelledImages.stream().map(img -> img.getImagePlus().getNFrames()).max(Comparator.naturalOrder()).orElse(1);
        progressInfo.log("Output size is " + outputWidth + "x" + outputHeight + " #z=" + numZ + " #c=" + numC + " #t=" + numT + " (" + nColumns + " columns, " + nRows + " rows)");

        final ImagePlus targetImage = IJ.createHyperStack("Montage", outputWidth, outputHeight, numC, numZ, numT, consensusBitDepth);

        // Color canvas
        ImageJUtils.forEachSlice(targetImage, ip -> {
            if(canvasParameters.borderSize > 0) {
                // Use border color instead
                ip.setColor(canvasParameters.borderColor);
                ip.fill();
            }
            else {
                // Fill with bgr color
                ip.setColor(canvasParameters.canvasBackgroundColor);
                ip.fill();
            }
        }, progressInfo.resolve("Prepare canvas"));

        for (int i = 0; i < labelledImages.size(); i++) {
            LabelledImage labelledImage = labelledImages.get(i);
            JIPipeProgressInfo mergingProgress = progressInfo.resolveAndLog("Merging", i, labelledImages.size());
            int col = i % nColumns;
            int row = i / nColumns;
            int imgX = col * gridWidth + canvasParameters.borderSize;
            int imgY = row * gridHeight + canvasParameters.borderSize;
            int imgAreaH = imageHeight;
            if(topLabel) {
                imgY += labelBorder;
            }
            else {
                imgAreaH += labelBorder;
            }
            int finalImgY = imgY;

            // Canvas background (if border)
            if(canvasParameters.borderSize > 0) {
                if(topLabel) {
                    int finalImgAreaH = imgAreaH;
                    int finalGridHeight = gridHeight;
                    ImageJUtils.forEachSlice(targetImage, ip -> {
                        ip.setColor(canvasParameters.canvasBackgroundColor);
                        ip.setRoi(new Rectangle(imgX, row * finalGridHeight + canvasParameters.borderSize, imageWidth, finalImgAreaH));
                        ip.fill();
                    }, mergingProgress.resolve("Prepare canvas"));
                }
                else {
                    int finalImgAreaH = imgAreaH;
                    ImageJUtils.forEachSlice(targetImage, ip -> {
                        ip.setColor(canvasParameters.canvasBackgroundColor);
                        ip.setRoi(new Rectangle(imgX, finalImgY, imageWidth, finalImgAreaH));
                        ip.fill();
                    }, mergingProgress.resolve("Prepare canvas"));
                }
            }

            // Write image
            ImagePlus rawImage = labelledImage.getImagePlus();
            ImagePlus processedImage;
            if(imageParameters.avoidScaling && rawImage.getWidth() <= imageWidth && rawImage.getHeight() <= imageHeight) {
                // No processing needed
                processedImage = rawImage;
            }
            else {
                // Scaling the image
                processedImage = ImageJUtils.generateForEachIndexedZCTSlice(rawImage, (ip, index) -> TransformScale2DAlgorithm.scaleProcessor(ip,
                        imageWidth,
                        imageHeight,
                        imageParameters.interpolationMethod,
                        imageParameters.interpolationMethod != InterpolationMethod.None,
                        imageParameters.scaleMode,
                        canvasParameters.canvasAnchor,
                        canvasParameters.canvasBackgroundColor), mergingProgress.resolve("Scaling"));
            }

            assert processedImage != null;

            // Color conversion
            if(consensusBitDepth == 24 && rgbUseRender) {
                processedImage = ImageJUtils.renderToRGBWithLUTIfNeeded(processedImage, mergingProgress);
            }
            else {
                processedImage = ImageJUtils.convertToBitDepthIfNeeded(processedImage, consensusBitDepth);
            }

            Rectangle finalRect = canvasParameters.canvasAnchor.placeInside(
                    new Rectangle(0, 0, processedImage.getWidth(), processedImage.getHeight()),
                    new Rectangle(0, 0, imageWidth, imageHeight));

            // Draw image
            ImageJUtils.forEachIndexedZCTSlice(processedImage, (sourceIp, index) -> {
                ImageProcessor targetIp = ImageJUtils.getSliceZero(targetImage, index);
                targetIp.setRoi((Roi) null);
                targetIp.insert(sourceIp, imgX + finalRect.x, finalImgY + finalRect.y);
            }, mergingProgress.resolve("Copy image"));

            // Draw label
            if(labelParameters.drawLabel && labelFont != null) {
                Rectangle drawArea = new Rectangle(imgX, row * gridHeight + canvasParameters.borderSize, imageWidth, imageHeight + labelBorder);
                Font lf = labelFont;
                FontMetrics finalLabelFontMetrics = labelFontMetrics;
                ImageJUtils.forEachIndexedZCTSlice(processedImage, (sourceIp, index) -> {
                    ImageProcessor targetIp = ImageJUtils.getSliceZero(targetImage, index);
                    targetIp.setRoi((Roi) null);
                    String label = labelledImage.labels.get(index);
                    if(labelParameters.limitWithEllipsis) {
                        label = StringUtils.limitWithEllipsis(label, drawArea.width, finalLabelFontMetrics);
                    }
                    else {
                        label = StringUtils.limitWithoutEllipsis(label, drawArea.width, finalLabelFontMetrics);
                    }
                    ImageJUtils.drawAnchoredStringLabel(targetIp,
                            label,
                            drawArea,
                            labelParameters.labelForeground,
                            labelParameters.labelBackground.getContent(),
                            lf,
                            labelParameters.labelBackground.isEnabled(),
                            labelParameters.labelLocation);
//                    targetIp.setColor(Color.RED);
//                    targetIp.drawRect(drawArea.x, drawArea.y, drawArea.width, drawArea.height);
                }, mergingProgress.resolve("Draw label"));
            }

        }
        return targetImage;

    }

    @JIPipeDocumentation(name = "Render to RGB", description = "If enabled 'Render to RGB' is used instead of a plain conversion to RGB. " +
            "Only applicable if images are converted to RGB.")
    @JIPipeParameter("rgb-use-render")
    public boolean isRgbUseRender() {
        return rgbUseRender;
    }

    @JIPipeParameter("rgb-use-render")
    public void setRgbUseRender(boolean rgbUseRender) {
        this.rgbUseRender = rgbUseRender;
    }

    @JIPipeDocumentation(name = "Force RGB colors", description = "If enabled, force rendering to RGB colors")
    @JIPipeParameter("force-rgb")
    public boolean isForceRGB() {
        return forceRGB;
    }

    @JIPipeParameter("force-rgb")
    public void setForceRGB(boolean forceRGB) {
        this.forceRGB = forceRGB;
    }

    @JIPipeDocumentation(name = "Label", description = "Expression that generates the labels. Applied per image.")
    @JIPipeParameter(value = "label-expression", important = true, uiOrder = -100)
    @JIPipeExpressionParameterSettings(hint = "per image slice")
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "default_label", name = "Default label", description = "Default label that summarizes the annotations")
    @JIPipeExpressionParameterVariable(fromClass = Image5DSliceIndexExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getLabelExpression() {
        return labelExpression;
    }

    @JIPipeParameter("label-expression")
    public void setLabelExpression(JIPipeExpressionParameter labelExpression) {
        this.labelExpression = labelExpression;
    }

    @JIPipeDocumentation(name = "Custom sorting label", description = "Optional expression that generates dedicated labels that are only used for sorting. Applied per image.")
    @JIPipeParameter("sorting-label-expression")
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "default_label", name = "Default label", description = "Default label that summarizes the annotations")
    @JIPipeExpressionParameterSettings(hint = "per image")
    @JIPipeExpressionParameterVariable(fromClass = Image5DSliceIndexExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getSortingLabelExpression() {
        return sortingLabelExpression;
    }

    @JIPipeParameter("sorting-label-expression")
    public void setSortingLabelExpression(OptionalJIPipeExpressionParameter sortingLabelExpression) {
        this.sortingLabelExpression = sortingLabelExpression;
    }

    @JIPipeDocumentation(name = "Force number of rows", description = "If enabled, forces the number of rows")
    @JIPipeParameter("force-rows")
    public OptionalIntegerParameter getForceNumRows() {
        return forceNumRows;
    }

    @JIPipeParameter("force-rows")
    public void setForceNumRows(OptionalIntegerParameter forceNumRows) {
        this.forceNumRows = forceNumRows;
    }

    @JIPipeDocumentation(name = "Force number of columns", description = "If enabled, forces the number of columns")
    @JIPipeParameter("force-columns")
    public OptionalIntegerParameter getForceNumColumns() {
        return forceNumColumns;
    }

    @JIPipeParameter("force-columns")
    public void setForceNumColumns(OptionalIntegerParameter forceNumColumns) {
        this.forceNumColumns = forceNumColumns;
    }

    @JIPipeDocumentation(name = "Canvas", description = "Settings related to how images are put into the canvas")
    @JIPipeParameter("canvas-parameters")
    public CanvasParameters getCanvasParameters() {
        return canvasParameters;
    }

    @JIPipeDocumentation(name = "Labels", description = "Settings related to the display of labels")
    @JIPipeParameter("label-parameters")
    public LabelParameters getLabelParameters() {
        return labelParameters;
    }

    @JIPipeDocumentation(name = "Tile width", description = "Expression that determines the width of the tiles")
    @JIPipeParameter("tile-width")
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "max_image_width", name = "Maximum image width", description = "The maximum width of the input images")
    @JIPipeExpressionParameterVariable(key = "max_image_height", name = "Maximum image height", description = "The maximum height of the input images")
    @JIPipeExpressionParameterVariable(key = "max_label_width", name = "Maximum label width", description = "The maximum width of all labels. Zero if no labels are drawn.")
    public JIPipeExpressionParameter getTileWidth() {
        return tileWidth;
    }

    @JIPipeParameter("tile-width")
    public void setTileWidth(JIPipeExpressionParameter tileWidth) {
        this.tileWidth = tileWidth;
    }

    @JIPipeDocumentation(name = "Tile height", description = "Expression that determines the height of the tiles")
    @JIPipeParameter("tile-height")
    @JIPipeExpressionParameterVariable(key = "max_image_width", name = "Maximum image width", description = "The maximum width of the input images")
    @JIPipeExpressionParameterVariable(key = "max_image_height", name = "Maximum image height", description = "The maximum height of the input images")
    @JIPipeExpressionParameterVariable(key = "max_label_width", name = "Maximum label width", description = "The maximum width of all labels. Zero if no labels are drawn.")
    public JIPipeExpressionParameter getTileHeight() {
        return tileHeight;
    }

    @JIPipeParameter("tile-height")
    public void setTileHeight(JIPipeExpressionParameter tileHeight) {
        this.tileHeight = tileHeight;
    }

    @JIPipeDocumentation(name = "Images", description = "Settings related to the scaling of images")
    @JIPipeParameter("image-parameters")
    public ImageParameters getImageParameters() {
        return imageParameters;
    }

    public static class LabelParameters extends AbstractJIPipeParameterCollection {
        private boolean drawLabel = true;
        private Color labelForeground = Color.WHITE;
        private FontFamilyParameter labelFontFamily = new FontFamilyParameter();
        private int labelSize = 12;
        private Anchor labelLocation = Anchor.BottomCenter;
        private boolean avoidLabelOnImage = true;
        private boolean limitWithEllipsis = true;

        private OptionalColorParameter labelBackground = new OptionalColorParameter(Color.BLACK, true);

        public LabelParameters() {
        }

        public LabelParameters(LabelParameters other) {
            this.drawLabel = other.drawLabel;
            this.labelForeground = other.labelForeground;
            this.labelFontFamily = new FontFamilyParameter(other.labelFontFamily);
            this.labelSize = other.labelSize;
            this.labelLocation = other.labelLocation;
            this.avoidLabelOnImage = other.avoidLabelOnImage;
            this.labelBackground = new OptionalColorParameter(other.labelBackground);
            this.limitWithEllipsis = other.limitWithEllipsis;
        }

        @JIPipeDocumentation(name = "Limit with ellipsis", description = "If enabled, long labels will be limited with an ellipsis. Otherwise, the text is just cut off.")
        @JIPipeParameter("limit-with-ellipsis")
        public boolean isLimitWithEllipsis() {
            return limitWithEllipsis;
        }

        @JIPipeParameter("limit-with-ellipsis")
        public void setLimitWithEllipsis(boolean limitWithEllipsis) {
            this.limitWithEllipsis = limitWithEllipsis;
        }

        @JIPipeDocumentation(name = "Avoid drawing label on image", description = "If enabled, avoid that that label is drawn on top of the image")
        @JIPipeParameter("avoid-label-on-image")
        public boolean isAvoidLabelOnImage() {
            return avoidLabelOnImage;
        }

        @JIPipeParameter("avoid-label-on-image")
        public void setAvoidLabelOnImage(boolean avoidLabelOnImage) {
            this.avoidLabelOnImage = avoidLabelOnImage;
        }

        @JIPipeDocumentation(name = "Label background", description = "If enabled, draw a background behind the label")
        @JIPipeParameter("label-background")
        public OptionalColorParameter getLabelBackground() {
            return labelBackground;
        }

        @JIPipeParameter("label-background")
        public void setLabelBackground(OptionalColorParameter labelBackground) {
            this.labelBackground = labelBackground;
        }

        @JIPipeDocumentation(name = "Draw label", description = "If enabled, draw a label for each image")
        @JIPipeParameter("draw-label")
        public boolean isDrawLabel() {
            return drawLabel;
        }

        @JIPipeParameter("draw-label")
        public void setDrawLabel(boolean drawLabel) {
            this.drawLabel = drawLabel;
        }

        @JIPipeDocumentation(name = "Label color", description = "The color of the label")
        @JIPipeParameter("label-foreground")
        public Color getLabelForeground() {
            return labelForeground;
        }

        @JIPipeParameter("label-foreground")
        public void setLabelForeground(Color labelForeground) {
            this.labelForeground = labelForeground;
        }

        @JIPipeDocumentation(name = "Label font family", description = "The font of the label")
        @JIPipeParameter("label-font-family")
        public FontFamilyParameter getLabelFontFamily() {
            return labelFontFamily;
        }

        @JIPipeParameter("label-font-family")
        public void setLabelFontFamily(FontFamilyParameter labelFontFamily) {
            this.labelFontFamily = labelFontFamily;
        }

        @JIPipeDocumentation(name = "Label font size", description = "The font size of the drawn label")
        @JIPipeParameter("label-font-size")
        public int getLabelSize() {
            return labelSize;
        }

        @JIPipeParameter("label-font-size")
        public void setLabelSize(int labelSize) {
            this.labelSize = labelSize;
        }

        @JIPipeDocumentation(name = "Label location", description = "The location of the label")
        @JIPipeParameter("label-location")
        public Anchor getLabelLocation() {
            return labelLocation;
        }

        @JIPipeParameter("label-location")
        public void setLabelLocation(Anchor labelLocation) {
            this.labelLocation = labelLocation;
        }
    }

    public static class CanvasParameters extends AbstractJIPipeParameterCollection {
        private Color canvasBackgroundColor = Color.BLACK;
        private Anchor canvasAnchor = Anchor.CenterCenter;
        private int borderSize = 0;
        private Color borderColor = Color.GRAY;

        public CanvasParameters() {

        }

        public CanvasParameters(CanvasParameters other) {
            this.canvasBackgroundColor = other.canvasBackgroundColor;
            this.canvasAnchor = other.canvasAnchor;
            this.borderSize = other.borderSize;
            this.borderColor = other.borderColor;
        }

        @JIPipeDocumentation(name = "Canvas background color", description = "The background color of the canvas")
        @JIPipeParameter("canvas-background-color")
        public Color getCanvasBackgroundColor() {
            return canvasBackgroundColor;
        }

        @JIPipeParameter("canvas-background-color")
        public void setCanvasBackgroundColor(Color canvasBackgroundColor) {
            this.canvasBackgroundColor = canvasBackgroundColor;
        }

        @JIPipeDocumentation(name = "Canvas image location", description = "Location of the image inside the canvas")
        @JIPipeParameter("canvas-anchor")
        public Anchor getCanvasAnchor() {
            return canvasAnchor;
        }

        @JIPipeParameter("canvas-anchor")
        public void setCanvasAnchor(Anchor canvasAnchor) {
            this.canvasAnchor = canvasAnchor;
        }

        @JIPipeDocumentation(name = "Border size", description = "Size of the border drawn around the image canvas")
        @JIPipeParameter("border-size")
        public int getBorderSize() {
            return borderSize;
        }

        @JIPipeParameter("border-size")
        public void setBorderSize(int borderSize) {
            this.borderSize = borderSize;
        }

        @JIPipeDocumentation(name = "Border color", description = "The color of the border")
        @JIPipeParameter("border-color")
        public Color getBorderColor() {
            return borderColor;
        }

        @JIPipeParameter("border-color")
        public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }
    }

    public static class ImageParameters extends AbstractJIPipeParameterCollection {
        private InterpolationMethod interpolationMethod = InterpolationMethod.None;
        private ScaleMode scaleMode = ScaleMode.Fit;
        private boolean avoidScaling = true;

        public ImageParameters() {
        }

        public ImageParameters(ImageParameters other) {
            this.interpolationMethod = other.interpolationMethod;
            this.scaleMode = other.scaleMode;
            this.avoidScaling = other.avoidScaling;
        }

        @JIPipeDocumentation(name = "Interpolation method", description = "The interpolation method if images are scaled")
        @JIPipeParameter("interpolation-method")
        public InterpolationMethod getInterpolationMethod() {
            return interpolationMethod;
        }

        @JIPipeParameter("interpolation-method")
        public void setInterpolationMethod(InterpolationMethod interpolationMethod) {
            this.interpolationMethod = interpolationMethod;
        }

        @JIPipeDocumentation(name = "Scale mode", description = "The way how the image is scaled (if enabled)")
        @JIPipeParameter("scale-mode")
        public ScaleMode getScaleMode() {
            return scaleMode;
        }

        @JIPipeParameter("scale-mode")
        public void setScaleMode(ScaleMode scaleMode) {
            this.scaleMode = scaleMode;
        }

        @JIPipeDocumentation(name = "Avoid scaling",description = "If enabled, images that are small enough will not be scaled")
        @JIPipeParameter("avoid-scaling")
        public boolean isAvoidScaling() {
            return avoidScaling;
        }

        @JIPipeParameter("avoid-scaling")
        public void setAvoidScaling(boolean avoidScaling) {
            this.avoidScaling = avoidScaling;
        }
    }

    public static class InputEntry {
        private final ImagePlus imagePlus;
        private final List<JIPipeTextAnnotation> annotationList;
        private final ExpressionVariables additionalVariables;

        public InputEntry(ImagePlus imagePlus, List<JIPipeTextAnnotation> annotationList, ExpressionVariables additionalVariables) {
            this.imagePlus = imagePlus;
            this.annotationList = annotationList;
            this.additionalVariables = additionalVariables;
        }

        public ImagePlus getImagePlus() {
            return imagePlus;
        }

        public List<JIPipeTextAnnotation> getAnnotationList() {
            return annotationList;
        }

        public ExpressionVariables getAdditionalVariables() {
            return additionalVariables;
        }
    }

    public static class LabelledImage {
        private final ImagePlus imagePlus;
        private final Map<ImageSliceIndex, String> labels;
        private final String sortLabel;

        public LabelledImage(ImagePlus imagePlus, Map<ImageSliceIndex, String> labels, String sortLabel) {
            this.imagePlus = imagePlus;
            this.labels = labels;
            this.sortLabel = sortLabel;
        }

        public ImagePlus getImagePlus() {
            return imagePlus;
        }

        public Map<ImageSliceIndex, String> getLabels() {
            return labels;
        }

        public String getSortLabel() {
            return sortLabel;
        }
    }
}
