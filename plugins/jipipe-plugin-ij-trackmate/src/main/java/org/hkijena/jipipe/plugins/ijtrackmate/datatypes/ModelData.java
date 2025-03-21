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

package org.hkijena.jipipe.plugins.ijtrackmate.datatypes;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlWriter;
import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "TrackMate model", description = "A TrackMate model")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains an *.xml file that stores the TrackMate model and a *.tif image file that contains the image that is the basis of the model.", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/trackmate-model-data.schema.json")
@LabelAsJIPipeHeavyData
public class ModelData implements JIPipeData {

    private final Model model;
    private final Settings settings;

    private final ImagePlus image;

    public ModelData(Model model, Settings settings, ImagePlus image) {
        this.model = model;
        this.settings = settings;
        this.image = image;
    }

    public ModelData(ModelData other) {
        this.model = other.getModel().copy();
        this.settings = other.getSettings().copyOn(other.getImage());
        this.image = other.getImage();
    }

    public static ModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path xmlFileName = storage.findFileByExtension(".xml").get();
        Path tiffFileName = storage.findFileByExtension(".tif", ".tiff").get();

        ImagePlus imagePlus = IJ.openImage(storage.getFileSystemPath().resolve(tiffFileName).toString());

        TmXmlReader reader = new TmXmlReader(storage.getFileSystemPath().resolve(xmlFileName).toFile());
        Model model = reader.getModel();
        Settings settings = reader.readSettings(imagePlus);

        return new ModelData(model, settings, imagePlus);
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        TmXmlWriter writer = new TmXmlWriter(PathUtils.ensureExtension(storage.getFileSystemPath().resolve(StringUtils.orElse(name, "model")), ".xml").toFile());
        writer.appendModel(model);
        writer.appendSettings(settings);
        try {
            writer.writeToFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        IJ.saveAs(image, "TIFF", PathUtils.ensureExtension(storage.getFileSystemPath().resolve(StringUtils.orElse(name, "model")), ".tif", ".tiff").toString());
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return (JIPipeData) ReflectionUtils.newInstance(getClass(), this);
    }

    public Model getModel() {
        return model;
    }

    public Settings getSettings() {
        return settings;
    }

    public ImagePlus getImage() {
        return image;
    }
}
