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

package org.hkijena.jipipe.plugins.ijtrackmate;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.filesystem.FilesystemPlugin;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.*;
import org.hkijena.jipipe.plugins.ijtrackmate.display.trackscheme.ShowTrackSchemeDataDisplayOperation;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.converters.ConvertSpotsToRoiNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.converters.ConvertTracksToRoiNode;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.converters.SpotsToRoiConverter;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.spots.*;
import org.hkijena.jipipe.plugins.ijtrackmate.nodes.tracks.*;
import org.hkijena.jipipe.plugins.ijtrackmate.parameters.*;
import org.hkijena.jipipe.plugins.ijtrackmate.settings.ImageViewerUISpotsDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.ijtrackmate.settings.ImageViewerUITracksDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.TrackDrawer;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.TrackMateUtils;
import org.hkijena.jipipe.plugins.ijtrackmate.viewers.TracksSpotsDataViewer;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaPlugin.class)
public class TrackMatePlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-trackmate",
            JIPipe.getJIPipeVersion(),
            "IJ TrackMate integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(TrackMatePlugin.class, "/org/hkijena/jipipe/plugins/ijtrackmate");

    public TrackMatePlugin() {
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, FilesystemPlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY, ImageJAlgorithmsPlugin.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_SEGMENTATION, PluginCategoriesEnumParameter.CATEGORY_TRACKING);
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        // Shared affiliations
        final JIPipeOrganizationMetadata pasteurImagopole = new JIPipeOrganizationMetadata.Builder()
                .name("Imagopole, Citech, Institut Pasteur, 75724 Paris, France")
                .ror("https://ror.org/0495fxg12")
                .website("https://research.pasteur.fr/en/team/imagopole/")
                .build();

        final JIPipeOrganizationMetadata lociUwMadison = new JIPipeOrganizationMetadata.Builder()
                .name("Laboratory for Optical and Computational Instrumentation, University of Wisconsin-Madison, Madison, WI 53706, USA")
                .ror("https://ror.org/01y2jtd41")
                .website("https://loci.wisc.edu")
                .build();

        final JIPipeOrganizationMetadata uwBiochem = new JIPipeOrganizationMetadata.Builder()
                .name("Department of Biochemistry, University of Wisconsin-Madison, Madison, WI 53706, USA")
                .website("https://biochem.wisc.edu")
                .build();

        final JIPipeOrganizationMetadata cnrsPasteur = new JIPipeOrganizationMetadata.Builder()
                .name("Laboratory of Signaling and Pathogenesis, Centre National de la Recherche Scientifique, UMR 3691, Institut Pasteur, 75724 Paris, France")
                .build();

        final JIPipeOrganizationMetadata morgridge = new JIPipeOrganizationMetadata.Builder()
                .name("Morgridge Institute for Research, Madison, WI 53719, USA")
                .ror("https://ror.org/05cb4rb43")
                .website("https://morgridge.org")
                .build();

        final JIPipeOrganizationMetadata pasteurImageHub = new JIPipeOrganizationMetadata.Builder()
                .name("Image Analysis Hub, C2RT / DTPS, Institut Pasteur, Paris, FR")
                .build();

        final JIPipeOrganizationMetadata pasteurBiostatHub = new JIPipeOrganizationMetadata.Builder()
                .name("Biostatistics and Bioinformatic Hub, Department of Computational Biology, Institut Pasteur, Paris, FR")
                .build();

        final JIPipeOrganizationMetadata turkuBiosci = new JIPipeOrganizationMetadata.Builder()
                .name("Turku Bioscience Centre, University of Turku and Åbo Akademi University, Turku, FI")
                .ror("https://ror.org/05vghhr25")
                .website("https://www.bioscience.fi")
                .build();

        final JIPipeOrganizationMetadata aboAkaBiosci = new JIPipeOrganizationMetadata.Builder()
                .name("Åbo Akademi University, Faculty of Science and Engineering, Biosciences, Turku, FI")
                .website("https://www.abo.fi")
                .build();

        final JIPipeOrganizationMetadata turkuBioimaging = new JIPipeOrganizationMetadata.Builder()
                .name("Turku Bioimaging, University of Turku and Åbo Akademi University, Turku, Finland")
                .website("https://www.bioimaging.fi")
                .build();

        final JIPipeOrganizationMetadata pasteurInserm = new JIPipeOrganizationMetadata.Builder()
                .name("Pathogenesis of Vascular Infections unit, INSERM, Institut Pasteur, Paris, FR")
                .build();

        final JIPipeOrganizationMetadata uParis = new JIPipeOrganizationMetadata.Builder()
                .name("Université de Paris, 75006, Paris, FR")
                .website("https://u-paris.fr")
                .build();

        final JIPipeOrganizationMetadata uclMrc = new JIPipeOrganizationMetadata.Builder()
                .name("MRC Laboratory for Molecular Cell Biology, University College London, London, UK")
                .ror("https://ror.org/00fv61j67")
                .website("https://www.ucl.ac.uk/lmcb")
                .build();

        final JIPipeOrganizationMetadata crick = new JIPipeOrganizationMetadata.Builder()
                .name("The Francis Crick Institute, London, UK")
                .ror("https://ror.org/04tnbqb63")
                .website("https://www.crick.ac.uk")
                .build();

        final JIPipeOrganizationMetadata sunyUpstate = new JIPipeOrganizationMetadata.Builder()
                .name("Department of Microbiology and Immunology, SUNY Upstate Medical University, Syracuse NY, USA")
                .website("https://www.upstate.edu")
                .build();

        return new JIPipeAuthorMetadata.List(
                new JIPipeAuthorMetadata.Builder().firstName("Jean-Yves").lastName("Tinevez").affiliations(List.of(pasteurImagopole)).firstAuthor(true).correspondingAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Nick").lastName("Perry").affiliations(List.of(pasteurImagopole)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Johannes").lastName("Schindelin").affiliations(List.of(lociUwMadison)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Genevieve M.").lastName("Hoopes").affiliations(List.of(uwBiochem)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Gregory D.").lastName("Reynolds").affiliations(List.of(uwBiochem)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Emmanuel").lastName("Laplantine").affiliations(List.of(cnrsPasteur)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Sebastian Y.").lastName("Bednarek").affiliations(List.of(uwBiochem)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Spencer L.").lastName("Shorte").affiliations(List.of(pasteurImagopole)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Kevin W.").lastName("Eliceiri").affiliations(List.of(lociUwMadison, morgridge)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Dmitry").lastName("Ershov").affiliations(List.of(pasteurImageHub, pasteurBiostatHub)).firstAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Minh-Son").lastName("Phan").affiliations(List.of(pasteurImageHub)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Joanna W.").lastName("Pylvänäinen").affiliations(List.of(turkuBiosci, aboAkaBiosci, turkuBioimaging)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Stéphane U.").lastName("Rigaud").affiliations(List.of(pasteurImageHub, pasteurBiostatHub)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Laure").lastName("Le Blanc").affiliations(List.of(pasteurInserm, uParis)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Arthur").lastName("Charles-Orszag").affiliations(List.of(pasteurInserm)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("James R. W.").lastName("Conway").affiliations(List.of(turkuBiosci)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Romain F.").lastName("Laine").affiliations(List.of(uclMrc, crick)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Nathan H.").lastName("Roy").affiliations(List.of(sunyUpstate)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Daria").lastName("Bonazzi").affiliations(List.of(pasteurInserm)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Guillaume").lastName("Duménil").affiliations(List.of(pasteurInserm)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Guillaume").lastName("Jacquemet").affiliations(List.of(turkuBiosci, aboAkaBiosci, turkuBioimaging)).build()
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
    public boolean isBeta() {
        return true;
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
        return Collections.singletonList(RESOURCES.getIcon32("trackmate.png"));
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        PluginService service = context.getService(PluginService.class);

        // Moved to different class to prevent crash if TrackMate is not available
        TrackMateUtils.registerSpotFeatures(progressInfo);
        TrackMateUtils.registerTrackFeatures(progressInfo);
        TrackMateUtils.registerEdgeFeatures(progressInfo);

        registerParameters();
        registerDataTypes();

        // Moved to different class to prevent crash if TrackMate is not available
        TrackMateUtils.registerSpotDetectors(this, progressInfo, service);
        TrackMateUtils.registerSpotTrackers(this, progressInfo, service);

        registerNodes();
        registerSettings();
    }

    private void registerParameters() {
        registerParameterType("trackmate-spot-feature", SpotFeature.class, JIPipeParameterArchetype.SingleSelect, "TrackMate spot feature", "A spot feature");
        registerParameterType("trackmate-track-feature", TrackFeature.class, JIPipeParameterArchetype.SingleSelect, "TrackMate track feature", "A track feature");
        registerParameterType("trackmate-edge-feature", EdgeFeature.class, JIPipeParameterArchetype.SingleSelect, "TrackMate edge feature", "An edge feature");
        registerParameterType("trackmate-spot-feature-penalty",
                SpotFeaturePenaltyParameter.class,
                JIPipeParameterArchetype.SingleSelect, SpotFeaturePenaltyParameter.List.class,
                null,
                null,
                "TrackMate spot feature penalty",
                "Associates a penalty value to a spot feature",
                null);
        registerParameterType("trackmate-spot-feature-filter",
                SpotFeatureFilterParameter.class,
                JIPipeParameterArchetype.SingleSelect, SpotFeatureFilterParameter.List.class,
                null,
                null,
                "TrackMate spot feature filter",
                "Filters spots by a feature",
                null);
        registerParameterType("trackmate-track-feature-filter",
                TrackFeatureFilterParameter.class,
                JIPipeParameterArchetype.SingleSelect, TrackFeatureFilterParameter.List.class,
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
        registerApplicationSettingsSheet(new ImageViewerUISpotsDisplayApplicationSettings());
        registerApplicationSettingsSheet(new ImageViewerUITracksDisplayApplicationSettings());
    }

    private void registerNodes() {
        registerNodeType("trackmate-detector", SpotDetectorNode.class, RESOURCES.getIcon16URL("trackmate.png"));
        registerNodeType("trackmate-tracking", TrackingNode.class, RESOURCES.getIcon16URL("trackmate.png"));
//        registerNodeType("trackmate-tracker", TrackerNode.class, RESOURCES.getIcon16URL("trackmate.png"));

        registerNodeType("trackmate-spots-to-roi", ConvertSpotsToRoiNode.class, JIPipe.RESOURCES.getIcon16URL("actions/reload.png"));
        registerNodeType("trackmate-filter-spots", SpotFilterNode.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("trackmate-measure-spots", MeasureSpotsNode.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));

        registerNodeType("trackmate-tracks-to-roi", ConvertTracksToRoiNode.class, JIPipe.RESOURCES.getIcon16URL("actions/reload.png"));
        registerNodeType("trackmate-filter-tracks", TrackFilterNode.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("trackmate-measure-tracks", MeasureTracksNode.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));
        registerNodeType("trackmate-measure-edges", MeasureEdgesNode.class, JIPipe.RESOURCES.getIcon16URL("actions/statistics.png"));
        registerNodeType("trackmate-measure-branches", MeasureBranchesNode.class, JIPipe.RESOURCES.getIcon16URL("actions/distribute-graph-directed.png"));

        registerNodeType("trackmate-split-spots", SplitSpotsNode.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("trackmate-split-tracks", SplitTracksNode.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("trackmate-merge-spots", MergeSpotsNode.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        registerNodeType("trackmate-merge-tracks", MergeTracksNode.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));

        registerNodeType("trackmate-visualize-track-scheme", TrackSchemeRendererNode.class, RESOURCES.getIcon16URL("trackscheme.png"));
        registerNodeType("trackmate-visualize-follow-spots", FollowSpotsPerTrackNode.class, RESOURCES.getIcon16URL("trackscheme.png"));
        registerNodeType("trackmate-visualize-spots", SpotsToRGBNode.class, JIPipe.RESOURCES.getIcon16URL("actions/color-management.png"));
        registerNodeType("trackmate-visualize-tracks", TracksToRGBNode.class, JIPipe.RESOURCES.getIcon16URL("actions/color-management.png"));
    }

    private void registerDataTypes() {
        registerDatatype("trackmate-spot-detector", SpotDetectorData.class, RESOURCES.getIcon16URL("trackmate-spots.png"));
        registerDatatype("trackmate-spot-tracker", SpotTrackerData.class, RESOURCES.getIcon16URL("trackmate-tracker.png"));
        registerDatatype("trackmate-model", ModelData.class, RESOURCES.getIcon16URL("trackmate.png"));
        registerDatatype("trackmate-spot-collection", SpotsCollectionData.class, RESOURCES.getIcon16URL("trackmate-spots.png"));
        registerDatatype("trackmate-track-collection", TrackCollectionData.class, RESOURCES.getIcon16URL("trackmate-tracker.png"), new ShowTrackSchemeDataDisplayOperation());
        registerDatatypeConversion(new SpotsToRoiConverter());

        registerDefaultDataTypeViewer(ModelData.class, TracksSpotsDataViewer.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-trackmate";
    }

}
