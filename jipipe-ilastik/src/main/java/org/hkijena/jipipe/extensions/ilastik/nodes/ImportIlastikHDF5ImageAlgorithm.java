package org.hkijena.jipipe.extensions.ilastik.nodes;

import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ilastik.utils.ImgUtils;
import org.hkijena.jipipe.extensions.ilastik.utils.hdf5.Hdf5;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.CalibrationParameters;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

import java.nio.file.Path;

import static org.hkijena.jipipe.extensions.ilastik.utils.ImgUtils.DEFAULT_AXES;

@JIPipeDocumentation(name = "Import Ilastik HDF5 image", description = "Imports an HDF5 image that was generated with Ilastik")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "HDF5 File", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
public class ImportIlastikHDF5ImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private DefaultExpressionParameter hdf5Path = new DefaultExpressionParameter("\"exported_data\"");
    private String axes = ImgUtils.toStringAxes(DEFAULT_AXES);
    private final CalibrationParameters calibrationParameters;

    public ImportIlastikHDF5ImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.calibrationParameters = new CalibrationParameters();
        registerSubParameter(calibrationParameters);
    }

    public ImportIlastikHDF5ImageAlgorithm(ImportIlastikHDF5ImageAlgorithm other) {
        super(other);
        this.hdf5Path = new DefaultExpressionParameter(other.hdf5Path);
        this.calibrationParameters = new CalibrationParameters(other.calibrationParameters);
        this.axes = other.axes;
        registerSubParameter(calibrationParameters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path path = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        String hdf5Path_ = hdf5Path.evaluateToString(variables);

        ImgPlus dataset = Hdf5.readDataset(path.toFile(), hdf5Path_, ImgUtils.toImagejAxes(axes));
        ImagePlus imagePlus = ImageJFunctions.wrap(dataset, path.getFileName().toString());
        ImageJUtils.calibrate(imagePlus, calibrationParameters.getCalibrationMode(), calibrationParameters.getCustomMin(), calibrationParameters.getCustomMax());

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(imagePlus), progressInfo);
    }

    @JIPipeDocumentation(name = "Axes", description = "The order of the axes. Allowed values are X, Y, Z, C, and T")
    @JIPipeParameter("axes")
    @StringParameterSettings(monospace = true)
    public String getAxes() {
        return axes;
    }

    @JIPipeParameter("axes")
    public void setAxes(String axes) {
        this.axes = axes;
    }

    @JIPipeDocumentation(name = "HDF5 internal path", description = "Path to the HDF5 data set to import")
    @JIPipeParameter("hdf5-path")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getHdf5Path() {
        return hdf5Path;
    }

    @JIPipeParameter("hdf5-path")
    public void setHdf5Path(DefaultExpressionParameter hdf5Path) {
        this.hdf5Path = hdf5Path;
    }

    @JIPipeDocumentation(name = "Display contrast settings", description = "The following settings determine how the display contrast is determined")
    @JIPipeParameter("calibration-parameters")
    public CalibrationParameters getCalibrationParameters() {
        return calibrationParameters;
    }
}
