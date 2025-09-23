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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentReference;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
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
import org.hkijena.jipipe.plugins.omero.nodes.upload.UploadOMEROImageToDatasetAlgorithm;
import org.hkijena.jipipe.plugins.omero.nodes.upload.UploadOMEROTableAlgorithm;
import org.hkijena.jipipe.plugins.omero.viewers.OMERODataViewer;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ReflectionUtils;
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

    public static JIPipeEnvironmentReference<OMEROCredentialsEnvironment> getEnvironment(JIPipeProject project, OptionalOMEROCredentialsEnvironment nodeEnvironment, JIPipeGraphNode node) {
        var selector = JIPipeEnvironmentReference.defaultOptions(OMEROCredentialsEnvironment.class)
                .application(OMEROPluginApplicationSettings.getInstance().getDefaultCredentials());
        if (nodeEnvironment != null) {
            selector.node(nodeEnvironment, node);
        }
        if (project != null) {
            selector.project(project.getSettingsSheet(OMEROPluginProjectSettings.class).getProjectDefaultEnvironment(), project);
        }
        return selector.select();
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
            new UnspecifiedValidationReportContext().error().title("No working OMERO detected!").explanation("The JIPipe OMERO extension requires a working OMERO installation. Preliminary checks determined that there is none.").solution("Please install OMERO from the official OMERO website or install the appropriate OMERO plugins via the ImageJ updater.").details("At least one of the following classes were not found: " + String.join(", ", classes)).report(report);
        }

        return result;
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        // Shared affiliations
        final JIPipeOrganizationMetadata dundee = new JIPipeOrganizationMetadata.Builder()
                .name("Wellcome Trust Centre for Gene Regulation and Expression, College of Life Sciences, University of Dundee, Dundee, Scotland, UK")
                .ror("https://ror.org/03h2bxq36")
                .website("https://www.dundee.ac.uk")
                .build();

        final JIPipeOrganizationMetadata glencoe = new JIPipeOrganizationMetadata.Builder()
                .name("Glencoe Software, Inc., Seattle, Washington, USA")
                .website("https://glencoesoftware.com")
                .build();

        final JIPipeOrganizationMetadata jic = new JIPipeOrganizationMetadata.Builder()
                .name("John Innes Centre Norwich Research Park, Norwich, UK")
                .ror("https://ror.org/055zmrh94")
                .website("https://jic.ac.uk")
                .build();

        final JIPipeOrganizationMetadata emblEbi = new JIPipeOrganizationMetadata.Builder()
                .name("European Molecular Biology Laboratory– European Bioinformatics Institute, Wellcome Trust Genome Campus, Hinxton, Cambridge, UK")
                .ror("https://ror.org/03mstc592")
                .website("https://www.ebi.ac.uk")
                .build();

        final JIPipeOrganizationMetadata crs4 = new JIPipeOrganizationMetadata.Builder()
                .name("CRS4, Pula, Italy")
                .ror("https://ror.org/03jdxdk20")
                .website("https://www.crs4.it")
                .build();

        // Author list
        return new JIPipeAuthorMetadata.List(
                new JIPipeAuthorMetadata.Builder().firstName("Chris").lastName("Allan").affiliations(List.of(dundee, glencoe)).firstAuthor(true).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Jean-Marie").lastName("Burel").affiliations(List.of(dundee, glencoe)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Josh").lastName("Moore").affiliations(List.of(glencoe)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Colin").lastName("Blackburn").affiliations(List.of(dundee, glencoe)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Melissa").lastName("Linkert").affiliations(List.of(glencoe)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Scott").lastName("Loynton").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Donald").lastName("MacDonald").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("William J.").lastName("Moore").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Carlos").lastName("Neves").affiliations(List.of(glencoe)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Andrew").lastName("Patterson").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Michael").lastName("Porter").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Aleksandra").lastName("Tarkowska").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Brian").lastName("Loranger").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Jerome").lastName("Avondo").affiliations(List.of(jic)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Ingvar").lastName("Lagerstedt").affiliations(List.of(emblEbi)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Luca").lastName("Lianas").affiliations(List.of(crs4)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Simone").lastName("Leo").affiliations(List.of(crs4)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Katherine").lastName("Hands").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Ron T.").lastName("Hay").affiliations(List.of(dundee)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Ardan").lastName("Patwardhan").affiliations(List.of(emblEbi)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Christoph").lastName("Best").affiliations(List.of(emblEbi)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Gerard J.").lastName("Kleywegt").affiliations(List.of(emblEbi)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Gianluigi").lastName("Zanetti").affiliations(List.of(crs4)).build(),
                new JIPipeAuthorMetadata.Builder().firstName("Jason R.").lastName("Swedlow").affiliations(List.of(dundee, glencoe)).build()
        );

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
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        OMEROPluginApplicationSettings omeroSettings = new OMEROPluginApplicationSettings();
        registerApplicationSettingsSheet(omeroSettings);
        registerEnvironment(OMEROCredentialsEnvironment.class,
                OMEROCredentialsEnvironment.List.class,
                omeroSettings,
                "omero-credentials",
                "OMERO Credentials",
                "Credentials for an OMERO server",
                RESOURCES.getIcon16("omero.png"));
        registerParameterType("optional-omero-credentials",
                OptionalOMEROCredentialsEnvironment.class,
                JIPipeParameterArchetype.OptionalValue, "Optimal OMERO credentials",
                "Optional OMERO credentials");
        registerProjectSettingsSheet(OMEROPluginProjectSettings.class);

        // Data types
        registerDatatype("omero-group-id", OMEROGroupReferenceData.class, RESOURCES.getIcon16URL("omero-group.png"));
        registerDatatype("omero-project-id", OMEROProjectReferenceData.class, RESOURCES.getIcon16URL("omero-project.png"));
        registerDatatype("omero-dataset-id", OMERODatasetReferenceData.class, RESOURCES.getIcon16URL("omero-dataset.png"));
        registerDatatype("omero-image-id", OMEROImageReferenceData.class, RESOURCES.getIcon16URL("omero-image.png"));
        registerDatatype("omero-annotation-id", OMEROAnnotationReferenceData.class, RESOURCES.getIcon16URL("omero-annotation.png"));
        registerDatatype("omero-screen-id", OMEROScreenReferenceData.class, RESOURCES.getIcon16URL("omero-screen.png"));
        registerDatatype("omero-plate-id", OMEROPlateReferenceData.class, RESOURCES.getIcon16URL("omero-plate.png"));
        registerDatatype("omero-well-id", OMEROWellReferenceData.class, RESOURCES.getIcon16URL("omero-well.png"));

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

        registerNodeType("omero-list-group-ids", OMEROListGroupsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/search.png"));
        registerNodeType("omero-list-project-ids", OMEROListProjectsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/search.png"));
        registerNodeType("omero-list-dataset-ids", OMEROListDatasetsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/search.png"));
        registerNodeType("omero-list-image-ids", OMEROListDatasetImagesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/search.png"));
        registerNodeType("omero-list-well-image-ids", OMEROListWellImagesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/search.png"));
        registerNodeType("omero-list-screen-ids", OMEROListScreensAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/search.png"));
        registerNodeType("omero-list-plate-ids", OMEROListPlatesAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/search.png"));
        registerNodeType("omero-list-well-ids", OMEROListWellsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/search.png"));

        registerNodeType("omero-annotate-dataset-from-remote", AnnotateOMERODatasetReferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("omero-annotate-image-from-remote", AnnotateOMEROImageReferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("omero-annotate-project-from-remote", AnnotateOMEROProjectReferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("omero-annotate-screen-from-remote", AnnotateOMEROScreenReferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("omero-annotate-plate-from-remote", AnnotateOMEROPlateReferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("omero-annotate-well-from-remote", AnnotateOMEROWellReferenceAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));

        registerNodeType("omero-create-dataset", OMEROCreateDatasetAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/group-new.png"));

        registerNodeType("omero-download-image", DownloadOMEROImageAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/omero-monochrome.png"));
        registerNodeType("omero-download-table", DownloadOMEROTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/omero-monochrome.png"));

        registerNodeType("omero-upload-image", UploadOMEROImageToDatasetAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/omero-monochrome.png"));
        registerNodeType("omero-upload-table", UploadOMEROTableAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/omero-monochrome.png"));
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(RESOURCES.getIcon32("omero.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:omero";
    }

}
