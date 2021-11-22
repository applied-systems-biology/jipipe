Unicode True
RequestExecutionLevel user

; HM NIS Edit Wizard helper defines
!define PRODUCT_NAME "JIPipe"
!define PRODUCT_VERSION "1.50.0"
!define PRODUCT_PUBLISHER "Applied Systems Biology - HKI Jena, Germany"
!define PRODUCT_WEB_SITE "https://www.jipipe.org/"
!define PRODUCT_DIR_REGKEY "Software\Microsoft\Windows\CurrentVersion\App Paths\JIPipe"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"
!define PRODUCT_UNINST_ROOT_KEY "HKLM"

; MUI 1.67 compatible ------
!include "MUI.nsh"

; MUI Settings
!define MUI_ABORTWARNING
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "header.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP "sidepanel.bmp"

; Welcome page
!insertmacro MUI_PAGE_WELCOME
; License page
!define MUI_LICENSEPAGE_RADIOBUTTONS
!insertmacro MUI_PAGE_LICENSE "LICENSE.rtf"
; Directory page
!insertmacro MUI_PAGE_DIRECTORY
; Instfiles page
!insertmacro MUI_PAGE_INSTFILES
; Finish page
!insertmacro MUI_PAGE_FINISH

; Uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

; Language files
!insertmacro MUI_LANGUAGE "English"

; MUI end ------

Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "JIPipe-${PRODUCT_VERSION}-Setup.exe"
InstallDir "$APPDATA\JIPipe"
InstallDirRegKey HKLM "${PRODUCT_DIR_REGKEY}" ""
ShowInstDetails show
ShowUnInstDetails show

Section "Common files (Required)" SEC01
  SetOutPath "$INSTDIR"

  ; Download ImageJ
  SetOverwrite ifnewer
  InitPluginsDir
  inetc::get "https://downloads.imagej.net/fiji/latest/fiji-win64.zip" "$INSTDIR\fiji-win64.zip"
  ;File "fiji-win64.zip"
  nsisunz::UnzipToLog "$INSTDIR\fiji-win64.zip" "$INSTDIR"
  Delete "$INSTDIR\fiji-win64.zip"

  ; Copy JIPipe icon
  File /oname=jipipe-icon.ico "..\..\jipipe-core\src\main\resources\org\hkijena\jipipe\icon.ico"

  ; Copy updater & JIPipe
  SetOutPath "$INSTDIR\Fiji.app\plugins"
  File "..\..\ij-updater-cli\target\ij-updater-cli-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-annotation\target\jipipe-annotation-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-clij\target\jipipe-clij-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-core\target\jipipe-core-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-filesystem\target\jipipe-filesystem-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-ij\target\jipipe-ij-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-ij-algorithms\target\jipipe-ij-algorithms-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-ij-omero\target\jipipe-ij-omero-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-ij-multi-template-matching\target\jipipe-ij-multi-template-matching-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-launcher\target\jipipe-launcher-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-multiparameters\target\jipipe-multiparameters-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-plots\target\jipipe-plots-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-python\target\jipipe-python-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-strings\target\jipipe-strings-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-tables\target\jipipe-tables-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-utils\target\jipipe-utils-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-forms\target\jipipe-forms-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-r\target\jipipe-r-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-cellpose\target\jipipe-cellpose-${PRODUCT_VERSION}.jar"
  File "..\..\jipipe-deep-learning\target\jipipe-deep-learning-${PRODUCT_VERSION}.jar"

  ; JIPipe dependencies
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/github/vatbub/mslinks/1.0.5/mslinks-1.0.5.jar" "$INSTDIR\Fiji.app\jars\mslinks-1.0.5.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/org/reflections/reflections/0.9.12/reflections-0.9.12.jar" "$INSTDIR\Fiji.app\jars\reflections-0.9.12.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util/0.62.2/flexmark-util-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-data/0.62.2/flexmark-util-data-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-data-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-ast/0.62.2/flexmark-util-ast-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-ast-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-misc/0.62.2/flexmark-util-misc-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-misc-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-dependency/0.62.2/flexmark-util-dependency-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-dependency-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-format/0.62.2/flexmark-util-format-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-format-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-sequence/0.62.2/flexmark-util-sequence-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-sequence-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-builder/0.62.2/flexmark-util-builder-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-builder-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-visitor/0.62.2/flexmark-util-visitor-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-visitor-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-options/0.62.2/flexmark-util-options-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-options-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-html/0.62.2/flexmark-util-html-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-html-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-collection/0.62.2/flexmark-util-collection-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-util-collection-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-pdf-converter/0.62.2/flexmark-pdf-converter-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-pdf-converter-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-toc/0.62.2/flexmark-ext-toc-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-ext-toc-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-autolink/0.62.2/flexmark-ext-autolink-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-ext-autolink-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark/0.62.2/flexmark-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-tables/0.62.2/flexmark-ext-tables-0.62.2.jar" "$INSTDIR\Fiji.app\jars\flexmark-ext-tables-0.62.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/releases/content/sc/fiji/Image_5D/2.0.2/Image_5D-2.0.2.jar" "$INSTDIR\Fiji.app\jars\Image_5D-2.0.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-databind/2.11.0/jackson-databind-2.11.0.jar" "$INSTDIR\Fiji.app\jars\jackson-databind-2.11.0.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-core/2.11.0/jackson-core-2.11.0.jar" "$INSTDIR\Fiji.app\jars\jackson-core-2.11.0.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-annotations/2.11.0/jackson-annotations-2.11.0.jar" "$INSTDIR\Fiji.app\jars\jackson-annotations-2.11.0.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-core/1.4.0/jgrapht-core-1.4.0.jar" "$INSTDIR\Fiji.app\jars\jgrapht-core-1.4.0.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/org/nibor/autolink/autolink/0.10.0/autolink-0.10.0.jar" "$INSTDIR\Fiji.app\jars\autolink-0.10.0.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/fontbox/2.0.4/fontbox-2.0.4.jar" "$INSTDIR\Fiji.app\jars\fontbox-2.0.4.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-jsoup-dom-converter/1.0.0/openhtmltopdf-jsoup-dom-converter-1.0.0.jar" "$INSTDIR\Fiji.app\jars\openhtmltopdf-jsoup-dom-converter-1.0.0.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-core/1.0.4/openhtmltopdf-core-1.0.4.jar" "$INSTDIR\Fiji.app\jars\openhtmltopdf-core-1.0.4.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/pdfbox/2.0.4/pdfbox-2.0.4.jar" "$INSTDIR\Fiji.app\jars\pdfbox-2.0.4.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-rtl-support/1.0.4/openhtmltopdf-rtl-support-1.0.4.jar" "$INSTDIR\Fiji.app\jars\openhtmltopdf-rtl-support-1.0.4.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.4/openhtmltopdf-pdfbox-1.0.4.jar" "$INSTDIR\Fiji.app\jars\openhtmltopdf-pdfbox-1.0.4.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/com/fathzer/javaluator/3.0.3/javaluator-3.0.3.jar" "$INSTDIR\Fiji.app\jars\javaluator-3.0.3.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/org/apache/commons/commons-exec/1.3/commons-exec-1.3.jar" "$INSTDIR\Fiji.app\jars\commons-exec-1.3.jar"
  inetc::get "https://github.com/ome/omero-insight/releases/download/v5.5.14/omero_ij-5.5.14-all.jar" "$INSTDIR\Fiji.app\plugins\omero_ij-5.5.14-all.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/central/content/net/java/dev/jna/jna-platform/4.5.2/jna-platform-4.5.2.jar" "$INSTDIR\Fiji.app\plugins\jna-platform-4.5.2.jar"
  inetc::get "https://maven.scijava.org/service/local/repositories/releases/content/de/biomedical-imaging/imagej/ij_ridge_detect/1.4.1/ij_ridge_detect-1.4.1.jar" "$INSTDIR\Fiji.app\plugins\ij_ridge_detect-1.4.1.jar"

  ; Bootstrap update sites
  SetOutPath "$INSTDIR\Fiji.app"
  nsExec::ExecToLog "$INSTDIR\Fiji.app\ImageJ-win64.exe --pass-classpath --full-classpath --main-class org.hkijena.ijupdatercli.Main activate clij clij2 IJPB-plugins ImageScience IJ-OpenCV-plugins Multi-Template-Matching"

  ; Start menu & desktop shortcuts
  CreateDirectory "$SMPROGRAMS\JIPipe"
  CreateShortCut "$SMPROGRAMS\JIPipe\JIPipe.lnk" "$INSTDIR\Fiji.app\ImageJ-win64.exe" "--pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher" "$INSTDIR\jipipe-icon.ico"
  CreateShortCut "$SMPROGRAMS\JIPipe\ImageJ+JIPipe.lnk" "$INSTDIR\Fiji.app\ImageJ-win64.exe"
  CreateShortCut "$DESKTOP\JIPipe.lnk" "$INSTDIR\Fiji.app\ImageJ-win64.exe" "--pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher" "$INSTDIR\jipipe-icon.ico"
  CreateShortCut "$DESKTOP\ImageJ+JIPipe.lnk" "$INSTDIR\Fiji.app\ImageJ-win64.exe"
SectionEnd

Section -AdditionalIcons
  CreateShortCut "$SMPROGRAMS\JIPipe\Uninstall.lnk" "$INSTDIR\uninst.exe"
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\Fiji.app\ImageJ-win64.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\Fiji.app\ImageJ-win64.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "Publisher" "${PRODUCT_PUBLISHER}"
SectionEnd


Function un.onUninstSuccess
  HideWindow
  MessageBox MB_ICONINFORMATION|MB_OK "$(^Name) was successfully removed."
FunctionEnd

Function un.onInit
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Do you really want to uninstall JIPipe?" IDYES +2
  Abort
FunctionEnd

Section Uninstall
  Delete "$SMPROGRAMS\JIPipe\Uninstall.lnk"
  Delete "$SMPROGRAMS\JIPipe\Website.lnk"
  Delete "$DESKTOP\JIPipe.lnk"
  Delete "$DESKTOP\ImageJ+JIPipe.lnk"
  Delete "$SMPROGRAMS\JIPipe\JIPipe.lnk"
  Delete "$SMPROGRAMS\JIPipe\ImageJ+JIPipe.lnk"

  RMDir "$SMPROGRAMS\JIPipe"
  RMDir /r "$INSTDIR"

  DeleteRegKey ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"
  SetAutoClose true
SectionEnd
