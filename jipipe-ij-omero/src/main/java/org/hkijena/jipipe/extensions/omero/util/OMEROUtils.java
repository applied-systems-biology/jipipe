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

package org.hkijena.jipipe.extensions.omero.util;

import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.targets.ImportTarget;
import omero.ServerError;
import omero.model.Annotation;
import omero.model.IObject;
import omero.model.Pixels;
import org.apache.commons.lang.StringUtils;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OMEROUtils {
    /**
     * Copy of importCandidates in {@link ome.formats.importer.ImportLibrary} that returns the list of uploaded images.
     * This method also throws all exceptions.
     * @param config
     * @param candidates
     * @return
     */
    public static List<Pixels> importImages(ImportLibrary library, OMEROMetadataStoreClient store, final ImportConfig config, ImportCandidates candidates)
    {
        List<ImportContainer> containers = candidates.getContainers();
        List<Pixels> result = new ArrayList<>();
        if (containers != null) {
            final int count = containers.size();
            ExecutorService filesetThreadPool = Executors.newFixedThreadPool(Math.min(count, config.parallelFileset.get()));
            ExecutorService uploadThreadPool = Executors.newFixedThreadPool(config.parallelUpload.get());
            try {
                final List<Callable<Boolean>> threads = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    final ImportContainer ic = containers.get(index);
                    final ImportTarget target = config.getTarget();
                    if (config.checksumAlgorithm.get() != null) {
                        ic.setChecksumAlgorithm(config.checksumAlgorithm.get());
                    }
                    final ExecutorService uploadThreadPoolFinal = uploadThreadPool;
                    final int indexFinal = index;
                    threads.add(() -> {
                        try {
                            if (target != null) {
                                try {
                                    IObject obj = target.load(store, ic);
                                    if (!(obj instanceof Annotation)) {
                                        Class<? extends IObject> targetClass = obj.getClass();
                                        while (targetClass.getSuperclass() != IObject.class) {
                                            targetClass = targetClass.getSuperclass().asSubclass(IObject.class);
                                        }
                                        ic.setTarget(obj);
                                    } else {
                                        // This is likely a "post-processing" annotation
                                        // so that we don't have to resolve the target
                                        // until later.
                                        ic.getCustomAnnotationList().add((Annotation) obj);
                                    }
                                } catch (Exception e) {
                                    throw new UserFriendlyRuntimeException(e,
                                            "OMERO upload was interrupted",
                                            "OMERO upload",
                                            "The input file " + ic.getFile() + " could not be loaded",
                                            "Please check if you can upload data via OMERO Insight.");
                                }
                            }
                            result.addAll(library.importImage(ic, uploadThreadPoolFinal, indexFinal));
                            return true;
                        } catch (Throwable t) {
                            throw new UserFriendlyRuntimeException(t,
                                    "OMERO upload was interrupted",
                                    "OMERO upload",
                                    "Please consider the technical information",
                                    "Please check if you can upload data via OMERO Insight.");
                        }
                    });
                }
                    final ExecutorCompletionService<Boolean> threadQueue = new ExecutorCompletionService<>(filesetThreadPool);
                final List<Future<Boolean>> outcomes = new ArrayList<>(count);
                for (final Callable<Boolean> thread : threads) {
                    outcomes.add(threadQueue.submit(thread));
                }
                try {
                    for (int index = 0; index < count; index++) {
                        if (!threadQueue.take().get()) {
                            throw new InterruptedException();
                        }
                    }
                } catch (InterruptedException | ExecutionException ie) {
                    throw new UserFriendlyRuntimeException(ie,
                            "OMERO upload was interrupted",
                            "OMERO upload",
                            "Please consider the technical information",
                            "Please check if you can upload data via OMERO Insight.");
                }
            } finally {
                if (filesetThreadPool != null) {
                    filesetThreadPool.shutdownNow();
                }
                if (uploadThreadPool != null) {
                    uploadThreadPool.shutdownNow();
                }
            }
        }
        return result;
    }
}
