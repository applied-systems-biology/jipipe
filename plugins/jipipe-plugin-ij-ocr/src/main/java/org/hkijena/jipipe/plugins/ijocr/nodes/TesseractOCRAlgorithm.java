/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ijocr.nodes;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijocr.OCRPlugin;
import org.hkijena.jipipe.plugins.ijocr.environments.OptionalTesseractOCREnvironment;
import org.hkijena.jipipe.plugins.ijocr.environments.TesseractOCREnvironment;
import org.hkijena.jipipe.plugins.ijocr.utils.TesseractOCREngineMode;
import org.hkijena.jipipe.plugins.ijocr.utils.TesseractPageSegmentationMethod;
import org.hkijena.jipipe.plugins.ijocr.utils.TesseractLanguagesSupplier;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicSetParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.DynamicStringSetParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Find text (Tesseract OCR)", description = "Detects text in the image using Tesseract OCR. " +
        "We highly recommend to preprocess the image by applying thresholding, otherwise Tesseract will apply its own Otsu-based method that may yield unsatisfactory results. " +
        "If higher-dimensional data is provided, the OCR is applied per plane.")
@AddJIPipeCitation("For additional guidance see: https://tesseract-ocr.github.io/tessdoc/ImproveQuality.html")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Results", create = true, description = "The results obtained using the TSV exporter")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "OCR")
public class TesseractOCRAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TesseractPageSegmentationMethod pageSegmentationMethod = TesseractPageSegmentationMethod.PSM11;
    private TesseractOCREngineMode engineMode = TesseractOCREngineMode.OEM3;
    private DynamicStringSetParameter languages = new DynamicStringSetParameter();
    private OptionalJIPipeExpressionParameter overrideDPI = new OptionalJIPipeExpressionParameter(false, "300");
    private OptionalJIPipeExpressionParameter overrideCharAllowList = new OptionalJIPipeExpressionParameter(false, "\"0123456789-\"");
    private OptionalTesseractOCREnvironment overrideTesseractOCREnvironment = new OptionalTesseractOCREnvironment();

    public TesseractOCRAlgorithm(JIPipeNodeInfo info) {
        super(info);
        languages.setValues(Collections.singleton("eng"));
        languages.setCollapsed(true);
    }

    public TesseractOCRAlgorithm(TesseractOCRAlgorithm other) {
        super(other);
        this.pageSegmentationMethod = other.pageSegmentationMethod;
        this.engineMode = other.engineMode;
        this.languages = new DynamicStringSetParameter(other.languages);
        this.overrideDPI = new OptionalJIPipeExpressionParameter(other.overrideDPI);
        this.overrideCharAllowList = new OptionalJIPipeExpressionParameter(other.overrideCharAllowList);
        this.overrideTesseractOCREnvironment = new OptionalTesseractOCREnvironment(other.overrideTesseractOCREnvironment);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();

        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap(iterationStep);
        ImagePlusPropertiesExpressionParameterVariablesInfo.extractValues(variablesMap, inputImage, Collections.emptyList());

        int dpi = overrideDPI.isEnabled() ? overrideDPI.getContent().evaluateToInteger(variablesMap) : 0;
        String allowedChars = overrideCharAllowList.isEnabled() ? overrideCharAllowList.getContent().evaluateToString(variablesMap) : null;
        TesseractOCREnvironment tesseractOCREnvironment = getConfiguredTesseractOCREnvironment();
        String languagesString = String.join("+", languages.getValues());
        if(StringUtils.isNullOrEmpty(languagesString)) {
            progressInfo.log("INFO: no language selected. Defaulting to eng");
            languagesString = "eng";
        }

        Path scratchBase = getNewScratch();

        ResultsTableData output = new ResultsTableData();
        String finalLanguagesString = languagesString;
        ImageJUtils.forEachIndexedZCTSliceWithProgress(inputImage, (ip, index, sliceProcess) -> {
            Path tmpPath = PathUtils.resolveAndMakeSubDirectory(scratchBase, index.getC() + "_" + index.getZ() + "_" + index.getT());
            Path tmpImagePath = tmpPath.resolve("img.png");
            IJ.saveAs(new ImagePlus("slice", ip), "PNG", tmpImagePath.toString());

            List<String> args = new ArrayList<>();
            args.add(tmpImagePath.toString());
            args.add(tmpImagePath.toString());
            args.add("-l");
            args.add(finalLanguagesString);

            args.add("--psm");
            args.add(String.valueOf(pageSegmentationMethod.getNativeValue()));

            args.add("--oem");
            args.add(String.valueOf(engineMode.getNativeValue()));

            if(dpi > 0) {
                args.add("--dpi");
                args.add(String.valueOf(dpi));
            }

            args.add("tsv");

            if(allowedChars != null) {
                args.add("-c");
                args.add("tessedit_char_whitelist=" + allowedChars);
            }

            tesseractOCREnvironment.runExecutable(args, Collections.emptyMap(), false, sliceProcess);

            // Find the TSV file
            Path tsvFile = PathUtils.findFileByExtensionIn(tmpPath, ".tsv");
            if(tsvFile != null) {
                ResultsTableData sliceResult = ResultsTableData.fromCSV(tsvFile, "\t");
                Objects.requireNonNull(sliceResult);
                sliceResult.addNumericColumn("slice_c");
                sliceResult.addNumericColumn("slice_z");
                sliceResult.addNumericColumn("slice_t");
                for (int i = 0; i < sliceResult.getRowCount(); i++) {
                    sliceResult.setValueAt(index.getC(), i, "slice_c");
                    sliceResult.setValueAt(index.getZ(), i, "slice_z");
                    sliceResult.setValueAt(index.getT(), i, "slice_t");
                }

                output.addRows(sliceResult);
            }
            else {
                sliceProcess.log("INFO: No output TSV detected");
            }

        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    public TesseractOCREnvironment getConfiguredTesseractOCREnvironment() {
        JIPipeGraphNode node = this;
        JIPipeProject project = node.getRuntimeProject();
        if (project == null) {
            project = node.getParentGraph().getProject();
        }
        return OCRPlugin.getTesseractOCREnvironment(project, getOverrideTesseractOCREnvironment());
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        target.add(getConfiguredTesseractOCREnvironment());
    }

    @SetJIPipeDocumentation(name = "Override Tesseract OCR environment", description = "Allows to override the Tesseract OCR environment")
    @JIPipeParameter("override-environment")
    public OptionalTesseractOCREnvironment getOverrideTesseractOCREnvironment() {
        return overrideTesseractOCREnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideTesseractOCREnvironment(OptionalTesseractOCREnvironment overrideTesseractOCREnvironment) {
        this.overrideTesseractOCREnvironment = overrideTesseractOCREnvironment;
    }

    @SetJIPipeDocumentation(name = "Page segmentation method", description = "By default Tesseract expects a page of text when it segments an image. " +
            "If you’re just seeking to OCR a small region, try a different segmentation mode, using the --psm argument. " +
            "Note that adding a white border to text which is too tightly cropped may also help")
    @JIPipeParameter("page-segmentation-method")
    public TesseractPageSegmentationMethod getPageSegmentationMethod() {
        return pageSegmentationMethod;
    }

    @JIPipeParameter("page-segmentation-method")
    public void setPageSegmentationMethod(TesseractPageSegmentationMethod pageSegmentationMethod) {
        this.pageSegmentationMethod = pageSegmentationMethod;
    }

    @SetJIPipeDocumentation(name = "Recognized language(s)", description = "Select the languages/scripts that Tesseract OCR will be able to recognize.")
    @JIPipeParameter("languages")
    @DynamicSetParameterSettings(supplier = TesseractLanguagesSupplier.class)
    public DynamicStringSetParameter getLanguages() {
        return languages;
    }

    @JIPipeParameter("languages")
    public void setLanguages(DynamicStringSetParameter languages) {
        this.languages = languages;
    }

    @SetJIPipeDocumentation(name = "OCR engine mode", description = "The OCR engine mode to use")
    @JIPipeParameter("engine-mode")
    public TesseractOCREngineMode getEngineMode() {
        return engineMode;
    }

    @JIPipeParameter("engine-mode")
    public void setEngineMode(TesseractOCREngineMode engineMode) {
        this.engineMode = engineMode;
    }

    @SetJIPipeDocumentation(name = "Specify DPI", description = "Allows to specify the DPI of the input image")
    @JIPipeParameter("override-dpi")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getOverrideDPI() {
        return overrideDPI;
    }

    @JIPipeParameter("override-dpi")
    public void setOverrideDPI(OptionalJIPipeExpressionParameter overrideDPI) {
        this.overrideDPI = overrideDPI;
    }

    @SetJIPipeDocumentation(name = "Specify recognized characters", description = "Allows to specify the set of recognized characters (provided as string)")
    @JIPipeParameter("override-char-allow-list")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getOverrideCharAllowList() {
        return overrideCharAllowList;
    }

    @JIPipeParameter("override-char-allow-list")
    public void setOverrideCharAllowList(OptionalJIPipeExpressionParameter overrideCharAllowList) {
        this.overrideCharAllowList = overrideCharAllowList;
    }
}
