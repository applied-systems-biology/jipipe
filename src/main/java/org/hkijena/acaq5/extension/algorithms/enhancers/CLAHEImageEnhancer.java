package org.hkijena.acaq5.extension.algorithms.enhancers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ij.ImagePlus;
import mpicbg.ij.clahe.Flat;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQParameter;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;

import java.io.File;
import java.io.IOException;

@ACAQDocumentation(name = "CLAHE enhancer")
public class CLAHEImageEnhancer extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData,
        ACAQGreyscaleImageData> {

    @ACAQParameter("blocks")
    private int blocks = 127;

    @ACAQParameter("bins")
    private int bins = 256;

    @ACAQParameter("max-slope")
    private float maxSlope = 3.0f;

    @ACAQParameter("fast-mode")
    private boolean fastMode = false;

    public CLAHEImageEnhancer() {
        super("Input image", ACAQGreyscaleImageDataSlot.class,
                "Output image", ACAQGreyscaleImageDataSlot.class);
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();

        ImagePlus result = img.duplicate();
        Flat clahe = fastMode ? Flat.getFastInstance() : Flat.getInstance();
        clahe.run(result, blocks, bins, maxSlope, null, true);
        getOutputSlot().setData(new ACAQGreyscaleImageData(result));
    }

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("/home/rgerst/tmp/test.json"), new CLAHEImageEnhancer());
    }

}