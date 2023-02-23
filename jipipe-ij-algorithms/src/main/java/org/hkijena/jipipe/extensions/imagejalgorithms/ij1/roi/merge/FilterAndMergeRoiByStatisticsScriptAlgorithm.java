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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.merge;

import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.scripting.JythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter & merge ROI by statistics (Script)", description = "Executes a script that has full control over the input ROI lists. The Python script contains a variable 'row_lists' " +
        "that contains dictionaries with following entries: 'roi_list' is a list of dictionaries, 'annotations' is a dictionary containing the annotations. Each 'data' dictionary has " +
        "an item 'data' containing the ImageJ ROI, and a dictionary 'stats' with the extracted statistics.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class FilterAndMergeRoiByStatisticsScriptAlgorithm extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode("ij1-roi-statistics");
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());
    private List<PyDictionary> pythonDataRow;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterAndMergeRoiByStatisticsScriptAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(scriptParameters);
    }

    /**
     * Instantiates a new node type.
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
    public void run(JIPipeProgressInfo progressInfo) {
        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        pythonDataRow = new ArrayList<>();
        super.run(progressInfo);
        // Pass input to script
        pythonInterpreter.set("roi_lists", pythonDataRow);
        pythonInterpreter.exec(code.getCode(getProjectDirectory()));
        pythonDataRow = (List<PyDictionary>) pythonInterpreter.get("roi_lists").__tojava__(List.class);

        // Generate output
        for (PyDictionary row : pythonDataRow) {
            ROIListData listData = new ROIListData();
            List<PyDictionary> pyListData = (List<PyDictionary>) row.get("roi_list");
            for (PyDictionary item : pyListData) {
                Roi roi = (Roi) item.get("data");
                listData.add(roi);
            }
            List<JIPipeTextAnnotation> annotations = JIPipeTextAnnotation.extractAnnotationsFromPython((PyDictionary) row.getOrDefault("annotations", new PyDictionary()));
            getFirstOutputSlot().addData(listData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }

        pythonInterpreter = null;
        pythonDataRow = null;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData inputRois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlusData inputReference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);

        // Obtain statistics
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.getInputSlot("ROI").addData(inputRois, progressInfo);
        if (inputReference != null) {
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(inputReference, progressInfo);
        }
        roiStatisticsAlgorithm.run(progressInfo);
        ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);

        List<PyDictionary> roiList = new ArrayList<>();
        for (int row = 0; row < inputRois.size(); row++) {
            PyDictionary roiItemDictionary = new PyDictionary();
            PyDictionary statisticsDictionary = new PyDictionary();
            for (int col = 0; col < statistics.getColumnCount(); col++) {
                statisticsDictionary.put(statistics.getColumnName(col), statistics.getValueAsDouble(row, col));
            }
            roiItemDictionary.put("stats", statisticsDictionary);
            roiItemDictionary.put("data", inputRois.get(row));
            roiList.add(roiItemDictionary);
        }

        PyDictionary annotationDict = JIPipeTextAnnotation.annotationMapToPython(dataBatch.getMergedTextAnnotations());
        PyDictionary rowDict = new PyDictionary();
        rowDict.put("annotations", annotationDict);
        rowDict.put("roi_list", roiList);
        pythonDataRow.add(rowDict);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        JythonUtils.checkScriptValidity(code.getCode(getProjectDirectory()), scriptParameters, report.resolve("Script"));
        JythonUtils.checkScriptParametersValidity(scriptParameters, report.resolve("Script parameters"));
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
        code.makeExternalScriptFileRelative(baseDirectory);
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
    @JIPipeParameter(value = "script-parameters", persistence = JIPipeParameterPersistence.NestedCollection)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
