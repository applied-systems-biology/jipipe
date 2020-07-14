package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.FloodFillDiamond;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.FloodFillDiamond}
 */
@JIPipeDocumentation(name = "CLIJ2 Flood Fill Diamond", description = "Replaces recursively all pixels of value a with value b if the pixels have a neighbor with value b. Works for following image dimensions: 2D, 3D. Developed by Robert Haase translated original work by Ignacio Arganda-Carreras. " + "Code was translated from  " + " Skeletonize3D plugin for ImageJ(C)." + " Copyright (C) 2008 Ignacio Arganda-Carreras " + " " + " This program is free software; you can redistribute it and/or" + " modify it under the terms of the GNU General Public License" + " as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )" + " This program is distributed in the hope that it will be useful," + " but WITHOUT ANY WARRANTY; without even the implied warranty of" + " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the" + " GNU General Public License for more details." + " " + " You should have received a copy of the GNU General Public License" + " along with this program; if not, write to the Free Software" + " Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA." + " " + "")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Morphology")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2FloodFillDiamond extends JIPipeSimpleIteratingAlgorithm {
    Float valueToReplace;
    Float valueReplacement;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2FloodFillDiamond(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2FloodFillDiamond(Clij2FloodFillDiamond other) {
        super(other);
        this.valueToReplace = other.valueToReplace;
        this.valueReplacement = other.valueReplacement;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        FloodFillDiamond.floodFillDiamond(clij2, src, dst, valueToReplace, valueReplacement);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("value-to-replace")
    public Float getValueToReplace() {
        return valueToReplace;
    }

    @JIPipeParameter("value-to-replace")
    public void setValueToReplace(Float value) {
        this.valueToReplace = value;
    }

    @JIPipeParameter("value-replacement")
    public Float getValueReplacement() {
        return valueReplacement;
    }

    @JIPipeParameter("value-replacement")
    public void setValueReplacement(Float value) {
        this.valueReplacement = value;
    }

}