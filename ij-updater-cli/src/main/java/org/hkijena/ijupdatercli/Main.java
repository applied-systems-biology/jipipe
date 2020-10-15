package org.hkijena.ijupdatercli;

import net.imagej.ui.swing.updater.ProgressDialog;
import net.imagej.ui.swing.updater.SwingAuthenticator;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import net.imagej.updater.UpdateSite;
import net.imagej.updater.util.AvailableSites;
import net.imagej.updater.util.Progress;
import net.imagej.updater.util.StderrProgress;
import net.imagej.updater.util.UpdaterUtil;
import org.scijava.util.AppUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        if(args.length == 0) {
            showHelp();
            return;
        }

        int firstArgIndex = 0;
        while(firstArgIndex < args.length && args[firstArgIndex].startsWith("-")) {
            firstArgIndex += 2;
        }

        if(args[firstArgIndex].contains("help")) {
           showHelp();
            return;
        }

        // Load the files collection
        FilesCollection filesCollection;

        try {
            UpdaterUtil.useSystemProxies();
            Authenticator.setDefault(new SwingAuthenticator());

            filesCollection = new FilesCollection(getImageJRoot().toFile());
            AvailableSites.initializeAndAddSites(filesCollection);
            filesCollection.downloadIndexAndChecksum(createProgress());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if(Objects.equals(args[firstArgIndex], "activate")) {
            Set<String> toActivateNames = new HashSet<>(Arrays.asList(args).subList(firstArgIndex + 1, args.length));
            Set<UpdateSite> toActivate = new HashSet<>();
            for (UpdateSite updateSite : filesCollection.getUpdateSites(true)) {
                if(toActivateNames.contains(updateSite.getName())) {
                    toActivate.add(updateSite);
                    toActivateNames.remove(updateSite.getName());
                }
            }
            if(!toActivateNames.isEmpty()) {
                System.err.println("Some update sites could not be found:");
                for (String name : toActivateNames) {
                    System.err.println(name);
                }
                System.err.println("Aborting.");
                throw new RuntimeException("Unable to activate update sites!");
            }
            for (UpdateSite updateSite : toActivate) {
                if(!updateSite.isActive()) {
                    try {
                        filesCollection.activateUpdateSite(updateSite, createProgress());
                    } catch (ParserConfigurationException | IOException | SAXException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            applyUpdates(filesCollection);
        }
        else if(Objects.equals(args[firstArgIndex], "deactivate")) {
            Set<String> toDeactivateNames = new HashSet<>(Arrays.asList(args).subList(firstArgIndex + 1, args.length));
            Set<UpdateSite> toDeactivate = new HashSet<>();
            for (UpdateSite updateSite : filesCollection.getUpdateSites(true)) {
                if(toDeactivateNames.contains(updateSite.getName())) {
                    toDeactivate.add(updateSite);
                    toDeactivateNames.remove(updateSite.getName());
                }
            }
            for (UpdateSite updateSite : toDeactivate) {
                if(updateSite.isActive()) {
                    filesCollection.deactivateUpdateSite(updateSite);
                }
            }
            applyUpdates(filesCollection);
        }
        else if(Objects.equals(args[firstArgIndex], "add")) {
            String name = args[firstArgIndex + 1];
            String url = args[firstArgIndex + 2];
            for (UpdateSite updateSite : filesCollection.getUpdateSites(true)) {
                if(Objects.equals(name, updateSite.getName())) {
                    System.out.println("Update site already exists. Nothing to do.");
                    return;
                }
            }
            UpdateSite site = new UpdateSite(name, url, null, null, null, null, 0);
            filesCollection.addUpdateSite(site);
            applyUpdates(filesCollection);
        }
        else if(Objects.equals(args[firstArgIndex], "remove")) {
            String name = args[1];
            filesCollection.removeUpdateSite(name);
            applyUpdates(filesCollection);
        }
        else if(Objects.equals(args[firstArgIndex], "list")) {
            boolean withActive = firstArgIndex == args.length - 1 || args[firstArgIndex + 1].equals("active");
            boolean withInactive = firstArgIndex == args.length - 1 || args[firstArgIndex + 1].equals("inactive");
            for (UpdateSite updateSite : filesCollection.getUpdateSites(true)) {
                if(updateSite.isActive() && withActive || !updateSite.isActive() && withInactive) {
                    System.out.println(updateSite.getName() + "\t" + updateSite.getURL() + "\t" + (updateSite.isActive() ? "active" : "inactive"));
                }
            }
        }
        else if(Objects.equals(args[firstArgIndex], "update")) {
            applyUpdates(filesCollection);
        }
        else {
            throw new RuntimeException("Invalid command!");
        }
    }

    public static Progress createProgress() {
        if(getOperatingSystem().toLowerCase().contains("windows")) {
            return new ProgressDialog(null, "ImageJ updater");
        }
        else {
            return new StderrProgress();
        }
    }

    public static String getOperatingSystem() {
        return System.getProperty("os.name");
    }

    private static void showHelp() {
        System.out.println("IJ Updater CLI");
        System.out.println("--------------");
        System.out.println("Part of JIPipe https://www.jipipe.org/");
        System.out.println("Developed by Applied Systems Biology, HKI Jena, Germany");
        System.out.println();
        System.out.println("list [active|inactive]");
        System.out.println("update");
        System.out.println("activate <Update Site Name> <Update Site Name> ...");
        System.out.println("deactivate <Update Site Name> <Update Site Name> ...");
        System.out.println("add <Update Site Name> <URL>");
        System.out.println("remove <Update Site Name>");
        System.out.println();
        System.out.println("To run this tool, execute following command:");
        System.out.println("<ImageJ executable> --debug --pass-classpath --full-classpath --main-class org.hkijena.ijupdatercli.Main");
    }

    public static void applyUpdates(FilesCollection filesCollection) {
        final Installer installer =
                new Installer(filesCollection, createProgress());
        try {
            installer.start();
            filesCollection.write();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            installer.done();
        }
    }

    public static Path getImageJRoot() {
        String imagejDirProperty = System.getProperty("imagej.dir");
        final File imagejRoot = imagejDirProperty != null ? new File(imagejDirProperty) :
                AppUtils.getBaseDirectory("ij.dir", FilesCollection.class, "updater");
        return imagejRoot.toPath();
    }
}
