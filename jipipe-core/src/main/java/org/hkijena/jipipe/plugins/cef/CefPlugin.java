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
import org.hkijena.jipipe.utils.PathUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(type = JIPipeJavaPlugin.class)
public class CefPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:cef", JIPipe.getJIPipeVersion(), "CEF Support");

    private static CefApp app;

    public static CefApp getApp() {
        return app;
    }

    public static boolean hasCef() {
        return app != null;
    }

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
        Path bundleDir = PathUtils.getImageJDir().resolve("jcef-bundle");
        try {
            progressInfo.log("JCEF bundle will be rerouted to " + bundleDir.toAbsolutePath());
            Files.createDirectories(bundleDir);
        } catch (Throwable ignored) {
            progressInfo.log("JCEF bundle directory not writable. Redirecting to JIPipe user directory.");
            bundleDir = PathUtils.resolveAndMakeSubDirectory(PathUtils.getJIPipeUserDir(), "jcef-bundle");
        }
        builder.setInstallDir(bundleDir.toFile());
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
}
