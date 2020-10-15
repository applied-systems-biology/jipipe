RequestExecutionLevel user

; HM NIS Edit Wizard helper defines
!define PRODUCT_NAME "JIPipe"
!define PRODUCT_VERSION "2020.10"
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
!define MUI_HEADERIMAGE_BITMAP "header.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP "sidepanel.bmp"

; Welcome page
!insertmacro MUI_PAGE_WELCOME
; License page
!define MUI_LICENSEPAGE_RADIOBUTTONS
!insertmacro MUI_PAGE_LICENSE "..\..\LICENSE"
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
  SetOverwrite ifnewer
  File /r "Fiji.app\*"
  CreateDirectory "$SMPROGRAMS\JIPipe"
  CreateShortCut "$SMPROGRAMS\JIPipe\JIPipe.lnk" "$INSTDIR\ImageJ-win64.exe" "--pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher" "$INSTDIR\jipipe-icon.ico"
  CreateShortCut "$SMPROGRAMS\JIPipe\ImageJ+JIPipe.lnk" "$INSTDIR\ImageJ-win64.exe"
  CreateShortCut "$DESKTOP\JIPipe.lnk" "$INSTDIR\ImageJ-win64.exe" "--pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher" "$INSTDIR\jipipe-icon.ico"
  CreateShortCut "$DESKTOP\ImageJ+JIPipe.lnk" "$INSTDIR\ImageJ-win64.exe"
SectionEnd

Section -AdditionalIcons
  CreateShortCut "$SMPROGRAMS\JIPipe\Uninstall.lnk" "$INSTDIR\uninst.exe"
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\ImageJ-win64.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\ImageJ-win64.exe"
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
  Delete "$SMPROGRAMS\JIPipe\JIPipe.lnk"

  RMDir /r "$SMPROGRAMS\JIPipe"
  RMDir "$INSTDIR"

  DeleteRegKey ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"
  SetAutoClose true
SectionEnd
