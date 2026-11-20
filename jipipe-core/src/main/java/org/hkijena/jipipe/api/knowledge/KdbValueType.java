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

package org.hkijena.jipipe.api.knowledge;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The closed set of value types a KDB slot can carry (design spec 5.1).
 * <p>
 * Each type owns its narrowing rule in {@link #narrow(JsonNode)}: loading
 * reconstructs typed values through the kind's slot schema at the boundary,
 * and a value that cannot narrow produces a finding, never a guess.
 * Narrowing an absent node is the caller's decision; {@code narrow} only
 * classifies a node that is present.
 */
public enum KdbValueType {

    /**
     * Free prose. Narrows from a YAML string.
     */
    TEXT,

    /**
     * A node registration id (stable string literal). Narrows from a YAML
     * string. Resolving the id against the node registry happens later.
     */
    NODE_REF,

    /**
     * The entry key of another corpus entry. Narrows from a YAML string.
     * Resolving the key happens later.
     */
    ENTRY_REF,

    /**
     * A number. Narrows via the Jackson number node: integral values become
     * {@link Long}, all other numbers {@link Double}.
     */
    NUMBER,

    /**
     * A version string such as {@code 6.0.0} or {@code 6.1.0-SNAPSHOT}.
     * Narrows from a YAML string shaped like a version: digits and dots,
     * optionally followed by a qualifier separated with a dash or a plus.
     */
    VERSION;

    private static final Pattern VERSION_SHAPE = Pattern.compile("\\d+(\\.\\d+)*([-+][0-9A-Za-z.]+)?");

    /**
     * Narrows a node to this value type.
     *
     * @param node a present YAML/JSON node (may be a null node, which does not narrow)
     * @return the narrowed value, or empty if the node cannot narrow to this type
     */
    public Optional<Object> narrow(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        switch (this) {
            case TEXT:
            case NODE_REF:
            case ENTRY_REF:
                return node.isTextual() ? Optional.of(node.textValue()) : Optional.empty();
            case NUMBER:
                if (node.isIntegralNumber()) {
                    return Optional.of(node.longValue());
                }
                return node.isNumber() ? Optional.of(node.doubleValue()) : Optional.empty();
            case VERSION:
                if (node.isTextual() && VERSION_SHAPE.matcher(node.textValue()).matches()) {
                    return Optional.of(node.textValue());
                }
                return Optional.empty();
            default:
                return Optional.empty();
        }
    }
}
