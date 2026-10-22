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

package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.nodes.taxonomy.FrameworkRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInferencePattern;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The canonical taxonomy registry: the {@link FunctionRegistry} (the curated
 * function tree) and the {@link FrameworkRegistry} (the flat framework set)
 * of the node taxonomy, owned by this service component.
 * <p>
 * Plugins register their own taxonomy members (functions, frameworks) through
 * this component; the methods 1:1 delegate to the underlying registries.
 * <p>
 * {@link #getFunctionRegistry()} and {@link #getFrameworkRegistry()} expose
 * the raw registries for the {@code TaxonomyValidationReport} and for the
 * nodes-service delegates that need direct registry access.
 * <p>
 * Name inference: every registered function declares weighted keyword
 * patterns ({@link JIPipeNodeTaxonomyFunction#getInferencePatterns()});
 * this component aggregates them into a keyword index and resolves
 * display names to the highest-scoring function via
 * {@link #inferFunction(String)}, falling back to the function set by
 * {@link #setInferenceFallback(JIPipeNodeTaxonomyFunction)}.
 */
public final class JIPipeNodeTaxonomyServiceComponent extends JIPipeServiceComponent {

    private final FunctionRegistry functionRegistry = new FunctionRegistry();
    private final FrameworkRegistry frameworkRegistry = new FrameworkRegistry();

    // name inference (weighted keyword index over all registered functions)
    private String fallbackFunctionId;
    private List<IndexedKeyword> keywordIndex;
    private volatile boolean indexDirty = true;

    public JIPipeNodeTaxonomyServiceComponent(JIPipeService service) {
        super(service);
    }

    /**
     * Registers a taxonomy function in the function registry.
     * Re-registering an existing id replaces the previous function.
     *
     * @param function the function to register (its stable id is the key)
     */
    public void registerFunction(JIPipeNodeTaxonomyFunction function) {
        functionRegistry.register(function);
        indexDirty = true;
    }

    /**
     * Unregisters a taxonomy function. Does nothing if the id is not registered.
     *
     * @param id the id of the function to remove
     * @return the removed function, or {@code null} if none was registered under that id
     */
    public JIPipeNodeTaxonomyFunction unregisterFunction(String id) {
        indexDirty = true;
        return functionRegistry.unregister(id);
    }

    /**
     * @param id the id of the function to look up
     * @return the registered function with that id, or {@code null} if none is registered
     */
    public JIPipeNodeTaxonomyFunction functionById(String id) {
        return functionRegistry.byId(id);
    }

    /**
     * @return an unmodifiable view of all registered functions
     */
    public Set<JIPipeNodeTaxonomyFunction> allFunctions() {
        return functionRegistry.all();
    }

    /**
     * @return an unmodifiable set of all registered top-level functions (those with no parent)
     */
    public Set<JIPipeNodeTaxonomyFunction> topLevelFunctions() {
        return functionRegistry.topLevel();
    }

    /**
     * @param parent the parent function
     * @return the registered direct children of the given function, ordered by name for deterministic iteration
     */
    public List<JIPipeNodeTaxonomyFunction> childrenOf(JIPipeNodeTaxonomyFunction parent) {
        return functionRegistry.children(parent);
    }

    /**
     * Resolves the base hue of a function: the nearest ancestor (including the
     * function itself) whose color hue is non-null.
     *
     * @param function the function
     * @return the nearest non-null hue in the parent chain
     * @throws IllegalStateException if the chain contains a cycle or ends without a hue
     */
    public float baseHue(JIPipeNodeTaxonomyFunction function) {
        return functionRegistry.baseHue(function);
    }

    /**
     * Resolves how far below the hue owner a function is, in number of tree edges.
     *
     * @param function the function
     * @return the number of edges below the nearest hue-owning ancestor; 0 for the hue owner itself
     * @throws IllegalStateException if the chain contains a cycle or ends without a hue
     */
    public int shadeDepth(JIPipeNodeTaxonomyFunction function) {
        return functionRegistry.shadeDepth(function);
    }

    /**
     * Validates the registered function tree.
     *
     * @return human-readable error strings; empty if the registry is valid
     */
    public Set<String> validateFunctions() {
        return functionRegistry.validate();
    }

    /**
     * Registers a taxonomy framework in the framework registry.
     * Re-registering an existing id replaces the previous framework for lookup purposes.
     *
     * @param framework the framework to register (its stable id is the key)
     */
    public void registerFramework(JIPipeNodeTaxonomyFramework framework) {
        frameworkRegistry.register(framework);
    }

    /**
     * @param id the id of the framework to look up
     * @return the registered framework with that id, or {@code null} if none is registered
     */
    public JIPipeNodeTaxonomyFramework frameworkById(String id) {
        return frameworkRegistry.byId(id);
    }

    /**
     * @return an unmodifiable snapshot of all registered frameworks, one entry per id
     */
    public Set<JIPipeNodeTaxonomyFramework> allFrameworks() {
        return frameworkRegistry.all();
    }

    /**
     * @return the function registry owned by this component. Exists for the
     * {@code TaxonomyValidationReport} and the nodes-service delegates that
     * need direct registry access
     */
    public FunctionRegistry getFunctionRegistry() {
        return functionRegistry;
    }

    /**
     * @return the framework registry owned by this component. Exists for the
     * {@code TaxonomyValidationReport} and the nodes-service delegates that
     * need direct registry access
     */
    public FrameworkRegistry getFrameworkRegistry() {
        return frameworkRegistry;
    }

    /**
     * Sets the inference fallback: the function returned for names without a
     * keyword match (including null/blank names). The fallback is stored as an
     * <em>id</em> and resolved against the live registry at inference time, so
     * the node simply stays unassigned (soft fail, {@code null}) if the
     * fallback function is not (or no longer) registered.
     *
     * @param fallback the fallback function, or {@code null} to clear the fallback
     */
    public void setInferenceFallback(JIPipeNodeTaxonomyFunction fallback) {
        fallbackFunctionId = fallback == null ? null : fallback.getId();
    }

    /**
     * Infers the primary taxonomy function for a discovered node's display
     * name: weighted keyword scoring over the aggregate index of ALL
     * registered functions (see
     * {@link JIPipeNodeTaxonomyFunction#getInferencePatterns()}); the highest
     * total wins, ties break to the lexicographically smallest function id.
     * Names without a keyword match (including null/blank names) yield the
     * inference fallback, resolved live by id — {@code null} (soft fail) if no
     * fallback is set or it is not registered. Never throws.
     *
     * @param name the node's display name, may be {@code null}
     * @return the inferred function, the fallback, or {@code null}
     */
    public JIPipeNodeTaxonomyFunction inferFunction(String name) {
        String lower = name == null ? null : name.toLowerCase(Locale.ROOT);
        JIPipeNodeTaxonomyFunction winner = lower == null || lower.isBlank()
                ? null
                : scoreName(lower, ensureIndex(), this::functionById);
        if (winner != null) {
            return winner;
        }
        return fallbackFunctionId == null ? null : functionById(fallbackFunctionId);
    }

    /**
     * (Re)builds the aggregate keyword index from every registered function's
     * inference patterns: one {@link IndexedKeyword} per pattern, with the
     * keyword lowercased with Locale.ROOT (blank keywords are skipped — they
     * would match every name).
     */
    private List<IndexedKeyword> ensureIndex() {
        if (keywordIndex != null && !indexDirty) {
            return keywordIndex;
        }
        List<IndexedKeyword> index = new ArrayList<>();
        for (JIPipeNodeTaxonomyFunction function : functionRegistry.all()) {
            for (JIPipeNodeTaxonomyInferencePattern pattern : function.getInferencePatterns()) {
                if (pattern.keyword() == null || pattern.keyword().isBlank()) {
                    continue;
                }
                index.add(new IndexedKeyword(
                        pattern.keyword().toLowerCase(Locale.ROOT),
                        function.getId(),
                        pattern.weight()));
            }
        }
        keywordIndex = index;
        indexDirty = false;
        return index;
    }

    /**
     * One keyword occurrence in the aggregate inference index.
     *
     * @param keyword    the lowercased keyword (matched as a case-insensitive substring)
     * @param functionId the stable id of the function the keyword contributes to
     * @param weight     the contribution of one occurrence to that function's score
     */
    public record IndexedKeyword(String keyword, String functionId, float weight) {
    }

    /**
     * Scores {@code name} (already lowercased with Locale.ROOT, or null) against
     * the aggregate keyword index.
     *
     * @param name         the display name, or null
     * @param index        every (keyword, functionId, weight) contribution
     * @param functionById live-id lookup (returns null for unregistered ids)
     * @return the highest-scoring REGISTERED function — ties break to the
     * lexicographically smallest function id; unregistered ids are
     * skipped (never thrown); no matched keyword -> null
     */
    public static JIPipeNodeTaxonomyFunction scoreName(String name,
                                                       List<IndexedKeyword> index,
                                                       Function<String, JIPipeNodeTaxonomyFunction> functionById) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Map<String, Float> scores = new HashMap<>();
        for (IndexedKeyword entry : index) {
            if (entry.keyword() == null || !name.contains(entry.keyword())) {
                continue;
            }
            // Unregistered ids are skipped (soft fail — never thrown)
            if (functionById.apply(entry.functionId()) == null) {
                continue;
            }
            scores.merge(entry.functionId(), entry.weight(), Float::sum);
        }
        JIPipeNodeTaxonomyFunction winner = null;
        String winnerId = null;
        float winnerScore = 0f;
        for (Map.Entry<String, Float> entry : scores.entrySet()) {
            if (winnerId == null
                    || entry.getValue() > winnerScore
                    || (entry.getValue() == winnerScore && entry.getKey().compareTo(winnerId) < 0)) {
                winnerId = entry.getKey();
                winnerScore = entry.getValue();
                winner = functionById.apply(winnerId);
            }
        }
        return winner;
    }
}
