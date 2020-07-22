package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.AffineTransform2D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ProcessorNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.AffineTransform2D}
 */
@JIPipeDocumentation(name = "CLIJ2 Affine Transform 2D", description = "Applies an affine transform to a 2D image. Individual transforms must be separated by spaces." + "Supported transforms:" + "* center: translate the coordinate origin to the center of the image" + "* -center: translate the coordinate origin back to the initial origin" + "* rotate=[angle]: rotate in X/Y plane (around Z-axis) by the given angle in degrees" + "* scale=[factor]: isotropic scaling according to given zoom factor" + "* scaleX=[factor]: scaling along X-axis according to given zoom factor" + "* scaleY=[factor]: scaling along Y-axis according to given zoom factor" + "* shearXY=[factor]: shearing along X-axis in XY plane according to given factor" + "* translateX=[distance]: translate along X-axis by distance given in pixels" + "* translateY=[distance]: translate along X-axis by distance given in pixels" + "Example transform:" + "transform = 'center scale=2 rotate=45 -center'; Works for following image dimensions: 2D. Developed by Robert Haase and Peter Haub based on work by Martin Weigert.  adapted from: https://github.com/maweigert/gputools/blob/master/gputools/transforms/kernels/transformations.cl" + " Copyright (c) 2016, Martin Weigert" + " All rights reserved." + " Redistribution and use in source and binary forms, with or without" + " modification, are permitted provided that the following conditions are met:" + " * Redistributions of source code must retain the above copyright notice, this" + "   list of conditions and the following disclaimer." + " * Redistributions in binary form must reproduce the above copyright notice," + "   this list of conditions and the following disclaimer in the documentation" + "   and/or other materials provided with the distribution." + " * Neither the name of gputools nor the names of its" + "   contributors may be used to endorse or promote products derived from" + "   this software without specific prior written permission." + " THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 'AS IS'" + " AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE" + " IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE" + " DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE" + " FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL" + " DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR" + " SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER" + " CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY," + " OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE" + " OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.")
@JIPipeOrganization(nodeTypeCategory = ProcessorNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2AffineTransform2d extends JIPipeSimpleIteratingAlgorithm {
    String transform;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2AffineTransform2d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2AffineTransform2d(Clij2AffineTransform2d other) {
        super(other);
        this.transform = other.transform;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        AffineTransform2D.affineTransform2D(clij2, input, output, transform);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

    @JIPipeParameter("transform")
    public String getTransform() {
        return transform;
    }

    @JIPipeParameter("transform")
    public void setTransform(String value) {
        this.transform = value;
    }

}