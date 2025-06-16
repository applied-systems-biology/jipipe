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

package org.hkijena.jipipe.plugins.cef;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = JIPipeJavaPlugin.class)
public class CefPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:cef", JIPipe.getJIPipeVersion(), "CEF Support");

    private static CefApp app;

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "CEF Support";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides support for Chromium Embedded Framework");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Initializing CEF ... (this may take some time)");

        CefAppBuilder builder = new CefAppBuilder();
        builder.addJcefArgs("--no-sandbox");
//        builder.addJcefArgs("--disable-gpu");

        try {
            app = builder.build();
        } catch (IOException | UnsupportedPlatformException | InterruptedException | CefInitializationException e) {
            progressInfo.log(e);
            progressInfo.log("Error: Unable to initialize CEF");
        }
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:cef";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    public static CefApp getApp() {
        return app;
    }
}
