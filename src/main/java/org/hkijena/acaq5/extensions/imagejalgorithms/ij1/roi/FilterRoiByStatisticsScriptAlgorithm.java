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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.LogicalOperation;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementColumn;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.MeasurementFilter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.scripts.PythonCode;
import org.hkijena.acaq5.utils.MacroUtils;
import org.hkijena.acaq5.utils.PythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;
import static org.hkijena.acaq5.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

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
    private PythonCode code = new PythonCode();
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
        this.code = new PythonCode(other.code);
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
        try {
            this.pythonInterpreter = new PythonInterpreter();
            pythonInterpreter.set("roi_list", new PyDictionary());
            PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
            if(pythonInterpreter.compile(code.getCode()) == null) {
                report.forCategory("Script").reportIsInvalid("The script is invalid!",
                        "The script could not be compiled.",
                        "Please check if your Python script is correct.",
                        this);
            }
            this.pythonInterpreter = null;
        }
        catch (Exception e) {
            report.forCategory("Script").reportIsInvalid("The script is invalid!",
                    "The script could not be compiled.",
                    "Please check if your Python script is correct.",
                    e);
        }
        for (String key : scriptParameters.getParameters().keySet()) {
            if (!MacroUtils.isValidVariableName(key)) {
                report.forCategory("Script Parameters").forCategory(key).reportIsInvalid("Invalid name!",
                        "'" + key + "' is an invalid Python variable name!",
                        "Please ensure that script variables are compatible with the Python language.",
                        this);
            }
        }
    }

    @ACAQDocumentation(name = "Script", description = " The Python script contains a variable 'roi_list' " +
            "that contains dictionaries. Each one has an item 'data' containing the ImageJ ROI, and a dictionary 'stats' containing the statistics.")
    @ACAQParameter("code")
    public PythonCode getCode() {
        return code;
    }

    @ACAQParameter("code")
    public void setCode(PythonCode code) {
        this.code = code;
    }

    @ACAQDocumentation(name = "Script parameters", description = "The following parameters are prepended to the script code:")
    @ACAQParameter("script-parameters")
    public ACAQDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
