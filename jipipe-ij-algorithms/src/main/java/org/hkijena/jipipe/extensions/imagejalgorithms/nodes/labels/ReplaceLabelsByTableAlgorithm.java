/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import gnu.trove.map.TFloatFloatMap;
import gnu.trove.map.hash.TFloatFloatHashMap;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.OptionalDefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

@JIPipeDocumentation(name = "Replace label values by table", description = "Replaces label values by a mapping as specified in a table. The table should contain two columns, one for the old label and a second column defining the replacement value.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Labels", autoCreate = true)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Mappings", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Labels", autoCreate = true)
public class ReplaceLabelsByTableAlgorithm extends JIPipeIteratingAlgorithm {

    private TableColumnSourceExpressionParameter oldLabelColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"old\"");
    private TableColumnSourceExpressionParameter newLabelColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"new\"");
    private OptionalDefaultExpressionParameter missingValueReplacement = new OptionalDefaultExpressionParameter(false, "0");

    private boolean ignoreZero = false;

    public ReplaceLabelsByTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ReplaceLabelsByTableAlgorithm(ReplaceLabelsByTableAlgorithm other) {
        super(other);
        this.oldLabelColumn = new TableColumnSourceExpressionParameter(other.oldLabelColumn);
        this.newLabelColumn = new TableColumnSourceExpressionParameter(other.newLabelColumn);
        this.missingValueReplacement = new OptionalDefaultExpressionParameter(other.missingValueReplacement);
        this.ignoreZero = other.ignoreZero;
    }

    @JIPipeDocumentation(name = "Old label column", description = "Table column that contains the old label")
    @JIPipeParameter("old-label-column")
    public TableColumnSourceExpressionParameter getOldLabelColumn() {
        return oldLabelColumn;
    }

    @JIPipeParameter("old-label-column")
    public void setOldLabelColumn(TableColumnSourceExpressionParameter oldLabelColumn) {
        this.oldLabelColumn = oldLabelColumn;
    }

    @JIPipeDocumentation(name = "New label column", description = "Table column that contains the new label")
    @JIPipeParameter("new-label-column")
    public TableColumnSourceExpressionParameter getNewLabelColumn() {
        return newLabelColumn;
    }

    @JIPipeParameter("new-label-column")
    public void setNewLabelColumn(TableColumnSourceExpressionParameter newLabelColumn) {
        this.newLabelColumn = newLabelColumn;
    }

    @JIPipeDocumentation(name = "Replace missing mappings", description = "If enabled, replace mappings missing from the table by the value defined by this expression. If disabled, missing mappings are ignored and affected labels are not changed.")
    @JIPipeParameter("missing-value-replacement")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public OptionalDefaultExpressionParameter getMissingValueReplacement() {
        return missingValueReplacement;
    }

    @JIPipeParameter("missing-value-replacement")
    public void setMissingValueReplacement(OptionalDefaultExpressionParameter missingValueReplacement) {
        this.missingValueReplacement = missingValueReplacement;
    }

    @JIPipeDocumentation(name = "Ignore zero label", description = "If enabled, the label value '0' is always ignored, regardless of mapping settings.")
    @JIPipeParameter("ignore-zero")
    public boolean isIgnoreZero() {
        return ignoreZero;
    }

    @JIPipeParameter("ignore-zero")
    public void setIgnoreZero(boolean ignoreZero) {
        this.ignoreZero = ignoreZero;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus outputImage = iterationStep.getInputData("Labels", ImagePlusGreyscale32FData.class, progressInfo).getDuplicateImage();
        ResultsTableData mappingsTable = iterationStep.getInputData("Mappings", ResultsTableData.class, progressInfo);

        TFloatFloatMap mapping = new TFloatFloatHashMap();
        TableColumn mappingOld = oldLabelColumn.pickOrGenerateColumn(mappingsTable);
        TableColumn mappingNew = newLabelColumn.pickOrGenerateColumn(mappingsTable);

        for (int i = 0; i < mappingOld.getRows(); i++) {
            mapping.put((float) mappingOld.getRowAsDouble(i), (float) mappingNew.getRowAsDouble(i));
        }

        float defaultMapping;
        if (missingValueReplacement.isEnabled()) {
            ExpressionVariables variables = new ExpressionVariables();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            defaultMapping = missingValueReplacement.getContent().evaluateToFloat(variables);
        } else {
            defaultMapping = 0;
        }

        ImageJUtils.forEachIndexedZCTSlice(outputImage, (ip, index) -> {
            float[] pixels = (float[]) ip.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                float value = pixels[i];
                if (ignoreZero && value == 0f) {
                    continue;
                }
                if (mapping.containsKey(value)) {
                    value = mapping.get(value);
                } else if (missingValueReplacement.isEnabled()) {
                    value = defaultMapping;
                }
                pixels[i] = value;
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(outputImage), progressInfo);
    }
}
