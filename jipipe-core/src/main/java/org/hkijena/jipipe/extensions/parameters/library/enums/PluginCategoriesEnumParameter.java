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
 *
 */

package org.hkijena.jipipe.extensions.parameters.library.enums;

import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.DynamicStringEnumParameter;

import java.util.Arrays;

/**
 * Categories enum (editable) that is used in metadata
 */
public class PluginCategoriesEnumParameter extends DynamicStringEnumParameter {

    public static final String CATEGORY_3D = "3D";
    public static final String CATEGORY_ANALYSIS = "Analysis";
    public static final String CATEGORY_ANNOTATION = "Annotation";
    public static final String CATEGORY_AUTOMATION = "Automation";
    public static final String CATEGORY_BENCHMARK = "Benchmark";
    public static final String CATEGORY_BINARY = "Binary";
    public static final String CATEGORY_CELLPROFILER = "CellProfiler";
    public static final String CATEGORY_CLASSIFICATION = "Classification";
    public static final String CATEGORY_COLOCALIZATION = "Colocalization";
    public static final String CATEGORY_COLOR_PROCESSING = "Color Processing";
    public static final String CATEGORY_COMPLEXITY = "Complexity";
    public static final String CATEGORY_COOKBOOK = "Cookbook";
    public static final String CATEGORY_DECONVOLUTION = "Deconvolution";
    public static final String CATEGORY_GPU = "GPU";
    public static final String CATEGORY_DEEP_LEARNING = "Deep-Learning";
    public static final String CATEGORY_DENOISING = "Denoising";
    public static final String CATEGORY_DEVELOPMENT = "Development";
    public static final String CATEGORY_DIGITAL_VOLUME_FLATTENING = "Digital Volume Flattening";
    public static final String CATEGORY_DIGITAL_VOLUME_UNROLLING = "Digital Volume Unrolling";
    public static final String CATEGORY_ENTROPY = "Entropy";
    public static final String CATEGORY_EXAMPLE_DATA = "Example Data";
    public static final String CATEGORY_FEATURE_EXTRACTION = "Feature Extraction";
    public static final String CATEGORY_FIJI = "Fiji";
    public static final String CATEGORY_FILTERING = "Filtering";
    public static final String CATEGORY_FRACTAL_DIMENSION = "Fractal dimension";
    public static final String CATEGORY_GUT = "Gut";
    public static final String CATEGORY_HELP = "Help";
    public static final String CATEGORY_IMAGE_ANNOTATION = "Image Annotation";
    public static final String CATEGORY_IMAGE_ANALYSIS = "Image analysis";
    public static final String CATEGORY_STATISTICS = "Statistics";
    public static final String CATEGORY_DATA_PROCESSING = "Data processing";
    public static final String CATEGORY_IMAGEJ2 = "ImageJ2";
    public static final String CATEGORY_IMAGE_SCIENCE = "ImageScience";
    public static final String CATEGORY_IMGLIB = "ImgLib";
    public static final String CATEGORY_IMPORT_EXPORT = "Import-Export";
    public static final String CATEGORY_INTEGRAL_IMAGE = "Integral Image";
    public static final String CATEGORY_INTERACTIVE = "Interactive";
    public static final String CATEGORY_LIFETIME = "Lifetime";
    public static final String CATEGORY_LISP = "Lisp";
    public static final String CATEGORY_MATLAB = "MATLAB";
    public static final String CATEGORY_MACHINE_LEARNING = "Machine Learning";
    public static final String CATEGORY_MATHEMATICAL_MORPHOLOGY = "Mathematical Morphology";
    public static final String CATEGORY_MICROSCOPY = "Microscopy";
    public static final String CATEGORY_MICROTUBULES = "Microtubules";
    public static final String CATEGORY_MONTAGE = "Montage";
    public static final String CATEGORY_NEUROANATOMY = "Neuroanatomy";
    public static final String CATEGORY_NEURON = "Neuron";
    public static final String CATEGORY_NOISE = "Noise";
    public static final String CATEGORY_OME = "OME";
    public static final String CATEGORY_OBJECT_DETECTION = "Object Detection";
    public static final String CATEGORY_OPS = "Ops";
    public static final String CATEGORY_OPTIC_FLOW = "Optic Flow";
    public static final String CATEGORY_PARTICLE_ANALYSIS = "Particle Analysis";
    public static final String CATEGORY_PATTERN_RECOGNITION = "Pattern Recognition";
    public static final String CATEGORY_PERFUSION = "Perfusion";
    public static final String CATEGORY_PHOTOGRAMMETRY = "Photogrammetry";
    public static final String CATEGORY_PHOTOGRAPHY = "Photography";
    public static final String CATEGORY_PLOTTING = "Plotting";
    public static final String CATEGORY_PROJECTION = "Projection";
    public static final String CATEGORY_REGISTRATION = "Registration";
    public static final String CATEGORY_SCIJAVA = "SciJava";
    public static final String CATEGORY_SCRIPTING = "Scripting";
    public static final String CATEGORY_SEGMENTATION = "Segmentation";
    public static final String CATEGORY_SKELETON = "Skeleton";
    public static final String CATEGORY_SKELETONIZATION = "Skeletonization";
    public static final String CATEGORY_STACKS = "Stacks";
    public static final String CATEGORY_STITCHING = "Stitching";
    public static final String CATEGORY_SUPER_RESOLUTION = "Super-resolution";
    public static final String CATEGORY_TIME_SIGNAL_ANALYSIS = "Time signal analysis";
    public static final String CATEGORY_TISSUE = "Tissue";
    public static final String CATEGORY_TRACKING = "Tracking";
    public static final String CATEGORY_TRACKEM2 = "TrakEM2";
    public static final String CATEGORY_TRANSFORM = "Transform";
    public static final String CATEGORY_TUTORIAL = "Tutorial";
    public static final String CATEGORY_TUTORIALS = "Tutorials";
    public static final String CATEGORY_UNCATEGORIZED = "Uncategorized";
    public static final String CATEGORY_USER_INTERFACE = "User Interface";
    public static final String CATEGORY_VISUALIZATION = "Visualization";
    public static java.util.List<String> DEFAULT_VALUES = Arrays.asList("3D",
            "Analysis",
            "Annotation",
            "Automation",
            "Benchmark",
            "Binary",
            "CellProfiler",
            "Classification",
            "Colocalization",
            "Color Processing",
            "Complexity",
            "Cookbook",
            "Deconvolution",
            "Deep-Learning",
            "Denoising",
            "Development",
            "Digital Volume Flattening",
            "Digital Volume Unrolling",
            "Entropy",
            "Example Data",
            "Feature Extraction",
            "Fiji",
            "Filtering",
            "Fractal dimension",
            "Gut",
            "Help",
            "Image Annotation",
            "Image analysis",
            "ImageJ2",
            "ImageScience",
            "ImgLib",
            "Import-Export",
            "Integral Image",
            "Interactive",
            "Lifetime",
            "Lisp",
            "MATLAB",
            "Machine Learning",
            "Mathematical Morphology",
            "Microscopy",
            "Microtubules",
            "Montage",
            "Neuroanatomy",
            "Neuron",
            "Noise",
            "OME",
            "Object Detection",
            "Ops",
            "Optic Flow",
            "Particle Analysis",
            "Pattern Recognition",
            "Perfusion",
            "Photogrammetry",
            "Photography",
            "Plotting",
            "Projection",
            "Registration",
            "SciJava",
            "Scripting",
            "Segmentation",
            "Skeleton",
            "Skeletonization",
            "Stacks",
            "Stitching",
            "Super-resolution",
            "Time signal analysis",
            "Tissue",
            "Tracking",
            "TrakEM2",
            "Transform",
            "Tutorial",
            "Tutorials",
            "Uncategorized",
            "User Interface",
            "Visualization",
            "GPU",
            "Statistics",
            "Data processing");

    public PluginCategoriesEnumParameter() {
        setAllowedValues(DEFAULT_VALUES);
    }

    public PluginCategoriesEnumParameter(DynamicStringEnumParameter other) {
        super(other);
        setAllowedValues(DEFAULT_VALUES);
    }

    public PluginCategoriesEnumParameter(String value) {
        super(value);
        setAllowedValues(DEFAULT_VALUES);
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    public static class List extends ListParameter<PluginCategoriesEnumParameter> {
        public List() {
            super(PluginCategoriesEnumParameter.class);
        }

        public List(PluginCategoriesEnumParameter... items) {
            this();
            this.addAll(Arrays.asList(items));
        }

        public List(String... items) {
            this();
            for (String item : items) {
                add(new PluginCategoriesEnumParameter(item));
            }
        }

        public List(List other) {
            super(PluginCategoriesEnumParameter.class);
            for (PluginCategoriesEnumParameter parameter : other) {
                add(new PluginCategoriesEnumParameter(parameter));
            }
        }
    }
}
