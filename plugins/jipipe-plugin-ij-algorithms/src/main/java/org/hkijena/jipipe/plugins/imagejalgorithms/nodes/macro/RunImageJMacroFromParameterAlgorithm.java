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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.macro;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.parameters.library.scripts.ImageJMacroParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.nio.file.Path;

/**
 * An algorithm that wraps around an ImageJ macro
 */
@SetJIPipeDocumentation(name = "Run ImageJ Macro (parameter)", description = "Runs a custom ImageJ macro. JIPipe will iterate through the iteration steps and execute operations to convert JIPipe data into their ImageJ equivalent (see JIPipe to ImageJ parameter). Then the macro code is executed, followed by operations to import " +
        "the result data into JIPipe data (see ImageJ to JIPipe parameter). Please feel free to click the 'Load example' button in the parameters to get started." +
        "\n\nPlease keep in mind the following remarks:\n\n" +
        "<ul>" +
        "<li>Input images are opened as windows named according to the input slot. You have to select windows with the select() function or comparable functions.</li>" +
        "<li>Output images are extracted by finding a window that is named according to the output slot. Ensure to rename() windows accordingly.</li>" +
        "<li>To extract the 'Results' table output, add an output of type 'Results table' and set the name to 'Results'. Alternatively, you can configure the output in 'JIPipe to ImageJ' and override the name to 'Results'</li>" +
        "<li>To import other tables, use a different slot name or set the appropriate configuration.</li>" +
        "<li>Please note that there is only one ROI manager. This is a restriction of ImageJ.</li>" +
        "<li>Annotations can also be accessed via a function getJIPipeAnnotation(key), which returns the string value of the annotation or an empty string if no value was set.</li>" +
        "<li>You can define variables that are passed from JIPipe to ImageJ. Variables are also created for incoming path-like data, named according to the slot name.</li>" +
        "</ul>")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(ImagePlusData.class)
@AddJIPipeInputSlot(ROI2DListData.class)
@AddJIPipeInputSlot(ResultsTableData.class)
@AddJIPipeInputSlot(PathData.class)
@AddJIPipeOutputSlot(ImagePlusData.class)
@AddJIPipeOutputSlot(ROI2DListData.class)
@AddJIPipeOutputSlot(ResultsTableData.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMacros", aliasName = "Run...")
public class RunImageJMacroFromParameterAlgorithm extends RunImageJMacroAlgorithm implements JIPipeScriptAlgorithm {

    private ImageJMacroParameter code = new ImageJMacroParameter();

    public RunImageJMacroFromParameterAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RunImageJMacroFromParameterAlgorithm(RunImageJMacroFromParameterAlgorithm other) {
        super(other);
        this.code = new ImageJMacroParameter(other.code);
    }

    @SetJIPipeDocumentation(name = "Code", description = "The macro code. " + "Images are opened as windows named according to the input slot. You have to select windows with " +
            "the select() function or comparable functions. You have have one results table input which " +
            "can be addressed via the global functions. Input ROI are merged into one ROI manager.\n\n" +
            "You can define variables that are passed from JIPipe to ImageJ. Variables are also created for incoming path-like data, named according to the slot name. " +
            "Annotations can also be accessed via a function getJIPipeAnnotation(key) or getJIPipeTextAnnotation(key), which returns the string value of the annotation or an empty string if no value was set.")
    @JIPipeParameter("code")
    public ImageJMacroParameter getCode() {
        return code;
    }

    @JIPipeParameter("code")
    public void setCode(ImageJMacroParameter code) {
        this.code = code;
    }

    @Override
    public JIPipeParameterAccess getScriptParameterAccess() {
        return getParameterAccess("code");
    }

    @Override
    protected String getMacroCode(JIPipeSingleIterationStep iterationStep, Path projectDirectory, JIPipeProgressInfo progressInfo) {
        return code.getCode(projectDirectory);
    }
}

