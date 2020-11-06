# ImageJ Updater CLI

Alternative to the updater CLI provided with the ImageJ binary.

Part of JIPipe https://www.jipipe.org/
Developed by Applied Systems Biology, HKI Jena, Germany

list [active|inactive]
update
activate <Update Site Name> <Update Site Name> ...
deactivate <Update Site Name> <Update Site Name> ...
add <Update Site Name> <URL>
remove <Update Site Name>

To run this tool, execute following command:
<ImageJ executable> --pass-classpath --full-classpath --main-class org.hkijena.ijupdatercli.Main