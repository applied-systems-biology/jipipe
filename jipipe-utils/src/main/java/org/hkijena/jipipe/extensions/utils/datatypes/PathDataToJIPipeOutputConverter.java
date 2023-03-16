package org.hkijena.jipipe.extensions.utils.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;

public class PathDataToJIPipeOutputConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return PathData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return JIPipeOutputData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        return new JIPipeOutputData(((PathData) input).getPath());
    }
}
