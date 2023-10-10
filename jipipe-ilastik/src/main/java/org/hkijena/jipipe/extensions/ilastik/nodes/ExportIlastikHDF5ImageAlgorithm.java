package org.hkijena.jipipe.extensions.ilastik.nodes;

import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ilastik.utils.ImgUtils;
import org.hkijena.jipipe.extensions.ilastik.utils.hdf5.Hdf5;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hkijena.jipipe.extensions.ilastik.utils.ImgUtils.DEFAULT_AXES;

@JIPipeDocumentation(name = "Export Ilastik HDF5 image", description = "Exports an image into the Ilastik HDF5 format")
@JIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@JIPipeOutputSlot(value = FileData.class, slotName = "HDF5 File", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
public class ExportIlastikHDF5ImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter hdf5Path = new DefaultExpressionParameter("\"exported_data\"");
    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();

    private String axes = ImgUtils.toStringAxes(DEFAULT_AXES);

    public ExportIlastikHDF5ImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportIlastikHDF5ImageAlgorithm(ExportIlastikHDF5ImageAlgorithm other) {
        super(other);
        this.hdf5Path = new DefaultExpressionParameter(other.hdf5Path);
        this.filePath = new DataExportExpressionParameter(other.filePath);
        this.axes = other.axes;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);

        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getDirectoryMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }
        Path outputPath = filePath.generatePath(getFirstOutputSlot().getSlotStoragePath(),
                getProjectDirectory(),
                projectDataDirs,
                inputData.toString(),
                dataBatch.getInputRow(getFirstInputSlot()),
                new ArrayList<>(dataBatch.getMergedTextAnnotations().values()));

        Path outputFile = PathUtils.ensureExtension(outputPath, ".h5", ".hdf5", ".hdf");
        progressInfo.log("Saving to " + outputFile);
        PathUtils.ensureParentDirectoriesExist(outputFile);

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        String hdf5Path_ = hdf5Path.evaluateToString(variables);

        List<AxisType> axisTypeList = ImgUtils.toImagejAxes(axes);

        DefaultDataset dataset = new DefaultDataset(JIPipe.getInstance().getContext(), new ImgPlus(ImageJFunctions.wrap(inputData.getImage())));
        Hdf5.writeDataset(outputFile.toFile(), hdf5Path_, (ImgPlus) dataset.getImgPlus(), 1, axisTypeList, value -> {
        });
    }

    @JIPipeDocumentation(name = "HDF5 internal path", description = "Path to the HDF5 data set to export")
    @JIPipeParameter("hdf5-path")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getHdf5Path() {
        return hdf5Path;
    }

    @JIPipeParameter("hdf5-path")
    public void setHdf5Path(DefaultExpressionParameter hdf5Path) {
        this.hdf5Path = hdf5Path;
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
}
