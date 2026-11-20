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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads a KDB corpus: one YAML file per entry (design spec 5.2).
 * <p>
 * The loader is headless and registry free: it parses YAML and reconstructs
 * typed values through the kind's slot schema, but it never touches the node
 * registry (NODE_REF and ENTRY_REF resolution is the integrity gate's job).
 * <p>
 * Malformed content never throws: every defect becomes a {@link KdbFinding}.
 * Files whose classification is broken (unknown kind, unknown domain, kind
 * and path disagreement, wrong path layout, unparseable YAML) are skipped
 * with a finding; classifiable files still load, minus the values that could
 * not narrow. The entry key is always derived from the path, never from a
 * YAML field.
 */
public class KdbEntryLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Loads all entries below a corpus root. The root is expected to contain
     * {@code <domain>/<kind-directory>/<entry-name>.yaml} paths.
     *
     * @param root the corpus root
     * @return the load result with entries and findings
     */
    public KdbLoadResult loadAll(Path root) {
        List<KdbEntry> entries = new ArrayList<>();
        List<KdbFinding> findings = new ArrayList<>();
        if (root == null || !Files.isDirectory(root)) {
            findings.add(new KdbFinding(KdbFindingCode.EMPTY_CORPUS, null,
                    "Corpus root " + root + " does not exist or is not a directory"));
            return new KdbLoadResult(entries, findings);
        }
        List<Path> files = collectYamlFiles(root, findings);
        if (files.isEmpty()) {
            findings.add(new KdbFinding(KdbFindingCode.EMPTY_CORPUS, null,
                    "Corpus root " + root + " contains no .yaml entry files"));
        }
        Set<String> seenEntryKeys = new HashSet<>();
        for (Path file : files) {
            loadEntry(root, file, entries, findings, seenEntryKeys);
        }
        return new KdbLoadResult(entries, findings);
    }

    private List<Path> collectYamlFiles(Path root, List<KdbFinding> findings) {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yaml"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException | UncheckedIOException e) {
            findings.add(new KdbFinding(KdbFindingCode.EMPTY_CORPUS, null,
                    "Corpus root " + root + " could not be walked: " + e.getMessage()));
            return Collections.emptyList();
        }
    }

    private void loadEntry(Path root, Path file, List<KdbEntry> entries, List<KdbFinding> findings,
                           Set<String> seenEntryKeys) {
        Path relative = root.relativize(file);
        if (relative.getNameCount() != 3) {
            findings.add(new KdbFinding(KdbFindingCode.UNKNOWN_KIND, null,
                    joinPath(relative) + ": path does not match <domain>/<kind-directory>/<name>.yaml"));
            return;
        }
        String domainSegment = relative.getName(0).toString();
        String kindDirectory = relative.getName(1).toString();
        String fileName = relative.getName(2).toString();
        String entryName = fileName.substring(0, fileName.length() - ".yaml".length());
        String entryKey = domainSegment + "/" + kindDirectory + "/" + entryName;

        Optional<KdbDomain> domain = KdbDomain.byId(domainSegment);
        if (!domain.isPresent()) {
            findings.add(new KdbFinding(KdbFindingCode.UNKNOWN_KIND, entryKey,
                    entryKey + ": unknown domain directory '" + domainSegment + "'"));
            return;
        }
        Optional<KdbEntryKind> pathKind = KdbEntryKind.byDirectoryName(kindDirectory);
        if (!pathKind.isPresent()) {
            findings.add(new KdbFinding(KdbFindingCode.UNKNOWN_KIND, entryKey,
                    entryKey + ": unknown kind directory '" + kindDirectory + "'"));
            return;
        }
        JsonNode document;
        try {
            document = YAML_MAPPER.readTree(file.toFile());
        } catch (IOException | UncheckedIOException e) {
            findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                    entryKey + ": could not parse YAML: " + firstLine(e.getMessage())));
            return;
        }
        if (document == null || !document.isObject()) {
            findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                    entryKey + ": entry file must contain a YAML mapping"));
            return;
        }

        // The kind comes from the path; a declared kind field must agree.
        KdbEntryKind kind = pathKind.get();
        JsonNode kindNode = document.get("kind");
        if (kindNode != null && !kindNode.isNull()) {
            if (!kindNode.isTextual()) {
                findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                        entryKey + ": 'kind' must be a string"));
                return;
            }
            Optional<KdbEntryKind> declaredKind = KdbEntryKind.byId(kindNode.textValue());
            if (!declaredKind.isPresent()) {
                findings.add(new KdbFinding(KdbFindingCode.UNKNOWN_KIND, entryKey,
                        entryKey + ": unknown kind '" + kindNode.textValue() + "'"));
                return;
            }
            if (declaredKind.get() != kind) {
                findings.add(new KdbFinding(KdbFindingCode.KIND_PATH_MISMATCH, entryKey,
                        entryKey + ": declared kind '" + kindNode.textValue()
                                + "' does not match the kind directory '" + kindDirectory + "'"));
                return;
            }
        }

        String title = null;
        JsonNode titleNode = document.get("title");
        if (titleNode != null && !titleNode.isNull() && titleNode.isTextual()
                && !titleNode.textValue().isBlank()) {
            title = titleNode.textValue();
        } else {
            findings.add(new KdbFinding(KdbFindingCode.MISSING_TITLE, entryKey,
                    entryKey + ": no usable 'title' (must be a non-empty string)"));
        }

        String body = null;
        JsonNode bodyNode = document.get("body");
        if (bodyNode != null && !bodyNode.isNull()) {
            if (bodyNode.isTextual()) {
                body = bodyNode.textValue();
            } else {
                findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                        entryKey + ": 'body' must be a string"));
            }
        }

        List<String> aliases = loadStrings(document.get("aliases"), "aliases", entryKey, findings);
        List<String> sourceRefs = loadStrings(document.get("sourceRefs"), "sourceRefs", entryKey, findings);
        String sinceVersion = loadVersion(document.get("sinceVersion"), "sinceVersion", entryKey, findings);
        String untilVersion = loadVersion(document.get("untilVersion"), "untilVersion", entryKey, findings);

        Map<String, List<Object>> slotValues = new LinkedHashMap<>();
        JsonNode slotsNode = document.get("slots");
        if (slotsNode != null && !slotsNode.isNull()) {
            if (!slotsNode.isObject()) {
                findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                        entryKey + ": 'slots' must be a mapping"));
            } else {
                Iterator<Map.Entry<String, JsonNode>> slotFields = slotsNode.fields();
                while (slotFields.hasNext()) {
                    Map.Entry<String, JsonNode> slotField = slotFields.next();
                    loadSlot(kind, entryKey, slotField.getKey(), slotField.getValue(), slotValues, findings);
                }
            }
        }

        Map<String, Object> properties = loadProperties(kind, document.get("properties"), entryKey, findings);

        if (!seenEntryKeys.add(entryKey)) {
            findings.add(new KdbFinding(KdbFindingCode.DUPLICATE_ENTRY_KEY, entryKey,
                    entryKey + ": duplicate entry key; keeping the first occurrence"));
            return;
        }
        entries.add(new KdbEntry(entryKey, UUID.randomUUID(), kind, domain.get(), title, slotValues,
                body, aliases, sinceVersion, untilVersion, sourceRefs, properties));
    }

    private void loadSlot(KdbEntryKind kind, String entryKey, String slotName, JsonNode node,
                          Map<String, List<Object>> slotValues, List<KdbFinding> findings) {
        Optional<KdbSlotDef> slotDef = kind.getSlot(slotName);
        if (!slotDef.isPresent()) {
            findings.add(new KdbFinding(KdbFindingCode.UNKNOWN_SLOT, entryKey,
                    entryKey + ": unknown slot '" + slotName + "' for kind '" + kind.getId() + "'"));
            return;
        }
        if (node == null || node.isNull()) {
            return;
        }
        List<Object> values = new ArrayList<>();
        if (node.isArray()) {
            if (slotDef.get().cardinality() == KdbCardinality.SINGLE) {
                findings.add(new KdbFinding(KdbFindingCode.CARDINALITY_VIOLATION, entryKey,
                        entryKey + ": slot '" + slotName + "' is SINGLE but received " + node.size() + " values"));
                return;
            }
            int index = 0;
            for (JsonNode element : node) {
                if (!element.isNull()) {
                    Optional<Object> narrowed = slotDef.get().type().narrow(element);
                    if (narrowed.isPresent()) {
                        values.add(narrowed.get());
                    } else {
                        findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                                entryKey + ": slot '" + slotName + "[" + index + "]' cannot narrow to "
                                        + slotDef.get().type()));
                    }
                }
                index++;
            }
        } else if (node.isObject()) {
            findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                    entryKey + ": slot '" + slotName + "' must be a scalar or a list"));
            return;
        } else {
            Optional<Object> narrowed = slotDef.get().type().narrow(node);
            if (narrowed.isPresent()) {
                values.add(narrowed.get());
            } else {
                findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                        entryKey + ": slot '" + slotName + "' cannot narrow to " + slotDef.get().type()));
                return;
            }
        }
        if (!values.isEmpty()) {
            slotValues.put(slotName, values);
        }
    }

    private List<String> loadStrings(JsonNode node, String field, String entryKey, List<KdbFinding> findings) {
        if (node == null || node.isNull()) {
            return Collections.emptyList();
        }
        if (node.isTextual()) {
            return List.of(node.textValue());
        }
        if (!node.isArray()) {
            findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                    entryKey + ": '" + field + "' must be a string or a list of strings"));
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        int index = 0;
        for (JsonNode element : node) {
            if (element.isNull()) {
                index++;
                continue;
            }
            if (element.isTextual()) {
                values.add(element.textValue());
            } else {
                findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                        entryKey + ": '" + field + "[" + index + "]' must be a string"));
            }
            index++;
        }
        return values;
    }

    private String loadVersion(JsonNode node, String field, String entryKey, List<KdbFinding> findings) {
        if (node == null || node.isNull()) {
            return null;
        }
        Optional<Object> narrowed = KdbValueType.VERSION.narrow(node);
        if (narrowed.isPresent()) {
            return (String) narrowed.get();
        }
        findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                entryKey + ": '" + field + "' must be a version string such as 6.0.0"));
        return null;
    }

    private Map<String, Object> loadProperties(KdbEntryKind kind, JsonNode node, String entryKey,
                                               List<KdbFinding> findings) {
        if (node == null || node.isNull()) {
            return Collections.emptyMap();
        }
        if (!node.isObject()) {
            findings.add(new KdbFinding(KdbFindingCode.TYPE_MISMATCH, entryKey,
                    entryKey + ": 'properties' must be a mapping"));
            return Collections.emptyMap();
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> propertyFields = node.fields();
        while (propertyFields.hasNext()) {
            Map.Entry<String, JsonNode> propertyField = propertyFields.next();
            String key = propertyField.getKey();
            if (kind.getSlot(key).isPresent()) {
                findings.add(new KdbFinding(KdbFindingCode.PROPERTY_SHADOWS_SLOT, entryKey,
                        entryKey + ": property '" + key + "' shadows a schema slot"));
            }
            properties.put(key, YAML_MAPPER.convertValue(propertyField.getValue(), Object.class));
        }
        return properties;
    }

    private static String joinPath(Path relative) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < relative.getNameCount(); i++) {
            names.add(relative.getName(i).toString());
        }
        return String.join("/", names);
    }

    private static String firstLine(String text) {
        if (text == null) {
            return "";
        }
        int newline = text.indexOf('\n');
        return newline >= 0 ? text.substring(0, newline) : text;
    }
}
