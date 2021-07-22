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
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.scripting.JythonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Algorithm that annotates all data with the same annotation
 */
@JIPipeDocumentation(name = "Modify & merge tables (Script)", description = "Executes a Python-script that allows full control over the input data table. " +
        "In the Python script, there is an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
        "'data' is a dictionary with the column name as key and values being an array of strings or doubles. " +
        "'nrow' is an integer that contains the number of rows. " +
        "'annotations' is a dictionary from string to string containing all annotations.")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ModifyAndMergeTablesScript extends JIPipeAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());

    /**
     * @param info the info
     */
    public ModifyAndMergeTablesScript(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ModifyAndMergeTablesScript(ModifyAndMergeTablesScript other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            code.setCode("# This script is executed once\n" +
                    "# All tables are passed as array 'tables'\n" +
                    "# It contains dictionaries with following structure:\n" +
                    "# { 'data' : {}, 'nrow': x, 'annotations': {} }\n" +
                    "# 'data' is a dictionary from column name to a list of row data\n" +
                    "# 'nrow' is the number of rows (str/float)\n" +
                    "# 'annotations' is a dictionary from annotation name to value (str)" +
                    "\n\n" +
                    "tables = [\n" +
                    "    { \"data\": { \"example\" : [1,2,3] } }\n" +
                    "]");
            getEventBus().post(new ParameterChangedEvent(this, "code"));
        }
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
        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(progressInfo);
            return;
        }
        this.pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);

        List<PyDictionary> rows = new ArrayList<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            PyDictionary rowDictionary = new PyDictionary();
            ResultsTableData tableData = getFirstInputSlot().getData(row, ResultsTableData.class, progressInfo);

            rowDictionary.put("data", tableData.toPython());
            rowDictionary.put("nrow", tableData.getRowCount());
            rowDictionary.put("annotations", JIPipeAnnotation.annotationListToPython(getFirstInputSlot().getAnnotations(row)));

            rows.add(rowDictionary);
        }

        pythonInterpreter.set("tables", rows);
        pythonInterpreter.exec(code.getCode(getProjectWorkDirectory()));
        rows = (List<PyDictionary>) pythonInterpreter.get("tables").__tojava__(List.class);

        for (PyDictionary row : rows) {
            ResultsTableData data = ResultsTableData.fromPython((PyDictionary) row.get("data"));
            List<JIPipeAnnotation> annotations = JIPipeAnnotation.extractAnnotationsFromPython((PyDictionary) row.getOrDefault("annotations", new PyDictionary()));
            getFirstOutputSlot().addData(data, annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }

        this.pythonInterpreter = null;
    }

    @JIPipeDocumentation(name = "Script", description = "There is an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
            "'data' is a dictionary with the column name as key and values being an array of strings or doubles. " +
            "'nrow' is an integer that contains the number of rows. " +
            "'annotations' is a dictionary from string to string containing all annotations")
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
