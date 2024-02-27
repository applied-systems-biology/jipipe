/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.ijtrackmate.utils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.TrackMatePlugin;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.detectors.CreateSpotDetectorNodeInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.detectors.CreateSpotTrackerNodeInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.EdgeFeature;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.SpotFeature;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.TrackFeature;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TrackMateUtils {
    public static final DecimalFormat FEATURE_DECIMAL_FORMAT = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
    public static Map<String, PluginInfo<SpotDetectorFactory>> SPOT_DETECTORS = new HashMap<>();
    public static Map<String, PluginInfo<SpotTrackerFactory>> SPOT_TRACKERS = new HashMap<>();

    public static Map<String, PluginInfo<SpotDetectorFactory>> getSpotDetectors() {
        return Collections.unmodifiableMap(SPOT_DETECTORS);
    }

    public static Map<String, PluginInfo<SpotTrackerFactory>> getSpotTrackers() {
        return Collections.unmodifiableMap(SPOT_TRACKERS);
    }

    public static void registerSpotFeatures(JIPipeProgressInfo progressInfo) {
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

    public static void registerEdgeFeatures(JIPipeProgressInfo progressInfo) {
        Model model = new Model();
        for (Map.Entry<String, String> entry : model.getFeatureModel().getEdgeFeatureNames().entrySet()) {
            String internalName = entry.getKey();
            String displayName = entry.getValue();
            progressInfo.log("Edge feature detected: " + internalName + " (" + displayName + ")");
            EdgeFeature.ALLOWED_VALUES.add(internalName);
            EdgeFeature.VALUE_LABELS.put(internalName, displayName);
        }
        Settings settings = new Settings();
        settings.addAllAnalyzers();
        for (EdgeAnalyzer edgeAnalyzer : settings.getEdgeAnalyzers()) {
            for (Map.Entry<String, String> entry : edgeAnalyzer.getFeatureNames().entrySet()) {
                String internalName = entry.getKey();
                String displayName = entry.getValue();
                progressInfo.log("Edge feature detected: " + internalName + " (" + displayName + ")");
                EdgeFeature.ALLOWED_VALUES.add(internalName);
                EdgeFeature.VALUE_LABELS.put(internalName, displayName);
            }
        }
    }

    public static void registerTrackFeatures(JIPipeProgressInfo progressInfo) {
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

    public static void registerSpotTrackers(TrackMatePlugin trackMateExtension, JIPipeProgressInfo progressInfo, PluginService service) {
        JIPipeProgressInfo spotDetectorProgress = progressInfo.resolveAndLog("Spot trackers");
        for (PluginInfo<SpotTrackerFactory> info : service.getPluginsOfType(SpotTrackerFactory.class)) {
            JIPipeProgressInfo detectorProgress = spotDetectorProgress.resolveAndLog(info.toString());
            try {
                SpotTrackerFactory instance = info.createInstance();
                CreateSpotTrackerNodeInfo nodeInfo = new CreateSpotTrackerNodeInfo(instance);
                trackMateExtension.registerNodeType(nodeInfo, TrackMatePlugin.RESOURCES.getIcon16URLFromResources("trackmate.png"));
                SPOT_TRACKERS.put(instance.getKey(), info);
            } catch (Throwable throwable) {
                detectorProgress.log("Unable to register: " + throwable.getMessage());
            }
        }
    }

    public static void registerSpotDetectors(TrackMatePlugin trackMateExtension, JIPipeProgressInfo progressInfo, PluginService service) {
        JIPipeProgressInfo spotDetectorProgress = progressInfo.resolveAndLog("Spot detectors");
        for (PluginInfo<SpotDetectorFactory> info : service.getPluginsOfType(SpotDetectorFactory.class)) {
            JIPipeProgressInfo detectorProgress = spotDetectorProgress.resolveAndLog(info.toString());
            try {
                SpotDetectorFactory instance = info.createInstance();
                CreateSpotDetectorNodeInfo nodeInfo = new CreateSpotDetectorNodeInfo(instance);
                trackMateExtension.registerNodeType(nodeInfo, TrackMatePlugin.RESOURCES.getIcon16URLFromResources("trackmate.png"));
                SPOT_DETECTORS.put(instance.getKey(), info);
            } catch (Throwable throwable) {
                detectorProgress.log("Unable to register: " + throwable.getMessage());
            }
        }
    }
}
