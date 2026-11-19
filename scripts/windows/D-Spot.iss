; D-Spot - Inno Setup script (needs Inno Setup 6.3 or newer)
;
; Put this file at: scripts/windows/D-Spot.iss
; Build:  iscc /DAppVersion=2.3.0 scripts\windows\D-Spot.iss
; Output: scripts\windows\Output\D-Spot-Windows-Setup.exe
;
; Before compiling, create the app folder with Compose Desktop (must run on Windows):
;   ./gradlew :composeApp:mpvSetupAll :desktopApp:createReleaseDistributable
; then check that SourceDir below points to the folder that contains D-Spot.exe.

#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif

#define AppName       "D-Spot"
#define AppPublisher  "Mika"
#define AppURL        "https://dspot-web.vercel.app/"
#define AppExeName    "D-Spot.exe"

; Folder made by createReleaseDistributable (relative to this .iss file).
; If your build puts it somewhere else, only change this line.
#define SourceDir     "..\..\desktopApp\build\compose\binaries\main-release\app\D-Spot"

[Setup]
; NEVER change this GUID. Updates use it to know it's the same app.
AppId={{D0EDAF80-A428-4DF9-808D-41048E42FB54}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}
AppComments=Your music. Your library. Your listening space.

; Install per-user by default (no admin / UAC prompt). The user can still choose
; "all users" in the dialog at the start.
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
DisableDirPage=auto

; 64-bit only (same as your current MSIX)
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

; Look
SetupIconFile=..\..\composeApp\icon\circle_app_icon.ico
UninstallDisplayIcon={app}\{#AppExeName}
WizardStyle=modern
Compression=lzma2/ultra64
SolidCompression=yes

; Output
OutputDir=Output
OutputBaseFilename=D-Spot-Windows-Setup

; Auto-update support: close the running app, install over it, start it again.
CloseApplications=yes
RestartApplications=yes
UsePreviousAppDir=yes
UsePreviousTasks=yes

; Optional: text pages in the wizard (create the files, then remove the semicolons)
;LicenseFile=..\..\LICENSE
;InfoBeforeFile=welcome.txt

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "startmenuicon"; Description: "Create a Start menu shortcut"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Tasks: startmenuicon
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Tasks: desktopicon

[Registry]
; Deep links: dspot:// (website) and wordbyword:// (Last.fm login callback).
; HKA = HKCU for per-user installs, HKLM for all-users installs.
Root: HKA; Subkey: "Software\Classes\dspot"; ValueType: string; ValueName: ""; ValueData: "URL:D-Spot"; Flags: uninsdeletekey
Root: HKA; Subkey: "Software\Classes\dspot"; ValueType: string; ValueName: "URL Protocol"; ValueData: ""
Root: HKA; Subkey: "Software\Classes\dspot\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#AppExeName}"" ""%1"""
Root: HKA; Subkey: "Software\Classes\wordbyword"; ValueType: string; ValueName: ""; ValueData: "URL:D-Spot Last.fm"; Flags: uninsdeletekey
Root: HKA; Subkey: "Software\Classes\wordbyword"; ValueType: string; ValueName: "URL Protocol"; ValueData: ""
Root: HKA; Subkey: "Software\Classes\wordbyword\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#AppExeName}"" ""%1"""

[Run]
; Normal install: "Launch D-Spot" checkbox on the last page.
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent
; Silent update (/VERYSILENT): start the app again automatically.
Filename: "{app}\{#AppExeName}"; Flags: nowait; Check: WizardSilent
