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

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageDimensions;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Crop to ROI", description = "Crops the incoming images to fit into the boundaries defined by the ROI.")
@JIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Tracks", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Image", autoCreate = true)
public class FollowSpotsPerTrackNode extends JIPipeIteratingAlgorithm {

    private OptionalAnnotationNameParameter trackIDAnnotation = new OptionalAnnotationNameParameter("Track ID", true);

    public FollowSpotsPerTrackNode(JIPipeNodeInfo info) {
        super(info);
    }

    public FollowSpotsPerTrackNode(FollowSpotsPerTrackNode other) {
        super(other);
        this.trackIDAnnotation = new OptionalAnnotationNameParameter(other.trackIDAnnotation);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
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
