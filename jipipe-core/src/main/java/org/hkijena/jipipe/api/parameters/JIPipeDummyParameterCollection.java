/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.parameters;

import com.google.common.eventbus.EventBus;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link JIPipeParameterCollection} that stores exactly one value (an object)
 * This is used in conjunction with {@link JIPipeManualParameterAccess}
 */
public class JIPipeDummyParameterCollection implements JIPipeParameterCollection, Consumer<Object>, Supplier<Object> {

    private final EventBus eventBus = new EventBus();
    private Object value;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void accept(Object o) {
        this.value = o;
        eventBus.post(new ParameterChangedEvent(this, "value"));
    }

    @Override
    public Object get() {
        return value;
    }
}
