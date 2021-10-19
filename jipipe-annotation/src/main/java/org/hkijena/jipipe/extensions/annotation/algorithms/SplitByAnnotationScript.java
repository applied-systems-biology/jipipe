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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.scripting.JythonUtils;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;

/**
 * Algorithm that annotates all data with the same annotation
 */
@JIPipeDocumentation(name = "Split & filter by annotation (Script)", description = "Executes a Python-script for each annotation row that allows redirection to a specified output slot (or to remove it). " +
        "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
        "converted into their respective JIPipe types. The target slot is extracted from a variable 'output_slot' that should be present within the script." +
        " If the variable is set to null or empty, the data is discarded.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input")
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output")
public class SplitByAnnotationScript extends JIPipeSimpleIteratingAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());

    /**
     * @param info the info
     */
    public SplitByAnnotationScript(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", JIPipeData.class)
                .addOutputSlot("Output", JIPipeData.class, "Input")
                .sealInput()
                .build());
        code.setCode("# This script is executed for each row\n" +
                "# Annotations are passed as dictionary 'annotations'\n" +
                "# Modifications are copied into JIPipe\n" +
                "# The output slot is determined by a variable 'output_slot'" +
                "\n\n" +
                "output_slot = \"lhs\" if \"condition\" in annotation[\"sample\"] else \"rhs\"");
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public SplitByAnnotationScript(SplitByAnnotationScript other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
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

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        this.pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        super.run(progressInfo);
        this.pythonInterpreter = null;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        PyDictionary annotationDict = JIPipeAnnotation.annotationMapToPython(dataBatch.getGlobalAnnotations());
        pythonInterpreter.set("annotations", annotationDict);
        pythonInterpreter.exec(code.getCode(getProjectWorkDirectory()));
        annotationDict = (PyDictionary) pythonInterpreter.get("annotations");

        // Convert the results back into JIPipe
        dataBatch.getGlobalAnnotations().clear();
        JIPipeAnnotation.setAnnotationsFromPython(annotationDict, dataBatch.getGlobalAnnotations());

        // Get the output slot
        PyObject outputSlotPy = pythonInterpreter.get("output_slot");
        String outputSlotName = null;
        if (outputSlotPy != null) {
            outputSlotName = "" + outputSlotPy;
        }

        if (!StringUtils.isNullOrEmpty(outputSlotName)) {
            dataBatch.addOutputData(getOutputSlot(outputSlotName), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Script", description = "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
            "converted into their respective JIPipe types. The target slot is extracted from a variable 'output_slot' that should be present within the script." +
            " If the variable is set to null or empty, the data is discarded.")
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
