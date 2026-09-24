@echo off
chcp 65001 > nul
if not exist bin mkdir bin
if not exist bin\resources mkdir bin\resources
if exist src\resources xcopy /Y /Q /E src\resources\* bin\resources\ > nul 2>&1
if exist susitk.png copy /Y susitk.png bin\resources\susitk.png > nul 2>&1
if exist susitk.ico copy /Y susitk.ico bin\resources\susitk.ico > nul 2>&1
javac -encoding UTF-8 -d bin -sourcepath src src/com/barcodeprinter/model/*.java src/com/barcodeprinter/generator/*.java src/com/barcodeprinter/parser/*.java src/com/barcodeprinter/service/*.java src/com/barcodeprinter/api/*.java src/com/barcodeprinter/util/*.java src/com/barcodeprinter/ui/*.java src/com/barcodeprinter/*.java
if %ERRORLEVEL% EQU 0 (
    jar cfm BarcodePrinter.jar manifest.mf -C bin .
    echo [SUCCESS] BarcodePrinter.jar created successfully!
) else (
    echo [ERROR] Build failed!
)
pause