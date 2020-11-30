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

package org.hkijena.jipipe.extensions.filesystem.compat;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows importing of path data from within ImageJ
 * The main 'ImageJ' data type is {@link File}, but this also allows conversion from {@link String}, {@link Path} and {@link ResultsTable}.
 */
public class PathDataImageJAdapter implements ImageJDatatypeAdapter {

    private Class<? extends PathData> jipipeDataClass;

    /**
     * Creates a new instance
     *
     * @param jipipeDataClass the exact data class that is imported
     */
    public PathDataImageJAdapter(Class<? extends PathData> jipipeDataClass) {
        this.jipipeDataClass = jipipeDataClass;
    }

    @Override
    public boolean canConvertImageJToJIPipe(Object imageJData) {
        return false;
    }

    @Override
    public boolean canConvertJIPipeToImageJ(JIPipeData jipipeData) {
        return false;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return File.class;
    }

    @Override
    public Class<? extends JIPipeData> getJIPipeDatatype() {
        return jipipeDataClass;
    }

    @Override
    public JIPipeData convertImageJToJIPipe(Object imageJData) {
        if (imageJData instanceof String) {
            return (JIPipeData) ReflectionUtils.newInstance(jipipeDataClass, Paths.get((String) imageJData));
        } else if (imageJData instanceof File) {
            return (JIPipeData) ReflectionUtils.newInstance(jipipeDataClass, ((File) imageJData).toPath());
        } else if (imageJData instanceof Path) {
            return (JIPipeData) ReflectionUtils.newInstance(jipipeDataClass, imageJData);
        } else if (imageJData instanceof ResultsTable) {
            ResultsTableData resultsTableData = new ResultsTableData((ResultsTable) imageJData);
            return (JIPipeData) ReflectionUtils.newInstance(jipipeDataClass, Paths.get(resultsTableData.getValueAsString(0, 0)));
        } else {
            throw new IllegalArgumentException("Cannot convert to " + jipipeDataClass + ": " + imageJData);
        }
    }

    @Override
    public Object convertJIPipeToImageJ(JIPipeData jipipeData, boolean activate, boolean noWindow, String windowName) {
        PathData data = (PathData) jipipeData;
        ResultsTableData resultsTableData = new ResultsTableData();
        resultsTableData.addColumn("Path", true);
        resultsTableData.addRow();
        resultsTableData.setValueAt(data.getPath().toString(), 0, 0);
        if (activate && !noWindow) {
            resultsTableData.getTable().show(windowName);
        }
        return data.toPath().toFile();
    }

    @Override
    public List<Object> convertMultipleJIPipeToImageJ(List<JIPipeData> jipipeData, boolean activate, boolean noWindow, String windowName) {
        ResultsTableData resultsTableData = new ResultsTableData();
        resultsTableData.addColumn("Path", true);
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < jipipeData.size(); ++i) {
            PathData data = (PathData) jipipeData.get(i);
            result.add(data.getPath());

            resultsTableData.addRow();
            resultsTableData.setValueAt(data.getPath() + "", i, 0);
        }
        if (activate && !noWindow) {
            resultsTableData.getTable().show(windowName);
        }
        return result;
    }

    @Override
    public JIPipeData importFromImageJ(String parameters) {
        return (JIPipeData) ReflectionUtils.newInstance(jipipeDataClass, Paths.get(parameters));
    }
}
