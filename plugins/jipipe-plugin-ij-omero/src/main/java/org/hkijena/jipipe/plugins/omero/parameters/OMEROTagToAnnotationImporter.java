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

package org.hkijena.jipipe.plugins.omero.parameters;

import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.DataObject;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.omero.util.OMEROUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.List;
import java.util.Set;

public class OMEROTagToAnnotationImporter extends AbstractJIPipeParameterCollection {
    private OptionalTextAnnotationNameParameter tagsToListAnnotation = new OptionalTextAnnotationNameParameter("OMERO:Tags", true);
    private StringQueryExpression tagsToListFilter = new StringQueryExpression("true");

    public OMEROTagToAnnotationImporter() {

    }

    public OMEROTagToAnnotationImporter(OMEROTagToAnnotationImporter other) {
        this.tagsToListFilter = new StringQueryExpression(other.tagsToListFilter);
        this.tagsToListAnnotation = new OptionalTextAnnotationNameParameter(other.tagsToListAnnotation);
    }

    @SetJIPipeDocumentation(name = "Convert tags into single annotation", description = "If enabled, write all tag names into a single annotation (as list). " +
            "The name of the annotation is determined by the value of this parameter")
    @JIPipeParameter("tags-to-list-annotation")
    public OptionalTextAnnotationNameParameter getTagsToListAnnotation() {
        return tagsToListAnnotation;
    }

    @JIPipeParameter("tags-to-list-annotation")
    public void setTagsToListAnnotation(OptionalTextAnnotationNameParameter tagsToListAnnotation) {
        this.tagsToListAnnotation = tagsToListAnnotation;
    }

    @SetJIPipeDocumentation(name = "Convert tags into single annotation (filter)", description = "Allows to filter out tags that will be included into 'Convert tags into single annotation'")
    @JIPipeParameter("tags-to-list-annotation-filter")
    public StringQueryExpression getTagsToListFilter() {
        return tagsToListFilter;
    }

    @JIPipeParameter("tags-to-list-annotation-filter")
    public void setTagsToListFilter(StringQueryExpression tagsToListFilter) {
        this.tagsToListFilter = tagsToListFilter;
    }

    public void createAnnotations(List<JIPipeTextAnnotation> target, MetadataFacility metadata, SecurityContext context, DataObject dataObject) throws DSOutOfServiceException, DSAccessException {
        if (tagsToListAnnotation.isEnabled()) {
            Set<String> tags = OMEROUtils.getTags(metadata, context, dataObject);
            List<String> filteredTags = tagsToListFilter.queryAll(tags, new JIPipeExpressionVariablesMap());
            target.add(tagsToListAnnotation.createAnnotation(JsonUtils.toJsonString(filteredTags)));
        }
    }
}
