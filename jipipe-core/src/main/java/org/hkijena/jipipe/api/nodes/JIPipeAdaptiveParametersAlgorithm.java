package org.hkijena.jipipe.api.nodes;

/**
 * An algorithm that has support for adaptive parameters
 */
public interface JIPipeAdaptiveParametersAlgorithm {
    JIPipeAdaptiveParameterSettings getAdaptiveParameterSettings();
}
