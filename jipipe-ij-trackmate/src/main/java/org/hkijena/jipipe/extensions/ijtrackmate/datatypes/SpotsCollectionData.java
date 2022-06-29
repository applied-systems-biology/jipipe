package org.hkijena.jipipe.extensions.ijtrackmate.datatypes;

import fiji.plugin.trackmate.*;
import ij.ImagePlus;
import ij.gui.EllipseRoi;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.display.CachedROIListDataViewerWindow;
import org.hkijena.jipipe.extensions.parameters.library.colors.ColorMap;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.Component;
import java.util.Optional;

@JIPipeDocumentation(name = "TrackMate spots", description = "Spots detected by TrackMate")
@JIPipeDataStorageDocumentation(humanReadableDescription = "TODO", jsonSchemaURL = "TODO")
public class SpotsCollectionData extends ModelData {
    public SpotsCollectionData(Model model, Settings settings, ImagePlus image) {
        super(model, settings, image);
    }

    public SpotsCollectionData(ModelData other) {
        super(other);
    }

    public static SpotsCollectionData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        ModelData modelData = ModelData.importData(storage, progressInfo);
        return new SpotsCollectionData(modelData.getModel(), modelData.getSettings(), modelData.getImage());
    }

    @Override
    public String toString() {
        return getModel().getSpots().getNSpots(true) + " spots";
    }

    @Override
    public String toDetailedString() {
        return getModel().getSpots().toString();
    }

    public SpotCollection getSpots() {
        return getModel().getSpots();
    }

    @Override
    public Component preview(int width, int height) {
        ROIListData rois = spotsToROIList();
        int dMax = 1;
        for (Roi roi : rois) {
            int d = roi.getZPosition() + roi.getCPosition() + roi.getTPosition();
            dMax = Math.max(d, dMax);
        }
        for (Roi roi : rois) {
            int d = roi.getZPosition() + roi.getCPosition() + roi.getTPosition();
            roi.setStrokeColor(ColorMap.hsv.apply(1.0 * d / dMax));
        }
        return rois.preview(width, height);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        // Possible due to conversion of Spots to ROI
        CachedROIListDataViewerWindow window = new CachedROIListDataViewerWindow(workbench, JIPipeDataTableDataSource.wrap(this, source), displayName, false);
        window.setVisible(true);
    }

    public ROIListData spotsToROIList() {
        ROIListData result = new ROIListData();
        for (Spot spot : getSpots().iterable(true)) {
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
}
