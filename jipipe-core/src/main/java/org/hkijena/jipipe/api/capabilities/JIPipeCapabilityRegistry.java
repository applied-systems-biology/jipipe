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

package org.hkijena.jipipe.api.capabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A domain-agnostic registry of capabilities, domains and providers.
 * <p>
 * Plugins register capability definitions ({@link #register(JIPipeCapability)}),
 * the domains they belong to ({@link #registerDomain(JIPipeCapabilityDomain)})
 * and the concrete providers that can satisfy them
 * ({@link #registerProvider(JIPipeCapabilityProvider)}). Status is never
 * stored: {@link #getStatus(String)} resolves it on demand from the providers'
 * presence probes and the requirement graph, so a provider that appears or
 * disappears between two queries is picked up on the next query without
 * restarting anything.
 * <p>
 * A provider with the id {@code artifact:python3.v312} does not just
 * satisfy its own id. It also satisfies the less specific levels that are
 * derived from it: strip one trailing qualifier segment at a time
 * ({@code python3.v312 -> python3}), then strip the provider kind prefix
 * ({@code artifact:python3 -> python3}), and finally follow the alias edges
 * declared by any of the registered providers ({@code python3 -> python}).
 * A plain
 * query such as {@code python3} is matched against all of these derived
 * levels, so a qualified provider can satisfy a family-level capability.
 * A provider additionally matches when the query is the last dot-segment of
 * the base capability id of one of its ids: the base is the kind prefix
 * stripped with a single trailing qualifier segment removed, e.g. the query
 * {@code cellpose4} matches
 * {@code artifact:com.github.mouseland.cellpose4.cu124} (base
 * {@code com.github.mouseland.cellpose4}). This is how a short model name
 * reaches a qualified artifact id; it never matches an intermediate segment
 * (e.g. {@code github}) or a partial name (e.g. {@code model} does not match
 * {@code model2}).
 * <p>
 * The registry is not thread-safe; register everything during plugin
 * initialization and query from the UI afterwards.
 */
public class JIPipeCapabilityRegistry {
    private final Map<String, JIPipeCapabilityDomain> domains = new LinkedHashMap<>();
    private final Map<String, JIPipeCapability> capabilities = new LinkedHashMap<>();
    private final List<JIPipeCapabilityProvider> providers = new ArrayList<>();

    /**
     * Registers a domain. Duplicate ids are rejected.
     *
     * @throws IllegalArgumentException when the id is already registered
     */
    public void registerDomain(JIPipeCapabilityDomain domain) {
        Objects.requireNonNull(domain, "domain");
        if (domains.containsKey(domain.getId())) {
            throw new IllegalArgumentException("Duplicate capability domain id: " + domain.getId());
        }
        domains.put(domain.getId(), domain);
    }

    /**
     * Registers a capability definition. The requirement graph must be
     * acyclic; a cycle is rejected at registration with a message naming the
     * cycle, so the resolver can never hang.
     *
     * @throws IllegalArgumentException on a duplicate id or a circular
     *                                  requirement chain
     */
    public void register(JIPipeCapability capability) {
        Objects.requireNonNull(capability, "capability");
        if (capabilities.containsKey(capability.getId())) {
            throw new IllegalArgumentException("Duplicate capability id: " + capability.getId());
        }
        checkRequirementCycle(capability);
        capabilities.put(capability.getId(), capability);
    }

    /**
     * Registers a provider. Providers are evaluated in registration order.
     */
    public void registerProvider(JIPipeCapabilityProvider provider) {
        Objects.requireNonNull(provider, "provider");
        providers.add(provider);
    }

    /**
     * @return the registered domains in registration order
     */
    public List<JIPipeCapabilityDomain> getDomains() {
        return new ArrayList<>(domains.values());
    }

    /**
     * @param capabilityId the capability id to look up
     * @return the capability definition, or empty when it is not registered
     */
    public Optional<JIPipeCapability> find(String capabilityId) {
        return Optional.ofNullable(capabilities.get(capabilityId));
    }

    /**
     * Resolves the status of one capability id.
     * <p>
     * The query is matched against the ids provided by the registered
     * providers and against every level derived from them, and a provider
     * also matches when the query is the last dot-segment of one of its base
     * capability ids (e.g. {@code cellpose4} reaches
     * {@code artifact:com.github.mouseland.cellpose4.cu124}); the candidates
     * are then evaluated in registration order.
     * <ul>
     * <li>{@link JIPipeCapabilityStatus.State#AVAILABLE} when a present
     * provider satisfies the capability and every requirement of its
     * definition resolves to available;</li>
     * <li>{@link JIPipeCapabilityStatus.State#BLOCKED} when some requirement
     * is not available; the status then carries the flattened set of the
     * deepest missing requirements (the leaves of the unsatisfied chain),
     * never intermediate chain members, so {@code acp -> mcp -> embedding}
     * with nothing provided reports {@code [embedding]};</li>
     * <li>{@link JIPipeCapabilityStatus.State#NEEDS_SETUP} when only absent
     * providers satisfy the capability and at least one of them carries an
     * install hint whose method is not {@code none};</li>
     * <li>{@link JIPipeCapabilityStatus.State#MISSING} when no provider,
     * present or absent, can satisfy the capability or any of its derived
     * levels, or when the only absent providers cannot be installed.</li>
     * </ul>
     */
    public JIPipeCapabilityStatus getStatus(String capabilityId) {
        Objects.requireNonNull(capabilityId, "capabilityId");
        Set<String> levels = deriveLevels(capabilityId);
        List<JIPipeCapabilityProvider> candidates = new ArrayList<>();
        for (JIPipeCapabilityProvider provider : providers) {
            if (satisfiesQuery(provider, levels, capabilityId)) {
                candidates.add(provider);
            }
        }
        List<String> providedBy = new ArrayList<>();
        boolean present = false;
        boolean installable = false;
        for (JIPipeCapabilityProvider provider : candidates) {
            if (provider.getProbe().get() == JIPipeCapabilityStatus.State.AVAILABLE) {
                providedBy.add(provider.getId());
                present = true;
            } else if (provider.getInstallHint() != null && provider.getInstallHint().isInstallable()) {
                installable = true;
            }
        }
        // The requirement graph is walked even when no provider satisfies the
        // capability: a capability with no provider is still blocked when one
        // of its requirements is, e.g. acp with nothing installed. The list is
        // a set: two requirements with the same deepest missing capability
        // must not appear twice.
        Set<String> blocked = new LinkedHashSet<>();
        JIPipeCapability definition = capabilities.get(capabilityId);
        if (definition != null) {
            for (String requirement : definition.getRequires()) {
                JIPipeCapabilityStatus requirementStatus = getStatus(requirement);
                blocked.addAll(deepestBlockedOf(requirement, requirementStatus));
            }
        }
        JIPipeCapabilityStatus.State state;
        if (!blocked.isEmpty()) {
            state = JIPipeCapabilityStatus.State.BLOCKED;
        } else if (present) {
            state = JIPipeCapabilityStatus.State.AVAILABLE;
        } else if (installable) {
            state = JIPipeCapabilityStatus.State.NEEDS_SETUP;
        } else {
            state = JIPipeCapabilityStatus.State.MISSING;
        }
        return new JIPipeCapabilityStatus(capabilityId, state, new ArrayList<>(blocked), providedBy);
    }

    /**
     * Queries one capability id: its resolved status, the install hint to act
     * on, and the ranked list of providers that can satisfy it.
     * <p>
     * The candidates are ordered present-first, then by registration order,
     * so the first absent candidate is the default install target. {@code
     * install} is the hint of the highest-ranked absent candidate whose hint
     * is installable, or a {@code none} hint when there is none, so a query
     * always carries an answer an agent can act on.
     *
     * @param capabilityId the capability id to query
     * @return the query result for the id
     */
    public JIPipeCapabilityQuery query(String capabilityId) {
        Objects.requireNonNull(capabilityId, "capabilityId");
        Set<String> levels = deriveLevels(capabilityId);
        List<JIPipeCapabilityCandidate> candidateEntries = new ArrayList<>();
        for (JIPipeCapabilityProvider provider : providers) {
            if (!satisfiesQuery(provider, levels, capabilityId)) {
                continue;
            }
            boolean present = provider.getProbe().get() == JIPipeCapabilityStatus.State.AVAILABLE;
            candidateEntries.add(new JIPipeCapabilityCandidate(provider.getId(), provider.getDisplayName(),
                    provider.getInstallHint(), present));
        }
        // Present providers rank first; within each group the registration
        // order is kept, so the first absent entry is the default install
        // target. The sort is stable, so equal entries never reorder.
        Comparator<JIPipeCapabilityCandidate> byPresenceThenRegistration =
                Comparator.comparing(JIPipeCapabilityCandidate::present).reversed();
        List<JIPipeCapabilityCandidate> ranked = new ArrayList<>(candidateEntries);
        ranked.sort(byPresenceThenRegistration);
        JIPipeInstallHint install = JIPipeInstallHint.none();
        for (JIPipeCapabilityCandidate candidate : ranked) {
            if (!candidate.present() && candidate.hint() != null && candidate.hint().isInstallable()) {
                install = candidate.hint();
                break;
            }
        }
        JIPipeCapabilityStatus status = getStatus(capabilityId);
        return new JIPipeCapabilityQuery(capabilityId, status, install, ranked);
    }

    /**
     * Builds the plan for making one capability available.
     * <p>
     * The plan carries one {@link JIPipeCapabilityRequirementPlan.JIPipeRequirementRow}
     * per capability that is not available yet: the requested capability
     * itself when it is not available, plus its missing requirements,
     * expanded transitively. Expansion is depth-first in the declared
     * requirement order and a capability is attributed to the parent that
     * first reaches it: when {@code acp} requires {@code [mcp, embedding]}
     * and {@code mcp} requires {@code embedding} and none is available, the
     * rows are {@code acp} (reason {@code acp}), {@code mcp} (reason
     * {@code acp}) and {@code embedding} (reason {@code mcp}, discovered
     * through {@code mcp} before the direct edge of {@code acp}). Each row's
     * {@code options} are the ranked candidates of a {@link #query(String)}
     * for the capability, and the preselected provider is the first candidate
     * whose install hint method is not {@code none}, or null when no candidate
     * carries an installable hint.
     * <p>
     * {@code installOrder} is the topological order of the missing
     * requirements, requirements first; the requested capability itself is
     * excluded, so the order lists exactly what has to be installed to
     * satisfy the request.
     *
     * @param requestedId the capability id to plan the install for
     * @return the install plan for the id
     */
    public JIPipeCapabilityRequirementPlan planInstall(String requestedId) {
        Objects.requireNonNull(requestedId, "requestedId");
        if (getStatus(requestedId).state() == JIPipeCapabilityStatus.State.AVAILABLE) {
            return new JIPipeCapabilityRequirementPlan(requestedId, List.of(), List.of());
        }
        RowBuilder rowBuilder = new RowBuilder(this);
        rowBuilder.addRow(requestedId, requestedId);
        JIPipeCapability definition = capabilities.get(requestedId);
        if (definition != null) {
            for (String requirement : definition.getRequires()) {
                expandRequirement(requestedId, requirement, rowBuilder);
            }
        }
        return new JIPipeCapabilityRequirementPlan(requestedId, rowBuilder.rows(),
                resolveInstallOrder(requestedId));
    }

    /**
     * Resolves the order in which the missing requirements of one capability
     * have to be installed: a DFS post-order over the requirement graph,
     * restricted to the capabilities that are not available, so every
     * requirement appears before the capability that requires it. The
     * requested capability itself is excluded.
     * <p>
     * The cycle guard is defensive: {@link #register(JIPipeCapability)}
     * already rejects circular requirement graphs, so a reachable cycle can
     * only appear when the registry is built up in a way that bypasses it.
     *
     * @param id the capability whose missing requirements are ordered
     * @return the missing requirement ids in install order, requirements first
     * @throws IllegalStateException when a cycle is reachable from {@code id}
     *                               despite the registration guard
     */
    public List<String> resolveInstallOrder(String id) {
        Objects.requireNonNull(id, "id");
        Set<String> installed = new LinkedHashSet<>();
        Set<String> inProgress = new LinkedHashSet<>();
        JIPipeCapability definition = capabilities.get(id);
        if (definition != null) {
            for (String requirement : definition.getRequires()) {
                orderInstall(requirement, installed, inProgress);
            }
        }
        return new ArrayList<>(installed);
    }

    /**
     * Appends one row to the plan when the capability is not available yet;
     * the availability check is done per capability, never per parent.
     */
    private void expandRequirement(String parent, String requirement, RowBuilder rowBuilder) {
        if (!rowBuilder.markVisited(requirement)) {
            return;
        }
        if (getStatus(requirement).state() != JIPipeCapabilityStatus.State.AVAILABLE) {
            rowBuilder.addRow(requirement, parent);
            JIPipeCapability definition = capabilities.get(requirement);
            if (definition != null) {
                for (String nested : definition.getRequires()) {
                    expandRequirement(requirement, nested, rowBuilder);
                }
            }
        }
    }

    /**
     * Walks one requirement and its missing requirements in DFS post-order,
     * appending them to {@code installed} (the result), and fails when the
     * walk reaches a capability that is already on the current path.
     */
    private void orderInstall(String requirement, Set<String> installed, Set<String> inProgress) {
        if (installed.contains(requirement)) {
            return;
        }
        if (!inProgress.add(requirement)) {
            throw new IllegalStateException("Circular capability requirements reached while resolving the install order: "
                    + requirement + " is already being expanded");
        }
        if (getStatus(requirement).state() != JIPipeCapabilityStatus.State.AVAILABLE) {
            JIPipeCapability definition = capabilities.get(requirement);
            if (definition != null) {
                for (String nested : definition.getRequires()) {
                    orderInstall(nested, installed, inProgress);
                }
            }
            installed.add(requirement);
        }
        inProgress.remove(requirement);
    }

    /**
     * @return true when the provider satisfies a query for {@code id}: one of
     * its provided ids equals {@code id} or derives down to it (qualified
     * segments, kind prefix, alias edges), or {@code id} is the last
     * dot-segment of the base capability id of a provided id (e.g. the query
     * {@code cellpose4} matches
     * {@code artifact:com.github.mouseland.cellpose4.cu124}, whose base
     * {@code com.github.mouseland.cellpose4} ends in {@code .cellpose4}; the
     * query {@code github} does not)
     */
    private boolean satisfiesQuery(JIPipeCapabilityProvider provider, Set<String> levels, String id) {
        for (String provided : provider.getProvides()) {
            if (levels.contains(provided) || lastSegmentMatches(provided, id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true when {@code id} is the last dot-segment of the base
     * capability id of {@code provided}, e.g. {@code cellpose4} for
     * {@code artifact:com.github.mouseland.cellpose4.cu124} but not
     * {@code github}. A single-segment base has no last segment to match
     * here; equality with a provided id is covered by the derivation match
     * ({@code levels}), not by this rule
     */
    private boolean lastSegmentMatches(String provided, String id) {
        String base = baseCapabilityId(provided);
        int dot = base.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return base.substring(dot + 1).equals(id);
    }

    /**
     * @return the base capability id of {@code provided}: the kind prefix
     * stripped and a single trailing qualifier segment removed when present,
     * e.g. {@code artifact:python3.v312} bases to {@code python3} and
     * {@code artifact:com.github.mouseland.cellpose4.cu124} bases to
     * {@code com.github.mouseland.cellpose4}
     */
    private String baseCapabilityId(String provided) {
        String plain = provided;
        for (JIPipeProviderKind kind : JIPipeProviderKind.values()) {
            if (plain.startsWith(kind.getPrefix()) && plain.length() > kind.getPrefix().length()) {
                plain = plain.substring(kind.getPrefix().length());
                break;
            }
        }
        int dot = plain.lastIndexOf('.');
        if (dot > 0 && dot < plain.length() - 1) {
            plain = plain.substring(0, dot);
        }
        return plain;
    }

    /**
     * @return the deepest missing part of one requirement: its flattened
     * {@code blockedBy} when the requirement is blocked, the requirement's
     * own id when it is not available and has no deeper missing part, and
     * nothing when it is available
     */
    private List<String> deepestBlockedOf(String requirement, JIPipeCapabilityStatus requirementStatus) {
        if (requirementStatus.state() == JIPipeCapabilityStatus.State.AVAILABLE) {
            return List.of();
        }
        if (requirementStatus.state() == JIPipeCapabilityStatus.State.BLOCKED) {
            return new ArrayList<>(requirementStatus.blockedBy());
        }
        return List.of(requirement);
    }

    /**
     * Walks the requirement graph of the candidate (without inserting it
     * yet) and fails with a message naming the cycle when the candidate's
     * own id would become reachable from itself.
     */
    private void checkRequirementCycle(JIPipeCapability candidate) {
        List<String> path = new ArrayList<>();
        path.add(candidate.getId());
        Set<String> visited = new HashSet<>();
        if (walkRequirements(candidate.getId(), candidate.getRequires(), path, visited)) {
            throw new IllegalArgumentException("Circular capability requirements: " + String.join(" -> ", path));
        }
    }

    /**
     * @return true when {@code candidateId} is reachable through
     * {@code requires}, and appends the offending edge to {@code path}
     */
    private boolean walkRequirements(String candidateId, List<String> requires, List<String> path, Set<String> visited) {
        for (String next : requires) {
            if (next.equals(candidateId)) {
                path.add(next);
                return true;
            }
            if (!visited.add(next)) {
                continue;
            }
            JIPipeCapability definition = capabilities.get(next);
            List<String> childRequires = definition == null ? List.of() : definition.getRequires();
            path.add(next);
            if (walkRequirements(candidateId, childRequires, path, visited)) {
                return true;
            }
            path.remove(path.size() - 1);
        }
        return false;
    }

    /**
     * Computes every level that a query for {@code id} should match: the id
     * itself plus, for each provider that can satisfy it, the ids it
     * provides and every level derived from them.
     */
    private Set<String> deriveLevels(String id) {
        Set<String> levels = new LinkedHashSet<>();
        levels.add(id);
        // Alias edges are consulted registry-wide: which provider happens to
        // be matched must not change the derivation.
        List<JIPipeCapabilityProvider.Alias> aliases = allAliases();
        for (JIPipeCapabilityProvider provider : providers) {
            for (String provided : provider.getProvides()) {
                Set<String> derived = deriveFrom(provided, aliases);
                if (provided.equals(id) || derived.contains(id)) {
                    levels.add(provided);
                    levels.addAll(derived);
                    break;
                }
            }
        }
        return levels;
    }

    /**
     * @return the alias edges declared by every registered provider, in
     * declaration order; derivation consults them all, not only the edges of
     * the provider whose id is being derived
     */
    private List<JIPipeCapabilityProvider.Alias> allAliases() {
        Set<JIPipeCapabilityProvider.Alias> result = new LinkedHashSet<>();
        for (JIPipeCapabilityProvider provider : providers) {
            result.addAll(provider.getAliases());
        }
        return new ArrayList<>(result);
    }

    /**
     * Computes every level derived from one provider id by repeatedly
     * stripping qualifier segments, stripping kind prefixes and following
     * alias edges, until nothing new remains.
     */
    private Set<String> deriveFrom(String providerId, List<JIPipeCapabilityProvider.Alias> aliases) {
        Set<String> derived = new LinkedHashSet<>();
        Set<String> frontier = new LinkedHashSet<>();
        frontier.add(providerId);
        while (!frontier.isEmpty()) {
            Set<String> next = new LinkedHashSet<>();
            for (String current : frontier) {
                for (String successor : successors(current, aliases)) {
                    if (derived.add(successor)) {
                        next.add(successor);
                    }
                }
            }
            frontier = next;
        }
        return derived;
    }

    /**
     * @return the direct successors of {@code current}: one level up by
     * stripping the trailing qualifier segment (if any), one level up by
     * stripping the provider kind prefix (if any), and the alias targets
     * (if any)
     */
    private List<String> successors(String current, List<JIPipeCapabilityProvider.Alias> aliases) {
        List<String> result = new ArrayList<>();
        int dot = current.lastIndexOf('.');
        if (dot > 0 && dot < current.length() - 1) {
            result.add(current.substring(0, dot));
        }
        for (JIPipeProviderKind kind : JIPipeProviderKind.values()) {
            if (current.startsWith(kind.getPrefix()) && current.length() > kind.getPrefix().length()) {
                result.add(current.substring(kind.getPrefix().length()));
                break;
            }
        }
        for (JIPipeCapabilityProvider.Alias alias : aliases) {
            if (alias.from().equals(current)) {
                result.addAll(alias.to());
            }
        }
        return result;
    }

    /**
     * Accumulates the rows of one install plan in discovery order and keeps
     * track of which capabilities have already been visited, so a capability
     * reachable through several parents contributes exactly one row,
     * attributed to the first parent that reaches it.
     */
    private static final class RowBuilder {
        private final JIPipeCapabilityRegistry registry;
        private final Set<String> visited = new LinkedHashSet<>();
        private final List<JIPipeCapabilityRequirementPlan.JIPipeRequirementRow> rows = new ArrayList<>();

        private RowBuilder(JIPipeCapabilityRegistry registry) {
            this.registry = registry;
        }

        /**
         * @return true when this capability has not been visited before
         */
        private boolean markVisited(String capabilityId) {
            return visited.add(capabilityId);
        }

        /**
         * Appends the row of one missing capability, with its ranked
         * candidates and the preselected provider.
         */
        private void addRow(String capabilityId, String reason) {
            JIPipeCapabilityQuery query = registry.query(capabilityId);
            String preselected = null;
            for (JIPipeCapabilityCandidate candidate : query.candidates()) {
                if (candidate.hint() != null && candidate.hint().isInstallable()) {
                    preselected = candidate.providerId();
                    break;
                }
            }
            rows.add(new JIPipeCapabilityRequirementPlan.JIPipeRequirementRow(capabilityId, reason,
                    query.candidates(), preselected));
        }

        private List<JIPipeCapabilityRequirementPlan.JIPipeRequirementRow> rows() {
            return rows;
        }
    }
}
