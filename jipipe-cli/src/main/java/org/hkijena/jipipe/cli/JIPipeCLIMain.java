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

package org.hkijena.jipipe.cli;

import java.util.*;

public class JIPipeCLIMain {
    /**
     * @param args ignored
     */
    public static void main(final String... args) {
        if (args.length == 0) {
            JIPipeCLIHelp.showHelp();
            return;
        }

        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        if (argsList.contains("help")) {
            JIPipeCLIHelp.showHelp();
            return;
        }
        if (argsList.contains("run")) {
            int runIndex = argsList.lastIndexOf("run");
            while (runIndex > 0) {
                argsList.remove(0);
                --runIndex;
            }
            // remove run
            argsList.remove(0);
            JIPipeCLIPipelineRun.doRunPipeline(argsList);
        }
        else if (argsList.contains("render")) {
            int renderIndex = argsList.lastIndexOf("render");
            while (renderIndex > 0) {
                argsList.remove(0);
                --renderIndex;
            }
            // remove run
            argsList.remove(0);
            JIPipeCLIPipelineRender.doRenderPipeline(argsList);
        }
        else {
            JIPipeCLIHelp.showHelp();
        }
    }

}
