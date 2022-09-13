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

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.filesystem.FilesystemExtension;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.*;
import org.hkijena.jipipe.extensions.ijtrackmate.display.trackscheme.TrackSchemeDataDisplayOperation;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters.ConvertSpotsToRoiNode;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters.ConvertTracksToRoiNode;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters.SpotsToRoiConverter;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots.*;
import org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks.*;
import org.hkijena.jipipe.extensions.ijtrackmate.parameters.*;
import org.hkijena.jipipe.extensions.ijtrackmate.settings.ImageViewerUISpotsDisplaySettings;
import org.hkijena.jipipe.extensions.ijtrackmate.settings.ImageViewerUITracksDisplaySettings;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackDrawer;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackMateUtils;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.parameters.library.enums.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaExtension.class)
public class TrackMateExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-trackmate",
            JIPipe.getJIPipeVersion(),
            "IJ TrackMate integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(TrackMateExtension.class, "/org/hkijena/jipipe/extensions/ijtrackmate");

    public TrackMateExtension() {
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, FilesystemExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY, ImageJAlgorithmsExtension.AS_DEPENDENCY);
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_TRACKING);
    }

    @Override
    public ImageParameter getThumbnail() {
        return new ImageParameter(RESOURCES.getResourceURL("thumbnail.png"));
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List(
                new JIPipeAuthorMetadata("", "Jean-Yves", "Tinevez", new StringList("Imagopole, Citech, Institut Pasteur, 75724 Paris, France"), "", "", true, true),
                new JIPipeAuthorMetadata("", "Nick", "Perry", new StringList("Imagopole, Citech, Institut Pasteur, 75724 Paris, France"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Johannes", "Schindelin", new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin-Madison, Madison, WI 53706, USA."), "", "", false, false),
                new JIPipeAuthorMetadata("", "Genevieve M.", "Hoopes", new StringList("Department of Biochemistry, University of Wisconsin-Madison, Madison, WI 53706, USA."), "", "", false, false),
                new JIPipeAuthorMetadata("", "Gregory D.", "Reynolds", new StringList("Department of Biochemistry, University of Wisconsin-Madison, Madison, WI 53706, USA."), "", "", false, false),
                new JIPipeAuthorMetadata("", "Emmanuel", "Laplantine", new StringList("Laboratory of Signaling and Pathogenesis, Centre National de la Recherche Scientifique, UMR 3691, Institut Pasteur, 75724 Paris, France."), "", "", false, false),
                new JIPipeAuthorMetadata("", "Sebastian Y.", "Bednarek", new StringList("Department of Biochemistry, University of Wisconsin-Madison, Madison, WI 53706, USA."), "", "", false, false),
                new JIPipeAuthorMetadata("", "Spencer L.", "Shorte", new StringList("Imagopole, Citech, Institut Pasteur, 75724 Paris, France."), "", "", false, false),
                new JIPipeAuthorMetadata("", "Kevin W.", "Eliceiri", new StringList("Laboratory for Optical and Computational Instrumentation, University of Wisconsin-Madison, Madison, WI 53706, USA; Morgridge Institute for Research, Madison, WI 53719, USA."), "", "", false, false),
                new JIPipeAuthorMetadata("", "Dmitry", "Ershov", new StringList("Image Analysis Hub, C2RT / DTPS, Institut Pasteur, Paris, FR", "Biostatistics and Bioinformatic Hub, Department of Computational Biology, Institut Pasteur, Paris, FR"), "", "", true, false),
                new JIPipeAuthorMetadata("", "Minh-Son", "Phan", new StringList("Image Analysis Hub, C2RT / DTPS, Institut Pasteur, Paris, FR"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Joanna W.", "Pylvänäinen", new StringList("Turku Bioscience Centre, University of Turku and Åbo Akademi University, Turku, FI",
                        "Åbo Akademi University, Faculty of Science and Engineering, Biosciences, Turku, FI",
                        "Turku Bioimaging, University of Turku and Åbo Akademi University, Turku, Finland"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Stéphane U.", "Rigaud", new StringList("Image Analysis Hub, C2RT / DTPS, Institut Pasteur, Paris, FR\", \"Biostatistics and Bioinformatic Hub, Department of Computational Biology, Institut Pasteur, Paris, FR"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Laure", "Le Blanc", new StringList("Pathogenesis of Vascular Infections unit, INSERM, Institut Pasteur, Paris, FR",
                        "Université de Paris, 75006, Paris, FR"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Arthur", "Charles-Orszag", new StringList("Pathogenesis of Vascular Infections unit, INSERM, Institut Pasteur, Paris, FR"), "", "", false, false),
                new JIPipeAuthorMetadata("", "James R. W.", "Conway", new StringList("Turku Bioscience Centre, University of Turku and Åbo Akademi University, Turku, FI"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Romain F.", "Laine", new StringList("MRC Laboratory for Molecular Cell Biology, University College London, London, UK",
                        "The Francis Crick Institute, London, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Nathan H.", "Roy", new StringList("Department of Microbiology and Immunology, SUNY Upstate Medical University, Syracuse NY, USA"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Daria", "Bonazzi", new StringList("Pathogenesis of Vascular Infections unit, INSERM, Institut Pasteur, Paris, FR"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Guillaume", "Duménil", new StringList("Pathogenesis of Vascular Infections unit, INSERM, Institut Pasteur, Paris, FR"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Guillaume", "Jacquemet", new StringList("Turku Bioscience Centre, University of Turku and Åbo Akademi University, Turku, FI",
                        "Åbo Akademi University, Faculty of Science and Engineering, Biosciences, Turku, FI", "Turku Bioimaging, University of Turku and Åbo Akademi University, Turku, Finland"), "", "", false, false)
        );
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
        return Collections.singletonList(RESOURCES.getIcon32FromResources("trackmate.png"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        PluginService service = context.getService(PluginService.class);

        registerParameters();

        // Moved to different class to prevent crash if TrackMate is not available
        TrackMateUtils.registerSpotFeatures(progressInfo);
        TrackMateUtils.registerTrackFeatures(progressInfo);
        TrackMateUtils.registerEdgeFeatures(progressInfo);

        registerDataTypes();

        // Moved to different class to prevent crash if TrackMate is not available
        TrackMateUtils.registerSpotDetectors(this, progressInfo, service);
        TrackMateUtils.registerSpotTrackers(this, progressInfo, service);

        registerNodes();
        registerSettings();
    }

    private void registerParameters() {
        registerParameterType("trackmate-spot-feature", SpotFeature.class, "TrackMate spot feature", "A spot feature");
        registerParameterType("trackmate-track-feature", TrackFeature.class, "TrackMate track feature", "A track feature");
        registerParameterType("trackmate-edge-feature", EdgeFeature.class, "TrackMate edge feature", "An edge feature");
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
        registerParameterType("trackmate-track-feature-filter",
                TrackFeatureFilterParameter.class,
                TrackFeatureFilterParameter.List.class,
                null,
                null,
                "TrackMate track feature filter",
                "Filters tracks by a feature",
                null);
        registerEnumParameterType("trackmate-track-display-mode",
                DisplaySettings.TrackDisplayMode.class,
                "Track display mode",
                "Determines how tracks are displayed");
        registerEnumParameterType("trackmate-track-drawer:stroke-color",
                TrackDrawer.StrokeColorMode.class,
                "Track stroke color",
                "Mode for coloring track strokes");
    }

    private void registerSettings() {
        registerSettingsSheet(ImageViewerUISpotsDisplaySettings.ID,
                "Image viewer: Spots display",
                "Settings for the spots manager component of the JIPipe image viewer",
                RESOURCES.getIconFromResources("trackmate-spots.png"),
                "UI",
                null,
                new ImageViewerUISpotsDisplaySettings());
        registerSettingsSheet(ImageViewerUITracksDisplaySettings.ID,
                "Image viewer: Tracks display",
                "Settings for the track manager component of the JIPipe image viewer",
                RESOURCES.getIconFromResources("trackmate-tracker.png"),
                "UI",
                null,
                new ImageViewerUITracksDisplaySettings());
    }

    private void registerNodes() {
        registerNodeType("trackmate-detector", SpotDetectorNode.class, RESOURCES.getIcon16URLFromResources("trackmate.png"));
        registerNodeType("trackmate-tracking", TrackingNode.class, RESOURCES.getIcon16URLFromResources("trackmate.png"));
//        registerNodeType("trackmate-tracker", TrackerNode.class, RESOURCES.getIcon16URLFromResources("trackmate.png"));

        registerNodeType("trackmate-spots-to-roi", ConvertSpotsToRoiNode.class, UIUtils.getIconURLFromResources("actions/reload.png"));
        registerNodeType("trackmate-filter-spots", SpotFilterNode.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("trackmate-measure-spots", MeasureSpotsNode.class, UIUtils.getIconURLFromResources("actions/statistics.png"));

        registerNodeType("trackmate-tracks-to-roi", ConvertTracksToRoiNode.class, UIUtils.getIconURLFromResources("actions/reload.png"));
        registerNodeType("trackmate-filter-tracks", TrackFilterNode.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("trackmate-measure-tracks", MeasureTracksNode.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("trackmate-measure-edges", MeasureEdgesNode.class, UIUtils.getIconURLFromResources("actions/statistics.png"));
        registerNodeType("trackmate-measure-branches", MeasureBranchesNode.class, UIUtils.getIconURLFromResources("actions/distribute-graph-directed.png"));

        registerNodeType("trackmate-split-spots", SplitSpotsNode.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("trackmate-split-tracks", SplitTracksNode.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("trackmate-merge-spots", MergeSpotsNode.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("trackmate-merge-tracks", MergeTracksNode.class, UIUtils.getIconURLFromResources("actions/merge.png"));

        registerNodeType("trackmate-visualize-track-scheme", TrackSchemeRendererNode.class, RESOURCES.getIcon16URLFromResources("trackscheme.png"));
        registerNodeType("trackmate-visualize-follow-spots", FollowSpotsPerTrackNode.class, RESOURCES.getIcon16URLFromResources("trackscheme.png"));
        registerNodeType("trackmate-visualize-spots", SpotsToRGBNode.class, UIUtils.getIconURLFromResources("actions/color-management.png"));
        registerNodeType("trackmate-visualize-tracks", TracksToRGBNode.class, UIUtils.getIconURLFromResources("actions/color-management.png"));
    }

    private void registerDataTypes() {
        registerDatatype("trackmate-spot-detector", SpotDetectorData.class, RESOURCES.getIcon16URLFromResources("trackmate-spots.png"));
        registerDatatype("trackmate-spot-tracker", SpotTrackerData.class, RESOURCES.getIcon16URLFromResources("trackmate-tracker.png"));
        registerDatatype("trackmate-model", ModelData.class, RESOURCES.getIcon16URLFromResources("trackmate.png"));
        registerDatatype("trackmate-spot-collection", SpotsCollectionData.class, RESOURCES.getIcon16URLFromResources("trackmate-spots.png"));
        registerDatatype("trackmate-track-collection", TrackCollectionData.class, RESOURCES.getIcon16URLFromResources("trackmate-tracker.png"), new TrackSchemeDataDisplayOperation());
        registerDatatypeConversion(new SpotsToRoiConverter());
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-trackmate";
    }

}
