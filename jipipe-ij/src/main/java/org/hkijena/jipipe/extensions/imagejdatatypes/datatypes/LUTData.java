package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.ImagePlus;
import ij.process.LUT;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@JIPipeDocumentation(name = "LUT", description = "A function that converts an intensity to a RGB color value")
@JIPipeDataStorageDocumentation("Contains one file in *.json format. " +
        "*.roi is a single ImageJ ROI. *.zip contains multiple ImageJ ROI. Please note that if multiple *.roi/*.zip are present, only " +
        "one will be loaded.")
public class LUTData implements JIPipeData {

    private List<ImageJUtils.GradientStop> gradientStops = new ArrayList<>();

    public LUTData() {

    }

    public LUTData(LUTData other) {
        for (ImageJUtils.GradientStop stop : other.gradientStops) {
            gradientStops.add(new ImageJUtils.GradientStop(stop));
        }
    }

    /**
     * Creates a {@link LUTData} object from an image. The image must RGB color and
     * contain the value mappings in the first row. LUTs always have 256 colors, but other
     * number of columns are supported (in such case, the image will be stretched)
     *
     * @param img the LUT image
     * @return the LUT
     */
    public static LUTData fromImage(ImagePlus img, boolean simplify) {
        return fromLUT(ImageJUtils.lutFromImage(img), simplify);
    }

    /**
     * Imports an ImageJ LUT into a {@link LUTData} object.
     * Please note, that ImageJ represents LUTs as array of colors, while {@link LUTData} represents LUTs
     * as set of gradient stops. The conversion might not be always 100% perfect.
     *
     * @param lut      the LUT
     * @param simplify attempt to remove spurious gradient stops
     * @return the LUT
     */
    public static LUTData fromLUT(LUT lut, boolean simplify) {
        Set<Integer> thumbLocations = new TreeSet<>();
        thumbLocations.add(0);
        thumbLocations.add(255);

        //        // We check how if the R, G, and B functions change their monoticity
//        int lastMonoticityR = 0;
//        int lastMonoticityG = 0;
//        int lastMonoticityB = 0;
//
//        for (int i = 1; i < 256; i++) {
//            int lastR = lut.getRed(i - 1);
//            int lastG = lut.getGreen(i - 1);
//            int lastB = lut.getBlue(i - 1);
//            int currentR = lut.getRed(i);
//            int currentG = lut.getGreen(i);
//            int currentB = lut.getBlue(i);
//            int currentMonoticityR = (int)Math.signum(currentR - lastR);
//            int currentMonoticityG = (int)Math.signum(currentG - lastG);
//            int currentMonoticityB = (int)Math.signum(currentB - lastB);
//            if(i != 1) {
//                if (currentMonoticityR != 0 && lastMonoticityR != 0 && currentMonoticityR != lastMonoticityR) {
//                    thumbLocations.add(i);
//                }
//                if (currentMonoticityG != 0 && lastMonoticityG != 0 && currentMonoticityG != lastMonoticityG) {
//                    thumbLocations.add(i);
//                }
//                if (currentMonoticityB != 0 && lastMonoticityB != 0 &&currentMonoticityB != lastMonoticityB) {
//                    thumbLocations.add(i);
//                }
//            }
//
//            if (currentMonoticityR != 0)
//                lastMonoticityR = currentMonoticityR;
//            if (currentMonoticityG != 0)
//                lastMonoticityG = currentMonoticityG;
//            if(currentMonoticityB != 0)
//                lastMonoticityB = currentMonoticityB;
//        }

        // We check how if the R, G, and B functions change their monoticity
        int[] bufferR = new int[3];
        int[] bufferG = new int[3];
        int[] bufferB = new int[3];
        TIntList derivationsR = new TIntArrayList(256);
        TIntList derivationsG = new TIntArrayList(256);
        TIntList derivationsB = new TIntArrayList(256);
        derivationsR.add(0);
        derivationsG.add(0);
        derivationsB.add(0);
        for (int i = 1; i < 255; i++) {
            bufferR[0] = lut.getRed(i - 1);
            bufferR[1] = lut.getRed(i);
            bufferR[2] = lut.getRed(i + 1);
            bufferG[0] = lut.getGreen(i - 1);
            bufferG[1] = lut.getGreen(i);
            bufferG[2] = lut.getGreen(i + 1);
            bufferB[0] = lut.getBlue(i - 1);
            bufferB[1] = lut.getBlue(i);
            bufferB[2] = lut.getBlue(i + 1);

            int derivationR = (bufferR[0] - bufferR[2]) / 2;
            int derivationG = (bufferG[0] - bufferG[2]) / 2;
            int derivationB = (bufferB[0] - bufferB[2]) / 2;

            derivationsR.add(derivationR);
            derivationsG.add(derivationG);
            derivationsB.add(derivationB);
        }
        int lastLocation = 0;
        for (int i = 2; i < 255; i++) {
            int lastDerivationR = (int) Math.signum(derivationsR.get(i - 1));
            int derivationR = (int) Math.signum(derivationsR.get(i));
            int lastDerivationG = (int) Math.signum(derivationsG.get(i - 1));
            int derivationG = (int) Math.signum(derivationsG.get(i));
            int lastDerivationB = (int) Math.signum(derivationsB.get(i - 1));
            int derivationB = (int) Math.signum(derivationsB.get(i));
            if (lastDerivationR != derivationR || lastDerivationB != derivationB || lastDerivationG != derivationG) {
                if (i - lastLocation > 20 || !simplify) {
                    thumbLocations.add(i);
                    lastLocation = i;
                }
            }
        }

        LUTData data = new LUTData();
        for (int lutIndex : thumbLocations) {
            float thumbLocation = lutIndex / 255.0f;
            data.addStop(thumbLocation, new Color(lut.getRGB(lutIndex)));
        }
        return data;
    }

    public static LUTData importFrom(Path storagePath) {
        Path path = PathUtils.findFileByExtensionIn(storagePath, ".json");
        return JsonUtils.readFromFile(path, LUTData.class);
    }

    @JsonGetter("gradient-stops")
    public List<ImageJUtils.GradientStop> getGradientStops() {
        return gradientStops;
    }

    @JsonSetter("gradient-stops")
    public void setGradientStops(List<ImageJUtils.GradientStop> gradientStops) {
        this.gradientStops = gradientStops;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        try {
            if (!forceName) {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(storageFilePath.resolve("lut.json").toFile(), this);
            } else {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(storageFilePath.resolve(name + ".json").toFile(), this);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate() {
        return new LUTData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        ImagePlus image = toImage(256, 256);
        ImageViewerPanel.showImage(image, displayName);
    }

    @Override
    public Component preview(int width, int height) {
        return new ImagePlusData(toImage(width, height)).preview(width, height);
    }

    /**
     * Converts the {@link LUTData} into an ImageJ {@link LUT}
     *
     * @return the LUT
     */
    public LUT toLUT() {
        return ImageJUtils.createLUTFromGradient(gradientStops);
    }

    /**
     * Renders the {@link LUTData} into an RGB Image
     *
     * @return the image
     */
    public ImagePlus toImage(int width, int height) {
        return ImageJUtils.lutToImage(toLUT(), width, height);
    }

    public int size() {
        return gradientStops.size();
    }

    public ImageJUtils.GradientStop get(int index) {
        return gradientStops.get(index);
    }

    public void addStop(float position, Color color) {
        gradientStops.add(new ImageJUtils.GradientStop(position, color));
    }
}
