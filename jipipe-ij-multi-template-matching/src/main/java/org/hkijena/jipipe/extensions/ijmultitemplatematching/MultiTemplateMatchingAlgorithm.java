/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.ijmultitemplatematching;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.OptionalDataInfoRefParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.python.core.PyList;
import org.python.util.PythonInterpreter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Multi-Template matching", description = "Template matching is an algorithm that can be used for object-detections in grayscale images. " +
        "To perform template matching you will need a template image that is searched in the target image.\n" +
        "The best is to simply crop a typical region of interest from a representative image.\n\n\n\n" +
        "The algorithm computes the probability to find one (or several) template images provided by the user into a larger image. The algorithm uses the template image as a sliding window translated over the image, and at each position of the template computes a similarity score between the template and the image patch.\n" +
        "This results in a correlation map, for which the pixel value at position (x,y) is proportional to the similarity between the template and image patch, at this (x,y) position in the image. " +
        "The computation of the correlation map is followed by extrema detection to list the possible locations of the template in the image. Each extrema corresponds to the possible location of a bounding box of dimensions identical to the template. " +
        "The extrema detection can list a large number of extrema in the first place. Usually a threshold on the score is then used to limit this number (i.e. returning local extrema with a score above/below the threshold).\n" +
        "To prevent overlapping detection of the same object, Non-Maxima Supression is performed after extrema detection.\n\n\n\n" +
        "Please visit https://github.com/multi-template-matching/MultiTemplateMatching-Fiji/wiki for more information about the Multi-Template Matching plugin.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Template", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", autoCreate = true, description = "Table containing information about the matched templates. To access the templates directly, enable 'Output matched templates'")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Assembled templates")
@JIPipeNode(menuPath = "Analyze", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMulti-Template-Matching")
public class MultiTemplateMatchingAlgorithm extends JIPipeMergingAlgorithm {

    public static final JIPipeDataSlotInfo OUTPUT_SLOT_MATCHED_TEMPLATES = new JIPipeDataSlotInfo(JIPipeDataTable.class, JIPipeSlotType.Output, "Matched templates", "Measurements attached to the matched templates.");

    private final static String SCRIPT = loadScriptFromResources();

    private boolean flipTemplateVertically = true;
    private boolean flipTemplateHorizontally = true;
    private IntegerRange rotateTemplate = new IntegerRange();
    private TemplateMatchingMethod templateMatchingMethod = TemplateMatchingMethod.NormalizedZeroMeanCrossCorrelation;
    private int expectedNumberOfObjects = 1;
    private double multiObjectScoreThreshold = 0.5;
    private double multiObjectMaximumBoundingBoxOverlap = 0.3;
    private boolean restrictToROI = false;
    private boolean assembleTemplates = false;

    private boolean outputMatchedTemplates = false;
    private boolean withNonMaximaSuppression = true;
    private OptionalColorParameter assembleTemplatesBackground = new OptionalColorParameter();
    private OptionalDataInfoRefParameter assembleTemplatesOutput = new OptionalDataInfoRefParameter();

    public MultiTemplateMatchingAlgorithm(JIPipeNodeInfo info) {
        super(info);
        assembleTemplatesBackground.setContent(Color.BLACK);
        rotateTemplate.setValue("90,180,270");
        assembleTemplatesOutput.setContent(new JIPipeDataInfoRef(JIPipeDataInfo.getInstance(ImagePlusData.class)));
    }

    public MultiTemplateMatchingAlgorithm(MultiTemplateMatchingAlgorithm other) {
        super(other);
        this.flipTemplateVertically = other.flipTemplateVertically;
        this.flipTemplateHorizontally = other.flipTemplateHorizontally;
        this.rotateTemplate = new IntegerRange(other.rotateTemplate);
        this.templateMatchingMethod = other.templateMatchingMethod;
        this.expectedNumberOfObjects = other.expectedNumberOfObjects;
        this.multiObjectScoreThreshold = other.multiObjectScoreThreshold;
        this.multiObjectMaximumBoundingBoxOverlap = other.multiObjectMaximumBoundingBoxOverlap;
        this.setAssembleTemplates(other.assembleTemplates);
        this.assembleTemplatesBackground = new OptionalColorParameter(other.assembleTemplatesBackground);
        this.setRestrictToROI(other.restrictToROI);
        this.withNonMaximaSuppression = other.withNonMaximaSuppression;
        this.setOutputMatchedTemplates(other.outputMatchedTemplates);
    }

    private static String loadScriptFromResources() {
        return ResourceUtils.getPluginResourceAsString("extensions/ijmultitemplatematching/template-matching-script.py");
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        List<ImagePlus> images = new ArrayList<>();
        List<ImagePlus> templates = new ArrayList<>();
        Map<ImagePlus, Integer> templateSourceRows = new HashMap<>();
        ROIListData mergedSearchRois = new ROIListData();

        for (ImagePlusData image : iterationStep.getInputData("Image", ImagePlusData.class, progressInfo)) {
            images.add(image.getImage());
        }
        // Each template has its own index
        for (Integer row : iterationStep.getInputRows("Template")) {
            ImagePlus duplicateImage = getInputSlot("Template").getData(row, ImagePlusData.class, progressInfo).getDuplicateImage();
            duplicateImage.setTitle("" + templates.size());
            templates.add(duplicateImage);
            templateSourceRows.put(duplicateImage, row);
        }
        if (restrictToROI) {
            for (ROIListData roi : iterationStep.getInputData("ROI", ROIListData.class, progressInfo)) {
                mergedSearchRois.addAll(roi);
            }
        }
        Roi searchRoi = mergedSearchRois.isEmpty() ? null : new ShapeRoi(mergedSearchRois.getBounds());

        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        pythonInterpreter.set("Method", templateMatchingMethod.getIndex());
        pythonInterpreter.set("fliph", flipTemplateHorizontally);
        pythonInterpreter.set("flipv", flipTemplateVertically);
        pythonInterpreter.set("angles", rotateTemplate.getIntegers(0, 365, new JIPipeExpressionVariablesMap()).stream().map(Object::toString).collect(Collectors.joining(",")));
        pythonInterpreter.set("n_hit", expectedNumberOfObjects);
        pythonInterpreter.set("score_threshold", multiObjectScoreThreshold);
        pythonInterpreter.set("tolerance", 0);
        pythonInterpreter.set("max_overlap", multiObjectMaximumBoundingBoxOverlap);
        pythonInterpreter.set("List_Template", new PyList(templates));
        pythonInterpreter.set("Bool_SearchRoi", restrictToROI && searchRoi != null);
        pythonInterpreter.set("searchRoi", searchRoi);
        pythonInterpreter.set("progress", progressInfo);
        pythonInterpreter.set("with_nms", withNonMaximaSuppression);

        for (int i = 0; i < images.size(); i++) {
            ImagePlus image = images.get(i);
            ROIListData detectedROIs = new ROIListData();
            ResultsTableData measurements = new ResultsTableData();

            pythonInterpreter.set("ImpImage", image);
            pythonInterpreter.set("rm", detectedROIs);
            pythonInterpreter.set("Table", measurements.getTable());
            pythonInterpreter.set("progress", progressInfo.resolve("Image", i, images.size()));
            pythonInterpreter.exec(SCRIPT);

            iterationStep.addOutputData("ROI", detectedROIs, progressInfo);
            iterationStep.addOutputData("Measurements", measurements, progressInfo);

            if (assembleTemplates) {
                ImagePlus assembled = assembleTemplates(image, templates, measurements, progressInfo.resolveAndLog("Assemble templates", i, images.size()));
                iterationStep.addOutputData("Assembled templates", new ImagePlusData(assembled), progressInfo);
            }
            if (outputMatchedTemplates) {
                JIPipeDataTable matchedTemplates = new JIPipeDataTable(ImagePlus2DData.class);
                for (int j = 0; j < measurements.getRowCount(); j++) {
                    String templateName = measurements.getValueAsString(j, "Template");
                    int templateIndex = Integer.parseInt(templateName.split("_")[0]);
                    ImagePlus template = templates.get(templateIndex);

                    int sourceRow = templateSourceRows.get(template);
                    List<JIPipeTextAnnotation> textAnnotations = new ArrayList<>(getInputSlot("Template").getTextAnnotations(sourceRow));
                    List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>(getInputSlot("Template").getDataAnnotations(sourceRow));

                    for (int k = 0; k < measurements.getColumnCount(); k++) {
                        textAnnotations.add(new JIPipeTextAnnotation(measurements.getColumnName(k), measurements.getValueAsString(j, k)));
                    }

                    matchedTemplates.addData(new ImagePlus2DData(template),
                            textAnnotations,
                            JIPipeTextAnnotationMergeMode.OverwriteExisting,
                            dataAnnotations,
                            JIPipeDataAnnotationMergeMode.OverwriteExisting,
                            iterationStep.createNewContext(),
                            progressInfo);
                }

                iterationStep.addOutputData("Matched templates", matchedTemplates, progressInfo);
            }
        }

    }

    private ImagePlus assembleTemplates(ImagePlus original, List<ImagePlus> templates, ResultsTableData measurements, JIPipeProgressInfo progressInfo) {
        // Create a target image of the appropriate type
        ImagePlus target;
        if (assembleTemplatesOutput.isEnabled()) {
            ImagePlusData data = (ImagePlusData) JIPipe.createData(assembleTemplatesOutput.getContent().getInfo().getDataClass(), original);
            if (data.getImage() == original)
                target = data.getDuplicateImage();
            else
                target = data.getImage();
        } else {
            target = ImageJUtils.duplicate(original);
        }

        // Fill with color if requested
        target = ImageJUtils.channelsToRGB(target);
        if (assembleTemplatesBackground.isEnabled()) {
            if (target.getType() == ImagePlus.COLOR_RGB) {
                Color color = assembleTemplatesBackground.getContent();
                ImageJUtils.forEachSlice(target, ip -> {
                    ColorProcessor colorProcessor = (ColorProcessor) ip;
                    ip.setRoi(0, 0, ip.getWidth(), ip.getHeight());
                    colorProcessor.setColor(color);
                    ip.fill();
                    ip.setRoi((Roi) null);
                }, progressInfo);
            } else {
                Color color = assembleTemplatesBackground.getContent();
                double value = (color.getRed() + color.getGreen() + color.getBlue()) / 3.0;
                ImageJUtils.forEachSlice(target, ip -> ip.set(value), progressInfo);
            }
        }

        // Draw the templates
        ImageProcessor targetProcessor = target.getProcessor();
        for (int row = 0; row < measurements.getRowCount(); row++) {
            String templateName = measurements.getValueAsString(row, "Template");
            boolean verticalFlip = templateName.contains("Vertical_Flip");
            boolean horizontalFlip = templateName.contains("Horizontal_Flip");
            int rotation = 0;
            for (String s : templateName.split("_")) {
                if (s.endsWith("degrees")) {
                    rotation = Integer.parseInt(s.substring(0, s.indexOf("degrees")));
                }
            }
            int templateIndex = Integer.parseInt(templateName.split("_")[0]);
            ImagePlus template = ImageJUtils.duplicate(templates.get(templateIndex));
            ImageProcessor templateProcessor = template.getProcessor();
            if (verticalFlip)
                templateProcessor.flipVertical();
            if (horizontalFlip)
                templateProcessor.flipHorizontal();
            if (rotation != 0) {
                template = ImageJUtils.rotate(template, rotation, true, Color.BLACK, true, progressInfo);
                templateProcessor = template.getProcessor();
            }
            if (template.getRoi() == null) {
                template.setRoi(new Rectangle(0, 0, template.getWidth(), template.getHeight()));
            }
            Roi templateRoi = template.getRoi();
            int locationX = (int) measurements.getValueAsDouble(row, "Xcorner");
            int locationY = (int) measurements.getValueAsDouble(row, "Ycorner");
            for (int y = 0; y < templateProcessor.getHeight(); y++) {
                int targetY = y + locationY;
                if (targetY < 0)
                    continue;
                if (targetY >= targetProcessor.getHeight())
                    break;
                for (int x = 0; x < templateProcessor.getWidth(); x++) {
                    int targetX = x + locationX;
                    if (targetX < 0)
                        continue;
                    if (targetX >= targetProcessor.getWidth())
                        break;
                    if (!templateRoi.contains(x, y))
                        continue;
                    targetProcessor.setf(targetX, targetY, templateProcessor.getf(x, y));
                }
            }
        }

        return target;
    }

    @JIPipeDocumentation(name = "Output matched templates", description = "If enabled, the measurements are also returned as data table containing the actual template images.")
    @JIPipeParameter("output-matched-templates")
    public boolean isOutputMatchedTemplates() {
        return outputMatchedTemplates;
    }

    @JIPipeParameter("output-matched-templates")
    public void setOutputMatchedTemplates(boolean outputMatchedTemplates) {
        this.outputMatchedTemplates = outputMatchedTemplates;
        toggleSlot(OUTPUT_SLOT_MATCHED_TEMPLATES, outputMatchedTemplates);
    }

    @JIPipeDocumentation(name = "Flip template vertically", description = "Performing additional searches with the transformed template allows to maximize the probability to find the object, if the object is expected to have different orientations in the image.\n" +
            "This is due to the fact that the template matching only looks for translated version of the templates provided.\n" +
            "Possible transformations include flipping (also called mirroring), rotation (below). Scaling is not proposed in the interface but several templates at different scale can be provided in the multiple template version of the plugin.\n" +
            "If vertical and horizontal flipping are selected, then the plugin generates 2 additional templates for the corresponding transformation.")
    @JIPipeParameter("flip-template-vertically")
    public boolean isFlipTemplateVertically() {
        return flipTemplateVertically;
    }

    @JIPipeParameter("flip-template-vertically")
    public void setFlipTemplateVertically(boolean flipTemplateVertically) {
        this.flipTemplateVertically = flipTemplateVertically;
    }

    @JIPipeDocumentation(name = "Flip template horizontally", description = "Performing additional searches with the transformed template allows to maximize the probability to find the object, if the object is expected to have different orientations in the image.\n" +
            "This is due to the fact that the template matching only looks for translated version of the templates provided.\n" +
            "Possible transformations include flipping (also called mirroring), rotation (below). Scaling is not proposed in the interface but several templates at different scale can be provided in the multiple template version of the plugin.\n" +
            "If vertical and horizontal flipping are selected, then the plugin generates 2 additional templates for the corresponding transformation.")
    @JIPipeParameter("flip-template-horizontally")
    public boolean isFlipTemplateHorizontally() {
        return flipTemplateHorizontally;
    }

    @JIPipeParameter("flip-template-horizontally")
    public void setFlipTemplateHorizontally(boolean flipTemplateHorizontally) {
        this.flipTemplateHorizontally = flipTemplateHorizontally;
    }

    @JIPipeDocumentation(name = "Additional template rotations", description = "It is possible to provide a list of clockwise rotations in degrees e.g.: 45,90,180.\n" +
            "As with flipping, performing searches with rotated version of the template increases the probability to find the object if it is expected to be rotated.\n" +
            "If flipping is selected, both the original and flipped versions of the template will be rotated.\n\n\n" +
            "NOTE: The template must be of rectangular shape, i.e. for angles not corresponding to \"square rotations\" (not a multiple of 90°) the rotated template will have some background area which are filled either with the modal gray value of the template (Fiji) or" +
            " with the pixel at the border in the initial template (KNIME). For higher performance, the non square rotations can be manually generated before calling the plugin and saved as templates.")
    @JIPipeParameter("template-rotations")
    public IntegerRange getRotateTemplate() {
        return rotateTemplate;
    }

    @JIPipeParameter("template-rotations")
    public void setRotateTemplate(IntegerRange rotateTemplate) {
        this.rotateTemplate = rotateTemplate;
    }

    @JIPipeDocumentation(name = "Scoring method", description = "This is the formula used to compute the probability map. " +
            "The choice is limited to normalised scores to be able to compare different correlation maps when multiple templates are used.\n\n" +
            "<ul>" +
            "\n" +
            "<li>Normalised Square Difference (NSD): The pixels in the probability map are computed as the sum of difference between the gray level of pixels from the image patch and from the template normalised by the square root of the sum of the squared pixel values. " +
            "Therefore a high probability to find the object corresponds to a low score value (not as good as the correlation scores usually).</li>" +
            "<li>Normalised Cross-Correlation (NCC) : The pixels in the probability map are computed as the sum of the pixel/pixel product between the template and current image patch, also normalized for the difference. a high probability to find the object corresponds to a high score value.</li>" +
            "<li>0-mean Normalized Cross-Correlation (0-mean NCC) : The mean value of the template and the image patch is substracted to each pixel value before computing the cross-correlation as above. " +
            "Like the correlation method, a high probability to find the object corresponds to a high score value. (usually this method is most robust to change of illumination)</li>" +
            "</ul>" +
            "\n\nPlease take a look at the OpenCV documentation for more information: https://www.docs.opencv.org/2.4/doc/tutorials/imgproc/histograms/template_matching/template_matching.html")
    @JIPipeParameter("template-matching-method")
    public TemplateMatchingMethod getTemplateMatchingMethod() {
        return templateMatchingMethod;
    }

    @JIPipeParameter("template-matching-method")
    public void setTemplateMatchingMethod(TemplateMatchingMethod templateMatchingMethod) {
        this.templateMatchingMethod = templateMatchingMethod;
    }

    @JIPipeDocumentation(name = "Enable non-maxima suppression", description = "Enables the non-maxima-suppression algorithm that removes bounding boxes that overlap too much.")
    @JIPipeParameter("with-non-maxima-suppression")
    public boolean isWithNonMaximaSuppression() {
        return withNonMaximaSuppression;
    }

    @JIPipeParameter("with-non-maxima-suppression")
    public void setWithNonMaximaSuppression(boolean withNonMaximaSuppression) {
        this.withNonMaximaSuppression = withNonMaximaSuppression;
    }

    @JIPipeDocumentation(name = "Expected number of objects (N)", description = "This is the expected number of object expected in each image. The plugin will return N or less predicted locations of the object.")
    @JIPipeParameter("expected-number-of-objects")
    public int getExpectedNumberOfObjects() {
        return expectedNumberOfObjects;
    }

    @JIPipeParameter("expected-number-of-objects")
    public boolean setExpectedNumberOfObjects(int expectedNumberOfObjects) {
        if (expectedNumberOfObjects <= 0)
            return false;
        this.expectedNumberOfObjects = expectedNumberOfObjects;
        return true;
    }

    @JIPipeDocumentation(name = "Score threshold (N>1)", description = "Ranges from 0.0 to 1.0. Used for the extrema detection on the score map(s).\n" +
            "If the difference-score is used, only minima below this threshold are collected before NMS (i.e. increase to evaluate more hits).\n" +
            "If a correlation-score is used, only maxima above this threshold are collected before NMS (i.e. decrease to evaluate more hits).")
    @JIPipeParameter("multi-object-score-threshold")
    public double getMultiObjectScoreThreshold() {
        return multiObjectScoreThreshold;
    }

    @JIPipeParameter("multi-object-score-threshold")
    public boolean setMultiObjectScoreThreshold(double multiObjectScoreThreshold) {
        if (multiObjectScoreThreshold < 0 || multiObjectScoreThreshold > 1)
            return false;
        this.multiObjectScoreThreshold = multiObjectScoreThreshold;
        return true;
    }

    @JIPipeDocumentation(name = "Maximum overlap (N>1)", description = "Ranges from 0.0 to 1.0. Typically in the range 0.1-0.5.\n" +
            "This parameter is for the Non-Maxima Suppression (NMS). It must be adjusted to prevent overlapping detections while keeping detections of close objects. " +
            "This is the maximal value allowed for the ratio of the Intersection Over Union (IoU) area between overlapping bounding boxes.\n" +
            "If 2 bounding boxes are overlapping above this threshold, then the lower score one is discarded.")
    @JIPipeParameter("multi-object-max-bounding-box-overlap")
    public double getMultiObjectMaximumBoundingBoxOverlap() {
        return multiObjectMaximumBoundingBoxOverlap;
    }

    @JIPipeParameter("multi-object-max-bounding-box-overlap")
    public boolean setMultiObjectMaximumBoundingBoxOverlap(double multiObjectMaximumBoundingBoxOverlap) {
        if (multiObjectMaximumBoundingBoxOverlap < 0 || multiObjectMaximumBoundingBoxOverlap > 1)
            return false;
        this.multiObjectMaximumBoundingBoxOverlap = multiObjectMaximumBoundingBoxOverlap;
        return true;
    }

    @JIPipeDocumentation(name = "Restrict to ROI", description = "If enabled, the template matching is restricted to the bounding box of the supplied ROI.")
    @JIPipeParameter("restrict-to-roi")
    public boolean isRestrictToROI() {
        return restrictToROI;
    }

    @JIPipeParameter("restrict-to-roi")
    public void setRestrictToROI(boolean restrictToROI) {
        this.restrictToROI = restrictToROI;
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
        if (restrictToROI && !getInputSlotMap().containsKey("ROI")) {
            slotConfiguration.addSlot("ROI", new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input), false);
        } else if (!restrictToROI && getInputSlotMap().containsKey("ROI")) {
            slotConfiguration.removeInputSlot("ROI", false);
        }
    }

    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/draw-use-tilt.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/draw-use-tilt.png")
    @JIPipeDocumentation(name = "Generate angles", description = "Generates additional rotation angles by providing the distance between them.")
    public void generateRotations(JIPipeWorkbench workbench) {
        JSpinner startAngle = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        JSpinner endAngle = new JSpinner(new SpinnerNumberModel(360, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        JSpinner angleStep = new JSpinner(new SpinnerNumberModel(90, 1, Integer.MAX_VALUE, 1));
        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
        formPanel.addToForm(startAngle, new JLabel("Start angle"), null);
        formPanel.addToForm(endAngle, new JLabel("End angle"), null);
        formPanel.addToForm(angleStep, new JLabel("Increment"), null);
        int result = JOptionPane.showOptionDialog(
                workbench.getWindow(),
                new Object[]{formPanel},
                "Generate angles",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            int startAngleValue = ((SpinnerNumberModel) startAngle.getModel()).getNumber().intValue();
            int endAngleValue = ((SpinnerNumberModel) endAngle.getModel()).getNumber().intValue();
            int step = ((SpinnerNumberModel) angleStep.getModel()).getNumber().intValue();
            if (endAngleValue < startAngleValue) {
                JOptionPane.showMessageDialog(workbench.getWindow(),
                        "The start angle must be less than the end angle!",
                        "Generate angles",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            int angle = startAngleValue;
            do {
                if (stringBuilder.length() > 0)
                    stringBuilder.append(",");
                stringBuilder.append(angle);
                angle += step;
            }
            while (angle < endAngleValue);
            ParameterUtils.setParameter(this, "template-rotations", new IntegerRange(stringBuilder.toString()));
        }
    }

    @JIPipeDocumentation(name = "Assemble templates", description = "If enabled, all matched templates are put at their matched located within the original image. You can choose to overlay them over the original image or generate an empty image.")
    @JIPipeParameter("assemble-templates")
    public boolean isAssembleTemplates() {
        return assembleTemplates;
    }

    @JIPipeParameter("assemble-templates")
    public void setAssembleTemplates(boolean assembleTemplates) {
        this.assembleTemplates = assembleTemplates;
        updateSlots();
    }

    private void updateSlots() {
        if (assembleTemplates) {
            if (!hasOutputSlot("Assembled templates")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addOutputSlot("Assembled templates", "The assembled templates", ImagePlusData.class, null, false);
            }
        } else {
            if (hasOutputSlot("Assembled templates")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.removeOutputSlot("Assembled templates", false);
            }
        }
    }

    @JIPipeDocumentation(name = "Assemble templates background", description = "If enabled, 'Assemble templates' will be put to an image of the given background. Please note that ")
    @JIPipeParameter("assemble-templates-background")
    public OptionalColorParameter getAssembleTemplatesBackground() {
        return assembleTemplatesBackground;
    }

    @JIPipeParameter("assemble-templates-background")
    public void setAssembleTemplatesBackground(OptionalColorParameter assembleTemplatesBackground) {
        this.assembleTemplatesBackground = assembleTemplatesBackground;
    }

    @JIPipeDocumentation(name = "Assemble templates output", description = "If enabled, override the type of the generated assembly. If disabled, it has the same type as the input image.")
    @JIPipeParameter("assemble-templates-output")
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusData.class)
    public OptionalDataInfoRefParameter getAssembleTemplatesOutput() {
        return assembleTemplatesOutput;
    }

    @JIPipeParameter("assemble-templates-output")
    public void setAssembleTemplatesOutput(OptionalDataInfoRefParameter assembleTemplatesOutput) {
        this.assembleTemplatesOutput = assembleTemplatesOutput;
    }
}
