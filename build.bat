@echo off
chcp 65001 > nul
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin -sourcepath src src/com/barcodeprinter/model/*.java src/com/barcodeprinter/generator/*.java src/com/barcodeprinter/parser/*.java src/com/barcodeprinter/service/*.java src/com/barcodeprinter/api/*.java src/com/barcodeprinter/util/*.java src/com/barcodeprinter/ui/*.java src/com/barcodeprinter/*.java
if %ERRORLEVEL% EQU 0 (
    jar cfm BarcodePrinter.jar manifest.mf -C bin .
    echo [SUCCESS] BarcodePrinter.jar created successfully!
) else (
    echo [ERROR] Build failed!
)
pause
