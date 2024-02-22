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

package org.hkijena.jipipe.extensions.tables.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.scripts.PythonScript;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.scripting.JythonUtils;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Algorithm that annotates all data with the same annotation
 */
@SetJIPipeDocumentation(name = "Modify & merge tables (Script)", description = "Executes a Python-script that allows full control over the input data table. " +
        "In the Python script, there is an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
        "'data' is a dictionary with the column name as key and values being an array of strings or doubles. " +
        "'nrow' is an integer that contains the number of rows. " +
        "'annotations' is a dictionary from string to string containing all annotations.")
@DefineJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
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

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        JythonUtils.checkScriptValidity(code.getCode(getProjectDirectory()), scriptParameters, new ParameterValidationReportContext(reportContext, this, "Script", "script"), report);
        JythonUtils.checkScriptParametersValidity(scriptParameters, new ParameterValidationReportContext(reportContext, this, "Script parameters", "script-parameters"), report);
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
        code.makeExternalScriptFileRelative(baseDirectory);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(runContext, progressInfo);
            return;
        }
        this.pythonInterpreter = new PythonInterpreter();
        JythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);

        List<PyDictionary> rows = new ArrayList<>();
        List<JIPipeDataContext> contexts = new ArrayList<>();
        List<List<JIPipeDataAnnotation>> dataAnnotations = new ArrayList<>();
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            PyDictionary rowDictionary = new PyDictionary();
            ResultsTableData tableData = getFirstInputSlot().getData(row, ResultsTableData.class, progressInfo);
            contexts.add(getFirstInputSlot().getDataContext(row));
            dataAnnotations.add(getFirstInputSlot().getDataAnnotations(row));

            rowDictionary.put("data", tableData.toPython());
            rowDictionary.put("nrow", tableData.getRowCount());
            rowDictionary.put("annotations", JIPipeTextAnnotation.annotationListToPython(getFirstInputSlot().getTextAnnotations(row)));

            rows.add(rowDictionary);
        }

        pythonInterpreter.set("tables", rows);
        pythonInterpreter.exec(code.getCode(getProjectDirectory()));
        rows = (List<PyDictionary>) pythonInterpreter.get("tables").__tojava__(List.class);

        for (int i = 0; i < rows.size(); i++) {
            PyDictionary pyDictionary = rows.get(i);
            ResultsTableData data = ResultsTableData.fromPython((PyDictionary) pyDictionary.get("data"));
            List<JIPipeTextAnnotation> annotations = JIPipeTextAnnotation.extractAnnotationsFromPython((PyDictionary) pyDictionary.getOrDefault("annotations", new PyDictionary()));
            getFirstOutputSlot().addData(data, annotations, JIPipeTextAnnotationMergeMode.Merge, dataAnnotations.get(i), JIPipeDataAnnotationMergeMode.Merge, contexts.get(i).branch(this), progressInfo);
        }

        this.pythonInterpreter = null;
    }

    @SetJIPipeDocumentation(name = "Script", description = "There is an array 'tables' that contains all input rows as dictionary with following entries: 'data', 'nrow', and 'annotations'. " +
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

    @SetJIPipeDocumentation(name = "Script parameters", description = "The following parameters will be passed to the Python script. The variable name is equal to the unique parameter identifier.")
    @JIPipeParameter(value = "script-parameters", persistence = JIPipeParameterSerializationMode.Object)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
