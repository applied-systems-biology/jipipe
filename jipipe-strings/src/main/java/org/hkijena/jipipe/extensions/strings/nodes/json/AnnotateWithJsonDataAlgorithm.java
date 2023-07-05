package org.hkijena.jipipe.extensions.strings.nodes.json;

import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.strings.JsonData;

@JIPipeDocumentation(name = "Annotate with JSON values", description = "Extracts a value from the input JSON data (via JsonPath) and annotates the data with the result. " +
        "Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath")
@JIPipeCitation("JsonPath: https://goessner.net/articles/JsonPath/")
@JIPipeInputSlot(value = JsonData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JsonData.class, slotName = "Output", autoCreate = true)
public class AnnotateWithJsonDataAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public AnnotateWithJsonDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateWithJsonDataAlgorithm(AnnotateWithJsonDataAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }
}
