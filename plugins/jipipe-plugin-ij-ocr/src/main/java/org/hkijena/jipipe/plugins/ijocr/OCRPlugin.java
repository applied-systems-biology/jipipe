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

package org.hkijena.jipipe.plugins.ijocr;

import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.ijocr.environments.OptionalTesseractOCREnvironment;
import org.hkijena.jipipe.plugins.ijocr.environments.TesseractOCREnvironment;
import org.hkijena.jipipe.plugins.ijocr.nodes.TesseractOCRAlgorithm;
import org.hkijena.jipipe.plugins.ijocr.settings.OCRPluginProjectSettings;
import org.hkijena.jipipe.plugins.ijocr.settings.TesseractOCRApplicationSettings;
import org.hkijena.jipipe.plugins.ijocr.utils.TesseractOCREngineMode;
import org.hkijena.jipipe.plugins.ijocr.utils.TesseractPageSegmentationMethod;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.strings.StringsPlugin;
import org.hkijena.jipipe.plugins.tables.TablesPlugin;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Set;

/**
 * Extension that adds filaments supports
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class OCRPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:ij-ocr",
            JIPipe.getJIPipeVersion(),
            "Filaments");

    public OCRPlugin() {
    }

    public static TesseractOCREnvironment getTesseractOCREnvironment(JIPipeProject project, OptionalTesseractOCREnvironment nodeEnvironment) {
        if (nodeEnvironment != null && nodeEnvironment.isEnabled()) {
            return nodeEnvironment.getContent();
        }
        if (project != null && project.getSettingsSheet(OCRPluginProjectSettings.class).getProjectDefaultEnvironment().isEnabled()) {
            return project.getSettingsSheet(OCRPluginProjectSettings.class).getProjectDefaultEnvironment().getContent();
        }
        return TesseractOCRApplicationSettings.getInstance().getReadOnlyDefaultEnvironment();
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY,
                TablesPlugin.AS_DEPENDENCY,
                StringsPlugin.AS_DEPENDENCY,
                ImageJDataTypesPlugin.AS_DEPENDENCY);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public PluginCategoriesEnumParameter.List getCategories() {
        return new PluginCategoriesEnumParameter.List(PluginCategoriesEnumParameter.CATEGORY_FEATURE_EXTRACTION, PluginCategoriesEnumParameter.CATEGORY_OBJECT_DETECTION);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ij-ocr";
    }

    @Override
    public String getName() {
        return "Optical Character Recognition (OCR)";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Algorithm for extracting text from images.");
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {

        TesseractOCRApplicationSettings tesseractOCRApplicationSettings = new TesseractOCRApplicationSettings();
        registerEnvironment(TesseractOCREnvironment.class,
                TesseractOCREnvironment.List.class,
                tesseractOCRApplicationSettings,
                "tesseract-ocr-environment",
                "Tesseract OCR Environment",
                "Installation of Tesseract OCR",
                UIUtils.getIconFromResources("actions/text_outer_style.png"));
        registerParameterType("optional-tesseract-ocr-environment",
                OptionalTesseractOCREnvironment.class,
                "Optional Tesseract OCR Environment",
                "Installation of Tesseract OCR");

        registerApplicationSettingsSheet(tesseractOCRApplicationSettings);
        registerProjectSettingsSheet(OCRPluginProjectSettings.class);

        registerEnumParameterType("tesseract-ocr-psm", TesseractPageSegmentationMethod.class, "Tesseract OCR Page Segmentation Method", "Methods for page segmentation");
        registerEnumParameterType("tesseract-ocr-oem", TesseractOCREngineMode.class, "Tesseract OCR Engine Mode", "OCR engine modes");
        registerNodeType("tesseract-ocr", TesseractOCRAlgorithm.class, UIUtils.getIconURLFromResources("actions/text_outer_style.png"));
    }


}
