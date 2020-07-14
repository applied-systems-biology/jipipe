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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter ROI by statistics (Script)", description = "Filters the ROI list elements via statistics. The Python script contains a variable 'roi_list' " +
        "that contains dictionaries. Each one has an item 'data' containing the ImageJ ROI, and a dictionary 'stats' containing the statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@JIPipeOrganization(menuPath = "ROI", algorithmCategory = JIPipeNodeCategory.Processor)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class FilterRoiByStatisticsScriptAlgorithm extends ImageRoiProcessorAlgorithm {

    private RoiStatisticsAlgorithm roiStatisticsAlgorithm = JIPipeAlgorithm.newInstance("ij1-roi-statistics");
    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipeParameterTypeRegistry.getInstance().getRegisteredParameters().values());

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public FilterRoiByStatisticsScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, ROIListData.class, "Output");
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
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/algorithms/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (JOptionPane.showConfirmDialog(parent.getWindow(),
                "This will reset most of the properties. Continue?",
                "Load example",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
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
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        this.pythonInterpreter = new PythonInterpreter();
        PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        super.run(subProgress, algorithmProgress, isCancelled);
        this.pythonInterpreter = null;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = dataBatch.getInputData("ROI", ROIListData.class);
        ImagePlusData referenceImageData = new ImagePlusData(getReferenceImage(dataBatch, subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled));

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

        PyDictionary annotationDict = JIPipeAnnotation.annotationMapToPython(dataBatch.getAnnotations());
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
        dataBatch.getAnnotations().clear();
        JIPipeAnnotation.setAnnotationsFromPython(annotationDict, dataBatch.getAnnotations());

        dataBatch.addOutputData(getFirstOutputSlot(), outputData);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        PythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        PythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
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
    @JIPipeParameter(value = "script-parameters", persistence = JIPipeParameterPersistence.Object)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
