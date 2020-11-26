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
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeCustomParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.primitives.IntegerList;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.python.core.PyList;
import org.python.util.PythonInterpreter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
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
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", autoCreate = true)
@JIPipeOrganization(menuPath = "Analyze", nodeTypeCategory = ImagesNodeTypeCategory.class)
public class MultiTemplateMatchingAlgorithm extends JIPipeMergingAlgorithm {

    private final static String SCRIPT = loadScriptFromResources();

    private boolean flipTemplateVertically = true;
    private boolean flipTemplateHorizontally = true;
    private IntegerRange rotateTemplate = new IntegerRange();
    private TemplateMatchingMethod templateMatchingMethod = TemplateMatchingMethod.NormalizedZeroMeanCrossCorrelation;
    private int expectedNumberOfObjects = 1;
    private double multiObjectScoreThreshold = 0.5;
    private double multiObjectMaximumBoundingBoxOverlap = 0.3;
    private boolean restrictToROI = false;

    public MultiTemplateMatchingAlgorithm(JIPipeNodeInfo info) {
        super(info);
        rotateTemplate.setValue("90,180,270");
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
        this.setRestrictToROI(other.restrictToROI);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<ImagePlus> images = new ArrayList<>();
        List<ImagePlus> templates = new ArrayList<>();
        ROIListData mergedRois = new ROIListData();

        for (ImagePlusData image : dataBatch.getInputData("Image", ImagePlusData.class, progressInfo)) {
            images.add(image.getImage());
        }
        for (ImagePlusData template : dataBatch.getInputData("Template", ImagePlusData.class, progressInfo)) {
            templates.add(template.getImage());
        }
        if (restrictToROI) {
            for (ROIListData roi : dataBatch.getInputData("ROI", ROIListData.class, progressInfo)) {
                mergedRois.addAll(roi);
            }
        }
        Roi searchRoi = mergedRois.isEmpty() ? null : new ShapeRoi(mergedRois.getBounds());

        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        pythonInterpreter.set("Method", templateMatchingMethod.getIndex());
        pythonInterpreter.set("fliph", flipTemplateHorizontally);
        pythonInterpreter.set("flipv", flipTemplateVertically);
        pythonInterpreter.set("angles", rotateTemplate.getIntegers().stream().map(Object::toString).collect(Collectors.joining(",")));
        pythonInterpreter.set("n_hit", expectedNumberOfObjects);
        pythonInterpreter.set("score_threshold", multiObjectScoreThreshold);
        pythonInterpreter.set("tolerance", 0);
        pythonInterpreter.set("max_overlap", multiObjectMaximumBoundingBoxOverlap);
        pythonInterpreter.set("List_Template", new PyList(templates));
        pythonInterpreter.set("Bool_SearchRoi", restrictToROI && searchRoi != null);
        pythonInterpreter.set("searchRoi", searchRoi);
        pythonInterpreter.set("progress", progressInfo);

        for (int i = 0; i < images.size(); i++) {
            ImagePlus image = images.get(i);
            RoiManager roiManager = new RoiManager(false);
            ResultsTableData measurements = new ResultsTableData();
            pythonInterpreter.set("ImpImage", image);
            pythonInterpreter.set("rm", roiManager);
            pythonInterpreter.set("Table", measurements.getTable());
            pythonInterpreter.set("progress", progressInfo.resolve("Image", i, images.size()));
            pythonInterpreter.exec(SCRIPT);

            dataBatch.addOutputData("ROI", new ROIListData(roiManager), progressInfo);
            dataBatch.addOutputData("Measurements", measurements, progressInfo);
        }

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
            slotConfiguration.addSlot("ROI", new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, null), false);
        } else if (!restrictToROI && getInputSlotMap().containsKey("ROI")) {
            slotConfiguration.removeInputSlot("ROI", false);
        }
    }

    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/draw-use-tilt.png")
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
            int startAngleValue = ((SpinnerNumberModel)startAngle.getModel()).getNumber().intValue();
            int endAngleValue = ((SpinnerNumberModel)endAngle.getModel()).getNumber().intValue();
            int step = ((SpinnerNumberModel)angleStep.getModel()).getNumber().intValue();
            if(endAngleValue < startAngleValue) {
                JOptionPane.showMessageDialog(workbench.getWindow(),
                        "The start angle must be less than the end angle!",
                        "Generate angles",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            int angle = startAngleValue;
            do {
                if(stringBuilder.length() > 0)
                    stringBuilder.append(",");
                stringBuilder.append(angle);
                angle += step;
            }
            while(angle < endAngleValue);
            JIPipeParameterCollection.setParameter(this, "template-rotations", new IntegerRange(stringBuilder.toString()));
        }
    }

    private static String loadScriptFromResources() {
        return ResourceUtils.getPluginResourceAsString("extensions/ijmultitemplatematching/template-matching-script.py");
    }
}
