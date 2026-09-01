package com.barcodeprinter.service;

import javax.print.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends raw TSPL/printer byte streams directly to local USB / Windows Spooler printers.
 * Uses standard javax.print PrintService with BYTE_STREAM.AUTOSENSE flavor.
 */
public class RawPrintService {

    /**
     * Lists all installed printers recognized by the Operating System.
     */
    public static List<String> getAvailablePrinters() {
        List<String> printerNames = new ArrayList<String>();
        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService service : services) {
                printerNames.add(service.getName());
            }
        } catch (Exception e) {
            System.err.println("Error discovering printers: " + e.getMessage());
        }
        return printerNames;
    }

    /**
     * Gets the system default printer name if configured.
     */
    public static String getDefaultPrinterName() {
        try {
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultService != null) {
                return defaultService.getName();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Sends raw command string directly to the designated printer without opening any dialogs.
     */
    public static void printRaw(String printerName, String rawData, String jobTitle) throws Exception {
        if (printerName == null || printerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Printer name cannot be empty. Please select a printer.");
        }
        if (rawData == null || rawData.trim().isEmpty()) {
            throw new IllegalArgumentException("No print commands to send.");
        }

        PrintService targetService = null;
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(printerName.trim())) {
                targetService = service;
                break;
            }
        }

        if (targetService == null) {
            throw new PrintException("Printer not found: " + printerName);
        }

        byte[] bytes = rawData.getBytes(StandardCharsets.ISO_8859_1);
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(bytes, flavor, null);

        DocPrintJob printJob = targetService.createPrintJob();
        printJob.print(doc, null);
    }
}
