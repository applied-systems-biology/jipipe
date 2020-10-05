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

package org.hkijena.jipipe.extensions.python;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Python nodes
 */
@Plugin(type = JIPipeJavaExtension.class)
public class PythonExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public String getName() {
        return "Python integration";
    }

    @Override
    public String getDescription() {
        return "Provides algorithms and data types that allow Python scripting";
    }

    @Override
    public void register() {
        registerNodeType("python-script", PythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-multi-parameter", ParameterSlotPythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-iterating-simple", SimpleIteratingPythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-iterating", IteratingPythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("python-script-merging", MergingPythonScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/python.png"));
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/python.png"));
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:python";
    }

    @Override
    public String getDependencyVersion() {
        return "2020.10";
    }
}
