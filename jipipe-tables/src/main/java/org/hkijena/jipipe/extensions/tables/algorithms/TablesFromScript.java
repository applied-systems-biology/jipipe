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
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PythonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.List;

/**
 * Algorithm that annotates all data with the same annotation
 */
@JIPipeDocumentation(name = "Table from script", description = "Executes a Python-script that generates a table. " +
        "There must be an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
        "'data' is a dictionary with the column name as key and values being an array of strings or doubles. " +
        "'nrow' is an integer that contains the number of rows. " +
        "'annotations' is a dictionary from string to string containing all annotations")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class TablesFromScript extends JIPipeAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());

    /**
     * @param info the info
     */
    public TablesFromScript(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public TablesFromScript(TablesFromScript other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            code.setCode("# This script is executed once\n" +
                    "# The results are extracted from an array 'tables'\n" +
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
    public void reportValidity(JIPipeValidityReport report) {
        PythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        PythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        this.pythonInterpreter = new PythonInterpreter();
        PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);

        pythonInterpreter.exec(code.getCode());
        List<PyDictionary> rows = (List<PyDictionary>) pythonInterpreter.get("tables").__tojava__(List.class);

        for (PyDictionary row : rows) {
            ResultsTableData data = ResultsTableData.fromPython((PyDictionary) row.get("data"));
            List<JIPipeAnnotation> annotations = JIPipeAnnotation.extractAnnotationsFromPython((PyDictionary) row.getOrDefault("annotations", new PyDictionary()));
            getFirstOutputSlot().addData(data, annotations, JIPipeAnnotationMergeStrategy.Merge, progressInfo);
        }

        this.pythonInterpreter = null;
    }

    @JIPipeDocumentation(name = "Script", description = "here must be an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
            "<ul><li>'data' is a dictionary with the column name as key and values being an array of strings or doubles.</li>" +
            "<li>'nrow' is an integer that contains the number of rows.</li>" +
            "<li>'annotations' is a dictionary from string to string containing all annotations</li></ul>")
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
