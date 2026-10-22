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
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;

import java.util.List;
import java.util.Set;

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
 * Inference support is not part of this component yet.
 */
public final class JIPipeNodeTaxonomyServiceComponent extends JIPipeServiceComponent {

    private final FunctionRegistry functionRegistry = new FunctionRegistry();
    private final FrameworkRegistry frameworkRegistry = new FrameworkRegistry();

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
    }

    /**
     * Unregisters a taxonomy function. Does nothing if the id is not registered.
     *
     * @param id the id of the function to remove
     * @return the removed function, or {@code null} if none was registered under that id
     */
    public JIPipeNodeTaxonomyFunction unregisterFunction(String id) {
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
}
