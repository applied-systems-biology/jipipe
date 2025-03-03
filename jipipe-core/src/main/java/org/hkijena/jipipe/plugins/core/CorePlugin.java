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

package org.hkijena.jipipe.plugins.core;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeStandardMetadata;
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
import org.hkijena.jipipe.api.project.JIPipeProjectInfoParameters;
import org.hkijena.jipipe.api.run.JIPipeGraphRunPartitionInheritedBoolean;
import org.hkijena.jipipe.desktop.app.project.JIPipeDesktopJIPipeProjectTabMetadata;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.data.CopyContainingFolderDataImportOperation;
import org.hkijena.jipipe.plugins.core.data.DefaultDataDisplayOperation;
import org.hkijena.jipipe.plugins.core.data.OpenContainingFolderDataImportOperation;
import org.hkijena.jipipe.plugins.core.viewers.JIPipeDataTableDataViewer;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
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
@Plugin(type = JIPipeJavaPlugin.class)
public class CorePlugin extends JIPipePrepackagedDefaultJavaPlugin {

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
    public StringList getDependencyProvides() {
        return new StringList();
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

        registerDefaultDataTypeViewer(JIPipeDataTable.class, JIPipeDataTableDataViewer.class);

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
                JIPipeDesktopUITheme.class,
                "Theme",
                "A theme for the JIPipe GUI");
        registerEnumParameterType("jipipe:graph-run-partition-inherited-boolean",
                JIPipeGraphRunPartitionInheritedBoolean.class,
                "Graph run partition boolean (inheritable)",
                "A boolean where the value can be inherited from the partition");

        registerProjectTemplatesFromResources(JIPipe.RESOURCES, "templates");

        // Graph editors
        registerGraphEditorTool(JIPipeDefaultGraphEditorTool.class);
        registerGraphEditorTool(JIPipeConnectGraphEditorTool.class);
        registerGraphEditorTool(JIPipeMoveNodesGraphEditorTool.class);
        registerGraphEditorTool(JIPipeCropViewGraphEditorTool.class);
        registerGraphEditorTool(JIPipeRewireGraphEditorTool.class);

        // Metadata objects
        registerMetadataObjectType(JIPipeStandardMetadata.class, "jipipe:standard-metadata");
        registerMetadataObjectType(JIPipeProjectInfoParameters.class,
                "jipipe:project-info-parameters",
                "org.hkijena.jipipe.ui.settings.JIPipeProjectInfoParameters",
                "org.hkijena.jipipe.api.project.JIPipeProjectInfoParameters");
        registerMetadataObjectType(JIPipeDesktopJIPipeProjectTabMetadata.class,
                "jipipe:desktop:project-tabs",
                "org.hkijena.jipipe.ui.project.JIPipeProjectTabMetadata",
                "org.hkijena.jipipe.desktop.app.project.JIPipeDesktopJIPipeProjectTabMetadata");

        // Common file types
        registerFileChooserKnownFileType("JIPipe project file", "apps/jipipe.png", ".jip");

        registerFileChooserKnownFileType("JSON file", "mimetypes/application-json.png", ".json", ".json5");
        registerFileChooserKnownFileType("XML file", "mimetypes/text-xml.png", ".xml");
        registerFileChooserKnownFileType("Text file", "mimetypes/text.png", ".txt");

        registerFileChooserKnownFileType("Markdown file", "mimetypes/text-markdown.png", ".md");
        registerFileChooserKnownFileType("HTML file", "mimetypes/text-html.png", ".html", ".htm");

        registerFileChooserKnownFileType("CSV table", "mimetypes/text-csv.png", ".csv", ".tsv");
        registerFileChooserKnownFileType("Excel table", "mimetypes/excel.png", ".xlsx", ".xls");

        registerFileChooserKnownFileType("ZIP archive", "mimetypes/application-zip.png", ".zip");
        registerFileChooserKnownFileType("RAR archive", "mimetypes/application-zip.png", ".rar");
        registerFileChooserKnownFileType("TAR archive", "mimetypes/tar.png", ".tar", ".tar.gz", ".tar.bz2", ".tar.xz");

        registerFileChooserKnownFileType("Java executable", "mimetypes/application-x-jar.png", ".jar");
        registerFileChooserKnownFileType("Windows executable", "mimetypes/x-content-win32-software.png", ".exe");
        registerFileChooserKnownFileType("Shell script", "mimetypes/shellscript.png", ".sh");
        registerFileChooserKnownFileType("Batch file", "mimetypes/shellscript.png", ".bat");
        registerFileChooserKnownFileType("Powershell script", "mimetypes/shellscript.png", ".ps1");

        registerFileChooserKnownFileType("Python script", "mimetypes/text-x-python.png", ".py");
        registerFileChooserKnownFileType("R script", "mimetypes/application-x-rdata.png", ".r");
        registerFileChooserKnownFileType("Java code file", "mimetypes/text-x-java.png", ".java");
        registerFileChooserKnownFileType("JavaScript script", "mimetypes/text-x-javascript.png", ".js");
        registerFileChooserKnownFileType("ImageJ macro", "apps/imagej.png", ".ijm");

        registerFileChooserKnownFileType("Image", "mimetypes/image-png.png", ".png", ".bmp", ".jpg", ".jpeg", ".gif");
        registerFileChooserKnownFileType("Vector image", "mimetypes/svg.png", ".svg", ".svgz", ".ai");
        registerFileChooserKnownFileType("TIFF image", "data-types/imgplus.png", ".tif", ".tiff");

        registerFileChooserKnownFileType("PDF file", "mimetypes/viewpdf.png", ".pdf");
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
    public boolean isCorePlugin() {
        return true;
    }

}
