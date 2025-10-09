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

package org.hkijena.jipipe.api.data.documentation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Must be attached to {@link JIPipeData} classes to explain how the data type stores its data.
 * The documentation follows a subset of the <a href="https://www.researchobject.org/ro-crate/specification/1.2/index.html">RO-Crate Metadata Specification 1.2</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JIPipeDataStorageDocumentation {
    /**
     * The entities that are describing the contents of the data storage
     * @return the entities
     */
    Entity[] value();

    /**
     * Allows inheriting storage documentation from the given data types.
     * @return the inherited documentation classes.
     */
    Class<? extends JIPipeData>[] inherits() default {};
}
