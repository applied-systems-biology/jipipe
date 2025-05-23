package org.hkijena.jipipe.plugins.imagejdatatypes.converters;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEXMLData;
import org.hkijena.jipipe.plugins.strings.XMLData;

public class XMLToOMEXMLTypeConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return XMLData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return OMEXMLData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        return new OMEXMLData(((XMLData) input).getData());
    }
}
