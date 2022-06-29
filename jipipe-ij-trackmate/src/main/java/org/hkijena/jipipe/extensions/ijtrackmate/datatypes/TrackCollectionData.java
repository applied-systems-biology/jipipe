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

package org.hkijena.jipipe.extensions.ijtrackmate.datatypes;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.EdgeFeatureCalculator;
import fiji.plugin.trackmate.features.TrackFeatureCalculator;
import ij.ImagePlus;
import ij.gui.EllipseRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.JIPipeLogger;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.Optional;

@JIPipeDocumentation(name = "TrackMate tracks", description = "Tracks detected by TrackMate")
@JIPipeDataStorageDocumentation(humanReadableDescription = "TODO", jsonSchemaURL = "TODO")
public class TrackCollectionData extends SpotsCollectionData {
    public TrackCollectionData(Model model, Settings settings, ImagePlus image) {
        super(model, settings, image);
    }

    public TrackCollectionData(ModelData other) {
        super(other);
    }

    public static TrackCollectionData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        ModelData modelData = ModelData.importData(storage, progressInfo);
        return new TrackCollectionData(modelData.getModel(), modelData.getSettings(), modelData.getImage());
    }

    public TrackModel getTracks() {
        return getModel().getTrackModel();
    }

    public void computeTrackFeatures(JIPipeProgressInfo progressInfo) {
        final Logger logger = new JIPipeLogger(progressInfo);
        getModel().setLogger(logger);
        final TrackFeatureCalculator calculator = new TrackFeatureCalculator( getModel(), getSettings(), true );
        calculator.setNumThreads( 1 );
        if ( calculator.checkInput() && calculator.process() ) {
           getModel().notifyFeaturesComputed();
        }
        else {
            throw new RuntimeException("Unable to compute track features: " + calculator.getErrorMessage());
        }
    }

    public void computeEdgeFeatures(JIPipeProgressInfo progressInfo) {
        final Logger logger = new JIPipeLogger(progressInfo);
        getModel().setLogger(logger);
        final EdgeFeatureCalculator calculator = new EdgeFeatureCalculator( getModel(), getSettings(), true );
        calculator.setNumThreads( 1 );
        if ( calculator.checkInput() && calculator.process() ) {
            getModel().notifyFeaturesComputed();
        }
        else {
            throw new RuntimeException("Unable to compute track features: " + calculator.getErrorMessage());
        }
    }

    public ROIListData trackToROIList(int trackId) {
        ROIListData result = new ROIListData();
        for (Spot spot : getTracks().trackSpots(trackId)) {
            double x = spot.getDoublePosition(0);
            double y = spot.getDoublePosition(1);
            int z = (int) spot.getFloatPosition(2);
            int t = Optional.ofNullable(spot.getFeature(Spot.POSITION_T)).orElse(-1d).intValue();
            double radius = Optional.ofNullable(spot.getFeature(Spot.RADIUS)).orElse(1d);

            double x1 = x - radius;
            double x2 = x + radius;
            double y1 = y - radius;
            double y2 = y + radius;
            EllipseRoi roi = new EllipseRoi(x1, y1, x2, y2, 1);
            roi.setPosition(0, z+1, t+1);
            roi.setName(spot.getName());

            result.add(roi);
        }
        return result;
    }

    @Override
    public String toString() {
        return getModel().getTrackModel().nTracks(true) + " tracks, " + super.toString();
    }

    @Override
    public String toDetailedString() {
        return toString();
    }
}
