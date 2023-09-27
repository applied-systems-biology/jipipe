package org.hkijena.jipipe.extensions.omero.parameters;

import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.DataObject;
import omero.gateway.model.ImageData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.List;
import java.util.Set;

public class OMEROTagToAnnotationImporter extends AbstractJIPipeParameterCollection {
    private OptionalAnnotationNameParameter tagsToListAnnotation = new OptionalAnnotationNameParameter("OMERO:Tags", true);
    private StringQueryExpression tagsToListFilter = new StringQueryExpression("true");

    public OMEROTagToAnnotationImporter() {

    }

    public OMEROTagToAnnotationImporter(OMEROTagToAnnotationImporter other) {
        this.tagsToListFilter = new StringQueryExpression(other.tagsToListFilter);
        this.tagsToListAnnotation = new OptionalAnnotationNameParameter(other.tagsToListAnnotation);
    }

    @JIPipeDocumentation(name = "Convert tags into single annotation", description = "If enabled, write all tag names into a single annotation (as list). " +
            "The name of the annotation is determined by the value of this parameter")
    @JIPipeParameter("tags-to-list-annotation")
    public OptionalAnnotationNameParameter getTagsToListAnnotation() {
        return tagsToListAnnotation;
    }

    @JIPipeParameter("tags-to-list-annotation")
    public void setTagsToListAnnotation(OptionalAnnotationNameParameter tagsToListAnnotation) {
        this.tagsToListAnnotation = tagsToListAnnotation;
    }

    @JIPipeDocumentation(name = "Convert tags into single annotation (filter)", description = "Allows to filter out tags that will be included into 'Convert tags into single annotation'")
    @JIPipeParameter("tags-to-list-annotation-filter")
    public StringQueryExpression getTagsToListFilter() {
        return tagsToListFilter;
    }

    @JIPipeParameter("tags-to-list-annotation-filter")
    public void setTagsToListFilter(StringQueryExpression tagsToListFilter) {
        this.tagsToListFilter = tagsToListFilter;
    }

    public void createAnnotations(List<JIPipeTextAnnotation> target, MetadataFacility metadata, SecurityContext context, DataObject dataObject) throws DSOutOfServiceException, DSAccessException {
        if(tagsToListAnnotation.isEnabled()) {
            Set<String> tags = OMEROUtils.getTags(metadata, context, dataObject);
            List<String> filteredTags = tagsToListFilter.queryAll(tags, new ExpressionVariables());
            target.add(tagsToListAnnotation.createAnnotation(JsonUtils.toJsonString(filteredTags)));
        }
    }
}
