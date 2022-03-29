package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import com.google.common.primitives.Ints;
import net.imglib2.algorithm.neighborhood.PeriodicLineShape;
import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.IntegerList;
import org.hkijena.jipipe.utils.json.JsonUtils;

@JIPipeDocumentation(name = "IJ2 Periodic Line Shape", description = "Iterate over what is termed \"Periodic lines\" (see Jones and Soilles, 1996)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
@JIPipeCitation("Jones and Soilles. Periodic lines: Definition, cascades, and application to granulometries. Pattern Recognition Letters (1996) vol. 17 (10) pp. 1057-1063")
public class PeriodicLineImageJ2ShapeData extends ImageJ2ShapeData {

    private long span = 2;
    private IntegerList increments = new IntegerList();

    public PeriodicLineImageJ2ShapeData() {

    }

    public PeriodicLineImageJ2ShapeData(PeriodicLineImageJ2ShapeData other) {
        this.span = other.span;
        this.increments = new IntegerList(other.increments);
    }

    public PeriodicLineImageJ2ShapeData(PeriodicLineShape shape) {
        this.span = shape.getSpan();
        this.increments.addAll(Ints.asList(shape.getIncrements()));
    }

    public static PeriodicLineImageJ2ShapeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (PeriodicLineImageJ2ShapeData) ImageJ2ShapeData.importData(storage, progressInfo);
    }

    @Override
    public Shape createShape() {
        return new PeriodicLineShape(span, Ints.toArray(increments));
    }

    @Override
    public String toString() {
        return "IJ2 Periodic Line Shape (span=" + span + ", increments=" + JsonUtils.toJsonString(increments) + ")";
    }

    @JIPipeDocumentation(name = "Span")
    @JIPipeParameter("span")
    public long getSpan() {
        return span;
    }

    @JIPipeParameter("span")
    public void setSpan(long span) {
        this.span = span;
    }

    @JIPipeDocumentation(name = "Increments")
    @JIPipeParameter("increments")
    public IntegerList getIncrements() {
        return increments;
    }

    @JIPipeParameter("increments")
    public void setIncrements(IntegerList increments) {
        this.increments = increments;
    }
}
