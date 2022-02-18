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

package org.hkijena.jipipe.extensions.imagej2;

import net.imagej.ops.OpInfo;
import net.imagej.ops.OpService;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extension that adds ImageJ2 algorithms
 */
@Plugin(type = JIPipeJavaExtension.class, priority = Priority.LOW)
public class ImageJ2Extension extends JIPipePrepackagedDefaultJavaExtension {

    @Override
    public StringList getDependencyCitations() {
        StringList result = new StringList();
        result.add("Rueden, C. T.; Schindelin, J. & Hiner, M. C. et al. (2017), \"ImageJ2: ImageJ for the next generation of scientific image data\", " +
                "BMC Bioinformatics 18:529");
        return result;
    }

    @Override
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "ImageJ2 algorithms";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates ImageJ2 algorithms into JIPipe");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        OpService opService = context.getService(OpService.class);
        Map<String, List<OpInfo>> groupedByFullName = opService.infos().stream().collect(Collectors.groupingBy(info -> StringUtils.nullToEmpty(info.getName())));
        for (Map.Entry<String, List<OpInfo>> entry : groupedByFullName.entrySet()) {
            if(StringUtils.isNullOrEmpty(entry.getKey()))
                continue;
            progressInfo.log("IJ2-Ops: Detected " + entry.getValue().size() + " ops of type " + entry.getKey());
            for (OpInfo opInfo : entry.getValue()) {
                try {
                    ImageJ2OpNodeInfo nodeInfo = new ImageJ2OpNodeInfo(jiPipe, context, opInfo, entry.getValue().size() <= 1, progressInfo);
                    if(!nodeInfo.isConversionSuccessful()) {
                        progressInfo.log("Op " + opInfo.getName() + " could not be converted to JIPipe! Skipping.");
                        continue;
                    }
                    registerNodeType(nodeInfo, UIUtils.getIconURLFromResources("actions/op.png"));
                } catch (Exception | Error e) {
                    progressInfo.log("Unable to register op " + opInfo.getName());
                    progressInfo.log(e.toString());
                }
            }
        }
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:imagej2";
    }

    @Override
    public String getDependencyVersion() {
        return "1.64.0";
    }
}



