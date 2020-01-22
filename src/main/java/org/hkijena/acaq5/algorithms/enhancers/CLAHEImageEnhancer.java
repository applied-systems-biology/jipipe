package org.hkijena.acaq5.algorithms.enhancers;

import ij.ImagePlus;
import mpicbg.ij.clahe.Flat;
import mpicbg.ij.clahe.PlugIn;
import org.hkijena.acaq5.ACAQInputDataSlot;
import org.hkijena.acaq5.ACAQOutputDataSlot;
import org.hkijena.acaq5.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.utils.ImageJUtils;
import org.hkijena.acaq5.utils.MacroFlag;
import org.hkijena.acaq5.utils.MacroSetting;

public class CLAHEImageEnhancer extends ACAQSimpleAlgorithm<ACAQInputDataSlot<ACAQGreyscaleImageData>,
        ACAQOutputDataSlot<ACAQGreyscaleImageData>> {

    private int blocks = 127;
    private int bins = 256;
    private float maxSlope = 3.0f;
    private boolean fastMode = false;

    public CLAHEImageEnhancer() {
        super(new ACAQInputDataSlot<>("Input image", ACAQGreyscaleImageData.class),
                new ACAQOutputDataSlot<>("Output image", ACAQGreyscaleImageData.class));
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();

        ImagePlus result = img.duplicate();
        Flat clahe = fastMode ? Flat.getFastInstance() : Flat.getInstance();
        clahe.run(result, blocks, bins, maxSlope, null, true);
        getOutputSlot().setData(new ACAQGreyscaleImageData(result));
    }
}