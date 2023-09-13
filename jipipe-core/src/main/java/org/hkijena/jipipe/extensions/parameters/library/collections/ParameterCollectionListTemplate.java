package org.hkijena.jipipe.extensions.parameters.library.collections;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Sets the template class for {@link ParameterCollectionList}.
 * Required to preserve annotations when creating the UI.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterCollectionListTemplate {
    Class<? extends JIPipeParameterCollection> value();
}
