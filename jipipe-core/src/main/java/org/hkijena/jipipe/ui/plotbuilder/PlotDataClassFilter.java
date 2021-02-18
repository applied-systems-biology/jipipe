package org.hkijena.jipipe.ui.plotbuilder;

import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.utils.ClassFilter;

public class PlotDataClassFilter implements ClassFilter {
    @Override
    public boolean test(Class<?> aClass) {
        return PlotData.class.isAssignableFrom(aClass) && aClass != PlotData.class;
    }
}
