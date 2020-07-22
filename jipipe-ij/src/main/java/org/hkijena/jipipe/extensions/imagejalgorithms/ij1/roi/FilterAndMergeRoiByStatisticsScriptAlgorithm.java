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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PythonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter & merge ROI by statistics (Script)", description = "Executes a script that has full control over the the input ROI lists. The Python script contains a variable 'row_lists' " +
        "that contains dictionaries with following entries: 'roi_list' is a list of dictionaries, 'annotations' is a dictionary containing the annotations. Each 'data' dictionary has " +
        "an item 'data' containing the ImageJ ROI, and a dictionary 'stats' with the extracted statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@JIPipeOrganization(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class FilterAndMergeRoiByStatisticsScriptAlgorithm extends ImageRoiProcessorAlgorithm {

    private RoiStatisticsAlgorithm roiStatisticsAlgorithm = JIPipeAlgorithm.newInstance("ij1-roi-statistics");
    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipeParameterTypeRegistry.getInstance().getRegisteredParameters().values());
    private List<PyDictionary> pythonDataRow;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterAndMergeRoiByStatisticsScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, ROIListData.class, "Output");
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

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (JOptionPane.showConfirmDialog(parent.getWindow(),
                "This will reset most of the properties. Continue?",
                "Load example",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData allROIs = new ROIListData();
        ResultsTableData allStatistics = new ResultsTableData();

        for (Map.Entry<ImagePlusData, ROIListData> entry : getReferenceImage(dataBatch, subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled).entrySet()) {
            // Obtain statistics
            roiStatisticsAlgorithm.clearSlotData();
            roiStatisticsAlgorithm.getInputSlot("ROI").addData(entry.getValue());
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(entry.getKey());
            roiStatisticsAlgorithm.run(subProgress.resolve("ROI statistics"), algorithmProgress, isCancelled);
            ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class);
            allROIs.addAll(entry.getValue());
            allStatistics.mergeWith(statistics);
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

        PyDictionary annotationDict = JIPipeAnnotation.annotationMapToPython(dataBatch.getAnnotations());
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
