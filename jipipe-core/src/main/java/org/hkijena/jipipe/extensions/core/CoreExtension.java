package org.hkijena.jipipe.extensions.core;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeEmptyData;
import org.hkijena.jipipe.api.data.JIPipeMergedDataAnnotationsData;
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
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * The core extension
 */
@Plugin(type = JIPipeJavaExtension.class)
public class CoreExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Core";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides core data types");
    }

    @Override
    public void register() {
        registerDatatype("jipipe:data",
                JIPipeData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"),
                null,
                null);
        registerDatatype("jipipe:empty-data",
                JIPipeEmptyData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"),
                null,
                null);
        registerDatatype("jipipe:merged-data-annotations",
                JIPipeMergedDataAnnotationsData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-table.png"),
                null,
                null);
        registerNodeTypeCategory(new InternalNodeTypeCategory());
        registerNodeTypeCategory(new DataSourceNodeTypeCategory());
        registerNodeTypeCategory(new FileSystemNodeTypeCategory());
        registerNodeTypeCategory(new MiscellaneousNodeTypeCategory());
        registerNodeTypeCategory(new ImagesNodeTypeCategory());
        registerNodeTypeCategory(new TableNodeTypeCategory());
        registerNodeTypeCategory(new RoiNodeTypeCategory());
        registerNodeTypeCategory(new AnnotationsNodeTypeCategory());
        registerNodeTypeCategory(new ExportNodeTypeCategory());

        // Global data importers
        registerDatatypeImportOperation("", new CopyContainingFolderDataImportOperation());
        registerDatatypeImportOperation("", new OpenContainingFolderDataImportOperation());
        registerDatatypeDisplayOperation("", new DefaultDataDisplayOperation());

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
        return result;
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:core";
    }

    @Override
    public String getDependencyVersion() {
        return "1.63.0";
    }
}
