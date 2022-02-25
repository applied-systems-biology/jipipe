package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import com.google.common.primitives.Longs;
import net.imglib2.algorithm.neighborhood.PairOfPointsShape;
import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.LongList;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;

@JIPipeDocumentation(name = "IJ2 Pair Of Points Shape", description = "A Shape representing a pair of points. " +
        "The Shape as its origin at the first point, and the second one is simply found by adding the value of the offset array to its position.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class PairOfPointsImageJ2ShapeData extends ImageJ2ShapeData {

    private LongList offset = new LongList();

    public PairOfPointsImageJ2ShapeData() {

    }

    public PairOfPointsImageJ2ShapeData(PairOfPointsImageJ2ShapeData other) {
        this.offset = new LongList(other.offset);
    }

    public PairOfPointsImageJ2ShapeData(PairOfPointsShape shape) {
        this.offset.addAll(Longs.asList(shape.getOffset()));
    }

    @Override
    public Shape createShape() {
        return new PairOfPointsShape(Longs.toArray(offset));
    }

    public static PairOfPointsImageJ2ShapeData importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        return (PairOfPointsImageJ2ShapeData) ImageJ2ShapeData.importFrom(storagePath, progressInfo);
    }

    @Override
    public String toString() {
        return "IJ2 Pair Of Points Shape (offset=" + JsonUtils.toJsonString(offset) + ")";
    }

    @JIPipeDocumentation(name = "Offset")
    @JIPipeParameter("offset")
    public LongList getOffset() {
        return offset;
    }

    @JIPipeParameter("offset")
    public void setOffset(LongList offset) {
        this.offset = offset;
    }
}
