@echo off
chcp 65001 > nul
set SCRIPT="%TEMP%\%RANDOM%-%RANDOM%_create_shortcut.vbs"
echo Set oWS = WScript.CreateObject("WScript.Shell") > %SCRIPT%
echo sLinkFile = oWS.SpecialFolders("Desktop") ^& "\Winpal Barcode Printer.lnk" >> %SCRIPT%
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> %SCRIPT%
echo oLink.TargetPath = "%~dp0run.bat" >> %SCRIPT%
echo oLink.WorkingDirectory = "%~dp0" >> %SCRIPT%
echo oLink.Description = "Winpal Barcode Label Printer" >> %SCRIPT%
echo oLink.IconLocation = "%~dp0susitk.ico,0" >> %SCRIPT%
echo oLink.Save >> %SCRIPT%
cscript /nologo %SCRIPT%
del %SCRIPT%
echo [SUCCESS] Desktop shortcut 'Winpal Barcode Printer' created with custom icon!
pause