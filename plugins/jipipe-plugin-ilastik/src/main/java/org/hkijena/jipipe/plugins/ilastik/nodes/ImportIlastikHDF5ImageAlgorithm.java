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

package org.hkijena.jipipe.plugins.ilastik.nodes;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.ilastik.utils.ImgUtils;
import org.hkijena.jipipe.plugins.ilastik.utils.hdf5.IJ1Hdf5;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.CalibrationParameters;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;

import java.nio.file.Path;

import static org.hkijena.jipipe.plugins.ilastik.utils.ImgUtils.DEFAULT_AXES;

@SetJIPipeDocumentation(name = "Import Ilastik HDF5 image", description = "Imports an HDF5 image that was generated with Ilastik")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, name = "HDF5 File", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Image", create = true)
public class ImportIlastikHDF5ImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private final CalibrationParameters calibrationParameters;
    private JIPipeExpressionParameter hdf5Path = new JIPipeExpressionParameter("\"exported_data\"");
    private OptionalStringParameter overrideAxes = new OptionalStringParameter(ImgUtils.toStringAxes(DEFAULT_AXES), false);

    public ImportIlastikHDF5ImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.calibrationParameters = new CalibrationParameters();
        registerSubParameter(calibrationParameters);
    }

    public ImportIlastikHDF5ImageAlgorithm(ImportIlastikHDF5ImageAlgorithm other) {
        super(other);
        this.hdf5Path = new JIPipeExpressionParameter(other.hdf5Path);
        this.calibrationParameters = new CalibrationParameters(other.calibrationParameters);
        this.overrideAxes = new OptionalStringParameter(other.overrideAxes);
        registerSubParameter(calibrationParameters);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path path = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
        String hdf5Path_ = hdf5Path.evaluateToString(variables);

        ImagePlus imagePlus = IJ1Hdf5.readImage(path, hdf5Path_, overrideAxes.isEnabled() ? ImgUtils.toImagejAxes(overrideAxes.getContent()) : null, progressInfo);
        ImageJUtils.calibrate(imagePlus, calibrationParameters.getCalibrationMode(), calibrationParameters.getCustomMin(), calibrationParameters.getCustomMax());

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(imagePlus), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Override axes", description = "Allows to override the default axes for reading the values. " +
            "By default, JIPipe attempts to guess the axes based on the default axis configuration XYCZT. " +
            "Allowed values are X, Y, Z, C, and T (case-insensitive)")
    @JIPipeParameter(value = "override-axes")
    @StringParameterSettings(monospace = true)
    public OptionalStringParameter getOverrideAxes() {
        return overrideAxes;
    }

    @JIPipeParameter("override-axes")
    public void setOverrideAxes(OptionalStringParameter overrideAxes) {
        this.overrideAxes = overrideAxes;
    }

    @SetJIPipeDocumentation(name = "HDF5 internal path", description = "Path to the HDF5 data set to import")
    @JIPipeParameter(value = "hdf5-path", important = true)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getHdf5Path() {
        return hdf5Path;
    }

    @JIPipeParameter("hdf5-path")
    public void setHdf5Path(JIPipeExpressionParameter hdf5Path) {
        this.hdf5Path = hdf5Path;
    }

    @SetJIPipeDocumentation(name = "Display contrast settings", description = "The following settings determine how the display contrast is determined")
    @JIPipeParameter("calibration-parameters")
    public CalibrationParameters getCalibrationParameters() {
        return calibrationParameters;
    }
}
