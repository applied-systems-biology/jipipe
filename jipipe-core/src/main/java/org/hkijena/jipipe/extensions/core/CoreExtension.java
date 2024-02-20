package org.hkijena.jipipe.extensions.core;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.compat.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeEmptyData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeEmptyThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeGridThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeImageThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeTextThumbnailData;
import org.hkijena.jipipe.api.data.utils.JIPipeWeakDataReferenceData;
import org.hkijena.jipipe.api.grapheditortool.*;
import org.hkijena.jipipe.api.nodes.JIPipeTextAnnotationMatchingMethod;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.data.CopyContainingFolderDataImportOperation;
import org.hkijena.jipipe.extensions.core.data.DefaultDataDisplayOperation;
import org.hkijena.jipipe.extensions.core.data.OpenContainingFolderDataImportOperation;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The core extension
 */
@Plugin(type = JIPipeJavaExtension.class)
public class CoreExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:core", JIPipe.getJIPipeVersion(), "Core");

    @Override
    public String getName() {
        return "Core";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides core data types");
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerDatatype("jipipe:data",
                JIPipeData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
        registerDatatype("jipipe:empty-data",
                JIPipeEmptyData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
        registerDatatype("jipipe:data-table",
                JIPipeDataTable.class,
                ResourceUtils.getPluginResource("icons/data-types/data-table.png"));
        registerDatatype("jipipe:weak-reference",
                JIPipeWeakDataReferenceData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
        registerDatatype("jipipe:thumbnail-image",
                JIPipeImageThumbnailData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
        registerDatatype("jipipe:thumbnail-text",
                JIPipeTextThumbnailData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
        registerDatatype("jipipe:thumbnail-empty",
                JIPipeEmptyThumbnailData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"));
        registerDatatype("jipipe:thumbnail-grid",
                JIPipeGridThumbnailData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"));

        registerNodeTypeCategory(new InternalNodeTypeCategory());
        registerNodeTypeCategory(new DataSourceNodeTypeCategory());
        registerNodeTypeCategory(new FileSystemNodeTypeCategory());
        registerNodeTypeCategory(new MiscellaneousNodeTypeCategory());
        registerNodeTypeCategory(new ImagesNodeTypeCategory());
        registerNodeTypeCategory(new TableNodeTypeCategory());
        registerNodeTypeCategory(new RoiNodeTypeCategory());
        registerNodeTypeCategory(new AnnotationsNodeTypeCategory());
        registerNodeTypeCategory(new ExportNodeTypeCategory());
        registerNodeTypeCategory(new ImageJNodeTypeCategory());

        // Global data importers
        registerDatatypeImportOperation("", new CopyContainingFolderDataImportOperation());
        registerDatatypeImportOperation("", new OpenContainingFolderDataImportOperation());
        registerDatatypeDisplayOperation("", new DefaultDataDisplayOperation());

        // Default ImageJ data adapters
        registerImageJDataImporter(DataTableImageJDataImporter.ID, new DataTableImageJDataImporter(), DefaultImageJDataImporterUI.class);
        registerImageJDataExporter(DataTableImageJDataImporter.ID, new DataTableImageJDataExporter(), DefaultImageJDataExporterUI.class);
        registerImageJDataImporter("none", new EmptyImageJDataImporter(), EmptyImageJDataImporterUI.class);
        registerImageJDataExporter("none", new EmptyImageJDataExporter(), EmptyImageJDataExporterUI.class);

        // Global parameters
        registerEnumParameterType("jipipe:annotation-matching-method",
                JIPipeTextAnnotationMatchingMethod.class,
                "Annotation matching method",
                "Determines how annotations are matched with each other");
        registerEnumParameterType("jipipe:annotation-merge-strategy",
                JIPipeTextAnnotationMergeMode.class,
                "Annotation merge strategy",
                "Determines how annotations are merged.");
        registerEnumParameterType("jipipe:data-annotation-merge-strategy",
                JIPipeDataAnnotationMergeMode.class,
                "Data annotation merge strategy",
                "Determines how data annotations are merged.");
        registerEnumParameterType("theme",
                JIPipeUITheme.class,
                "Theme",
                "A theme for the JIPipe GUI");

        registerProjectTemplatesFromResources(JIPipe.RESOURCES, "templates");

        // Graph editors
        registerGraphEditorTool(JIPipeDefaultGraphEditorTool.class);
        registerGraphEditorTool(JIPipeConnectGraphEditorTool.class);
        registerGraphEditorTool(JIPipeMoveNodesGraphEditorTool.class);
        registerGraphEditorTool(JIPipeCropViewGraphEditorTool.class);
        registerGraphEditorTool(JIPipeRewireGraphEditorTool.class);
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/imagej.png"),
                UIUtils.getIcon32FromResources("apps/fiji.png"),
                UIUtils.getIcon32FromResources("apps/scijava.png"));
    }

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Rueden, C. T.; Schindelin, J. & Hiner, M. C. et al. (2017), \"ImageJ2: ImageJ for the next generation of scientific image data\", " +
                "BMC Bioinformatics 18:529");
        result.add("Schindelin, J.; Arganda-Carreras, I. & Frise, E. et al. (2012), \"Fiji: an open-source platform for biological-image analysis\", " +
                "Nature methods 9(7): 676-682, PMID 22743772, doi:10.1038/nmeth.2019");
        result.add("Schneider, C. A.; Rasband, W. S. & Eliceiri, K. W. (2012), \"NIH Image to ImageJ: 25 years of image analysis\", " +
                "Nature methods 9(7): 671-675");
        result.add("Rueden, C., Schindelin, J., Hiner, M. & Eliceiri, K. (2016). SciJava Common [Software]. https://scijava.org/. ");
        result.add("Papirus Icon Theme: https://github.com/PapirusDevelopmentTeam/papirus-icon-theme (Licensed under GPL-3)");
        result.add("Breeze Icons: https://github.com/KDE/breeze-icons (Licensed under LGPL-2.1)");
        result.add("Font Awesome Free 5.12.1 (desktop): https://fontawesome.com/ (Licensed under Font Awesome Free License)");
        result.add("Font Awesome Free 6.5.1 (desktop): https://fontawesome.com/ (Licensed under Font Awesome Free License)");
        result.add("Fluent icon theme: https://github.com/vinceliuice/Fluent-icon-theme (Licensed under GPL-3)");
        return result;
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:core";
    }

    @Override
    public boolean isCoreExtension() {
        return true;
    }

}
