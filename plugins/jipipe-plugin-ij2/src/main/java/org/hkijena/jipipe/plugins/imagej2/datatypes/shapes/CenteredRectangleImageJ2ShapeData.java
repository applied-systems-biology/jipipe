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

package org.hkijena.jipipe.plugins.imagej2.datatypes.shapes;

import com.google.common.primitives.Ints;
import net.imglib2.algorithm.neighborhood.CenteredRectangleShape;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.IntegerList;
import org.hkijena.jipipe.utils.json.JsonUtils;

@SetJIPipeDocumentation(name = "IJ2 Centered Rectangle Shape", description = "This specific factory differs to RectangleShape in that it allows non-isotropic rectangular shapes. However, it constrains the the neighborhood to be symmetric by its origin.\n" +
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

    public static CenteredRectangleImageJ2ShapeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (CenteredRectangleImageJ2ShapeData) ImageJ2ShapeData.importData(storage, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Span")
    @JIPipeParameter("span")
    public IntegerList getSpan() {
        return span;
    }

    @JIPipeParameter("span")
    public void setSpan(IntegerList span) {
        this.span = span;
    }

    @SetJIPipeDocumentation(name = "Skip center")
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

    @Override
    public String toString() {
        return "IJ2 Rectangle Shape (span=" + JsonUtils.toJsonString(span) + ", skipCenter=" + skipCenter + ")";
    }
}
