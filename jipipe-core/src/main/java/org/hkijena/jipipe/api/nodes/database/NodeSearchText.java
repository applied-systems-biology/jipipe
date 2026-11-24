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

package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.contrib.libstemmer.ext.EnglishStemmer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The shared search-text vocabulary of the node database (#1404): the word
 * tokenizer the enhanced search uses, plus the content-word view and the
 * non-fuzzy coverage ladder that future consumers (the description-quality
 * gate of MR B) reuse, so every consumer judges "does X mention Y" the
 * same way the search matches.
 */
public final class NodeSearchText {

    /**
     * The word split of the enhanced search: whitespace and the separators
     * {@code _ - / .}, plus camelCase boundaries. Kept bit-identical with
     * the search's token stream.
     */
    public static final Pattern SPLIT_WORD_PATTERN = Pattern.compile(
            "[\\s_\\-/\\.]+|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])");

    /**
     * The stopword set of {@link #contentWords(String)}: English glue words
     * that carry no node identity.
     */
    public static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "of", "and", "or", "to", "in", "for", "with",
            "is", "are", "be", "as", "on", "by", "it", "its", "this", "that",
            "from", "into", "if", "you", "your", "can", "will");

    private NodeSearchText() {
    }

    /**
     * Splits the given text into words (see {@link #SPLIT_WORD_PATTERN}).
     */
    public static List<String> splitWords(String s) {
        String[] w = SPLIT_WORD_PATTERN.split(s);
        List<String> out = new ArrayList<>(w.length);
        for (String x : w) {
            if (!x.isEmpty()) {
                out.add(x);
            }
        }
        return out;
    }

    /**
     * The lowercase word set of the given text (no stopword filtering).
     */
    public static Set<String> wordSet(String text) {
        Set<String> out = new java.util.HashSet<>();
        for (String word : splitWords(text == null ? "" : text)) {
            out.add(word.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    /**
     * The content words of the given text: the split words, lowercased,
     * without stopwords, single-character words, and pure-digit words.
     * Order preserved, duplicates kept.
     */
    public static List<String> contentWords(String text) {
        List<String> out = new ArrayList<>();
        for (String word : splitWords(text == null ? "" : text)) {
            String lower = word.toLowerCase(Locale.ROOT);
            if (STOPWORDS.contains(lower) || lower.length() < 2 || lower.matches("[0-9]+")) {
                continue;
            }
            out.add(lower);
        }
        return out;
    }

    /**
     * The non-fuzzy coverage ladder of the enhanced search: whether the
     * given (lowercase) word is covered by the token set - exact, token
     * prefix in either direction, stem equality, or substring.
     */
    public static boolean coveredBy(String word, Set<String> tokens) {
        if (word == null || tokens == null || tokens.isEmpty()) {
            return false;
        }
        String w = word.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (token.equals(w)) {
                return true;
            }
        }
        for (String token : tokens) {
            if (token.startsWith(w) || w.startsWith(token)) {
                return true;
            }
        }
        String stem = stem(w);
        for (String token : tokens) {
            if (stem(token).equals(stem)) {
                return true;
            }
        }
        for (String token : tokens) {
            if (token.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stems a token with the same stemmer the search uses; unstemmable
     * input is returned unchanged.
     */
    public static String stem(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }
        EnglishStemmer s = new EnglishStemmer();
        s.setCurrent(token);
        if (s.stem()) {
            return s.getCurrent();
        }
        return token;
    }
}
