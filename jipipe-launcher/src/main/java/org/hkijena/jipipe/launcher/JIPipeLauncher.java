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

package org.hkijena.jipipe.launcher;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.launcher.commands.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JIPipeLauncher {
    /**
     * @param args ignored
     */
    public static void main(final String... args) {
        boolean keepRunning = false;
        try {
            List<String> argsList = new ArrayList<>(Arrays.asList(args));

            if (argsList.contains("help")) {
                HelpCommand.showHelp();
            } else if (argsList.contains("run")) {
                preprocessArgsListForSubCommand(argsList, "run");
                HeadlessRunCommand.doRunPipeline(argsList);
            } else if (argsList.contains("create-ro-crate")) {
                preprocessArgsListForSubCommand(argsList, "create-ro-crate");
                CreateROCrateCommand.doCreateROCrate(argsList);
            } else if (argsList.contains("render")) {
                preprocessArgsListForSubCommand(argsList, "render");
                RenderPipelineCommand.doRenderPipeline(argsList);
            }
            else if(argsList.contains("install-artifacts")) {
                preprocessArgsListForSubCommand(argsList, "install-artifacts");
                DeployArtifactsCommand.doInstallArtifacts(argsList);
            } else if (argsList.contains("gui")) {
                preprocessArgsListForSubCommand(argsList, "gui");
                keepRunning = true;
                GuiCommand.startGui(argsList);
            } else {
                keepRunning = true;
                GuiCommand.startGui(argsList);
            }
        } catch (Throwable t) {
            System.err.println(t);
            System.err.println(ExceptionUtils.getStackTrace(t));
        } finally {
            if(!keepRunning) {
                Runtime.getRuntime().halt(0);
            }
        }
    }

    private static void preprocessArgsListForSubCommand(List<String> argsList, String subCommand) {
        int runIndex = argsList.lastIndexOf(subCommand);
        while (runIndex > 0) {
            argsList.removeFirst();
            --runIndex;
        }
        argsList.removeFirst();
    }
}
