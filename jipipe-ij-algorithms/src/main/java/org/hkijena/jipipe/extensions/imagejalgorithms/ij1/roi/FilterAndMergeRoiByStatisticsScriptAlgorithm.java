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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.scripting.JythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter & merge ROI by statistics (Script)", description = "Executes a script that has full control over the the input ROI lists. The Python script contains a variable 'row_lists' " +
        "that contains dictionaries with following entries: 'roi_list' is a list of dictionaries, 'annotations' is a dictionary containing the annotations. Each 'data' dictionary has " +
        "an item 'data' containing the ImageJ ROI, and a dictionary 'stats' with the extracted statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class FilterAndMergeRoiByStatisticsScriptAlgorithm extends ImageRoiProcessorAlgorithm {

    private RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode("ij1-roi-statistics", RoiStatisticsAlgorithm.class);
    private PythonInterpreter pythonInterpreter;
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
        super(info, ROIListData.class, "Output");
        registerSubParameter(scriptParameters);
        roiStatisticsAlgorithm.setAllSlotsVirtual(false, false, null);
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
        roiStatisticsAlgorithm.setAllSlotsVirtual(false, false, null);
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
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
            getEventBus().post(new ParameterChangedEvent(this, "code"));
        }
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        this.pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        pythonDataRow = new ArrayList<>();
        super.run(progressInfo);
        // Pass input to script
        pythonInterpreter.set("roi_lists", pythonDataRow);
        pythonInterpreter.exec(code.getCode(getProjectWorkDirectory()));
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
            getFirstOutputSlot().addData(listData, annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }

        this.pythonInterpreter = null;
        pythonDataRow = null;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData allROIs = new ROIListData();
        ResultsTableData allStatistics = new ResultsTableData();

        for (Map.Entry<ImagePlusData, ROIListData> entry : getReferenceImage(dataBatch, progressInfo).entrySet()) {
            // Obtain statistics
            roiStatisticsAlgorithm.clearSlotData();
            roiStatisticsAlgorithm.getInputSlot("ROI").addData(entry.getValue(), progressInfo);
            if (entry.getKey() == null) {
                roiStatisticsAlgorithm.setOverrideReferenceImage(false);
            } else {
                roiStatisticsAlgorithm.setOverrideReferenceImage(true);
                roiStatisticsAlgorithm.getInputSlot("Reference").addData(entry.getKey(), progressInfo);
            }
            roiStatisticsAlgorithm.run(progressInfo);
            ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
            allROIs.addAll(entry.getValue());
            allStatistics.addRows(statistics);
        }

        List<PyDictionary> roiList = new ArrayList<>();
        for (int row = 0; row < allROIs.size(); row++) {
            PyDictionary roiItemDictionary = new PyDictionary();
            PyDictionary statisticsDictionary = new PyDictionary();
            for (int col = 0; col < allStatistics.getColumnCount(); col++) {
                statisticsDictionary.put(allStatistics.getColumnName(col), allStatistics.getValueAsDouble(row, col));
            }
            roiItemDictionary.put("stats", statisticsDictionary);
            roiItemDictionary.put("data", allROIs.get(row));
            roiList.add(roiItemDictionary);
        }

        PyDictionary annotationDict = JIPipeAnnotation.annotationMapToPython(dataBatch.getGlobalAnnotations());
        PyDictionary rowDict = new PyDictionary();
        rowDict.put("annotations", annotationDict);
        rowDict.put("roi_list", roiList);
        pythonDataRow.add(rowDict);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        JythonUtils.checkScriptValidity(code.getCode(getProjectWorkDirectory()), scriptParameters, report.resolve("Script"));
        JythonUtils.checkScriptParametersValidity(scriptParameters, report.resolve("Script parameters"));
    }

    @Override
    public void setProjectWorkDirectory(Path projectWorkDirectory) {
        super.setProjectWorkDirectory(projectWorkDirectory);
        code.makeExternalScriptFileRelative(projectWorkDirectory);
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
