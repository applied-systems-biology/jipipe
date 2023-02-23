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

package org.hkijena.jipipe.extensions.omero;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.omero.algorithms.DownloadOMEROImageAlgorithm;
import org.hkijena.jipipe.extensions.omero.algorithms.DownloadOMEROTableAlgorithm;
import org.hkijena.jipipe.extensions.omero.algorithms.UploadOMEROImageAlgorithm;
import org.hkijena.jipipe.extensions.omero.algorithms.UploadOMEROTableAlgorithm;
import org.hkijena.jipipe.extensions.omero.datasources.*;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROGroupReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
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
@Plugin(type = JIPipeJavaExtension.class)
public class OMEROExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:omero",
            JIPipe.getJIPipeVersion(),
            "OMERO Integration");

    public OMEROExtension() {
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_IMPORT_EXPORT, PluginCategoriesEnumParameter.CATEGORY_SCIJAVA, PluginCategoriesEnumParameter.CATEGORY_OME);
    }

    @Override
    public ImageParameter getThumbnail() {
        return new ImageParameter(ResourceUtils.getPluginResource("thumbnails/omero.png"));
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY, ImageJDataTypesExtension.AS_DEPENDENCY);
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
        registerSettingsSheet(OMEROSettings.ID,
                "OMERO",
                "Settings for the OMERO integration (e.g., default credentials)",
                UIUtils.getIconFromResources("apps/omero.png"),
                "Extensions",
                null,
                new OMEROSettings());
        registerDatatype("omero-group-id", OMEROGroupReferenceData.class, UIUtils.getIconURLFromResources("apps/omero.png"));
        registerDatatype("omero-project-id", OMEROProjectReferenceData.class, UIUtils.getIconURLFromResources("apps/omero.png"));
        registerDatatype("omero-dataset-id", OMERODatasetReferenceData.class, UIUtils.getIconURLFromResources("apps/omero.png"));
        registerDatatype("omero-image-id", OMEROImageReferenceData.class, UIUtils.getIconURLFromResources("apps/omero.png"));

        registerNodeType("omero-image-id-definition", OMEROImageReferenceDataSource.class);
        registerNodeType("omero-dataset-id-definition", OMERODatasetReferenceDataSource.class);
        registerNodeType("omero-project-id-definition", OMEROProjectReferenceDataSource.class);
        registerNodeType("omero-group-id-definition", OMEROGroupReferenceDataSource.class);
        registerNodeType("omero-find-group-id", OMEROFindGroupAlgorithm.class);
        registerNodeType("omero-find-project-id", OMEROFindProjectAlgorithm.class);
        registerNodeType("omero-find-dataset-id", OMEROFindDatasetAlgorithm.class);
        registerNodeType("omero-find-image-id", OMEROFindImageAlgorithm.class);
        registerNodeType("omero-download-image", DownloadOMEROImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/cloud-download.png"));
        registerNodeType("omero-upload-image", UploadOMEROImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/cloud-upload.png"));
        registerNodeType("omero-download-table", DownloadOMEROTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/cloud-download.png"));
        registerNodeType("omero-upload-table", UploadOMEROTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/cloud-upload.png"));
    }

//    @Override
//    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
//        return Collections.singletonList(
//            new JIPipeImageJUpdateSiteDependency(new UpdateSite("OMERO 5.4", "https://sites.imagej.net/OMERO-5.4/", "", "", "", "", 0))
//        );
//    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/omero.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:omero";
    }

}
