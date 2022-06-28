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

package org.hkijena.jipipe.extensions.ijtrackmate;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.ModelData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotDetectorData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotTrackerData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters.ConvertSpotsToRoiNode;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters.SpotsToRoiConverter;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.detectors.CreateSpotDetectorNodeInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.detectors.CreateSpotTrackerNodeInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.TrackerNode;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots.DetectNode;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots.MeasureSpotsNode;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots.SpotFilterNode;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeature;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeatureFilterParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeaturePenaltyParameter;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.TrackFeature;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.TrackFeatureFilterParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = JIPipeJavaExtension.class)
public class TrackMateExtension extends JIPipePrepackagedDefaultJavaExtension {

    public static final String RESOURCE_BASE_PATH = "/org/hkijena/jipipe/extensions/ijtrackmate";

    private static Map<String, PluginInfo<SpotDetectorFactory>> SPOT_DETECTORS = new HashMap<>();

    private static Map<String, PluginInfo<SpotTrackerFactory>> SPOT_TRACKERS = new HashMap<>();

    public static Map<String, PluginInfo<SpotDetectorFactory>> getSpotDetectors() {
        return Collections.unmodifiableMap(SPOT_DETECTORS);
    }

    public static Map<String, PluginInfo<SpotTrackerFactory>> getSpotTrackers() {
        return Collections.unmodifiableMap(SPOT_TRACKERS);
    }

    @Override
    public StringList getDependencyCitations() {
        StringList strings = new StringList();
        strings.add("Ershov, D., Phan, M.-S., Pylvänäinen, J. W., Rigaud, S. U., Le Blanc, L., Charles-Orszag, A., … Tinevez, J.-Y. (2021, September 3). Bringing TrackMate into the era of machine-learning and deep-learning. Cold Spring Harbor Laboratory. doi:10.1101/2021.09.03.458852");
        strings.add("Tinevez, J.-Y., Perry, N., Schindelin, J., Hoopes, G. M., Reynolds, G. D., Laplantine, E., … Eliceiri, K. W. (2017). TrackMate: An open and extensible platform for single-particle tracking. Methods, 115, 80–90. doi:10.1016/j.ymeth.2016.09.016");
        return strings;
    }

    @Override
    public String getName() {
        return "IJ TrackMate integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates TrackMate into JIPipe");
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Collections.singletonList(new ImageIcon(getClass().getResource(RESOURCE_BASE_PATH + "/trackmate-32.png")));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        URL trackMateIcon16 = getClass().getResource(RESOURCE_BASE_PATH + "/trackmate-16.png");
        URL trackMateSpotsIcon16 = getClass().getResource(RESOURCE_BASE_PATH + "/trackmate-spots.png");
        URL trackMateTrackerIcon16 = getClass().getResource(RESOURCE_BASE_PATH + "/trackmate-tracker.png");
        PluginService service = context.getService(PluginService.class);

        registerParameterType("trackmate-spot-feature", SpotFeature.class, "TrackMate spot feature", "A spot feature");
        registerParameterType("trackmate-spot-feature-penalty",
                SpotFeaturePenaltyParameter.class,
                SpotFeaturePenaltyParameter.List.class,
                null,
                null,
                "TrackMate spot feature penalty",
                "Associates a penalty value to a spot feature",
                null);
        registerParameterType("trackmate-spot-feature-filter",
                SpotFeatureFilterParameter.class,
                SpotFeatureFilterParameter.List.class,
                null,
                null,
                "TrackMate spot feature filter",
                "Filters spots by a feature",
                null);

        registerParameterType("trackmate-track-feature", TrackFeature.class, "TrackMate track feature", "A track feature");
        registerParameterType("trackmate-track-feature-filter",
                TrackFeatureFilterParameter.class,
                TrackFeatureFilterParameter.List.class,
                null,
                null,
                "TrackMate track feature filter",
                "Filters tracks by a feature",
                null);

        registerSpotFeatures(progressInfo);
        registerTrackFeatures(progressInfo);

        registerDatatype("trackmate-spot-detector", SpotDetectorData.class, trackMateSpotsIcon16);
        registerDatatype("trackmate-spot-tracker", SpotTrackerData.class, trackMateTrackerIcon16);
        registerDatatype("trackmate-model", ModelData.class, trackMateIcon16);
        registerDatatype("trackmate-spot-collection", SpotsCollectionData.class, trackMateSpotsIcon16);
        registerDatatypeConversion(new SpotsToRoiConverter());

        registerSpotDetectors(progressInfo, trackMateIcon16, service);
        registerSpotTrackers(progressInfo, trackMateIcon16, service);

        registerNodeType("trackmate-detector", DetectNode.class, trackMateIcon16);
        registerNodeType("trackmate-tracker", TrackerNode.class, trackMateIcon16);

        registerNodeType("trackmate-spots-to-roi", ConvertSpotsToRoiNode.class, UIUtils.getIconURLFromResources("actions/reload.png"));
        registerNodeType("trackmate-filter-spots", SpotFilterNode.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("trackmate-measure-spots", MeasureSpotsNode.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
    }

    private void registerSpotFeatures(JIPipeProgressInfo progressInfo) {
        Model model = new Model();
        for (Map.Entry<String, String> entry : model.getFeatureModel().getSpotFeatureNames().entrySet()) {
            String internalName = entry.getKey();
            String displayName = entry.getValue();
            progressInfo.log("Spot feature detected: " + internalName + " (" + displayName + ")");
            SpotFeature.ALLOWED_VALUES.add(internalName);
            SpotFeature.VALUE_LABELS.put(internalName, displayName);
        }
        Settings settings = new Settings();
        settings.addAllAnalyzers();
        for (SpotAnalyzerFactoryBase<?> spotAnalyzerFactory : settings.getSpotAnalyzerFactories()) {
            for (Map.Entry<String, String> entry : spotAnalyzerFactory.getFeatureNames().entrySet()) {
                String internalName = entry.getKey();
                String displayName = entry.getValue();
                progressInfo.log("Spot feature detected: " + internalName + " (" + displayName + ")");
                SpotFeature.ALLOWED_VALUES.add(internalName);
                SpotFeature.VALUE_LABELS.put(internalName, displayName);
            }
        }
    }

    private void registerTrackFeatures(JIPipeProgressInfo progressInfo) {
        Model model = new Model();
        for (Map.Entry<String, String> entry : model.getFeatureModel().getTrackFeatureNames().entrySet()) {
            String internalName = entry.getKey();
            String displayName = entry.getValue();
            progressInfo.log("Track feature detected: " + internalName + " (" + displayName + ")");
            TrackFeature.ALLOWED_VALUES.add(internalName);
            TrackFeature.VALUE_LABELS.put(internalName, displayName);
        }
        Settings settings = new Settings();
        settings.addAllAnalyzers();
        for (TrackAnalyzer trackAnalyzer : settings.getTrackAnalyzers()) {
            for (Map.Entry<String, String> entry : trackAnalyzer.getFeatureNames().entrySet()) {
                String internalName = entry.getKey();
                String displayName = entry.getValue();
                progressInfo.log("Track feature detected: " + internalName + " (" + displayName + ")");
                TrackFeature.ALLOWED_VALUES.add(internalName);
                TrackFeature.VALUE_LABELS.put(internalName, displayName);
            }
        }
    }

    private void registerSpotTrackers(JIPipeProgressInfo progressInfo, URL trackMateIcon16, PluginService service) {
        JIPipeProgressInfo spotDetectorProgress = progressInfo.resolveAndLog("Spot trackers");
        for (PluginInfo<SpotTrackerFactory> info : service.getPluginsOfType(SpotTrackerFactory.class)) {
            JIPipeProgressInfo detectorProgress = spotDetectorProgress.resolveAndLog(info.toString());
            try {
                SpotTrackerFactory instance = info.createInstance();
                CreateSpotTrackerNodeInfo nodeInfo = new CreateSpotTrackerNodeInfo(instance);
                registerNodeType(nodeInfo, trackMateIcon16);
                SPOT_TRACKERS.put(instance.getKey(), info);
            } catch (Throwable throwable) {
                detectorProgress.log("Unable to register: " + throwable.getMessage());
            }
        }
    }

    private void registerSpotDetectors(JIPipeProgressInfo progressInfo, URL trackMateIcon16, PluginService service) {
        JIPipeProgressInfo spotDetectorProgress = progressInfo.resolveAndLog("Spot detectors");
        for (PluginInfo<SpotDetectorFactory> info : service.getPluginsOfType(SpotDetectorFactory.class)) {
            JIPipeProgressInfo detectorProgress = spotDetectorProgress.resolveAndLog(info.toString());
            try {
                SpotDetectorFactory instance = info.createInstance();
                CreateSpotDetectorNodeInfo nodeInfo = new CreateSpotDetectorNodeInfo(instance);
                registerNodeType(nodeInfo, trackMateIcon16);
                SPOT_DETECTORS.put(instance.getKey(), info);
            } catch (Throwable throwable) {
                detectorProgress.log("Unable to register: " + throwable.getMessage());
            }
        }
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-trackmate";
    }

}
