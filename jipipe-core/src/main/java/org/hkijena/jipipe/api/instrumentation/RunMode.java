package org.hkijena.jipipe.api.instrumentation;

/**
 * Defines how a pipeline or node should be run.
 */
public enum RunMode {
    /** Run to target node, cache only target output in memory. */
    UPDATE_CACHE,
    /** Run to target node, cache all intermediate outputs in memory. */
    CACHE_INTERMEDIATE,
    /** Run to predecessors of target (skip target itself), cache in memory. */
    UPDATE_PREDECESSOR_CACHE,
    /** Run entire pipeline, cache in memory only. */
    PIPELINE_CACHE,
    /** Run entire pipeline, write to output folder. */
    PIPELINE_FILESYSTEM,
    /** Run entire pipeline, discard results. */
    PIPELINE_DISCARD
}
