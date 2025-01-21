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

package org.hkijena.jipipe.plugins.napari;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryInstallArtifactRun;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.*;

@Plugin(type = JIPipeJavaPlugin.class)
public class NapariPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:napari",
            JIPipe.getJIPipeVersion(),
            "Napari integration");

    public static PythonEnvironment getEnvironment() {
        return NapariPluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment();
    }

    public static void launchNapari(JIPipeDesktopWorkbench workbench, List<String> arguments, boolean interactive) {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        progressInfo.setLogToStdOut(true);
        launchNapari(workbench, arguments, progressInfo, interactive);
    }

    public static void launchNapari(JIPipeDesktopWorkbench workbench, List<String> arguments, JIPipeProgressInfo progressInfo, boolean interactive) {
        PythonEnvironment environment = getEnvironment();
        if (!environment.generateValidityReport(new UnspecifiedValidationReportContext()).isValid()) {
            if (interactive) {
                JOptionPane.showMessageDialog(workbench.getWindow(),
                        "Napari is currently not correctly installed. Please check the project/application settings and ensure that Napari is setup correctly.",
                        "Launch Napari",
                        JOptionPane.ERROR_MESSAGE);
                return;
            } else {
                throw new RuntimeException("Napari is currently not correctly installed. Please check the project/application settings and ensure that Napari is setup correctly.");
            }
        }
        if (environment.isLoadFromArtifact()) {
            JIPipeArtifact artifact = JIPipe.getArtifacts().searchClosestCompatibleArtifactFromQuery(environment.getArtifactQuery().getQuery());
            if (artifact instanceof JIPipeLocalArtifact) {
                environment.applyConfigurationFromArtifact((JIPipeLocalArtifact) artifact, new JIPipeProgressInfo());
            } else if (artifact instanceof JIPipeRemoteArtifact) {
                if (interactive) {
                    if (JOptionPane.showConfirmDialog(workbench.getWindow(), "The Napari version " + artifact.getVersion() + " is currently not downloaded. " +
                            "Download it now?", "Run Napari", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        JIPipeArtifactRepositoryInstallArtifactRun run = new JIPipeArtifactRepositoryInstallArtifactRun((JIPipeRemoteArtifact) artifact);
                        JIPipeDesktopRunExecuteUI.runInDialog(workbench, workbench.getWindow(), run);
                        artifact = JIPipe.getArtifacts().queryCachedArtifact(artifact.getFullId());
                        if (artifact instanceof JIPipeLocalArtifact) {
                            environment.applyConfigurationFromArtifact((JIPipeLocalArtifact) artifact, new JIPipeProgressInfo());
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    progressInfo.log("Napari is not installed. Downloading now.");
                    JIPipeArtifactRepositoryInstallArtifactRun run = new JIPipeArtifactRepositoryInstallArtifactRun((JIPipeRemoteArtifact) artifact);
                    run.setProgressInfo(progressInfo.resolve("Napari download"));
                    run.run();
                    if (progressInfo.isCancelled()) {
                        return;
                    }
                    artifact = JIPipe.getArtifacts().queryCachedArtifact(artifact.getFullId());
                    if (artifact instanceof JIPipeLocalArtifact) {
                        environment.applyConfigurationFromArtifact((JIPipeLocalArtifact) artifact, new JIPipeProgressInfo());
                    } else {
                        throw new RuntimeException("Artifact not found!");
                    }
                }
            }
        }
        workbench.sendStatusBarText("Launching Napari ...");
        runNapari(environment, arguments, true, progressInfo);
    }

    /**
     * Runs Napari
     *
     * @param environment  the environment. can be null (then the {@link NapariPluginApplicationSettings} environment is taken)
     * @param parameters   the cli parameters
     * @param detached     if the process is launched detached
     * @param progressInfo the progress info
     */
    public static void runNapari(PythonEnvironment environment, List<String> parameters, boolean detached, JIPipeProgressInfo progressInfo) {

        // Environment variables
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("LANG", "en_US.UTF-8");
        environmentVariables.put("LC_ALL", "en_US.UTF-8");
        environmentVariables.put("LC_CTYPE", "en_US.UTF-8");

        List<String> finalParameters = new ArrayList<>();
        finalParameters.add("-m");
        finalParameters.add("napari");
        finalParameters.addAll(parameters);

        PythonUtils.runPython(finalParameters.toArray(new String[0]), environment, Collections.emptyList(), environmentVariables, false, detached, progressInfo);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList("Sofroniew, N., Lambert, T., Bokota, G., Nunez-Iglesias, J., Sobolewski, P., Sweet, A., Gaifas, L., Evans, K., Burt, A., Doncila Pop, D., Yamauchi, K., Weber Mendonça, M., Buckley, G., Vierdag, W.-M., Royer, L., Can Solak, A., Harrington, K. I. S., Ahlers, J., Althviz Moré, D., … Zhao, R. (2025). napari: a multi-dimensional image viewer for Python (v0.5.6rc0). Zenodo. https://doi.org/10.5281/zenodo.14680029");
    }

    @Override
    public String getName() {
        return "Napari integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Basic Napari integration into JIPipe");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerApplicationSettingsSheet(new NapariPluginApplicationSettings());
        registerMenuExtension(RunNapariDesktopMenuExtension.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:napari";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }
}
