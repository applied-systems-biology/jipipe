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

package org.hkijena.jipipe.plugins.omero;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.omero.datatypes.*;
import org.hkijena.jipipe.plugins.omero.nodes.annotate.*;
import org.hkijena.jipipe.plugins.omero.nodes.datasources.*;
import org.hkijena.jipipe.plugins.omero.nodes.download.DownloadOMEROImageAlgorithm;
import org.hkijena.jipipe.plugins.omero.nodes.download.DownloadOMEROTableAlgorithm;
import org.hkijena.jipipe.plugins.omero.nodes.manage.OMEROCreateDatasetAlgorithm;
import org.hkijena.jipipe.plugins.omero.nodes.navigate.*;
import org.hkijena.jipipe.plugins.omero.nodes.upload.UploadOMEROImageAlgorithm;
import org.hkijena.jipipe.plugins.omero.nodes.upload.UploadOMEROTableAlgorithm;
import org.hkijena.jipipe.plugins.omero.viewers.OMERODataViewer;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Provides data types dor handling strings
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class OMEROPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:omero",
            JIPipe.getJIPipeVersion(),
            "OMERO Integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(OMEROPlugin.class, "org/hkijena/jipipe/plugins/omero");

    public OMEROPlugin() {
    }

    public static OMEROCredentialsEnvironment getEnvironment(JIPipeProject project, OptionalOMEROCredentialsEnvironment nodeEnvironment) {
        if (nodeEnvironment.isEnabled()) {
            return nodeEnvironment.getContent();
        }
        if (project != null && project.getSettingsSheet(OMEROPluginProjectSettings.class).getProjectDefaultEnvironment().isEnabled()) {
            return project.getSettingsSheet(OMEROPluginProjectSettings.class).getProjectDefaultEnvironment().getContent();
        }
        return OMEROPluginApplicationSettings.getInstance().getDefaultCredentials();
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_IMPORT_EXPORT, PluginCategoriesEnumParameter.CATEGORY_SCIJAVA, PluginCategoriesEnumParameter.CATEGORY_OME);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY, ImageJDataTypesPlugin.AS_DEPENDENCY);
    }

    @Override
    public boolean canActivate(JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {

        String[] classes = new String[]{
                "ome.xml.meta.OMEXMLMetadata",
                "omero.gateway.SecurityContext",
                "omero.gateway.LoginCredentials",
                "omero.gateway.model.ImageData",
                "omero.gateway.model.TableData",
                "omero.gateway.model.DatasetData",
                "omero.model.Pixels",
                "omero.model.NamedValue",
                "omero.gateway.facility.BrowseFacility",
                "omero.gateway.facility.DataManagerFacility",
                "omero.gateway.facility.MetadataFacility"
        };
        boolean result = true;
        for (String aClass : classes) {
            boolean exists = ReflectionUtils.classExists(aClass);
            progressInfo.resolve("Checking classes").log(aClass + ": " + (exists ? "success" : "FAILURE"));
            result &= exists;
        }

        if (!result) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new UnspecifiedValidationReportContext(),
                    "No working OMERO detected!",
                    "The JIPipe OMERO extension requires a working OMERO installation. Preliminary checks determined that there is none.",
                    "Please install OMERO from the official OMERO website or install the appropriate OMERO plugins via the ImageJ updater.",
                    "At least one of the following classes were not found: " + String.join(", ", classes)));
        }

        return result;
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("", "Chris", "Allan", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK",
                "Glencoe Software, Inc., Seattle, Washington, USA"), "", "", true, false),
                new JIPipeAuthorMetadata("", "Jean-Marie", "Burel", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK",
                        "Glencoe Software, Inc., Seattle, Washington, USA"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Josh", "Moore", new StringList("Glencoe Software, Inc., Seattle, Washington, USA"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Colin", "Blackburn", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK",
                        "Glencoe Software, Inc., Seattle, Washington, USA"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Melissa", "Linkert", new StringList("Glencoe Software, Inc., Seattle, Washington, USA"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Scott", "Loynton", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Donald", "MacDonald", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "William J.", "Moore", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Carlos", "Neves", new StringList("Glencoe Software, Inc., Seattle, Washington, USA"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Andrew", "Patterson", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Michael", "Porter", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Aleksandra", "Tarkowska", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Brian", "Loranger", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Jerome", "Avondo", new StringList("John Innes Centre Norwich Research Park, Norwich, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Ingvar", "Lagerstedt", new StringList("European Molecular B iology Laboratory– European Bioinformatics Institute, Wellcome Trust Genome Campus, Hinxton, Cambridge, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Luca", "Lianas", new StringList("CRS4, Pula, Italy"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Simone", "Leo", new StringList("CRS4, Pula, Italy"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Katherine", "Hands", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Ron T.", "Hay", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Ardan", "Patwardhan", new StringList("European Molecular B iology Laboratory– European Bioinformatics Institute, Wellcome Trust Genome Campus, Hinxton, Cambridge, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Christoph", "Best", new StringList("European Molecular B iology Laboratory– European Bioinformatics Institute, Wellcome Trust Genome Campus, Hinxton, Cambridge, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Gerard J.", "Kleywegt", new StringList("European Molecular B iology Laboratory– European Bioinformatics Institute, Wellcome Trust Genome Campus, Hinxton, Cambridge, UK"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Gianluigi", "Zanetti", new StringList("CRS4, Pula, Italy"), "", "", false, false),
                new JIPipeAuthorMetadata("", "Jason R.", "Swedlow", new StringList("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK",
                        "Glencoe Software, Inc., Seattle, Washington, USA"), "", "", false, false));
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList("Chris Allan, Jean-Marie Burel, Josh Moore, Colin Blackburn, Melissa Linkert, Scott Loynton, Donald MacDonald, William J Moore, Carlos Neves, Andrew Patterson, Michael Porter, Aleksandra Tarkowska, Brian Loranger, " +
                "Jerome Avondo, Ingvar Lagerstedt, Luca Lianas, Simone Leo, Katherine Hands, Ron T Hay, Ardan Patwardhan, Christoph Best, Gerard J Kleywegt, Gianluigi Zanetti & Jason R Swedlow (2012) OMERO: flexible, model-driven data management for experimental biology. Nature Methods 9, 245–253. Published: 28 February 2012");
    }

    @Override
    public String getName() {
        return "OMERO Integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates OMERO");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        OMEROPluginApplicationSettings omeroSettings = new OMEROPluginApplicationSettings();
        registerApplicationSettingsSheet(omeroSettings);
        registerEnvironment(OMEROCredentialsEnvironment.class,
                OMEROCredentialsEnvironment.List.class,
                omeroSettings,
                "omero-credentials",
                "OMERO Credentials",
                "Credentials for an OMERO server",
                RESOURCES.getIconFromResources("omero.png"));
        registerParameterType("optional-omero-credentials",
                OptionalOMEROCredentialsEnvironment.class,
                "Optimal OMERO credentials",
                "Optional OMERO credentials");
        registerProjectSettingsSheet(OMEROPluginProjectSettings.class);

        // Data types
        registerDatatype("omero-group-id", OMEROGroupReferenceData.class, RESOURCES.getIconURLFromResources("omero-group.png"));
        registerDatatype("omero-project-id", OMEROProjectReferenceData.class, RESOURCES.getIconURLFromResources("omero-project.png"));
        registerDatatype("omero-dataset-id", OMERODatasetReferenceData.class, RESOURCES.getIconURLFromResources("omero-dataset.png"));
        registerDatatype("omero-image-id", OMEROImageReferenceData.class, RESOURCES.getIconURLFromResources("omero-image.png"));
        registerDatatype("omero-annotation-id", OMEROAnnotationReferenceData.class, RESOURCES.getIconURLFromResources("omero-annotation.png"));
        registerDatatype("omero-screen-id", OMEROScreenReferenceData.class, RESOURCES.getIconURLFromResources("omero-screen.png"));
        registerDatatype("omero-plate-id", OMEROPlateReferenceData.class, RESOURCES.getIconURLFromResources("omero-plate.png"));
        registerDatatype("omero-well-id", OMEROWellReferenceData.class, RESOURCES.getIconURLFromResources("omero-well.png"));

        registerDefaultDataTypeViewer(OMEROGroupReferenceData.class, OMERODataViewer.class);
        registerDefaultDataTypeViewer(OMEROProjectReferenceData.class, OMERODataViewer.class);
        registerDefaultDataTypeViewer(OMERODatasetReferenceData.class, OMERODataViewer.class);
        registerDefaultDataTypeViewer(OMEROImageReferenceData.class, OMERODataViewer.class);
        registerDefaultDataTypeViewer(OMEROAnnotationReferenceData.class, OMERODataViewer.class);
        registerDefaultDataTypeViewer(OMEROScreenReferenceData.class, OMERODataViewer.class);
        registerDefaultDataTypeViewer(OMEROPlateReferenceData.class, OMERODataViewer.class);
        registerDefaultDataTypeViewer(OMEROWellReferenceData.class, OMERODataViewer.class);

        // Data sources
        registerNodeType("omero-image-id-definition", OMEROImageReferenceDataSource.class);
        registerNodeType("omero-dataset-id-definition", OMERODatasetReferenceDataSource.class);
        registerNodeType("omero-project-id-definition", OMEROProjectReferenceDataSource.class);
        registerNodeType("omero-group-id-definition", OMEROGroupReferenceDataSource.class);
        registerNodeType("omero-screen-id-definition", OMEROScreenReferenceDataSource.class);
        registerNodeType("omero-plate-id-definition", OMEROPlateReferenceDataSource.class);
        registerNodeType("omero-well-id-definition", OMEROWellReferenceDataSource.class);

        registerNodeType("omero-list-group-ids", OMEROListGroupsAlgorithm.class, UIUtils.getIconURLFromResources("actions/search.png"));
        registerNodeType("omero-list-project-ids", OMEROListProjectsAlgorithm.class, UIUtils.getIconURLFromResources("actions/search.png"));
        registerNodeType("omero-list-dataset-ids", OMEROListDatasetsAlgorithm.class, UIUtils.getIconURLFromResources("actions/search.png"));
        registerNodeType("omero-list-image-ids", OMEROListImagesAlgorithm.class, UIUtils.getIconURLFromResources("actions/search.png"));
        registerNodeType("omero-list-screen-ids", OMEROListScreensAlgorithm.class, UIUtils.getIconURLFromResources("actions/search.png"));
        registerNodeType("omero-list-plate-ids", OMEROListPlatesAlgorithm.class, UIUtils.getIconURLFromResources("actions/search.png"));
        registerNodeType("omero-list-well-ids", OMEROListWellsAlgorithm.class, UIUtils.getIconURLFromResources("actions/search.png"));

        registerNodeType("omero-annotate-dataset-from-remote", AnnotateOMERODatasetReferenceAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("omero-annotate-image-from-remote", AnnotateOMEROImageReferenceAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("omero-annotate-project-from-remote", AnnotateOMEROProjectReferenceAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("omero-annotate-screen-from-remote", AnnotateOMEROScreenReferenceAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("omero-annotate-plate-from-remote", AnnotateOMEROPlateReferenceAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("omero-annotate-well-from-remote", AnnotateOMEROWellReferenceAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));

        registerNodeType("omero-create-dataset", OMEROCreateDatasetAlgorithm.class, UIUtils.getIconURLFromResources("actions/group-new.png"));

        registerNodeType("omero-download-image", DownloadOMEROImageAlgorithm.class, UIUtils.getIconURLFromResources("apps/omero-monochrome.png"));
        registerNodeType("omero-download-table", DownloadOMEROTableAlgorithm.class, UIUtils.getIconURLFromResources("apps/omero-monochrome.png"));

        registerNodeType("omero-upload-image", UploadOMEROImageAlgorithm.class, UIUtils.getIconURLFromResources("apps/omero-monochrome.png"));
        registerNodeType("omero-upload-table", UploadOMEROTableAlgorithm.class, UIUtils.getIconURLFromResources("apps/omero-monochrome.png"));
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(RESOURCES.getIcon32FromResources("omero.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:omero";
    }

}
