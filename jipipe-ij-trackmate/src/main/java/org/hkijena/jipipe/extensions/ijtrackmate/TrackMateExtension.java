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
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class TrackMateExtension extends JIPipePrepackagedDefaultJavaExtension {

    public static final String RESOURCE_BASE_PATH = "/org/hkijena/jipipe/extensions/ijtrackmate";

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
        PluginService service = context.getService(PluginService.class);

        JIPipeProgressInfo spotDetectorProgress = progressInfo.resolveAndLog("Spot detectors");
        for (PluginInfo<SpotDetectorFactory> info : service.getPluginsOfType(SpotDetectorFactory.class)) {
            spotDetectorProgress.resolveAndLog(info.toString());
        }
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-trackmate";
    }

}
