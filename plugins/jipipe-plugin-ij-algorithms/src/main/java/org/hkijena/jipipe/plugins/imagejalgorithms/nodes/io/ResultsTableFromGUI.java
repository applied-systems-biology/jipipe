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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.io;

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeNodeAlias;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.awt.*;

/**
 * Imports {@link ImagePlusData} from the GUI
 */
@SetJIPipeDocumentation(name = "Table from ImageJ", description = "Imports one or multiple active ImageJ results table windows into JIPipe")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nImport")
public class ResultsTableFromGUI extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter titleFilterExpression = new JIPipeExpressionParameter("title EQUALS \"Results\"");

    public ResultsTableFromGUI(JIPipeNodeInfo info) {
        super(info);
    }

    public ResultsTableFromGUI(ResultsTableFromGUI other) {
        super(other);
        this.titleFilterExpression = new JIPipeExpressionParameter(other.titleFilterExpression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);
        for (Window window : WindowManager.getAllNonImageWindows()) {
            if (window instanceof TextWindow) {
                ResultsTable resultsTable = ((TextWindow) window).getResultsTable();
                if (resultsTable != null) {
                    String title = ((TextWindow) window).getTitle();
                    variableSet.set("title", title);
                    if (titleFilterExpression.test(variableSet)) {
                        ResultsTableData tableData = new ResultsTableData(resultsTable);
                        iterationStep.addOutputData(getFirstOutputSlot(), tableData.duplicate(progressInfo), progressInfo);
                    }
                }
            }
        }
    }

    @SetJIPipeDocumentation(name = "Filter titles", description = "Allows to filter for the window titles. By default it is set to <pre>title EQUALS \"Results\"</pre>, which selects windows with the name 'Results'. If you want to apply no filtering, set it to 'TRUE'.")
    @JIPipeParameter("title-filters")
    public JIPipeExpressionParameter getTitleFilterExpression() {
        return titleFilterExpression;
    }

    @JIPipeParameter("title-filters")
    public void setTitleFilterExpression(JIPipeExpressionParameter titleFilterExpression) {
        this.titleFilterExpression = titleFilterExpression;
    }
}
