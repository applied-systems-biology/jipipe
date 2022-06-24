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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.filter;

import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.scripts.PythonScript;
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

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter ROI by statistics (Script)", description = "Filters the ROI list elements via statistics. The Python script contains a variable 'roi_list' " +
        "that contains dictionaries. Each one has an item 'data' containing the ImageJ ROI, and a dictionary 'stats' containing the statistics.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class FilterRoiByStatisticsScriptAlgorithm extends ImageRoiProcessorAlgorithm {

    private RoiStatisticsAlgorithm roiStatisticsAlgorithm = JIPipe.createNode("ij1-roi-statistics"
    );
    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterRoiByStatisticsScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, ROIListData.class, "Output");
        registerSubParameter(scriptParameters);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public FilterRoiByStatisticsScriptAlgorithm(FilterRoiByStatisticsScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
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
            getEventBus().post(new ParameterChangedEvent(this, "code"));
        }
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        this.pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        super.run(progressInfo);
        this.pythonInterpreter = null;
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

        PyDictionary annotationDict = JIPipeTextAnnotation.annotationMapToPython(dataBatch.getMergedTextAnnotations());
        pythonInterpreter.set("annotations", annotationDict);
        pythonInterpreter.set("roi_list", roiList);
        pythonInterpreter.exec(code.getCode(getProjectDirectory()));
        roiList = (List<PyDictionary>) pythonInterpreter.get("roi_list").__tojava__(List.class);
        annotationDict = (PyDictionary) pythonInterpreter.get("annotations");

        ROIListData outputData = new ROIListData();
        for (PyDictionary roiItemDictionary : roiList) {
            Roi roi = (Roi) roiItemDictionary.get("data");
            outputData.add(roi);
        }
        dataBatch.getMergedTextAnnotations().clear();
        JIPipeTextAnnotation.setAnnotationsFromPython(annotationDict, dataBatch.getMergedTextAnnotations());

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
        code.makeExternalScriptFileRelative(baseDirectory);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        JythonUtils.checkScriptValidity(code.getCode(getProjectDirectory()), scriptParameters, report.resolve("Script"));
        JythonUtils.checkScriptParametersValidity(scriptParameters, report.resolve("Script parameters"));
    }

    @JIPipeDocumentation(name = "Script", description = " The Python script contains a variable 'roi_list' " +
            "that contains dictionaries. Each one has an item 'data' containing the ImageJ ROI, and a dictionary 'stats' containing the statistics.")
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
