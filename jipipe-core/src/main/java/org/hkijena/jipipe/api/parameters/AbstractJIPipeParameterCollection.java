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

package org.hkijena.jipipe.api.parameters;

/**
 * {@link JIPipeParameterCollection} that implements the event emitters
 */
public class AbstractJIPipeParameterCollection implements JIPipeParameterCollection, JIPipeParameterCollection.ParameterChangedEventListener, JIPipeParameterCollection.ParameterStructureChangedEventListener, JIPipeParameterCollection.ParameterUIChangedEventListener {

    private final ParameterChangedEventEmitter parameterChangedEventEmitter = new ParameterChangedEventEmitter();
    private final ParameterStructureChangedEventEmitter parameterStructureChangedEventEmitter = new ParameterStructureChangedEventEmitter();
    private final ParameterUIChangedEventEmitter parameterUIChangedEventEmitter = new ParameterUIChangedEventEmitter();

    @Override
    public ParameterChangedEventEmitter getParameterChangedEventEmitter() {
        return parameterChangedEventEmitter;
    }

    @Override
    public ParameterStructureChangedEventEmitter getParameterStructureChangedEventEmitter() {
        return parameterStructureChangedEventEmitter;
    }

    @Override
    public ParameterUIChangedEventEmitter getParameterUIChangedEventEmitter() {
        return parameterUIChangedEventEmitter;
    }

    /**
     * Registers a sub-parameter instance to pass {@link ParameterStructureChangedEvent} via this algorithm's events
     *
     * @param subParameter the sub-parameter
     */
    public void registerSubParameter(JIPipeParameterCollection subParameter) {
        subParameter.getParameterChangedEventEmitter().subscribeWeak(this);
        subParameter.getParameterStructureChangedEventEmitter().subscribeWeak(this);
        subParameter.getParameterUIChangedEventEmitter().subscribeWeak(this);
    }

    /**
     * Registers a sub-parameter instance to pass {@link ParameterStructureChangedEvent} via this algorithm's events
     *
     * @param subParameters the sub-parameters
     */
    public void registerSubParameters(JIPipeParameterCollection... subParameters) {
        for (JIPipeParameterCollection subParameter : subParameters) {
            registerSubParameter(subParameter);
        }
    }

    /**
     * Triggered when the parameter structure of this collection was changed
     *
     * @param event generated event
     */
    @Override
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        if (event.getVisitors().contains(this))
            return;
        event.getVisitors().add(this);
        getParameterStructureChangedEventEmitter().emit(event);
    }

    /**
     * Triggered when the parameter UI structure of this collection was changed
     *
     * @param event generated event
     */
    @Override
    public void onParameterUIChanged(ParameterUIChangedEvent event) {
        if (event.getVisitors().contains(this))
            return;
        event.getVisitors().add(this);
        getParameterUIChangedEventEmitter().emit(event);
    }

    /**
     * Triggered when a parameter of this collection was changed
     *
     * @param event the generated event
     */
    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        if (event.getSource() != this) {
            getParameterChangedEventEmitter().emit(event);
        }
    }
}
