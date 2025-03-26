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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels;

import gnu.trove.map.TFloatFloatMap;
import gnu.trove.map.hash.TFloatFloatHashMap;
import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

@SetJIPipeDocumentation(name = "Replace label values by table", description = "Replaces label values by a mapping as specified in a table. The table should contain two columns, one for the old label and a second column defining the replacement value.")
@ConfigureJIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, name = "Labels", create = true)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Mappings", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Labels", create = true)
public class ReplaceLabelsByTableAlgorithm extends JIPipeIteratingAlgorithm {

    private TableColumnSourceExpressionParameter oldLabelColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"old\"");
    private TableColumnSourceExpressionParameter newLabelColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"new\"");
    private OptionalJIPipeExpressionParameter missingValueReplacement = new OptionalJIPipeExpressionParameter(false, "0");

    private boolean ignoreZero = false;

    public ReplaceLabelsByTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ReplaceLabelsByTableAlgorithm(ReplaceLabelsByTableAlgorithm other) {
        super(other);
        this.oldLabelColumn = new TableColumnSourceExpressionParameter(other.oldLabelColumn);
        this.newLabelColumn = new TableColumnSourceExpressionParameter(other.newLabelColumn);
        this.missingValueReplacement = new OptionalJIPipeExpressionParameter(other.missingValueReplacement);
        this.ignoreZero = other.ignoreZero;
    }

    @SetJIPipeDocumentation(name = "Old label column", description = "Table column that contains the old label")
    @JIPipeParameter("old-label-column")
    public TableColumnSourceExpressionParameter getOldLabelColumn() {
        return oldLabelColumn;
    }

    @JIPipeParameter("old-label-column")
    public void setOldLabelColumn(TableColumnSourceExpressionParameter oldLabelColumn) {
        this.oldLabelColumn = oldLabelColumn;
    }

    @SetJIPipeDocumentation(name = "New label column", description = "Table column that contains the new label")
    @JIPipeParameter("new-label-column")
    public TableColumnSourceExpressionParameter getNewLabelColumn() {
        return newLabelColumn;
    }

    @JIPipeParameter("new-label-column")
    public void setNewLabelColumn(TableColumnSourceExpressionParameter newLabelColumn) {
        this.newLabelColumn = newLabelColumn;
    }

    @SetJIPipeDocumentation(name = "Replace missing mappings", description = "If enabled, replace mappings missing from the table by the value defined by this expression. If disabled, missing mappings are ignored and affected labels are not changed.")
    @JIPipeParameter("missing-value-replacement")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getMissingValueReplacement() {
        return missingValueReplacement;
    }

    @JIPipeParameter("missing-value-replacement")
    public void setMissingValueReplacement(OptionalJIPipeExpressionParameter missingValueReplacement) {
        this.missingValueReplacement = missingValueReplacement;
    }

    @SetJIPipeDocumentation(name = "Ignore zero label", description = "If enabled, the label value '0' is always ignored, regardless of mapping settings.")
    @JIPipeParameter("ignore-zero")
    public boolean isIgnoreZero() {
        return ignoreZero;
    }

    @JIPipeParameter("ignore-zero")
    public void setIgnoreZero(boolean ignoreZero) {
        this.ignoreZero = ignoreZero;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus outputImage = iterationStep.getInputData("Labels", ImagePlusGreyscale32FData.class, progressInfo).getDuplicateImage();
        ResultsTableData mappingsTable = iterationStep.getInputData("Mappings", ResultsTableData.class, progressInfo);

        TFloatFloatMap mapping = new TFloatFloatHashMap();
        TableColumnData mappingOld = oldLabelColumn.pickOrGenerateColumn(mappingsTable, new JIPipeExpressionVariablesMap(iterationStep));
        TableColumnData mappingNew = newLabelColumn.pickOrGenerateColumn(mappingsTable, new JIPipeExpressionVariablesMap(iterationStep));

        for (int i = 0; i < mappingOld.getRows(); i++) {
            mapping.put((float) mappingOld.getRowAsDouble(i), (float) mappingNew.getRowAsDouble(i));
        }

        float defaultMapping;
        if (missingValueReplacement.isEnabled()) {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
            defaultMapping = missingValueReplacement.getContent().evaluateToFloat(variables);
        } else {
            defaultMapping = 0;
        }

        ImageJIterationUtils.forEachIndexedZCTSlice(outputImage, (ip, index) -> {
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
