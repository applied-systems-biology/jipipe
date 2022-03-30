package org.hkijena.jipipe.extensions.clij2;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.macro.*;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.scijava.InstantiableException;

import java.util.HashMap;
import java.util.Map;

public class CLIJCommandNode extends JIPipeIteratingAlgorithm {

    private final JIPipeDynamicParameterCollection clijParameters;
    private CLIJMacroPlugin pluginInstance;

    public CLIJCommandNode(JIPipeNodeInfo info) {
        super(info);
        this.clijParameters = new JIPipeDynamicParameterCollection (((CLIJCommandNodeInfo)info).getNodeParameters());
    }

    public CLIJCommandNode(CLIJCommandNode other) {
        super(other);
        this.clijParameters = new JIPipeDynamicParameterCollection(other.clijParameters);
    }

    @JIPipeDocumentation(name = "CLIJ parameters", description = "Following parameters were extracted from the CLIJ2 operation:")
    @JIPipeParameter("clij-parameters")
    public JIPipeDynamicParameterCollection getClijParameters() {
        return clijParameters;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        CLIJCommandNodeInfo info = (CLIJCommandNodeInfo) getInfo();
        try {
            this.pluginInstance = info.getPluginInfo().createInstance();
        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }
        CLIJ2 clij2 = CLIJ2.getInstance();
        if(this.pluginInstance instanceof AbstractCLIJ2Plugin) {
            ((AbstractCLIJ2Plugin) this.pluginInstance).setCLIJ2(clij2);
        }
        this.pluginInstance.setClij(clij2.getCLIJ());

        super.run(progressInfo);
        this.pluginInstance = null;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJCommandNodeInfo info = (CLIJCommandNodeInfo) getInfo();

        // Prepare inputs
        Object[] args = new Object[info.getNumArgs()];
        pluginInstance.setArgs(args);

        for (String key : info.getNodeParameters().getParameters().keySet()) {
            int argIndex = info.getParameterIdToArgIndexMap().get(key);
            args[argIndex] = clijParameters.getParameter(key).get(Object.class);
        }
        Map<String, ClearCLBuffer> outputs = new HashMap<>();
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            int argIndex = info.getInputSlotToArgIndexMap().get(inputSlot.getName());
            CLIJImageData imageData = dataBatch.getInputData(inputSlot, CLIJImageData.class, progressInfo);
            if(info.getIoInputSlots().contains(inputSlot.getName())) {
                imageData = (CLIJImageData) imageData.duplicate(progressInfo);
                outputs.put(inputSlot.getName(), imageData.getImage());
            }
            args[argIndex] = imageData.getImage();
        }

        // Prepare outputs (dst buffer)
        ClearCLBuffer referenceImage = null;
        for (Object arg : args) {
            if(arg instanceof ClearCLBuffer) {
                referenceImage = (ClearCLBuffer) arg;
                break;
            }
        }

        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            if(outputs.containsKey(outputSlot.getName()))
                continue;
            if(outputSlot.getName().equals("Results table"))
                continue;
            int argIndex = info.getOutputSlotToArgIndexMap().get(outputSlot.getName());
            ClearCLBuffer buffer = pluginInstance.createOutputBufferFromSource(referenceImage);
            args[argIndex] = buffer;
            outputs.put(outputSlot.getName(), buffer);
        }

        // Run algorithm
        if (pluginInstance instanceof CLIJOpenCLProcessor) {
            ((CLIJOpenCLProcessor)pluginInstance).executeCL();
        } else if (pluginInstance instanceof CLIJImageJProcessor) {
            ((CLIJImageJProcessor)pluginInstance).executeIJ();
        }
        else {
            throw new UnsupportedOperationException("Unable to run CLIJ plugin of type " + pluginInstance.getClass());
        }

        // Extract outputs
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            ClearCLBuffer buffer = outputs.get(outputSlot.getName());
            CLIJImageData imageData = new CLIJImageData(buffer);
            dataBatch.addOutputData(outputSlot, imageData, progressInfo);
        }

        // Extract outputs table
        if(!info.getOutputTableColumnInfos().isEmpty()) {
            ResultsTableData resultsTableData = new ResultsTableData();
            for (CLIJCommandNodeInfo.OutputTableColumnInfo columnInfo : info.getOutputTableColumnInfos()) {
                resultsTableData.addColumn(columnInfo.getName(), columnInfo.isStringColumn());
            }
            resultsTableData.addRow();
            for (CLIJCommandNodeInfo.OutputTableColumnInfo columnInfo : info.getOutputTableColumnInfos()) {
                Object value = args[columnInfo.getArgIndex()];
                resultsTableData.setValueAt(value, 0, columnInfo.getName());
            }
            dataBatch.addOutputData("Results table", resultsTableData, progressInfo);
        }
    }
}
