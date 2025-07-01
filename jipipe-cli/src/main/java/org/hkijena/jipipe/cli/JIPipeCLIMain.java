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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JIPipeCLIMain {
    /**
     * @param args ignored
     */
    public static void main(final String... args) {
        try {
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
                    argsList.removeFirst();
                    --runIndex;
                }
                // remove run
                argsList.removeFirst();
                JIPipeCLIPipelineRun.doRunPipeline(argsList);
            } else if (argsList.contains("render")) {
                int renderIndex = argsList.lastIndexOf("render");
                while (renderIndex > 0) {
                    argsList.removeFirst();
                    --renderIndex;
                }
                // remove run
                argsList.removeFirst();
                JIPipeCLIPipelineRender.doRenderPipeline(argsList);
            } else {
                JIPipeCLIHelp.showHelp();
            }
        }
        catch (Throwable t) {
            System.err.println(t);
            System.err.println(ExceptionUtils.getStackTrace(t));
        }
        finally {
            Runtime.getRuntime().halt(0);
        }
    }
}
