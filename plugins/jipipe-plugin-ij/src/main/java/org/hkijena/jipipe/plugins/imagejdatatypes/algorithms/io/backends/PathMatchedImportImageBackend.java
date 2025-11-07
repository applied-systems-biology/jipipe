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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io.backends;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;

import java.nio.file.Path;

public abstract class PathMatchedImportImageBackend extends ImportImageBackend {

    private OptionalJIPipeExpressionParameter pathMatching = new OptionalJIPipeExpressionParameter();

    public PathMatchedImportImageBackend() {
        this.pathMatching.setContent(new JIPipeExpressionParameter(getDefaultPathMatching()));
    }

    public PathMatchedImportImageBackend(PathMatchedImportImageBackend other) {
        super(other);
        this.pathMatching = new OptionalJIPipeExpressionParameter(other.pathMatching);
    }

    @SetJIPipeDocumentation(name = "Custom path matching", description = "Allows to customize the patch matching")
    @AddJIPipeExpressionParameterVariable(key = "path", name = "Path", description = "The input path")
    @AddJIPipeExpressionParameterVariable(key = "parent", name = "Parent directory", description = "The input path's parent directory")
    @AddJIPipeExpressionParameterVariable(key = "filename", name = "File name", description = "The input path's file name")
    @JIPipeParameter("path-matching")
    public OptionalJIPipeExpressionParameter getPathMatching() {
        return pathMatching;
    }

    @JIPipeParameter("path-matching")
    public void setPathMatching(OptionalJIPipeExpressionParameter pathMatching) {
        this.pathMatching = pathMatching;
    }

    @Override
    public boolean canImport(Path path, JIPipeExpressionVariablesMap variablesMap) {
        if(!isEnabled()) {
            return false;
        }
        variablesMap.put("path", path.toString());
        variablesMap.put("parent", path.getParent().toString());
        variablesMap.put("filename", path.getFileName().toString());
        JIPipeExpressionParameter expression = pathMatching.getContentOrDefault(new JIPipeExpressionParameter(getDefaultPathMatching()));
        return expression.test(variablesMap);
    }

    public abstract String getDefaultPathMatching();
}
