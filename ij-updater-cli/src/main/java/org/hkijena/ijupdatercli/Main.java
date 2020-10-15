package org.hkijena.ijupdatercli;

import net.imagej.ui.swing.updater.SwingAuthenticator;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import net.imagej.updater.UpdateSite;
import net.imagej.updater.util.AvailableSites;
import net.imagej.updater.util.StderrProgress;
import net.imagej.updater.util.UpdaterUtil;
import org.scijava.util.AppUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        if(args.length == 0 || args[0].contains("help")) {
            System.out.println("IJ Updater CLI");
            System.out.println("--------------");
            System.out.println("Part of JIPipe https://www.jipipe.org/");
            System.out.println("Developed by Applied Systems Biology, HKI Jena, Germany");
            System.out.println();
            System.out.println("activate <Update Site Name> <Update Site Name> ...");
            System.out.println("deactivate <Update Site Name> <Update Site Name> ...");
            System.out.println("add <Update Site Name> <URL>");
            System.out.println("remove <Update Site Name>");
            System.out.println();
            System.out.println("To run this tool, execute following command:");
            System.out.println("<ImageJ executable> --pass-classpath --full-classpath --main-class org.hkijena.ijupdatercli.Main");
            return;
        }

        // Load the files collection
        FilesCollection filesCollection;

        try {
            UpdaterUtil.useSystemProxies();
            Authenticator.setDefault(new SwingAuthenticator());

            filesCollection = new FilesCollection(getImageJRoot().toFile());
            AvailableSites.initializeAndAddSites(filesCollection);
            filesCollection.downloadIndexAndChecksum(new StderrProgress());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if(Objects.equals(args[0], "activate")) {
            Set<String> toActivateNames = new HashSet<>();
            for (int i = 1; i < args.length; i++) {
                toActivateNames.add(args[i]);
            }
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
                        filesCollection.activateUpdateSite(updateSite, new StderrProgress());
                    } catch (ParserConfigurationException | IOException | SAXException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            applyUpdates(filesCollection);
        }
        else if(Objects.equals(args[0], "deactivate")) {
            Set<String> toDeactivateNames = new HashSet<>();
            for (int i = 1; i < args.length; i++) {
                toDeactivateNames.add(args[i]);
            }
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
        else if(Objects.equals(args[0], "add")) {
            String name = args[1];
            String url = args[2];
            for (UpdateSite updateSite : filesCollection.getUpdateSites(true)) {
                if(Objects.equals(name, updateSite)) {
                    System.out.println("Update site already exists. Nothing to do.");
                    return;
                }
            }
            UpdateSite site = new UpdateSite(name, url, null, null, null, null, 0);
            filesCollection.addUpdateSite(site);
            applyUpdates(filesCollection);
        }
        else if(Objects.equals(args[0], "remove")) {
            String name = args[1];
            filesCollection.removeUpdateSite(name);
            applyUpdates(filesCollection);
        }
    }

    public static void applyUpdates(FilesCollection filesCollection) {
        final Installer installer =
                new Installer(filesCollection, new StderrProgress());
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
