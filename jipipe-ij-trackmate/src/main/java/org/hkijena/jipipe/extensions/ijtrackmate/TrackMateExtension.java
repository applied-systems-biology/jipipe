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

import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotDetectorData;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.CreateSpotDetectorNodeInfo;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
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

    public static Map<String, PluginInfo<SpotDetectorFactory>> getSpotDetectors() {
        return Collections.unmodifiableMap(SPOT_DETECTORS);
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
        PluginService service = context.getService(PluginService.class);

        registerDatatype("trackmate-spot-detector", SpotDetectorData.class, trackMateSpotsIcon16);

        registerSpotDetectors(progressInfo, trackMateIcon16, service);
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
