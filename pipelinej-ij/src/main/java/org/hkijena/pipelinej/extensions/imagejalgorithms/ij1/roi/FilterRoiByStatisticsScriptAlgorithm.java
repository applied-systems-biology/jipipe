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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.roi;

import ij.gui.Roi;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQAnnotation;
import org.hkijena.pipelinej.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;
import org.hkijena.pipelinej.extensions.parameters.scripts.PythonScript;
import org.hkijena.pipelinej.utils.PythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;
import static org.hkijena.pipelinej.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Filter ROI by statistics (Script)", description = "Filters the ROI list elements via statistics. The Python script contains a variable 'roi_list' " +
        "that contains dictionaries. Each one has an item 'data' containing the ImageJ ROI, and a dictionary 'stats' containing the statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Image")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class FilterRoiByStatisticsScriptAlgorithm extends ImageRoiProcessorAlgorithm {

    private RoiStatisticsAlgorithm roiStatisticsAlgorithm = ACAQAlgorithm.newInstance("ij1-roi-statistics");
    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private ACAQDynamicParameterCollection scriptParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public FilterRoiByStatisticsScriptAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ROIListData.class, "Output");
        code.setCode("# This script is executed for each ROI list\n" +
                "# The list is accessible via 'roi_list'\n" +
                "# It contains an entry 'data' with the ROI\n" +
                "# And a dictionary 'stats' with statistics" +
                "# Annotations can be modified via a dict 'annotations'\n" +
                "\n\n" +
                "filtered_rois = []\n" +
                "for item in roi_list:\n" +
                "\tif item[\"stats\"][\"Area\"] < 100:\n" +
                "\t\tfiltered_rois.append(item)\n" +
                "roi_list = filtered_rois");
        registerSubParameter(scriptParameters);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public FilterRoiByStatisticsScriptAlgorithm(FilterRoiByStatisticsScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new ACAQDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        this.pythonInterpreter = new PythonInterpreter();
        PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        super.run(subProgress, algorithmProgress, isCancelled);
        this.pythonInterpreter = null;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = dataInterface.getInputData("ROI", ROIListData.class);
        ImagePlusData referenceImageData = new ImagePlusData(getReferenceImage(dataInterface, subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled));

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(inputData);
        roiStatisticsAlgorithm.getInputSlot("Reference").addData(referenceImageData);
        roiStatisticsAlgorithm.run(subProgress.resolve("ROI statistics"), algorithmProgress, isCancelled);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class);

        List<PyDictionary> roiList = new ArrayList<>();
        for (int row = 0; row < inputData.size(); row++) {
            PyDictionary roiItemDictionary = new PyDictionary();
            PyDictionary statisticsDictionary = new PyDictionary();
            for (int col = 0; col < statistics.getColumnCount(); col++) {
                statisticsDictionary.put(statistics.getColumnName(col), statistics.getValueAsDouble(row, col));
            }
            roiItemDictionary.put("stats", statisticsDictionary);
            roiItemDictionary.put("data", inputData.get(row));
            roiList.add(roiItemDictionary);
        }

        PyDictionary annotationDict = ACAQAnnotation.annotationMapToPython(dataInterface.getAnnotations());
        pythonInterpreter.set("annotations", annotationDict);
        pythonInterpreter.set("roi_list", roiList);
        pythonInterpreter.exec(code.getCode());
        roiList = (List<PyDictionary>) pythonInterpreter.get("roi_list").__tojava__(List.class);
        annotationDict = (PyDictionary) pythonInterpreter.get("annotations");

        ROIListData outputData = new ROIListData();
        for (PyDictionary roiItemDictionary : roiList) {
            Roi roi = (Roi) roiItemDictionary.get("data");
            outputData.add(roi);
        }
        dataInterface.getAnnotations().clear();
        ACAQAnnotation.setAnnotationsFromPython(annotationDict, dataInterface.getAnnotations());

        dataInterface.addOutputData(getFirstOutputSlot(), outputData);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        PythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        PythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @ACAQDocumentation(name = "Script", description = " The Python script contains a variable 'roi_list' " +
            "that contains dictionaries. Each one has an item 'data' containing the ImageJ ROI, and a dictionary 'stats' containing the statistics.")
    @ACAQParameter("code")
    public PythonScript getCode() {
        return code;
    }

    @ACAQParameter("code")
    public void setCode(PythonScript code) {
        this.code = code;
    }

    @ACAQDocumentation(name = "Script parameters", description = "The following parameters will be passed to the Python script. The variable name is equal to the unique parameter identifier.")
    @ACAQParameter("script-parameters")
    public ACAQDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
