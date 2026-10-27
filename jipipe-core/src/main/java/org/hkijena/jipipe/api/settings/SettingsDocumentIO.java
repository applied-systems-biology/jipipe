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

package org.hkijena.jipipe.api.settings;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Serialization for {@link SettingsDocument}s: convert a document to and from
 * its JSON representation.
 * <p>
 * The round-trip guarantee is the core contract here: deserializing a
 * serialized document preserves <em>every</em> block and sub-path — including
 * blocks whose kind or id this version does not know, and unknown sub-paths
 * inside known blocks. No content is filtered or dropped on the way in, so a
 * settings file written by a newer version survives load→save verbatim.
 * <p>
 * This class performs no file IO: it operates on JSON trees and strings only;
 * writing the pretty string to disk is the caller's concern.
 */
public final class SettingsDocumentIO {
    private static final String SCHEMA_FIELD = "$schema";
    private static final String META_FIELD = "$meta";
    private static final int DEFAULT_SCHEMA_VERSION = 2;

    private SettingsDocumentIO() {
    }

    /**
     * Serializes the document to a JSON tree.
     * <p>
     * The returned node is a deep copy: mutating it does not affect the
     * document.
     *
     * @param doc the document to serialize
     * @return a deep copy of the document's root
     */
    public static ObjectNode toJson(SettingsDocument doc) {
        Objects.requireNonNull(doc, "doc");
        return (ObjectNode) doc.root().deepCopy();
    }

    /**
     * Deserializes a settings document from a JSON tree.
     * <p>
     * The input is deep-copied into a fresh document: mutating the returned
     * document does not affect the input tree, and vice versa. {@code $schema}
     * is applied to the document (defaulting to {@value #DEFAULT_SCHEMA_VERSION}
     * when absent) and {@code $meta} is preserved as-is. No filtering happens:
     * every block and sub-path of the input is kept, known or unknown.
     *
     * @param scope the scope the resulting document belongs to
     * @param node the JSON object tree to deserialize; must be an object
     * @param registeredBlock a predicate that receives a canonical block path
     *                        and returns {@code true} when the block is
     *                        registered in this version
     * @return the deserialized document
     */
    public static SettingsDocument fromJson(SettingsScope scope, JsonNode node, Predicate<String> registeredBlock) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(registeredBlock, "registeredBlock");
        if (!node.isObject()) {
            throw new IllegalArgumentException("A settings document must be a JSON object, but got: " + node.getNodeType());
        }
        ObjectNode source = (ObjectNode) node;
        SettingsDocument document = new SettingsDocument(scope, path -> {
            SettingsBlockPath blockPath = new SettingsBlockPath(path);
            return blockPath.kind() != null && registeredBlock.test(blockPath.path());
        });
        JsonNode schema = source.get(SCHEMA_FIELD);
        document.setSchemaVersion(schema == null ? DEFAULT_SCHEMA_VERSION : schema.asInt(DEFAULT_SCHEMA_VERSION));
        JsonNode meta = source.get(META_FIELD);
        if (meta != null) {
            document.setBlock(META_FIELD, meta.deepCopy());
        }
        source.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            if (!field.equals(SCHEMA_FIELD) && !field.equals(META_FIELD)) {
                document.setBlock(field, entry.getValue().deepCopy());
            }
        });
        return document;
    }

    /**
     * Serializes the document to a pretty-printed JSON string, using the same
     * default pretty printer as the existing settings writer, so the output
     * matches the legacy {@code settings.json} formatting closely.
     *
     * @param doc the document to serialize
     * @return the pretty-printed JSON string
     */
    public static String toPrettyString(SettingsDocument doc) {
        Objects.requireNonNull(doc, "doc");
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = JsonUtils.getObjectMapper().getFactory().createGenerator(writer)) {
            generator.useDefaultPrettyPrinter();
            JsonUtils.getObjectMapper().writeTree(generator, doc.root());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return writer.toString();
    }
}
