package org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Convert tracks to ROI", description = "Converts TrackMate tracks into ROI lists. Each lists contains the spot ROI of one track.")
@JIPipeNode(menuPath = "Tracking\nConvert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class ConvertTracksToRoiNode extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter trackIDAnnotation = new OptionalAnnotationNameParameter("Track ID", true);

    public ConvertTracksToRoiNode(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertTracksToRoiNode(ConvertTracksToRoiNode other) {
        super(other);
        trackIDAnnotation = new OptionalAnnotationNameParameter(other.trackIDAnnotation);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        TrackCollectionData data = dataBatch.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo);

        for (Integer trackID : data.getTracks().trackIDs(true)) {
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            trackIDAnnotation.addAnnotationIfEnabled(annotationList, trackID + "");

            ROIListData rois = data.trackToROIList(trackID);
            dataBatch.addOutputData(getFirstOutputSlot(), rois, annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Annotate with track ID", description = "If enabled, the track ID is annotated to each ROI")
    @JIPipeParameter("track-id-annotation")
    public OptionalAnnotationNameParameter getTrackIDAnnotation() {
        return trackIDAnnotation;
    }

    @JIPipeParameter("track-id-annotation")
    public void setTrackIDAnnotation(OptionalAnnotationNameParameter trackIDAnnotation) {
        this.trackIDAnnotation = trackIDAnnotation;
    }
}
