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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.datasources;

import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.events.NodeSlotsChangedEvent;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.datasources.ImagePlusFromFile;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.expressions.PathQueryExpression;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads an image data from a file via IJ.openFile()
 */
@JIPipeDocumentation(name = "Import image stack", description = "Loads an image stack via the native ImageJ functions. The current implementation only allows 2D images to be imported and will show an error if higher-dimensional data is provided.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(FolderData.class)
@JIPipeOutputSlot(ImagePlusData.class)
public class ImageStackFromFolder extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef generatedImageType = new JIPipeDataInfoRef("imagej-imgplus");
    private boolean removeLut = false;
    private HyperstackDimension outputDimension = HyperstackDimension.Depth;
    private boolean sortFilesNumerically = true;
    private PathQueryExpression filterExpression = new PathQueryExpression();
    private IntegerRange slicesToImport = new IntegerRange();
    private boolean ignoreInvalidSlices = false;

    /**
     * @param info algorithm info
     */
    public ImageStackFromFolder(JIPipeNodeInfo info) {
        super(info,
                JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Files", FileData.class)
                        .addOutputSlot("Image", ImagePlusData.class, null)
                        .sealOutput()
                        .sealInput()
                        .build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ImageStackFromFolder(ImageStackFromFolder other) {
        super(other);
        this.generatedImageType = new JIPipeDataInfoRef(other.generatedImageType);
        this.removeLut = other.removeLut;
        this.outputDimension = other.outputDimension;
        this.sortFilesNumerically = other.sortFilesNumerically;
        this.filterExpression = new PathQueryExpression(other.filterExpression);
        this.slicesToImport = new IntegerRange(other.slicesToImport);
        this.ignoreInvalidSlices = other.ignoreInvalidSlices;
    }

    @JIPipeDocumentation(name = "Remove LUT", description = "If enabled, remove the LUT information if present")
    @JIPipeParameter("remove-lut")
    public boolean isRemoveLut() {
        return removeLut;
    }

    @JIPipeParameter("remove-lut")
    public void setRemoveLut(boolean removeLut) {
        this.removeLut = removeLut;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path inputFolder = dataBatch.getInputData(getFirstInputSlot(), FolderData.class, progressInfo).getPath();
        try {
            progressInfo.log("Looking for files in " + inputFolder);
            List<Path> inputFiles = Files.list(inputFolder).filter(filterExpression).collect(Collectors.toList());
            Comparator<Path> comparator;
            if (sortFilesNumerically) {
                comparator = (o1, o2) -> NaturalOrderComparator.INSTANCE.compare(o1.getFileName().toString(), o2.getFileName().toString());
            } else {
                comparator = Comparator.naturalOrder();
            }
            inputFiles.sort(comparator);

            // Slicing
            if (!StringUtils.isNullOrEmpty(slicesToImport.getValue())) {
                List<Path> inputFilesSliced = new ArrayList<>();
                for (Integer index : slicesToImport.getIntegers()) {
                    if (ignoreInvalidSlices && index < 0 || index >= inputFiles.size()) {
                        continue;
                    }
                    inputFilesSliced.add(inputFiles.get(index));
                }
                inputFiles = inputFilesSliced;
            }

            progressInfo.log("Found " + inputFiles.size() + " files to import.");
            List<ImagePlus> images = new ArrayList<>();
            for (int i = 0; i < inputFiles.size(); i++) {
                Path file = inputFiles.get(i);
                JIPipeProgressInfo fileProgress = progressInfo.resolveAndLog(file.getFileName().toString(), i, inputFiles.size());
                ImagePlus image = ImagePlusFromFile.readImageFrom(file, fileProgress);
                images.add(image);
            }

            // Determine the dimensionality
            int dimensions = 2;
            for (ImagePlus image : images) {
                dimensions = Math.max(image.getNDimensions(), dimensions);
            }

            if (dimensions > 2)
                throw new RuntimeException("Only 2D planes are supported in the current implementation.");

            if (images.isEmpty())
                return;

            ImageStack stack = new ImageStack(images.get(0).getWidth(), images.get(0).getHeight(), inputFiles.size());
            for (int i = 0; i < inputFiles.size(); i++) {
                stack.setProcessor(images.get(i).getProcessor(), i + 1);
            }

            ImagePlus merged = new ImagePlus("Stack", stack);
            if (outputDimension == HyperstackDimension.Channel) {
                merged.setDimensions(merged.getNSlices(), 1, 1);
            } else if (outputDimension == HyperstackDimension.Frame) {
                merged.setDimensions(1, 1, merged.getNSlices());
            }
            dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.createData(generatedImageType.getInfo().getDataClass(), merged), progressInfo);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Generated image type", description = "The image type that is generated.")
    @JIPipeParameter("generated-image-type")
    public JIPipeDataInfoRef getGeneratedImageType() {
        return generatedImageType;
    }

    @JIPipeParameter("generated-image-type")
    public void setGeneratedImageType(JIPipeDataInfoRef generatedImageType) {
        this.generatedImageType = generatedImageType;
        getFirstOutputSlot().setAcceptedDataType(generatedImageType.getInfo().getDataClass());
        getEventBus().post(new NodeSlotsChangedEvent(this));
    }

    @JIPipeDocumentation(name = "Output dimension", description = "Determines in which dimension the stack grows. You can choose between Z (default), channel, and time.")
    @JIPipeParameter("output-dimension")
    public HyperstackDimension getOutputDimension() {
        return outputDimension;
    }

    @JIPipeParameter("output-dimension")
    public void setOutputDimension(HyperstackDimension outputDimension) {
        this.outputDimension = outputDimension;
    }

    @JIPipeDocumentation(name = "Sort numerically", description = "If true, the files are sorted in a natural order. For example a1, a2, a15, a100, b1. Otherwise, they are " +
            "ordered lexicographically. For example a1, a100, a15, a2, b1")
    @JIPipeParameter("sort-numerically")
    public boolean isSortFilesNumerically() {
        return sortFilesNumerically;
    }

    @JIPipeParameter("sort-numerically")
    public void setSortFilesNumerically(boolean sortFilesNumerically) {
        this.sortFilesNumerically = sortFilesNumerically;
    }

    @JIPipeDocumentation(name = "File filter", description = "Allows you to filter the files.")
    @JIPipeParameter("filter-expression")
    public PathQueryExpression getFilterExpression() {
        return filterExpression;
    }

    @JIPipeParameter("filter-expression")
    public void setFilterExpression(PathQueryExpression filterExpression) {
        this.filterExpression = filterExpression;
    }

    @JIPipeDocumentation(name = "Filter only *.tif", description = "Sets the filter, so only *.tif files are returned.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            setFilterExpression(new PathQueryExpression("STRING_MATCHES_GLOB(name, \"*.tif\")"));
            getEventBus().post(new ParameterChangedEvent(this, "filter-expression"));
        }
    }

    @JIPipeDocumentation(name = "Slices to import", description = "Determines which files should be imported based on the sorted and filtered list of files. The first index is 0. Duplicates are allowed." +
            " If left empty, all slices will be imported. " + IntegerRange.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("slice-indices")
    public IntegerRange getSlicesToImport() {
        return slicesToImport;
    }

    @JIPipeParameter("slice-indices")
    public void setSlicesToImport(IntegerRange slicesToImport) {
        this.slicesToImport = slicesToImport;
    }

    @JIPipeDocumentation(name = "Ignore invalid slice indices", description = "Used if you limit the imported slices via 'Slices to import'. If enabled, all invalid indices (e.g. negative ones or ones larger than the list) are ignored.")
    @JIPipeParameter("ignore-invalid-slice-indices")
    public boolean isIgnoreInvalidSlices() {
        return ignoreInvalidSlices;
    }

    @JIPipeParameter("ignore-invalid-slice-indices")
    public void setIgnoreInvalidSlices(boolean ignoreInvalidSlices) {
        this.ignoreInvalidSlices = ignoreInvalidSlices;
    }
}
