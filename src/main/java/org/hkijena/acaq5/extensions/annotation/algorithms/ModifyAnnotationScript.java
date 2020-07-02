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
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.pairs.IntegerAndIntegerPair;
import org.hkijena.acaq5.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.acaq5.extensions.parameters.pairs.StringAndStringPair;
import org.hkijena.acaq5.extensions.parameters.primitives.DoubleList;
import org.hkijena.acaq5.extensions.parameters.primitives.FloatList;
import org.hkijena.acaq5.extensions.parameters.primitives.IntegerList;
import org.hkijena.acaq5.extensions.parameters.primitives.PathList;
import org.hkijena.acaq5.extensions.parameters.primitives.StringList;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.extensions.parameters.scripts.PythonCode;
import org.hkijena.acaq5.utils.MacroUtils;
import org.python.core.PyArray;
import org.python.core.PyBoolean;
import org.python.core.PyDictionary;
import org.python.core.PyInteger;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that annotates all data with the same annotation
 */
@ACAQDocumentation(name = "Modify annotation rows (Script)", description = "Executes a Python-script for each annotation row. " +
        "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
        "converted into their respective ACAQ5 types.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Modify")
@AlgorithmInputSlot(value = ACAQData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class ModifyAnnotationScript extends ACAQSimpleIteratingAlgorithm {

    private PythonInterpreter pythonInterpreter;
    private PythonCode code = new PythonCode();
    public static Class<?>[] ALLOWED_PARAMETER_CLASSES = new Class[]{
            String.class,
            StringList.class,
            StringAndStringPair.List.class,
            Integer.class,
            IntegerList.class,
            IntegerAndIntegerPair.List.class,
            Double.class,
            DoubleList.class,
            Path.class,
            PathList.class,
            Boolean.class
    };
    private ACAQDynamicParameterCollection scriptParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    /**
     * @param declaration the declaration
     */
    public ModifyAnnotationScript(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        code.setCode("# This script is executed for each row\n" +
                "# Annotations are passed as dictionary 'annotations'\n" +
                "# Modifications are copied into ACAQ5\n\n");
        registerSubParameter(scriptParameters);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ModifyAnnotationScript(ModifyAnnotationScript other) {
        super(other);
        this.code = new PythonCode(other.code);
        this.scriptParameters = new ACAQDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        try {
            this.pythonInterpreter = new PythonInterpreter();
            pythonInterpreter.set("annotations", new PyDictionary());
            passScriptParametersToInterpreter();
            if(pythonInterpreter.compile(code.getCode()) == null) {
                report.forCategory("Script").reportIsInvalid("The script is invalid!",
                        "The script could not be compiled.",
                        "Please check if your Python script is correct.",
                        this);
            }
            this.pythonInterpreter = null;
        }
        catch (Exception e) {
            report.forCategory("Script").reportIsInvalid("The script is invalid!",
                    "The script could not be compiled.",
                    "Please check if your Python script is correct.",
                    e);
        }
        for (String key : scriptParameters.getParameters().keySet()) {
            if (!MacroUtils.isValidVariableName(key)) {
                report.forCategory("Macro Parameters").forCategory(key).reportIsInvalid("Invalid name!",
                        "'" + key + "' is an invalid Python variable name!",
                        "Please ensure that script variables are compatible with the ImageJ macro language.",
                        this);
            }
        }
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        this.pythonInterpreter = new PythonInterpreter();
        passScriptParametersToInterpreter();
        super.run(subProgress, algorithmProgress, isCancelled);
        this.pythonInterpreter = null;
    }

    private void passScriptParametersToInterpreter() {
        for (Map.Entry<String, ACAQParameterAccess> entry : scriptParameters.getParameters().entrySet()) {
              if(entry.getValue().getFieldClass() == StringAndStringPair.List.class) {
                  PyDictionary dictionary = new PyDictionary();
                  for (StringAndStringPair pair : entry.getValue().get(StringAndStringPair.List.class)) {
                    dictionary.put(pair.getKey(), pair.getValue());
                  }
                  pythonInterpreter.set(entry.getKey(), dictionary);
              }
              else  if(entry.getValue().getFieldClass() == IntegerAndIntegerPair.List.class) {
                  PyDictionary dictionary = new PyDictionary();
                  for (IntegerAndIntegerPair pair : entry.getValue().get(IntegerAndIntegerPair.List.class)) {
                      dictionary.put(pair.getKey(), pair.getValue());
                  }
                  pythonInterpreter.set(entry.getKey(), dictionary);
              }
              else {
                  pythonInterpreter.set(entry.getKey(), entry.getValue().get(Object.class));
              }
        }
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        PyDictionary annotationDict = new PyDictionary();
        for (Map.Entry<String, ACAQAnnotation> entry : dataInterface.getAnnotations().entrySet()) {
            annotationDict.put(new PyString(entry.getKey()), new PyString(entry.getValue().getValue()));
        }
        pythonInterpreter.set("annotations", annotationDict);
        pythonInterpreter.exec(code.getCode());

        // Convert the results back into ACAQ5
        dataInterface.getAnnotations().clear();
        for (Object key : annotationDict.keys()) {
            String keyString = "" + key;
            String valueString = "" + annotationDict.get(key);
            dataInterface.getAnnotations().put(keyString, new ACAQAnnotation(keyString, valueString));
        }

        dataInterface.addOutputData(getFirstOutputSlot(), dataInterface.getInputData(getFirstInputSlot(), ACAQData.class));
    }

    @ACAQDocumentation(name = "Script", description = "All annotations are passed as dictionary 'annotations' that can be modified using Python functions. The values are then extracted and " +
            "converted into their respective ACAQ5 types.")
    @ACAQParameter("code")
    public PythonCode getCode() {
        return code;
    }

    @ACAQParameter("code")
    public void setCode(PythonCode code) {
        this.code = code;
    }

     @ACAQDocumentation(name = "Script parameters", description = "The following parameters are prepended to the script code:")
     @ACAQParameter("script-parameters")
    public ACAQDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }
}
