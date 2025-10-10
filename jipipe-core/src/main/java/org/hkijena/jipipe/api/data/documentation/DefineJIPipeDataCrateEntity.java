package org.hkijena.jipipe.api.data.documentation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An entity, as described by the <a href="https://www.researchobject.org/ro-crate/specification/1.2/index.html">RO-Crate Metadata Specification 1.2</a>.
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DefineJIPipeDataCrateEntities.class)
public @interface DefineJIPipeDataCrateEntity {
    /**
     * The unique ID of the entity within the data container.
     * <p>
     *    In the case of type=File
     *    MUST be either a fully resolved path to a file, relative to the root of the data container (according to RO-Crate 1.2)
     *    OR can be also a glob by using the glob: protocol for matching the FIRST file that matches the GLOB operation.
     *    OR can be also a regex by using the regex: protocol for matching the FIRST file that matches the regular expression.
     *    OR can be a path spec where variables/placeholders are defined through {variable_name} using the path: protocol.
     *    MUST begin with a ./ (excluding protocol)
     * </p>
     * <p>
     *    In the case of type=Dataset (i.e., a directory)
     *    MUST be either a fully resolved path to a file, relative to the root of the data container (according to RO-Crate 1.2)
     *    OR can be also a glob by using the glob: protocol for matching the FIRST directory that matches the GLOB operation.
     *    OR can be also a regex by using the regex: protocol for matching the FIRST directory that matches the regular expression.
     *    OR can be a path spec where variables/placeholders are defined through {variable_name} using the path: protocol.
     *    MUST end with a /
     *    MUST begin with a ./ (excluding protocol)
     * </p>
     * <p>
     *     Examples:
     *     <ul>
     *         <li>./table.csv</li>
     *         <li>glob:./*.csv</li>
     *         <li>path:./{name}-{role}.csv</li>
     *         <li>regex:\\./image.*\\.(png|tif|bmp)</li>
     *         <li>./directory/</li>
     *         <li>path:./directory{num}/</li>
     *     </ul>
     * </p>
     * @return the entity ID
     */
    String id();

    /**
     * Returns the entity type
     * @return the entity type
     */
    JIPipeDataCrateEntityType type();

    /**
     * Human-readable name. Can be different from ID.
     * @return the human-readable name
     */
    String name();

    /**
     * Human-readable description
     * @return the description
     */
    String description();

    /**
     * The encoding format. CAN be a MIME-Type or a URL. See <a href="https://schema.org/encodingFormat">schema.org documentation</a>.
     * You can use the {@link EncodingFormats} constants to access common MIME-Types.
     * @return the encoding format
     */
    String[] encodingFormat() default {};

    /**
     * Allows marking the entity as optional, suggested, or mandatory
     * @return if the entity should be present or not
     */
    JIPipeDataCrateEntityPresence presence() default JIPipeDataCrateEntityPresence.Mandatory;
}
