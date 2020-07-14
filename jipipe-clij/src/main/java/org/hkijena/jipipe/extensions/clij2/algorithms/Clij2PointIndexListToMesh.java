package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.PointIndexListToMesh;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.PointIndexListToMesh}
 */
@JIPipeDocumentation(name = "CLIJ2 Point Index List To Mesh", description = "Meshes all points in a given point list which are indiced in a corresponding index list. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Point list")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "pointlist", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "indexlist", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "mesh", autoCreate = true)

public class Clij2PointIndexListToMesh extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2PointIndexListToMesh(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2PointIndexListToMesh(Clij2PointIndexListToMesh other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer pointlist = dataInterface.getInputData(getInputSlot("pointlist"), CLIJImageData.class).getImage();
        ClearCLBuffer indexlist = dataInterface.getInputData(getInputSlot("indexlist"), CLIJImageData.class).getImage();
        ClearCLBuffer mesh = clij2.create(pointlist);
        PointIndexListToMesh.pointIndexListToMesh(clij2, pointlist, indexlist, mesh);

        dataInterface.addOutputData(getOutputSlot("mesh"), new CLIJImageData(mesh));
    }

}