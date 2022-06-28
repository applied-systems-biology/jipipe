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
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.nodes;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotDetectorData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotTrackerData;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeature;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeatureFilterParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.TrackFeatureFilterParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.JIPipeLogger;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "TrackMate", description = "Applies a TrackMate tracking")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", description = "The image to be tracked", autoCreate = true)
@JIPipeInputSlot(value = SpotDetectorData.class, slotName = "Spot detector", description = "The algorithm that detects the spots", autoCreate = true)
@JIPipeInputSlot(value = SpotTrackerData.class, slotName = "Spot tracker", description = "The algorithm that tracks the spots", autoCreate = true)
public class TrackerNode extends JIPipeIteratingAlgorithm {

    private SpotFeatureFilterParameter.List spotFilters = new SpotFeatureFilterParameter.List();
    private TrackFeatureFilterParameter.List trackFilters = new TrackFeatureFilterParameter.List();

    public TrackerNode(JIPipeNodeInfo info) {
        super(info);
        spotFilters.add(new SpotFeatureFilterParameter(new SpotFeature("QUALITY"), 30, true));
    }

    public TrackerNode(TrackerNode other) {
        super(other);
        this.spotFilters = new SpotFeatureFilterParameter.List(other.spotFilters);
        this.trackFilters = new TrackFeatureFilterParameter.List(other.trackFilters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        SpotDetectorData spotDetectorData = dataBatch.getInputData("Spot detector", SpotDetectorData.class, progressInfo);
        SpotTrackerData spotTrackerData = dataBatch.getInputData("Spot tracker", SpotTrackerData.class, progressInfo);

        Settings settings = new Settings(image);

        // Detection
        settings.detectorFactory = spotDetectorData.getSpotDetectorFactory().copy();
        settings.detectorSettings = spotDetectorData.getSettings();
        for (SpotFeatureFilterParameter spotFilter : spotFilters) {
            settings.addSpotFilter(spotFilter.toFeatureFilter());
        }

        // Tracking
        settings.trackerFactory = spotTrackerData.getTrackerFactory().copy();
        settings.trackerSettings = spotTrackerData.getSettings();
        settings.addAllAnalyzers();
        for (TrackFeatureFilterParameter trackFilter : trackFilters) {
            settings.addTrackFilter(trackFilter.toFeatureFilter());
        }

        Model model = new Model();
        model.setLogger(new JIPipeLogger(progressInfo.resolve("TrackMate")));
        TrackMate trackMate = new TrackMate(model, settings);

        if(!trackMate.checkInput()) {
            progressInfo.log(trackMate.getErrorMessage());
            throw new UserFriendlyRuntimeException(trackMate.getErrorMessage(),
                    "TrackMate: Invalid input",
                    getDisplayName(),
                    "TrackMate detected an invalid input",
                    "Please check the parameters");
        }

        if(!trackMate.process()) {
            progressInfo.log(trackMate.getErrorMessage());
            throw new UserFriendlyRuntimeException(trackMate.getErrorMessage(),
                    "TrackMate: Error while processing",
                    getDisplayName(),
                    "TrackMate could not successfully process the data",
                    "Please check the error message");
        }
    }

    @JIPipeDocumentation(name = "Spot filters", description = "Allows to filter the detected spots by various features")
    @JIPipeParameter("spot-filters")
    public SpotFeatureFilterParameter.List getSpotFilters() {
        return spotFilters;
    }

    @JIPipeParameter("spot-filters")
    public void setSpotFilters(SpotFeatureFilterParameter.List spotFilters) {
        this.spotFilters = spotFilters;
    }

    @JIPipeDocumentation(name = "Track filters", description = "Allows to filter the detected tracks by various features")
    @JIPipeParameter("track-filters")
    public TrackFeatureFilterParameter.List getTrackFilters() {
        return trackFilters;
    }

    @JIPipeParameter("track-filters")
    public void setTrackFilters(TrackFeatureFilterParameter.List trackFilters) {
        this.trackFilters = trackFilters;
    }
}
