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

package org.hkijena.pipelinej.extensions.python;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithm;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.pipelinej.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.pipelinej.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.parameters.scripts.PythonScript;
import org.hkijena.pipelinej.utils.PythonUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.pipelinej.utils.PythonUtils.ALLOWED_PARAMETER_CLASSES;

/**
 * An algorithm that allows to run Python code
 */
@ACAQDocumentation(name = "Python script", description = "Runs a Python script that has direct access to all input data slots. Each slot is available as variable 'input_[slot name]' or 'output_[slot name]'. ")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Miscellaneous)
public class PythonScriptAlgorithm extends ACAQAlgorithm {

    private PythonScript code = new PythonScript();
    private ACAQDynamicParameterCollection scriptParameters = new ACAQDynamicParameterCollection(ALLOWED_PARAMETER_CLASSES);

    public PythonScriptAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().build());
        registerSubParameter(scriptParameters);
    }

    public PythonScriptAlgorithm(PythonScriptAlgorithm other) {
        super(other);
        this.code = new PythonScript(other.code);
        this.scriptParameters = new ACAQDynamicParameterCollection(other.scriptParameters);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (isPassThrough() && canAutoPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            runPassThrough();
            return;
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        super.reportValidity(report);
        PythonUtils.checkScriptValidity(code.getCode(), scriptParameters, report.forCategory("Script"));
        PythonUtils.checkScriptParametersValidity(scriptParameters, report.forCategory("Script parameters"));
    }

    @ACAQDocumentation(name = "Script", description = "")
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
