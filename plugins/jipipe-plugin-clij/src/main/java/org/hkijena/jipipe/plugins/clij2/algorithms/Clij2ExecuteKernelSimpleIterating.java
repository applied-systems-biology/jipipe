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

package org.hkijena.jipipe.plugins.clij2.algorithms;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.clij2.CLIJPlugin;
import org.hkijena.jipipe.plugins.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.plugins.clij2.parameters.OpenCLKernelScript;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.scripts.PythonScript;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.python.core.PyArray;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.*;

@SetJIPipeDocumentation(name = "CLIJ2 Execute OpenCL kernel (simple iterating)", description = "Executes an OpenCL kernel via CLIJ2 to process images. This node can have only one input.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "CLIJ")
public class Clij2ExecuteKernelSimpleIterating extends JIPipeSimpleIteratingAlgorithm {

    private OpenCLKernelScript kernelScript = new OpenCLKernelScript();
    private String kernelFunction = "";
    private PythonScript preprocessingScript = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            CLIJPlugin.ALLOWED_PARAMETER_TYPES);
    private StringAndStringPairParameter.List outputSizes = new StringAndStringPairParameter.List();

    public Clij2ExecuteKernelSimpleIterating(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(CLIJImageData.class)
                .restrictInputSlotCount(1)
                .restrictOutputTo(CLIJImageData.class)
                .build());
        registerSubParameter(scriptParameters);
    }

    public Clij2ExecuteKernelSimpleIterating(Clij2ExecuteKernelSimpleIterating other) {
        super(other);
        this.kernelScript = new OpenCLKernelScript(other.kernelScript);
        this.kernelFunction = other.kernelFunction;
        this.preprocessingScript = new PythonScript(other.preprocessingScript);
        this.scriptParameters = new JIPipeDynamicParameterCollection(other.scriptParameters);
        this.outputSizes = new StringAndStringPairParameter.List(other.outputSizes);
        registerSubParameter(scriptParameters);
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        super.runParameterSet(runContext, progressInfo, parameterAnnotations);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        pythonInterpreter.set("clij2", clij2);
        pythonInterpreter.set("data_batch", iterationStep);
        PyDictionary inputSlotMap = new PyDictionary();
        PyDictionary outputSlotMap = new PyDictionary();
        for (JIPipeDataSlot inputSlot : getNonParameterInputSlots()) {
            inputSlotMap.put(inputSlot.getName(), inputSlot);
        }
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            outputSlotMap.put(outputSlot.getName(), outputSlot);
        }
        pythonInterpreter.set("input_slots", new ArrayList<>(getNonParameterInputSlots()));
        pythonInterpreter.set("output_slots", new ArrayList<>(getOutputSlots()));
        pythonInterpreter.set("input_slot_map", inputSlotMap);
        pythonInterpreter.set("output_slot_map", outputSlotMap);
        if (!getNonParameterInputSlots().isEmpty()) {
            pythonInterpreter.set("input_slot", getFirstInputSlot());
        }
        pythonInterpreter.set("progress_info", progressInfo);
        pythonInterpreter.exec(preprocessingScript.getCode(getProjectDirectory()));

        // Fetch constants
        Map<String, Object> clParameters = new HashMap<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : scriptParameters.getParameters().entrySet()) {
            Object o = entry.getValue().get(Object.class);
            if (o == null) {
                o = JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getFieldClass()).newInstance();
            }
            if (o instanceof Boolean) {
                o = (boolean) o ? 1 : 0;
            }
            clParameters.put(entry.getKey(), o);
        }

        // Fetch parameters (data)
        for (Map.Entry<String, JIPipeInputDataSlot> entry : getInputSlotMap().entrySet()) {
            clParameters.put(entry.getKey(), iterationStep.getInputData(entry.getValue(), CLIJImageData.class, progressInfo).getImage());
        }
        PyDictionary clOutputBuffersDict = pythonInterpreter.get("cl_output_buffers", PyDictionary.class);
        for (Object key : clOutputBuffersDict.keySet()) {
            String name = "" + key;
            clParameters.put(name, clOutputBuffersDict.get(key));
        }

        // Fetch dimensions
        long[] clDimensionsArr = (long[]) ((PyArray) pythonInterpreter.get("cl_dimensions")).getArray();

        // Fetch global sizes
        long[] clGlobalSizesArr = (long[]) ((PyArray) pythonInterpreter.get("cl_global_sizes")).getArray();

        // Fetch executed function
        String kernelName = kernelFunction;
        if (pythonInterpreter.get("cl_program") != null) {
            kernelName = pythonInterpreter.get("cl_program").__tojava__(String.class).toString();
        }
        clij2.executeCode(kernelScript.getCode(getProjectDirectory()), kernelName, clDimensionsArr, clGlobalSizesArr, clParameters);

        // Extract outputs
        for (Map.Entry<String, JIPipeOutputDataSlot> entry : getOutputSlotMap().entrySet()) {
            iterationStep.addOutputData(entry.getKey(), new CLIJImageData((ClearCLBuffer) clParameters.get(entry.getKey())), progressInfo);
        }
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
        kernelScript.makeExternalScriptFileRelative(baseDirectory);
    }

    @SetJIPipeDocumentation(name = "Preprocessing", description = "CLIJ2 requires some information about the output image(s) and the memory that is allocated by the kernel operation. " +
            "This script is executed to generate those values. There are following variables: 'cl_output_buffers', 'cl_dimensions', 'cl_global_sizes'. 'cl_output_buffers' is a dict that should contain the pre-allocated " +
            "output images for each output slot. 'cl_dimensions' is a list of integers that contains the dimensions of the output image. 'cl_global_sizes' is a list of integers that contains " +
            "the global size where the tasks are executed over (usually the output size). A variable 'clij2' is available that provides access to the CLIJ2 API. " +
            "A variable 'data_batch' provides access to the current iteration step. " +
            "Input slots can be accessed from variables 'input_slots' (array), 'input_slots_map' (map from name to slot). " +
            "The first (and only) input slot is also accessible via the 'input_slot' variable. " +
            "Output slots can be accessed from variables 'output_slots' (array), 'output_slots_map' (map from name to slot). " +
            "You can change the executed program by setting a variable 'cl_program'. Otherwise the one generated from parameter values is used.")
    @JIPipeParameter("preprocessing-script")
    public PythonScript getPreprocessingScript() {
        return preprocessingScript;
    }

    @JIPipeParameter("preprocessing-script")
    public void setPreprocessingScript(PythonScript preprocessingScript) {
        this.preprocessingScript = preprocessingScript;
    }

    @SetJIPipeDocumentation(name = "Program parameters", description = "Following parameters will be available from within the OpenCL program:")
    @JIPipeParameter(value = "program-parameters", persistence = JIPipeParameterSerializationMode.Object)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }

    @SetJIPipeDocumentation(name = "Kernel", description = "The OpenCL kernel script.")
    @JIPipeParameter("kernel")
    public OpenCLKernelScript getKernelScript() {
        return kernelScript;
    }

    @JIPipeParameter("kernel")
    public void setKernelScript(OpenCLKernelScript kernelScript) {
        this.kernelScript = kernelScript;
    }

    @SetJIPipeDocumentation(name = "Kernel program name", description = "The program that should be executed. Might be overwritten by the preprocessing script.")
    @JIPipeParameter("kernel-program-name")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png")
    public String getKernelFunction() {
        return kernelFunction;
    }

    @JIPipeParameter("kernel-program-name")
    public void setKernelFunction(String kernelFunction) {
        this.kernelFunction = kernelFunction;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        HashSet<String> parameterNames = new HashSet<>(getInputSlotMap().keySet());
        parameterNames.addAll(getOutputSlotMap().keySet());
        parameterNames.addAll(scriptParameters.getParameters().keySet());
        if (parameterNames.size() != (getInputSlotMap().size() + getOutputSlotMap().size() + scriptParameters.getParameters().size())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Kernel", "kernel"),
                    "All slots and script parameters must have unique names!",
                    "Input and output slots are passed to OpenCL, meaning that you cannot have duplicate input and output parameter and slot names.",
                    "Rename the slots, so they are unique within the whole algorithm. Define new parameters that have a different unique key"));
        }
    }
}
