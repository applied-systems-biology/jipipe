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

package org.hkijena.jipipe.plugins.python;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.python.algorithms.jython.RunIteratingJythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.jython.RunJythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.jython.RunMergingJythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.jython.RunSimpleIteratingJythonScriptAlgorithm;
import org.hkijena.jipipe.plugins.python.algorithms.python.*;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Python nodes
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class PythonPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:python",
            JIPipe.getJIPipeVersion(),
            "Python integration");

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(PythonPlugin.class, "org/hkijena/jipipe/plugins/python");

    public PythonPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public String getName() {
        return "Python integration (nodes)";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides algorithms and data types that allow Python scripting");
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {

        registerNodeType("python-script", RunJythonScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("python-script-iterating-simple", RunSimpleIteratingJythonScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("python-script-iterating", RunIteratingJythonScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("python-script-merging", RunMergingJythonScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));

        registerNodeType("cpython-script", RunPythonScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("cpython-script-iterating", RunIteratingPythonScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("cpython-script-merging", RunMergingPythonScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));

        registerNodeType("define-python-script", DefinePythonScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("import-python-script", ImportPythonScriptAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));

        registerNodeExamplesFromResources(RESOURCES, "examples");
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:python";
    }

}
