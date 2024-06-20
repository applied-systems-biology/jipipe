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

package org.hkijena.jipipe.plugins.annotation.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.scripts.PythonScript;
import org.hkijena.jipipe.utils.scripting.JythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;


/**
 * Algorithm that annotates all data with the same annotation
 */
@SetJIPipeDocumentation(name = "Modify annotation rows (Script)", description = "Executes a Python-script for each annotation row. " +
        "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
        "converted into their respective JIPipe types.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Output", create = true)
public class ModifyAnnotationScript extends JIPipeSimpleIteratingAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());

    /**
     * @param info the info
     */
    public ModifyAnnotationScript(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ModifyAnnotationScript(ModifyAnnotationScript other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        JythonUtils.checkScriptValidity(code.getCode(getProjectDirectory()), scriptParameters, new ParameterValidationReportContext(reportContext, this, "Script", "code"), report);
        JythonUtils.checkScriptParametersValidity(scriptParameters, new ParameterValidationReportContext(reportContext, this, "Script parameters", "script-parameters"), report);
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
        code.makeExternalScriptFileRelative(baseDirectory);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        this.pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        super.run(runContext, progressInfo);
        this.pythonInterpreter = null;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        PyDictionary annotationDict = JIPipeTextAnnotation.annotationMapToPython(iterationStep.getMergedTextAnnotations());
        pythonInterpreter.set("annotations", annotationDict);
        pythonInterpreter.exec(code.getCode(getProjectDirectory()));
        annotationDict = (PyDictionary) pythonInterpreter.get("annotations");

        // Convert the results back into JIPipe
        iterationStep.getMergedTextAnnotations().clear();
        JIPipeTextAnnotation.setAnnotationsFromPython(annotationDict, iterationStep.getMergedTextAnnotations());

        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Script", description = "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
            "converted into their respective JIPipe types.")
    @JIPipeParameter("code")
    public PythonScript getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(PythonScript code) {
        this.code = code;
    }

    @SetJIPipeDocumentation(name = "Script parameters", description = "The following parameters will be passed to the Python script. The variable name is equal to the unique parameter identifier.")
    @JIPipeParameter(value = "script-parameters", persistence = JIPipeParameterSerializationMode.Object)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
