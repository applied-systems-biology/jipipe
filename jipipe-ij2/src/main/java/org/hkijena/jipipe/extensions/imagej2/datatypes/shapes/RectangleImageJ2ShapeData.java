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

package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

@SetJIPipeDocumentation(name = "IJ2 Rectangle Shape", description = "A 2D rectangle shape")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class RectangleImageJ2ShapeData extends ImageJ2ShapeData {

    private int span = 3;
    private boolean skipCenter = false;

    public RectangleImageJ2ShapeData() {

    }

    public RectangleImageJ2ShapeData(RectangleShape shape) {
        this.span = shape.getSpan();
        this.skipCenter = shape.isSkippingCenter();
    }

    public RectangleImageJ2ShapeData(RectangleImageJ2ShapeData other) {
        this.span = other.span;
        this.skipCenter = other.skipCenter;
    }

    public static RectangleImageJ2ShapeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (RectangleImageJ2ShapeData) ImageJ2ShapeData.importData(storage, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Span")
    @JIPipeParameter("span")
    public int getSpan() {
        return span;
    }

    @JIPipeParameter("span")
    public void setSpan(int span) {
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
        return new RectangleShape(span, skipCenter);
    }

    @Override
    public String toString() {
        return "IJ2 Rectangle Shape (span=" + span + ", skipCenter=" + skipCenter + ")";
    }
}
