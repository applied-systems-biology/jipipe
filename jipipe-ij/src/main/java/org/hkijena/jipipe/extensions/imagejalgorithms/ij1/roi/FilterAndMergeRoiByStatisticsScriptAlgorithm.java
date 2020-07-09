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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.tables.ResultsTableData;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.utils.PythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;
import static org.hkijena.jipipe.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter & merge ROI by statistics (Script)", description = "Executes a script that has full control over the the input ROI lists. The Python script contains a variable 'row_lists' " +
        "that contains dictionaries with following entries: 'roi_list' is a list of dictionaries, 'annotations' is a dictionary containing the annotations. Each 'data' dictionary has " +
        "an item 'data' containing the ImageJ ROI, and a dictionary 'stats' with the extracted statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@JIPipeOrganization(menuPath = "ROI", algorithmCategory = JIPipeAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Image")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class FilterAndMergeRoiByStatisticsScriptAlgorithm extends ImageRoiProcessorAlgorithm {

    private RoiStatisticsAlgorithm roiStatisticsAlgorithm = JIPipeAlgorithm.newInstance("ij1-roi-statistics");
    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);
    private List<PyDictionary> pythonDataRow;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public FilterAndMergeRoiByStatisticsScriptAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, ROIListData.class, "Output");
        code.setCode("# This script is executed once\n" +
                "# All ROI lists are passed as array 'roi_lists'\n" +
                "# It contains dictionaries with following structure:\n" +
                "# { 'roi_list' : [], 'annotations': {} }\n" +
                "# 'roi_list' contains dictionaries with following structure\n" +
                "# { 'data' : x, 'stats' : {} } where x is an ImageJ ROI and stats contains the statistics.\n" +
                "# 'annotations' is a dictionary from annotation name to value (str)" +
                "\n\n" +
                "for item in roi_lists:\n" +
                "\titem[\"roi_list\"] = item[\"roi_list\"][:10]");
        registerSubParameter(scriptParameters);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public FilterAndMergeRoiByStatisticsScriptAlgorithm(FilterAndMergeRoiByStatisticsScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        this.pythonInterpreter = new PythonInterpreter();
        PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        pythonDataRow = new ArrayList<>();
        super.run(subProgress, algorithmProgress, isCancelled);
        // Pass input to script
        pythonInterpreter.set("roi_lists", pythonDataRow);
        pythonInterpreter.exec(code.getCode());
        pythonDataRow = (List<PyDictionary>) pythonInterpreter.get("roi_lists").__tojava__(List.class);

        // Generate output
        for (PyDictionary row : pythonDataRow) {
            ROIListData listData = new ROIListData();
            List<PyDictionary> pyListData = (List<PyDictionary>) row.get("roi_list");
            for (PyDictionary item : pyListData) {
                Roi roi = (Roi) item.get("data");
                listData.add(roi);
            }
            List<JIPipeAnnotation> annotations = JIPipeAnnotation.extractAnnotationsFromPython((PyDictionary) row.getOrDefault("annotations", new PyDictionary()));
            getFirstOutputSlot().addData(listData, annotations);
        }

        this.pythonInterpreter = null;
        pythonDataRow = null;
    }

    @Override
    protected void runIteration(JIPipeDataInterface dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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

        PyDictionary annotationDict = JIPipeAnnotation.annotationMapToPython(dataInterface.getAnnotations());
        PyDictionary rowDict = new PyDictionary();
        rowDict.put("annotations", annotationDict);
        rowDict.put("roi_list", roiList);
        pythonDataRow.add(rowDict);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        PythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        PythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @JIPipeDocumentation(name = "Script", description = "Each table is passed as dictionary 'table' " +
            "with the column name as key and values being an array of strings or doubles. " +
            "The number of input rows can be accessed via the 'nrow' variable.")
    @JIPipeParameter("code")
    public PythonScript getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(PythonScript code) {
        this.code = code;
    }

    @JIPipeDocumentation(name = "Script parameters", description = "The following parameters will be passed to the Python script. The variable name is equal to the unique parameter identifier.")
    @JIPipeParameter(value = "script-parameters", persistence = JIPipeParameterPersistence.Object)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
