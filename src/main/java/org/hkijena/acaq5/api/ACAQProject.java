package org.hkijena.acaq5.api;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.ACAQSampleAddedEvent;
import org.hkijena.acaq5.api.events.ACAQSampleRemovedEvent;
import org.hkijena.acaq5.api.events.ACAQSampleRenamedEvent;

/**
 * An ACAQ5 project.
 * It contains all information to setup and run an analysis
 */
public class ACAQProject {
    private EventBus eventBus = new EventBus();
    private BiMap<String, ACAQProjectSample> samples = HashBiMap.create();
    private ACAQMutableSlotConfiguration preprocessingOutputConfiguration = new ACAQMutableSlotConfiguration(true, false);
    private ACAQMutableSlotConfiguration analysisOutputConfiguration = new ACAQMutableSlotConfiguration(true, false);
    private ACAQAlgorithmGraph analysis = new ACAQAlgorithmGraph();

    public ACAQProject() {
        analysis.insertNode(new ACAQPreprocessingOutput(new ACAQInputAsOutputSlotConfiguration(preprocessingOutputConfiguration)));
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ACAQAlgorithmGraph getAnalysis() {
        return analysis;
    }

    public ACAQMutableSlotConfiguration getPreprocessingOutputConfiguration() {
        return preprocessingOutputConfiguration;
    }

    public ACAQMutableSlotConfiguration getAnalysisOutputConfiguration() {
        return analysisOutputConfiguration;
    }

    public BiMap<String, ACAQProjectSample> getSamples() {
        return ImmutableBiMap.copyOf(samples);
    }

    public ACAQProjectSample addSample(String sampleName) {
        if(samples.containsKey(sampleName)) {
            return samples.get(sampleName);
        }
        else {
            ACAQProjectSample sample = new ACAQProjectSample(this);
            samples.put(sampleName, sample);
            eventBus.post(new ACAQSampleAddedEvent(sample));
            return sample;
        }
    }

    public boolean removeSample(ACAQProjectSample sample) {
        String name = sample.getName();
        if(samples.containsKey(name)) {
            samples.remove(name);
            eventBus.post(new ACAQSampleRemovedEvent(sample));
            return true;
        }
        return false;
    }

    public boolean renameSample(ACAQProjectSample sample, String name) {
        if(name == null)
            return false;
        name = name.trim();
        if(name.isEmpty() || samples.containsKey(name))
            return false;
        samples.remove(sample.getName());
        samples.put(name, sample);
        eventBus.post(new ACAQSampleRenamedEvent(sample));
        return true;
    }
}
