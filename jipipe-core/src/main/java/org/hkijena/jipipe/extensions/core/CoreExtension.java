package org.hkijena.jipipe.extensions.core;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.categories.AnalysisNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.AnnotationNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ConverterNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ProcessorNodeTypeCategory;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

/**
 * The core extension
 */
@Plugin(type = JIPipeJavaExtension.class)
public class CoreExtension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Core";
    }

    @Override
    public String getDescription() {
        return "Provides core data types";
    }

    @Override
    public void register() {
        registerDatatype("jipipe:data",
                JIPipeData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"),
                null,
                null);
        registerNodeTypeCategory(new InternalNodeTypeCategory());
        registerNodeTypeCategory(new ConverterNodeTypeCategory());
        registerNodeTypeCategory(new DataSourceNodeTypeCategory());
        registerNodeTypeCategory(new FileSystemNodeTypeCategory());
        registerNodeTypeCategory(new MiscellaneousNodeTypeCategory());
        registerNodeTypeCategory(new ProcessorNodeTypeCategory());
        registerNodeTypeCategory(new AnalysisNodeTypeCategory());
        registerNodeTypeCategory(new AnnotationNodeTypeCategory());
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:core";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
