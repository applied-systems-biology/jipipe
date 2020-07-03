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

package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.parameters.scripts.PythonScript;
import org.hkijena.acaq5.utils.PythonUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

/**
 * Algorithm that annotates all data with the same annotation
 */
@ACAQDocumentation(name = "Split & filter by annotation (Script)", description = "Executes a Python-script for each annotation row that allows redirection to a specified output slot (or to remove it). " +
        "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
        "converted into their respective ACAQ5 types. The target slot is extracted from a variable 'output_slot' that should be present within the script." +
        " If the variable is set to null or empty, the data is discarded.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Modify")
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Output")
public class SplitByAnnotationScript extends ACAQSimpleIteratingAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonScript code = new PythonScript();
    private ACAQDynamicParameterCollection scriptParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    /**
     * @param declaration the declaration
     */
    public SplitByAnnotationScript(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", ACAQData.class)
                .addOutputSlot("Output", ACAQData.class, "Input")
                .sealInput()
                .build());
        code.setCode("# This script is executed for each row\n" +
                "# Annotations are passed as dictionary 'annotations'\n" +
                "# Modifications are copied into ACAQ5\n" +
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
        this.scriptParameters = new ACAQDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        PythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        PythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        this.pythonInterpreter = new PythonInterpreter();
        PythonUtils.passParametersToPython(pythonInterpreter, scriptParameters);
        super.run(subProgress, algorithmProgress, isCancelled);
        this.pythonInterpreter = null;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PyDictionary annotationDict = ACAQAnnotation.annotationMapToPython(dataInterface.getAnnotations());
        pythonInterpreter.set("annotations", annotationDict);
        pythonInterpreter.exec(code.getCode());
        annotationDict = (PyDictionary) pythonInterpreter.get("annotations");

        // Convert the results back into ACAQ5
        dataInterface.getAnnotations().clear();
        ACAQAnnotation.setAnnotationsFromPython(annotationDict, dataInterface.getAnnotations());

        // Get the output slot
        PyObject outputSlotPy = pythonInterpreter.get("output_slot");
        String outputSlotName = null;
        if(outputSlotPy != null) {
            outputSlotName = "" + outputSlotPy;
        }

        if(!StringUtils.isNullOrEmpty(outputSlotName)) {
            dataInterface.addOutputData(getOutputSlot(outputSlotName), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
        }
    }

    @ACAQDocumentation(name = "Script", description = "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
            "converted into their respective ACAQ5 types. The target slot is extracted from a variable 'output_slot' that should be present within the script." +
            " If the variable is set to null or empty, the data is discarded.")
    @ACAQParameter("code")
    public PythonScript getCode() {
        return code;
    }

    @ACAQParameter("code")
    public void setCode(PythonScript code) {
        this.code = code;
    }

     @ACAQDocumentation(name = "Script parameters", description = "The following parameters will be passed to the Python script. The variable name is equal to the unique parameter identifier.")
     @ACAQParameter("script-parameters")
    public ACAQDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
