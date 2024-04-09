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

import net.imglib2.algorithm.neighborhood.HorizontalLineShape;
import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

@SetJIPipeDocumentation(name = "IJ2 Horizontal Line Shape", description = "A Shape representing finite, centered, symmetric lines, that are parallel to the image axes.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class HorizontalLineImageJ2ShapeData extends ImageJ2ShapeData {

    private long span = 2;
    private int lineDimension = 0;
    private boolean skipCenter = false;

    public HorizontalLineImageJ2ShapeData() {

    }

    public HorizontalLineImageJ2ShapeData(HorizontalLineImageJ2ShapeData other) {
        this.span = other.span;
        this.lineDimension = other.lineDimension;
        this.skipCenter = other.skipCenter;
    }

    public HorizontalLineImageJ2ShapeData(HorizontalLineShape shape) {
        this.span = shape.getSpan();
        this.lineDimension = shape.getLineDimension();
        this.skipCenter = shape.isSkippingCenter();
    }

    public static HorizontalLineImageJ2ShapeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (HorizontalLineImageJ2ShapeData) ImageJ2ShapeData.importData(storage, progressInfo);
    }

    @Override
    public Shape createShape() {
        return new HorizontalLineShape(span, lineDimension, skipCenter);
    }

    @Override
    public String toString() {
        return "IJ2 Horizontal Line Shape (span=" + span + ", lineDimension=" + lineDimension + ",skipCenter=" + skipCenter + ")";
    }

    @SetJIPipeDocumentation(name = "Span")
    @JIPipeParameter("span")
    public long getSpan() {
        return span;
    }

    @JIPipeParameter("span")
    public void setSpan(long span) {
        this.span = span;
    }

    @SetJIPipeDocumentation(name = "Line dimension")
    @JIPipeParameter("line-dimension")
    public int getLineDimension() {
        return lineDimension;
    }

    @JIPipeParameter("line-dimension")
    public void setLineDimension(int lineDimension) {
        this.lineDimension = lineDimension;
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
}
