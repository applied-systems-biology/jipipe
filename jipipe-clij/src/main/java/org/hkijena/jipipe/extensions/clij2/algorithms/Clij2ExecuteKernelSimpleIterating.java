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

package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.clij2.CLIJExtension;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.clij2.parameters.OpenCLKernelScript;
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.scripts.PythonScript;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.python.core.PyArray;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "CLIJ2 Execute OpenCL kernel (simple iterating)", description = "Executes an OpenCL kernel via CLIJ2 to process images. This node can have only one input.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class)
public class Clij2ExecuteKernelSimpleIterating extends JIPipeSimpleIteratingAlgorithm {

    private OpenCLKernelScript kernelScript = new OpenCLKernelScript();
    private String kernelFunction = "";
    private PythonScript preprocessingScript = new PythonScript();
    private JIPipeDynamicParameterCollection scriptParameters = new JIPipeDynamicParameterCollection(true,
            CLIJExtension.ALLOWED_PARAMETER_TYPES);
    private StringAndStringPairParameter.List outputSizes = new StringAndStringPairParameter.List();

    public Clij2ExecuteKernelSimpleIterating(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(CLIJImageData.class)
                .restrictInputSlotCount(1)
                .restrictOutputTo(CLIJImageData.class)
                .build());
        registerSubParameter(scriptParameters);
        setPreprocessingScriptToExample();
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

    private void setPreprocessingScriptToExample() {
        preprocessingScript.setCode("from org.hkijena.jipipe.extensions.clij2.datatypes import CLIJImageData\n" +
                "\n" +
                "# By default, we will generate outputs based on the (first) input slot\n" +
                "input_buffer = data_batch.getInputData(input_slot, CLIJImageData).getImage()\n" +
                "\n" +
                "# Create output buffers\n" +
                "cl_output_buffers = { }\n" +
                "for name in output_slot_map:\t\n" +
                "\toutput_buffer = clij2.create(input_buffer)\n" +
                "\tcl_output_buffers[name] = output_buffer\n" +
                "\n" +
                "# output and global dimensions\n" +
                "cl_dimensions = input_buffer.getDimensions()\n" +
                "cl_global_sizes = input_buffer.getDimensions()");
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            slotConfiguration.clearInputSlots(true);
            slotConfiguration.clearOutputSlots(true);
            slotConfiguration.addSlot("src", new JIPipeDataSlotInfo(CLIJImageData.class, JIPipeSlotType.Input, null), true);
            slotConfiguration.addSlot("dst", new JIPipeDataSlotInfo(CLIJImageData.class, JIPipeSlotType.Output, null), true);
            kernelScript.setCode("__kernel void flip_2d (\n" +
                    "    IMAGE_src_TYPE  src,\n" +
                    "    IMAGE_dst_TYPE  dst,\n" +
                    "    const          int        flipx,\n" +
                    "    const          int        flipy\n" +
                    ")\n" +
                    "{\n" +
                    "  const sampler_t intsampler  = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_NONE | CLK_FILTER_NEAREST;\n" +
                    "\n" +
                    "  const int x = get_global_id(0);\n" +
                    "  const int y = get_global_id(1);\n" +
                    "\n" +
                    "  const int width = get_global_size(0);\n" +
                    "  const int height = get_global_size(1);\n" +
                    "\n" +
                    "  const int2 pos = (int2)(flipx?(width-1-x):x,\n" +
                    "                          flipy?(height-1-y):y);\n" +
                    "\n" +
                    "  const float value = READ_IMAGE(src, intsampler, pos).x;\n" +
                    "\n" +
                    "  WRITE_IMAGE (dst, (int2)(x,y), CONVERT_dst_PIXEL_TYPE(value));\n" +
                    "}");
            getEventBus().post(new ParameterChangedEvent(this, "kernel"));
            setKernelFunction("flip_2d");
            getEventBus().post(new ParameterChangedEvent(this, "kernel-program-name"));
            setPreprocessingScriptToExample();
            scriptParameters.clear();
            scriptParameters.addParameter("flipx", Boolean.class).setName("Flip X");
            scriptParameters.addParameter("flipy", Boolean.class).setName("Flip Y");
            scriptParameters.getEventBus().post(new ParameterStructureChangedEvent(scriptParameters));
        }
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        super.runParameterSet(progressInfo, parameterAnnotations);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        pythonInterpreter.set("clij2", clij2);
        pythonInterpreter.set("data_batch", dataBatch);
        PyDictionary inputSlotMap = new PyDictionary();
        PyDictionary outputSlotMap = new PyDictionary();
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            inputSlotMap.put(inputSlot.getName(), inputSlot);
        }
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            outputSlotMap.put(outputSlot.getName(), outputSlot);
        }
        pythonInterpreter.set("input_slots", new ArrayList<>(getInputSlots()));
        pythonInterpreter.set("output_slots", new ArrayList<>(getOutputSlots()));
        pythonInterpreter.set("input_slot_map", inputSlotMap);
        pythonInterpreter.set("output_slot_map", outputSlotMap);
        if (!getInputSlots().isEmpty()) {
            pythonInterpreter.set("input_slot", getFirstInputSlot());
        }
        pythonInterpreter.exec(preprocessingScript.getCode());

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
        for (Map.Entry<String, JIPipeDataSlot> entry : getInputSlotMap().entrySet()) {
            clParameters.put(entry.getKey(), dataBatch.getInputData(entry.getValue(), CLIJImageData.class, progressInfo).getImage());
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
        clij2.executeCode(kernelScript.getCode(), kernelName, clDimensionsArr, clGlobalSizesArr, clParameters);

        // Extract outputs
        for (Map.Entry<String, JIPipeDataSlot> entry : getOutputSlotMap().entrySet()) {
            dataBatch.addOutputData(entry.getKey(), new CLIJImageData((ClearCLBuffer) clParameters.get(entry.getKey())), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Preprocessing", description = "CLIJ2 requires some information about the output image(s) and the memory that is allocated by the kernel operation. " +
            "This script is executed to generate those values. There are following variables: 'cl_output_buffers', 'cl_dimensions', 'cl_global_sizes'. 'cl_output_buffers' is a dict that should contain the pre-allocated " +
            "output images for each output slot. 'cl_dimensions' is a list of integers that contains the dimensions of the output image. 'cl_global_sizes' is a list of integers that contains " +
            "the global size where the tasks are executed over (usually the output size). A variable 'clij2' is available that provides access to the CLIJ2 API. " +
            "A variable 'data_batch' provides access to the current data batch. " +
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

    @JIPipeDocumentation(name = "Program parameters", description = "Following parameters will be available from within the OpenCL program:")
    @JIPipeParameter(value = "program-parameters", persistence = JIPipeParameterPersistence.Object)
    public JIPipeDynamicParameterCollection getScriptParameters() {
        return scriptParameters;
    }

    @JIPipeDocumentation(name = "Kernel", description = "The OpenCL kernel script.")
    @JIPipeParameter("kernel")
    public OpenCLKernelScript getKernelScript() {
        return kernelScript;
    }

    @JIPipeParameter("kernel")
    public void setKernelScript(OpenCLKernelScript kernelScript) {
        this.kernelScript = kernelScript;
    }

    @JIPipeDocumentation(name = "Kernel program name", description = "The program that should be executed. Might be overwritten by the preprocessing script.")
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
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        HashSet<String> parameterNames = new HashSet<>(getInputSlotMap().keySet());
        parameterNames.addAll(getOutputSlotMap().keySet());
        parameterNames.addAll(scriptParameters.getParameters().keySet());
        if (parameterNames.size() != (getInputSlotMap().size() + getOutputSlotMap().size() + scriptParameters.getParameters().size())) {
            report.forCategory("Kernel").reportIsInvalid("All slots and script parameters must have unique names!",
                    "Input and output slots are passed to OpenCL, meaning that you cannot have duplicate input and output parameter and slot names.",
                    "Rename the slots, so they are unique within the whole algorithm. Define new parameters that have a different unique key",
                    this);
        }
    }
}
