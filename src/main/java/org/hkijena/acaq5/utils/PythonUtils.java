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

package org.hkijena.acaq5.utils;

import org.hkijena.acaq5.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.pairs.IntegerAndIntegerPair;
import org.hkijena.acaq5.extensions.parameters.pairs.StringAndStringPair;
import org.hkijena.acaq5.extensions.parameters.primitives.DoubleList;
import org.hkijena.acaq5.extensions.parameters.primitives.IntegerList;
import org.hkijena.acaq5.extensions.parameters.primitives.PathList;
import org.hkijena.acaq5.extensions.parameters.primitives.StringList;
import org.python.core.PyDictionary;
import org.python.util.PythonInterpreter;

import java.nio.file.Path;
import java.util.Map;

/**
 * Utilities for Jython
 */
public class PythonUtils {

    public static Class<?>[] ALLOWED_PARAMETER_CLASSES = new Class[]{
            String.class,
            StringList.class,
            StringAndStringPair.List.class,
            Integer.class,
            IntegerList.class,
            IntegerAndIntegerPair.List.class,
            Double.class,
            DoubleList.class,
            Path.class,
            PathList.class,
            Boolean.class
    };

    private PythonUtils() {

    }

    public static void passParametersToPython(PythonInterpreter pythonInterpreter, ACAQCustomParameterCollection collection) {
        for (Map.Entry<String, ACAQParameterAccess> entry : collection.getParameters().entrySet()) {
            if(entry.getValue().getFieldClass() == StringAndStringPair.List.class) {
                PyDictionary dictionary = new PyDictionary();
                for (StringAndStringPair pair : entry.getValue().get(StringAndStringPair.List.class)) {
                    dictionary.put(pair.getKey(), pair.getValue());
                }
                pythonInterpreter.set(entry.getKey(), dictionary);
            }
            else  if(entry.getValue().getFieldClass() == IntegerAndIntegerPair.List.class) {
                PyDictionary dictionary = new PyDictionary();
                for (IntegerAndIntegerPair pair : entry.getValue().get(IntegerAndIntegerPair.List.class)) {
                    dictionary.put(pair.getKey(), pair.getValue());
                }
                pythonInterpreter.set(entry.getKey(), dictionary);
            }
            else {
                pythonInterpreter.set(entry.getKey(), entry.getValue().get(Object.class));
            }
        }
    }

}
