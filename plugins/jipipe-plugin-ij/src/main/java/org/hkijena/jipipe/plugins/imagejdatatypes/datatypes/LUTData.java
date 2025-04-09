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

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ij.ImagePlus;
import ij.process.LUT;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@SetJIPipeDocumentation(name = "LUT", description = "A function that converts an intensity to a RGB color value")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains one file in *.json format that describes the LUT gradient stops.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/lut-data.schema.json")
public class LUTData implements JIPipeData {

    private List<ColorUtils.GradientStop> gradientStops = new ArrayList<>();

    public LUTData() {

    }

    public LUTData(LUTData other) {
        for (ColorUtils.GradientStop stop : other.gradientStops) {
            gradientStops.add(new ColorUtils.GradientStop(stop));
        }
    }

    /**
     * Creates a LUT object from an image. The image must RGB color and
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
     * Imports an ImageJ LUT into a LUT object.
     * Please note, that ImageJ represents LUTs as array of colors, while LUT represents LUTs
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

    public static LUTData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path path = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        return JsonUtils.readFromFile(path, LUTData.class);
    }

    @JsonGetter("gradient-stops")
    public List<ColorUtils.GradientStop> getGradientStops() {
        return gradientStops;
    }

    @JsonSetter("gradient-stops")
    public void setGradientStops(List<ColorUtils.GradientStop> gradientStops) {
        this.gradientStops = gradientStops;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        try {
            if (!forceName) {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(storage.getFileSystemPath().resolve("lut.json").toFile(), this);
            } else {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(storage.getFileSystemPath().resolve(name + ".json").toFile(), this);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new LUTData(this);
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return new ImagePlusData(toImage(width, height)).createThumbnail(width, height, progressInfo);
    }

    /**
     * Converts the LUT into an ImageJ {@link LUT}
     *
     * @return the LUT
     */
    public LUT toLUT() {
        return ImageJUtils.createLUTFromGradient(gradientStops);
    }

    /**
     * Renders the LUT into an RGB Image
     *
     * @return the image
     */
    public ImagePlus toImage(int width, int height) {
        return ImageJUtils.lutToImage(toLUT(), width, height);
    }

    public int size() {
        return gradientStops.size();
    }

    public ColorUtils.GradientStop get(int index) {
        return gradientStops.get(index);
    }

    public void addStop(float position, Color color) {
        gradientStops.add(new ColorUtils.GradientStop(position, color));
    }
}
