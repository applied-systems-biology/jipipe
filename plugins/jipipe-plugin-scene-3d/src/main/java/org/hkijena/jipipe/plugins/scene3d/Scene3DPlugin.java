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

package org.hkijena.jipipe.plugins.scene3d;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.plugins.scene3d.nodes.*;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Set;

@Plugin(type = JIPipeJavaPlugin.class)
public class Scene3DPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:scene-3d",
            JIPipe.getJIPipeVersion(),
            "3D scenes support");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(Scene3DPlugin.class, "org/hkijena/jipipe/plugins/scene3d");

    public Scene3DPlugin() {
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_3D);
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY);
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
    public boolean isBeta() {
        return true;
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
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
        registerNodeType("export-3d-scene-to-collada-v2", ExportScene3DToColladaAlgorithm2.class, UIUtils.getIconURLFromResources("actions/document-export.png"));
        registerNodeType("group-scene-3d", GroupSceneAlgorithm.class, UIUtils.getIconURLFromResources("actions/object-group.png"));
        registerNodeType("merge-scene-3d", MergeScenesAlgorithm.class, UIUtils.getIconURLFromResources("actions/rabbitvcs-merge.png"));

        registerNodeType("scene-3d-create-sphere", CreateSphereMeshAlgorithm.class);
        registerNodeType("scene-3d-create-line", CreateLineMeshAlgorithm.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:scene-3d";
    }

}
