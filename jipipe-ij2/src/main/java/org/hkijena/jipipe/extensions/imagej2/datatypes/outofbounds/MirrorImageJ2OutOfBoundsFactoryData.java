package org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds;

import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagej2.datatypes.shapes.ImageJ2ShapeData;
import org.hkijena.jipipe.extensions.imagej2.datatypes.shapes.RectangleImageJ2ShapeData;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.nio.file.Path;

@JIPipeDocumentation(name = "IJ2 Mirror Out Of Bounds factory", description = "Creates appropriate strategies that virtually mirror an image at its borders.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class MirrorImageJ2OutOfBoundsFactoryData extends ImageJ2OutOfBoundsFactoryData {

    private OutOfBoundsMirrorFactory.Boundary boundary = OutOfBoundsMirrorFactory.Boundary.SINGLE;

    public MirrorImageJ2OutOfBoundsFactoryData() {

    }

    public MirrorImageJ2OutOfBoundsFactoryData(OutOfBoundsMirrorFactory<?,?> factory) {
        try {
            // Why is there no getter?
            this.boundary = (OutOfBoundsMirrorFactory.Boundary) ReflectionUtils.getFieldValue(OutOfBoundsMirrorFactory.class.getDeclaredField("boundary"), factory);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public MirrorImageJ2OutOfBoundsFactoryData(MirrorImageJ2OutOfBoundsFactoryData other) {
        this.boundary = other.boundary;
    }

    @JIPipeDocumentation(name = "Boundary", description = "Boundary pixels are either " +
            "duplicated or not.  Note that if boundary pixels should not be duplicated " +
            "then all dimensions of the source must be larger than 1")
    @JIPipeParameter("boundary")
    public OutOfBoundsMirrorFactory.Boundary getBoundary() {
        return boundary;
    }

    @JIPipeParameter("boundary")
    public void setBoundary(OutOfBoundsMirrorFactory.Boundary boundary) {
        this.boundary = boundary;
    }

    @Override
    public OutOfBoundsFactory<?, ?> createFactory() {
        return new OutOfBoundsMirrorFactory(boundary);
    }

    public static MirrorImageJ2OutOfBoundsFactoryData importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        return (MirrorImageJ2OutOfBoundsFactoryData) ImageJ2OutOfBoundsFactoryData.importFrom(storagePath, progressInfo);
    }

    @Override
    public String toString() {
        return "IJ2 Mirror Out Of Bounds Factory (" + boundary + ")";
    }
}
