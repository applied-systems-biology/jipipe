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

package org.hkijena.jipipe.extensions.ilastik.nodes;

import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
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

@SetJIPipeDocumentation(name = "Export Ilastik HDF5 image", description = "Exports an image into the Ilastik HDF5 format")
@ConfigureJIPipeNode(nodeTypeCategory = ExportNodeTypeCategory.class, menuPath = "Images")
@AddJIPipeOutputSlot(value = FileData.class, slotName = "HDF5 File", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nSave")
public class ExportIlastikHDF5ImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter hdf5Path = new JIPipeExpressionParameter("\"exported_data\"");
    private DataExportExpressionParameter filePath = new DataExportExpressionParameter();

    private String axes = ImgUtils.toStringAxes(DEFAULT_AXES);

    public ExportIlastikHDF5ImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExportIlastikHDF5ImageAlgorithm(ExportIlastikHDF5ImageAlgorithm other) {
        super(other);
        this.hdf5Path = new JIPipeExpressionParameter(other.hdf5Path);
        this.filePath = new DataExportExpressionParameter(other.filePath);
        this.axes = other.axes;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);

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
                iterationStep.getInputRow(getFirstInputSlot()),
                new ArrayList<>(iterationStep.getMergedTextAnnotations().values()));

        Path outputFile = PathUtils.ensureExtension(outputPath, ".h5", ".hdf5", ".hdf");
        progressInfo.log("Saving to " + outputFile);
        PathUtils.ensureParentDirectoriesExist(outputFile);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        String hdf5Path_ = hdf5Path.evaluateToString(variables);

        List<AxisType> axisTypeList = ImgUtils.toImagejAxes(axes);

        DefaultDataset dataset = new DefaultDataset(JIPipe.getInstance().getContext(), new ImgPlus(ImageJFunctions.wrap(inputData.getImage())));
        Hdf5.writeDataset(outputFile.toFile(), hdf5Path_, (ImgPlus) dataset.getImgPlus(), 1, axisTypeList, value -> {
        });
    }

    @SetJIPipeDocumentation(name = "HDF5 internal path", description = "Path to the HDF5 data set to export")
    @JIPipeParameter("hdf5-path")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getHdf5Path() {
        return hdf5Path;
    }

    @JIPipeParameter("hdf5-path")
    public void setHdf5Path(JIPipeExpressionParameter hdf5Path) {
        this.hdf5Path = hdf5Path;
    }

    @SetJIPipeDocumentation(name = "Axes", description = "The order of the axes. Allowed values are X, Y, Z, C, and T")
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
