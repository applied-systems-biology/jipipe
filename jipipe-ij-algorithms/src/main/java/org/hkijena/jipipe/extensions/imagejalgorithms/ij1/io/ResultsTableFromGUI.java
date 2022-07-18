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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io;

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.awt.*;

/**
 * Imports {@link ImagePlusData} from the GUI
 */
@JIPipeDocumentation(name = "Table from ImageJ", description = "Imports one or multiple active ImageJ results table windows into JIPipe")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File\nImport")
public class ResultsTableFromGUI extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter titleFilterExpression = new DefaultExpressionParameter("title EQUALS \"Results\"");

    public ResultsTableFromGUI(JIPipeNodeInfo info) {
        super(info);
    }

    public ResultsTableFromGUI(ResultsTableFromGUI other) {
        super(other);
        this.titleFilterExpression = new DefaultExpressionParameter(other.titleFilterExpression);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ExpressionVariables variableSet = new ExpressionVariables();
        for (Window window : WindowManager.getAllNonImageWindows()) {
            if (window instanceof TextWindow) {
                ResultsTable resultsTable = ((TextWindow) window).getResultsTable();
                if (resultsTable != null) {
                    String title = ((TextWindow) window).getTitle();
                    variableSet.set("title", title);
                    if (titleFilterExpression.test(variableSet)) {
                        ResultsTableData tableData = new ResultsTableData(resultsTable);
                        dataBatch.addOutputData(getFirstOutputSlot(), tableData.duplicate(progressInfo), progressInfo);
                    }
                }
            }
        }
    }

    @JIPipeDocumentation(name = "Filter titles", description = "Allows to filter for the window titles. By default it is set to <pre>title EQUALS \"Results\"</pre>, which selects windows with the name 'Results'. If you want to apply no filtering, set it to 'TRUE'.")
    @JIPipeParameter("title-filters")
    public DefaultExpressionParameter getTitleFilterExpression() {
        return titleFilterExpression;
    }

    @JIPipeParameter("title-filters")
    public void setTitleFilterExpression(DefaultExpressionParameter titleFilterExpression) {
        this.titleFilterExpression = titleFilterExpression;
    }
}
