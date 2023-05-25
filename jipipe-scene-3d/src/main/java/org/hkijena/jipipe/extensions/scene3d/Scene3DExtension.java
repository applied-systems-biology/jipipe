package org.hkijena.jipipe.extensions.clij2;

import net.imagej.updater.UpdateSite;
import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.*;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.DefaultImageJDataExporterUI;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.CoreExtension;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.nodes.ExportScene3DToColladaAlgorithm;
import org.hkijena.jipipe.extensions.scene3d.nodes.MaskTo3DMeshAlgorithm;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Plugin(type = JIPipeJavaExtension.class)
public class Scene3DExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:scene-3d",
            JIPipe.getJIPipeVersion(),
            "3D scenes support");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(Scene3DExtension.class, "org/hkijena/jipipe/extensions/scene3d");

    public Scene3DExtension() {
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_3D);
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CoreExtension.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "3D Scenes";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Allows to create and export 3D scenes.");
    }
    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerDatatype("scene-3d", Scene3DData.class, RESOURCES.getIcon16URLFromResources("data-type-scene3d.png"));

        registerNodeType("mask-to-3d-mesh", MaskTo3DMeshAlgorithm.class, UIUtils.getIconURLFromResources("actions/shape-cuboid.png"));
        registerNodeType("export-3d-scene-to-collada", ExportScene3DToColladaAlgorithm.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:scene-3d";
    }

}
