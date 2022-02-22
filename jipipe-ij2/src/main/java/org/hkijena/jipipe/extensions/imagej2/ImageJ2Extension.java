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

import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.imagej2.compat.ImageJ2DataSetDataImageJAdapter;
import org.hkijena.jipipe.extensions.imagej2.converters.ImageJ1ToImageJ2Converter;
import org.hkijena.jipipe.extensions.imagej2.converters.ImageJ2ToImageJ1Converter;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImagePlusDataImporterUI;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

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
        registerDatatype("ij2-dataset", ImageJ2DatasetData.class, UIUtils.getIconURLFromResources("apps/imglib2.png"));
        registerDatatypeConversion(new ImageJ1ToImageJ2Converter());
        registerDatatypeConversion(new ImageJ2ToImageJ1Converter());
        registerImageJDataAdapter(new ImageJ2DataSetDataImageJAdapter(), ImagePlusDataImporterUI.class);

        ModuleService moduleService = context.getService(ModuleService.class);
        for (ModuleInfo module : moduleService.getModules()) {
            try {
                ImageJ2ModuleNodeInfo nodeInfo = new ImageJ2ModuleNodeInfo(context, module, progressInfo);
                if(nodeInfo.getInputSlots().isEmpty() && nodeInfo.getOutputSlots().isEmpty()) {
                    progressInfo.log(module.getTitle() + " has no data slots. Skipping.");
                    continue;
                }
                registerNodeType(nodeInfo);
            }
            catch (Exception e) {
                progressInfo.log("Unable to register " + module.getTitle() + " @ " + module.getDelegateClassName() );
                progressInfo.log(e.toString());
            }
        }
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Collections.singletonList(UIUtils.getIcon32FromResources("apps/imglib2.png"));
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



