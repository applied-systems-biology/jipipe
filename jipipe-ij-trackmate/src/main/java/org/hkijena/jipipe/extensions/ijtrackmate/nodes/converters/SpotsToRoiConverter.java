package org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

public class SpotsToRoiConverter implements JIPipeDataConverter {
    @Override
    public Class<? extends JIPipeData> getInputType() {
        return SpotsCollectionData.class;
    }

    @Override
    public Class<? extends JIPipeData> getOutputType() {
        return ROIListData.class;
    }

    @Override
    public JIPipeData convert(JIPipeData input) {
        return ((SpotsCollectionData)input).spotsToROIList();
    }
}
