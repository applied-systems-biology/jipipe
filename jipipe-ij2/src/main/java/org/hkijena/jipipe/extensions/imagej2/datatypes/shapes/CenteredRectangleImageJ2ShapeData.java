package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import com.google.common.primitives.Ints;
import net.imglib2.algorithm.neighborhood.CenteredRectangleShape;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.IntegerList;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;

@JIPipeDocumentation(name = "IJ2 Centered Rectangle Shape", description = "This specific factory differs to RectangleShape in that it allows non-isotropic rectangular shapes. However, it constrains the the neighborhood to be symmetric by its origin.\n" +
        "The size of the neighborhood is specified by an int[] span array, so that in every dimension d, the extent of the neighborhood is given by 2 × span[d] + 1.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class CenteredRectangleImageJ2ShapeData extends ImageJ2ShapeData {

    private IntegerList span = new IntegerList();
    private boolean skipCenter = false;

    public CenteredRectangleImageJ2ShapeData() {

    }

    public CenteredRectangleImageJ2ShapeData(RectangleShape shape) {
        this.span.addAll(Ints.asList(shape.getSpan()));
        this.skipCenter = shape.isSkippingCenter();
    }

    public CenteredRectangleImageJ2ShapeData(CenteredRectangleImageJ2ShapeData other) {
        this.span = other.span;
        this.skipCenter = other.skipCenter;
    }

    @JIPipeDocumentation(name = "Span")
    @JIPipeParameter("span")
    public IntegerList getSpan() {
        return span;
    }

    @JIPipeParameter("span")
    public void setSpan(IntegerList span) {
        this.span = span;
    }

    @JIPipeDocumentation(name = "Skip center")
    @JIPipeParameter("skip-center")
    public boolean isSkipCenter() {
        return skipCenter;
    }

    @JIPipeParameter("skip-center")
    public void setSkipCenter(boolean skipCenter) {
        this.skipCenter = skipCenter;
    }

    @Override
    public Shape createShape() {
        return new CenteredRectangleShape(Ints.toArray(span), skipCenter);
    }

    public static CenteredRectangleImageJ2ShapeData importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        return (CenteredRectangleImageJ2ShapeData) ImageJ2ShapeData.importFrom(storagePath, progressInfo);
    }

    @Override
    public String toString() {
        return "IJ2 Rectangle Shape (span=" + JsonUtils.toJsonString(span) + ", skipCenter=" + skipCenter + ")";
    }
}
