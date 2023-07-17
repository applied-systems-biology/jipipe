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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.causes.ParameterValidationReportEntryCause;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.classfilters.NonGenericClassFilter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Import data row folder", description = "Imports one data row from a standardized row folder. Please ensure to define the appropriate data type.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FolderData.class, slotName = "Data row folder", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
public class ImportDataRowFolder extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef dataType = new JIPipeDataInfoRef();
    private StringAndStringPairParameter.List annotations = new StringAndStringPairParameter.List();

    public ImportDataRowFolder(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportDataRowFolder(ImportDataRowFolder other) {
        super(other);
        setDataType(new JIPipeDataInfoRef(other.dataType));
        this.annotations = new StringAndStringPairParameter.List(other.annotations);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path folder = dataBatch.getInputData("Data row folder", FolderData.class, progressInfo).toPath();
        JIPipeData data = JIPipe.importData(new JIPipeFileSystemReadDataStorage(progressInfo, folder), dataType.getInfo().getDataClass(), progressInfo);
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        for (StringAndStringPairParameter item : this.annotations) {
            annotations.add(new JIPipeTextAnnotation(item.getKey(), item.getValue()));
        }

        dataBatch.addOutputData(getFirstOutputSlot(), data, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    @JIPipeDocumentation(name = "Annotations", description = "Use this list to set annotations")
    @JIPipeParameter("annotations")
    @StringParameterSettings(monospace = true)
    public StringAndStringPairParameter.List getAnnotations() {
        return annotations;
    }

    @JIPipeParameter("annotations")
    public void setAnnotations(StringAndStringPairParameter.List annotations) {
        this.annotations = annotations;
    }

    @JIPipeDocumentation(name = "Data type", description = "The data type that should be imported. ")
    @JIPipeParameter("data-type")
    @JIPipeDataParameterSettings(dataClassFilter = NonGenericClassFilter.class)
    public JIPipeDataInfoRef getDataType() {
        return dataType;
    }

    @JIPipeParameter("data-type")
    public void setDataType(JIPipeDataInfoRef dataType) {
        this.dataType = dataType;
    }

    @Override
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {
        super.reportValidity(parentCause, report);
        if (dataType.getInfo() == null) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    parentCause,
                    "Please select a data type!",
                    "This node requires you to select a data type that should be imported.",
                    "Please select a data type in the parameters"));
        } else if (ReflectionUtils.isAbstractOrInterface(dataType.getInfo().getDataClass())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    parentCause,
                    "Data type is generic!",
                    "This node requires you to select a data type that does not act as general concept.",
                    "Please select a data type in the parameters"));
        }
    }
}
