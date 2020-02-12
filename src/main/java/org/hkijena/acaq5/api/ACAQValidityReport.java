/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.api;

import java.util.*;

/**
 * Report about the validity of an object
 */
public class ACAQValidityReport {

    public static class Entry {

        public enum Type {
            Info,
            Warning,
            Error;

            static Type merge(Type t0, Type t1) {
                if(t0 == Error || t1 == Error) {
                    return Error;
                }
                else if(t0 == Warning || t1 == Warning) {
                    return  Warning;
                }
                else {
                    return Info;
                }
            }

            public boolean isAtLeast(Type comparison) {
                switch (comparison) {
                    case Info:
                        return true;
                    case Warning:
                        return this == Warning || this == Error;
                    default:
                        return this == Error;
                }
            }
        }

        private Object object;
        private List<String> categories;
        private Type type;
        private String message;

        public Entry(Object object, List<String> categories, Type type, String message) {
            this.object = object;
            this.categories = categories;
            this.type = type;
            this.message = message;
        }

        public Object getObject() {
            return object;
        }

        public List<String> getCategories() {
            return Collections.unmodifiableList(categories);
        }

        public void addCategories(List<String> categories) {
            this.categories.addAll(categories);
        }

        public void addCategory(String category) {
            if(category != null)
                this.categories.add(category);
        }

        public String getMessage() {
            return message;
        }

        public Type getEntryType() {
            return type;
        }

        public void markAs(String message, Type type) {
            this.type = type;
            this.message = message;
        }

        public void mergeWith(Type type) {
            this.type = Type.merge(this.type, type);
        }
    }

    private Map<Object, Entry> entries = new HashMap<>();

    public ACAQValidityReport() {

    }

    public ACAQValidityReport(Object target, String category, Entry.Type type, String message) {
        this.report(target, category, type, message);
    }

    public ACAQValidityReport(Object target, String category, boolean isValid, String message) {
        this.report(target, category, isValid ? Entry.Type.Info : Entry.Type.Error, message);
    }

    /**
     * Reports the target object to the validity report
     * @param target
     * @param category
     * @param isValid
     */
    public void report(Object target, String category, boolean isValid, String message) {
        report(target, category, isValid ? Entry.Type.Info : Entry.Type.Error, message);
    }

    /**
     * Reports the target object to the validity report
     * @param target
     * @param category
     * @param type
     */
    public void report(Object target, String category, Entry.Type type, String message) {
        Entry e = entries.getOrDefault(target, null);
        if(e == null) {
            List<String> c = new ArrayList<>();
            if(category != null)
                c.add(category);
            e = new Entry(target, c, type, message);
            entries.put(target, e);
        }
        else {
            e.addCategory(category);
            e.markAs(message, Entry.Type.merge(e.getEntryType(), type));
        }
    }

    /**
     * Merges another report into this report under the list of specified categories
     * @param subreport
     * @param category
     */
    public void merge(ACAQValidityReport subreport, String... category) {
        for(Object key : subreport.entries.keySet()) {
            if(entries.containsKey(key)) {
                Entry src = subreport.entries.get(key);
                Entry dst = entries.get(key);
                dst.addCategories(src.getCategories());
                dst.markAs(src.getMessage(), Entry.Type.merge(src.getEntryType(), dst.getEntryType()));
            }
            else {
                entries.put(key, subreport.entries.get(key));
            }
        }
    }

    /**
     * Returns true if all reports are positive
     * @return
     */
    public Entry.Type getResult() {
        Entry.Type result = Entry.Type.Info;
        for(Entry entry : entries.values()) {
            result = Entry.Type.merge(result, entry.getEntryType());
        }
        return result;
    }

    public boolean isValid() {
        return getResult() != Entry.Type.Error;
    }

    public Map<Object, Entry> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public Map<Object, Entry> getEntriesOfAtLeast(Entry.Type type) {
        HashMap<Object, Entry> result = new HashMap<>();
        for(Map.Entry<Object, Entry> kv : entries.entrySet()) {
            if(kv.getValue().getEntryType().isAtLeast(type)) {
                result.put(kv.getKey(), kv.getValue());
            }
        }
        return Collections.unmodifiableMap(result);
    }

}
