package org.hkijena.jipipe.desktop.app.commandbar;

import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The fuzzy name matching of the command bar (issue #1346, Task 7).
 * <p>
 * This is the same matching approach {@code JIPipeEnhancedNodeDatabaseSearch}
 * applies to node names, extracted into a pure utility so the bar does not
 * couple to the node database: a per-query-token ladder plus the
 * simmetrics {@code jaroWinkler} similarity and the identical
 * word-splitting pattern (whitespace/separator runs plus camel-case
 * boundaries).
 * <p>
 * <b>The ladder.</b> Each query token is scored against the name, taking the
 * best rung it reaches:
 * <ul>
 * <li>exact word match: 1.0</li>
 * <li>word prefix: 0.9</li>
 * <li>substring of a word: 0.8</li>
 * <li>acronym: 0.7 - the token's letters are the first letters of successive
 * words of the name, in order. This is what makes "cb" match "Command Bar"
 * (and, via the camel-case split, "rp" match "RunPipeline")</li>
 * <li>strong fuzzy: jaroWinkler similarity of 0.85 or more against a word,
 * scored {@code 0.65 + (sim - 0.85)}. The similarity reaches 1.0 only for
 * identical strings, which the exact rung catches first, so the fuzzy rung
 * is always strictly below 0.8 and can never outrank the substring rung</li>
 * </ul>
 * <b>Every token must match.</b> The score is the mean of the per-token
 * rung scores; if ANY token reaches no rung, the score is 0.0. Matching
 * therefore requires all query tokens to be found in the name, while the
 * name need not be covered fully.
 * <p>
 * <b>Ranking is the caller's job.</b> This class only decides whether and
 * how well a name matches a query. The command bar orders its results by
 * descending score, with recency as the secondary criterion and the
 * registry position last.
 * <p>
 * Query tokens are split the same way as name words, so a mixed-case query
 * token splits at camel-case boundaries: "cOpy" is scored as the tokens
 * "c" and "opy".
 */
public final class JIPipeDesktopCommandBarSearch {

    /**
     * The word-splitting pattern, byte-for-byte identical to the one
     * {@code JIPipeEnhancedNodeDatabaseSearch} uses: runs of whitespace and
     * separator characters, plus the two camel-case boundary clauses.
     * <p>
     * The node search lowercases the name BEFORE splitting, which leaves its
     * camel-case clauses inert (no uppercase remains). The command bar
     * instead splits the ORIGINAL text and lowercases the words afterwards,
     * so the camel-case clauses do their job: "RunPipeline" -> "run",
     * "pipeline" and "HTMLParser" -> "html", "parser".
     */
    private static final Pattern SPLIT_WORD_PATTERN = Pattern.compile(
            "[\\s_\\-/\\.]+|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])");
    /**
     * The strong-fuzzy metric, like the node database search
     * (simmetrics {@code StringMetrics.jaroWinkler()}).
     */
    private static final StringMetric JW = StringMetrics.jaroWinkler();
    /**
     * The jaroWinkler similarity a token must reach for the fuzzy rung.
     */
    private static final double MIN_FUZZY_SIM = 0.85;
    /**
     * The rung score of an acronym match.
     */
    private static final double ACRONYM_SCORE = 0.7;

    private JIPipeDesktopCommandBarSearch() {
    }

    /**
     * Scores how well {@code query} matches the name {@code name}.
     *
     * @param query the search query (split into tokens like the name)
     * @param name  the name to match against
     * @return the mean of the per-query-token rung scores (see the class
     *         javadoc), or 0.0 when the query is empty or blank, when the
     *         name has no words, or when any token reaches no rung. An
     *         empty/blank query is 0.0 by contract: the command bar never
     *         calls this with an empty query (an empty query lists
     *         everything instead of matching nothing).
     */
    public static double score(String query, String name) {
        List<String> queryTokens = words(query);
        if (queryTokens.isEmpty()) {
            return 0.0;
        }
        List<String> nameWords = words(name);
        if (nameWords.isEmpty()) {
            return 0.0;
        }
        String nameFirstLetters = firstLetters(nameWords);
        double sum = 0.0;
        for (String token : queryTokens) {
            double best = 0.0;
            for (String word : nameWords) {
                best = Math.max(best, wordRung(token, word));
            }
            if (isAcronym(token, nameFirstLetters)) {
                best = Math.max(best, ACRONYM_SCORE);
            }
            if (best <= 0.0) {
                return 0.0;
            }
            sum += best;
        }
        return sum / queryTokens.size();
    }

    /**
     * Whether the name matches the query at all.
     *
     * @param query the search query
     * @param name  the name to match against
     * @return {@code true} when {@link #score(String, String) score} is
     *         positive
     */
    public static boolean matches(String query, String name) {
        return score(query, name) > 0.0;
    }

    /**
     * Splits text into lower-cased words with {@link #SPLIT_WORD_PATTERN}.
     * The split runs on the original (case-preserved) text so the
     * camel-case clauses apply; the words are lower-cased afterwards so
     * every rung below is case-insensitive.
     */
    private static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String part : SPLIT_WORD_PATTERN.split(text)) {
            if (!part.isEmpty()) {
                out.add(part.toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    /**
     * The first letter of every word, in order ("Command Bar" -> "cb"). The
     * acronym rung tests the query token against this string.
     */
    private static String firstLetters(List<String> nameWords) {
        StringBuilder sb = new StringBuilder(nameWords.size());
        for (String word : nameWords) {
            sb.append(word.charAt(0));
        }
        return sb.toString();
    }

    /**
     * The best rung a token reaches against ONE word of the name, or 0.0.
     * The acronym rung is not part of this: it is name-level, so it is
     * handled by {@link #score(String, String)}.
     */
    private static double wordRung(String token, String word) {
        if (token.equals(word)) {
            return 1.0;
        }
        if (word.startsWith(token)) {
            return 0.9;
        }
        if (word.contains(token)) {
            return 0.8;
        }
        double sim = JW.compare(token, word);
        if (sim >= MIN_FUZZY_SIM) {
            return 0.65 + (sim - MIN_FUZZY_SIM);
        }
        return 0.0;
    }

    /**
     * Whether the token is an acronym of the name: its letters are a
     * subsequence of the words' first letters, in order. A token of a single
     * letter never qualifies here (it is caught by the word-prefix /
     * substring rungs instead).
     */
    private static boolean isAcronym(String token, String firstLetters) {
        if (token.length() < 2) {
            return false;
        }
        int i = 0;
        for (int j = 0; j < firstLetters.length() && i < token.length(); j++) {
            if (firstLetters.charAt(j) == token.charAt(i)) {
                i++;
            }
        }
        return i == token.length();
    }
}
