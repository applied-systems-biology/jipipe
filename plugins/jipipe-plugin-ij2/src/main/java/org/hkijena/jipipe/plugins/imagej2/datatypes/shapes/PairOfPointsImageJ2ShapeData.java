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

import com.google.common.primitives.Longs;
import net.imglib2.algorithm.neighborhood.PairOfPointsShape;
import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.LongList;
import org.hkijena.jipipe.utils.json.JsonUtils;

@SetJIPipeDocumentation(name = "IJ2 Pair Of Points Shape", description = "A Shape representing a pair of points. " +
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

    public static PairOfPointsImageJ2ShapeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (PairOfPointsImageJ2ShapeData) ImageJ2ShapeData.importData(storage, progressInfo);
    }

    @Override
    public Shape createShape() {
        return new PairOfPointsShape(Longs.toArray(offset));
    }

    @Override
    public String toString() {
        return "IJ2 Pair Of Points Shape (offset=" + JsonUtils.toJsonString(offset) + ")";
    }

    @SetJIPipeDocumentation(name = "Offset")
    @JIPipeParameter("offset")
    public LongList getOffset() {
        return offset;
    }

    @JIPipeParameter("offset")
    public void setOffset(LongList offset) {
        this.offset = offset;
    }
}
