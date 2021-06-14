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

package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JythonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;

/**
 * Algorithm that annotates all data with the same annotation
 */
@JIPipeDocumentation(name = "Modify tables (Script)", description = "Executes a Python-script for each table. " +
        "Each table is passed as dictionary 'table' with the column name as key and values being an array of strings or doubles. The number of input rows can be accessed via the 'nrow' variable.")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ModifyTablesScript extends JIPipeSimpleIteratingAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());

    /**
     * @param info the info
     */
    public ModifyTablesScript(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ModifyTablesScript(ModifyTablesScript other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            code.setCode("# This script is executed for each table\n" +
                    "# Tables are passed as dictionary 'table'\n" +
                    "# Key are the column names\n" +
                    "# Values are string/double arrays\n" +
                    "# Annotations can be modified via a dict 'annotations'\n\n" +
                    "areas = table[\"Area\"]\n" +
                    "areas_sq = [ x * x for x in areas ]\n" +
                    "table[\"Area2\"] = areas_sq");
            getEventBus().post(new ParameterChangedEvent(this, "code"));
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        JythonUtils.checkScriptValidity(code.getCode(getWorkDirectory()), scriptParameters, report.forCategory("Script"));
        JythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @Override
    public void setWorkDirectory(Path workDirectory) {
        super.setWorkDirectory(workDirectory);
        code.makeExternalScriptFileRelative(workDirectory);
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
        ResultsTableData inputData = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        PyDictionary tableDict = inputData.toPython();

        PyDictionary annotationDict = JIPipeAnnotation.annotationMapToPython(dataBatch.getAnnotations());
        pythonInterpreter.set("annotations", annotationDict);
        pythonInterpreter.set("table", tableDict);
        pythonInterpreter.set("nrow", inputData.getRowCount());
        pythonInterpreter.exec(code.getCode(getWorkDirectory()));
        tableDict = (PyDictionary) pythonInterpreter.get("table");
        annotationDict = (PyDictionary) pythonInterpreter.get("annotations");

        dataBatch.getAnnotations().clear();
        JIPipeAnnotation.setAnnotationsFromPython(annotationDict, dataBatch.getAnnotations());

        dataBatch.addOutputData(getFirstOutputSlot(), ResultsTableData.fromPython(tableDict), progressInfo);
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
