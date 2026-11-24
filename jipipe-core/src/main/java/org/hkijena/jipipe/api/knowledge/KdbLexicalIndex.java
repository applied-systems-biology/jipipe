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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The lexical retrieval lane: an immutable inverted index over the loaded
 * corpus with BM25 scoring (design spec 5.5, lane 1).
 * <p>
 * One document per entry aggregates the tokens of the entry key, the title,
 * the aliases, the TEXT and NODE_REF slot values, and the body. ENTRY_REF
 * values are relation material and are not indexed here; NUMBER and VERSION
 * values carry no lexical surface.
 * <p>
 * The tokenizer is the matching contract: tokens are runs of letters and
 * digits plus the punctuation characters hyphen, colon, and underscore. Node
 * ids such as {@code ij3-cellpose-import} and error strings such as
 * {@code error:xyz-42} therefore survive as single tokens and must be
 * carried by a query verbatim; a node id is never split on its hyphens.
 * Tokens are lowercased for matching; the original strings are never
 * modified, so display surfaces keep their original text.
 * <p>
 * An index is immutable once built: it is constructed from the corpus, and
 * every later call is a read.
 */
public final class KdbLexicalIndex {

    /**
     * The BM25 term frequency saturation parameter.
     */
    public static final double K1 = 1.2;

    /**
     * The BM25 document length normalization parameter.
     */
    public static final double B = 0.75;

    private final Map<String, Map<String, Integer>> termFrequencies;
    private final Map<String, Integer> documentLengths;
    private final Map<String, Integer> documentFrequencies;
    private final double averageDocumentLength;

    private KdbLexicalIndex(Map<String, Map<String, Integer>> termFrequencies,
                            Map<String, Integer> documentLengths,
                            Map<String, Integer> documentFrequencies,
                            double averageDocumentLength) {
        this.termFrequencies = termFrequencies;
        this.documentLengths = documentLengths;
        this.documentFrequencies = documentFrequencies;
        this.averageDocumentLength = averageDocumentLength;
    }

    /**
     * Builds the index over the given entries. The entries are not modified
     * and not retained: only their token statistics are kept.
     *
     * @param entries the loaded corpus entries
     * @return the immutable index
     */
    public static KdbLexicalIndex build(List<KdbEntry> entries) {
        Map<String, Map<String, Integer>> termFrequencies = new LinkedHashMap<>();
        Map<String, Integer> documentLengths = new LinkedHashMap<>();
        for (KdbEntry entry : entries) {
            List<String> tokens = documentTokens(entry);
            Map<String, Integer> frequencies = new LinkedHashMap<>();
            for (String token : tokens) {
                frequencies.merge(token, 1, Integer::sum);
            }
            termFrequencies.put(entry.getEntryKey(), frequencies);
            documentLengths.put(entry.getEntryKey(), tokens.size());
        }
        Map<String, Integer> documentFrequencies = new HashMap<>();
        for (Map<String, Integer> frequencies : termFrequencies.values()) {
            for (String token : frequencies.keySet()) {
                documentFrequencies.merge(token, 1, Integer::sum);
            }
        }
        double averageDocumentLength = documentLengths.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        return new KdbLexicalIndex(termFrequencies, documentLengths, documentFrequencies, averageDocumentLength);
    }

    /**
     * Tokenizes a text for matching: runs of letters and digits plus hyphen,
     * colon, and underscore, lowercased. Every separator (whitespace and all
     * other punctuation) ends a token, so node ids and error strings stay
     * whole.
     *
     * @param text the text, or null
     * @return the tokens, possibly empty
     */
    static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        text.codePoints().forEach(codePoint -> {
            if (isTokenCharacter(codePoint)) {
                current.appendCodePoint(codePoint);
            } else if (current.length() > 0) {
                tokens.add(current.toString().toLowerCase(Locale.ROOT));
                current.setLength(0);
            }
        });
        if (current.length() > 0) {
            tokens.add(current.toString().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }

    private static boolean isTokenCharacter(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '-' || codePoint == ':' || codePoint == '_';
    }

    private static List<String> documentTokens(KdbEntry entry) {
        List<String> tokens = new ArrayList<>();
        tokens.addAll(tokenize(entry.getEntryKey()));
        if (entry.getTitle() != null) {
            tokens.addAll(tokenize(entry.getTitle()));
        }
        for (String alias : entry.getAliases()) {
            tokens.addAll(tokenize(alias));
        }
        for (Map.Entry<String, List<Object>> slot : entry.getSlotValues().entrySet()) {
            Optional<KdbSlotDef> slotDef = entry.getKind().getSlot(slot.getKey());
            if (!slotDef.isPresent()) {
                // Unknown slots are loader findings, not index material.
                continue;
            }
            KdbValueType type = slotDef.get().type();
            if (type != KdbValueType.TEXT && type != KdbValueType.NODE_REF) {
                continue;
            }
            for (Object value : slot.getValue()) {
                if (value instanceof String string) {
                    tokens.addAll(tokenize(string));
                }
            }
        }
        if (entry.getBody() != null) {
            tokens.addAll(tokenize(entry.getBody()));
        }
        return tokens;
    }

    /**
     * Searches the index: BM25 over the query tokens, best first. Entries
     * that match no query token score zero and are not returned; the
     * remaining entries are ordered by score descending with the entry key
     * ascending as the stable tiebreak, capped at {@code topK}.
     *
     * @param query the query, or null or blank for an empty result
     * @param topK  the maximum number of results; non-positive yields nothing
     * @return the scored entries, best first
     */
    public List<ScoredEntry> search(String query, int topK) {
        if (topK <= 0) {
            return List.of();
        }
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        List<ScoredEntry> scored = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> document : termFrequencies.entrySet()) {
            double score = 0.0;
            for (String token : queryTokens) {
                Integer frequency = document.getValue().get(token);
                if (frequency == null) {
                    continue;
                }
                score += inverseDocumentFrequency(token)
                        * termScore(frequency, documentLengths.get(document.getKey()));
            }
            if (score > 0.0) {
                scored.add(new ScoredEntry(document.getKey(), score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredEntry::score).reversed()
                .thenComparing(ScoredEntry::entryKey));
        if (scored.size() <= topK) {
            return scored;
        }
        return new ArrayList<>(scored.subList(0, topK));
    }

    private double inverseDocumentFrequency(String token) {
        int documentFrequency = documentFrequencies.getOrDefault(token, 0);
        int total = documentLengths.size();
        return Math.log(1.0 + (total - documentFrequency + 0.5) / (documentFrequency + 0.5));
    }

    private double termScore(int frequency, int documentLength) {
        double normalization = 1.0 - B + B * documentLength / averageDocumentLength;
        return frequency * (K1 + 1.0) / (frequency + K1 * normalization);
    }

    /**
     * One lexical hit.
     *
     * @param entryKey the entry key of the matching entry
     * @param score    the BM25 score
     */
    public record ScoredEntry(String entryKey, double score) {
    }
}
