package org.hkijena.jipipe.extensions.ijweka.classifiers;

import hr.irb.fastRandomForest.FastRandomForest;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.JIPipeSerializedParameterCollectionData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaClassifierData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import weka.classifiers.AbstractClassifier;

@JIPipeDocumentation(name = "Fast Random Forest classifier", description = "The default classifier utilized by the Weka segmentation")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A JSON file that contains the serialized data",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class FastRandomForestWekaClassifierData extends WekaClassifierData {

    private int maxDepth = 0;
    private int numThreads = 0;
    private int numFeatures = 0;
    private int numTrees = 100;

    public FastRandomForestWekaClassifierData() {
    }

    public FastRandomForestWekaClassifierData(FastRandomForestWekaClassifierData other) {
        super(other);
        this.maxDepth = other.maxDepth;
        this.numThreads = other.numThreads;
        this.numFeatures = other.numFeatures;
        this.numTrees = other.numTrees;
    }

    @JIPipeDocumentation(name = "Maximum depth", description = "Set the maximum depth of the tree, 0 for unlimited.")
    @JIPipeParameter("max-depth")
    public int getMaxDepth() {
        return maxDepth;
    }

    @JIPipeParameter("max-depth")
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @JIPipeDocumentation(name = "Number of threads", description = "Set the number of simultaneous threads used in training, 0 for autodetect.")
    @JIPipeParameter("num-threads")
    public int getNumThreads() {
        return numThreads;
    }

    @JIPipeParameter("num-threads")
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    @JIPipeDocumentation(name = "Number of features", description = "Set the number of features to use in random selection, 0 for autodetect.")
    @JIPipeParameter("num-features")
    public int getNumFeatures() {
        return numFeatures;
    }

    @JIPipeParameter("num-features")
    public void setNumFeatures(int numFeatures) {
        this.numFeatures = numFeatures;
    }

    @JIPipeDocumentation(name = "Number of trees", description = "Set number of trees in the forest")
    @JIPipeParameter("num-trees")
    public int getNumTrees() {
        return numTrees;
    }

    @JIPipeParameter("num-trees")
    public void setNumTrees(int numTrees) {
        this.numTrees = numTrees;
    }

    public static JIPipeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return JIPipeSerializedParameterCollectionData.importData(storage, progressInfo);
    }

    @Override
    public AbstractClassifier newClassifierInstance() {
        FastRandomForest fastRandomForest = new FastRandomForest();
        fastRandomForest.setMaxDepth(maxDepth);
        fastRandomForest.setNumFeatures(numFeatures);
        fastRandomForest.setNumThreads(numThreads);
        fastRandomForest.setNumTrees(numTrees);
        return fastRandomForest;
    }
}
