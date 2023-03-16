package org.hkijena.jipipe.extensions.ijfilaments.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

public class FilamentsToRoiDataTypeConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return Filaments3DData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ROIListData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo) {
        return ((Filaments3DData) input).toRoi(false, true, true, -1, -1);
    }
}
