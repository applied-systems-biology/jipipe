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

package org.hkijena.jipipe.api.microservice;

import java.util.List;

/**
 * A microservice with a managed lifecycle.
 *
 * <p>Microservices have a lifecycle state ({@link MicroserviceState}), can declare
 * dependencies on other microservices (for lifecycle ordering), and fire
 * {@link MicroserviceStateChangeEvent}s when their state changes.</p>
 *
 * <p>The framework does not include a registry. Calling code is responsible for
 * creating and managing microservice instances. Global services (e.g., AI service,
 * server service) are managed by their respective {@code JIPipeServiceComponent}s.
 * Node-local services can be created transiently during pipeline runs.</p>
 */
public interface Microservice {
    String getName();

    void start();
    void stop();

    MicroserviceState getState();
    String getStateDetail();
    boolean isReady();

    List<Microservice> getDependencies();
    void addDependency(Microservice dependency);

    MicroserviceStateChangeEventEmitter getStateChangeEventEmitter();
}
