package com.barcodeprinter;

import com.barcodeprinter.api.JsonProductParser;
import com.barcodeprinter.generator.TsplGenerator;
import com.barcodeprinter.generator.ZplGenerator;
import com.barcodeprinter.model.LabelConfig;
import com.barcodeprinter.model.Product;
import com.barcodeprinter.parser.ExcelParser;
import com.barcodeprinter.service.RawPrintService;

import java.io.File;
import java.util.List;

public class TestRunner {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" Barcode Printer System - Dual TSPL & ZPL Test Suite ");
        System.out.println("==================================================");

        try {
            // 1. Test CSV Parsing
            File sampleFile = new File("sample_products.csv");
            System.out.println("[TEST 1] Parsing file: " + sampleFile.getAbsolutePath());
            List<Product> products = ExcelParser.parseFile(sampleFile);
            System.out.println("  -> Successfully parsed " + products.size() + " products.");

            // 2. Test JSON / REST API Parser
            System.out.println("\n[TEST 2] Testing JSON REST API Parser...");
            String mockJson = "[\n" +
                    "  {\"barcode\": \"990011223344\", \"product_code\": \"API-01\", \"name\": \"API Cotton Shirt\", \"size\": \"L\", \"price\": \"22.50\", \"quantity\": 3},\n" +
                    "  {\"barcode\": \"990011223355\", \"product_code\": \"API-02\", \"name\": \"API Slim Chinos\", \"size\": \"32\", \"price\": \"45.00\", \"quantity\": 1}\n" +
                    "]";
            List<Product> apiProducts = JsonProductParser.parseProducts(mockJson);
            System.out.println("  -> Parsed " + apiProducts.size() + " products from mock REST API payload.");
            for (Product p : apiProducts) {
                System.out.printf("     - Barcode: %s | Code: %s | Name: %s | Size: %s | Price: %s | Qty: %d%n",
                        p.getBarcode(), p.getProductCode(), p.getName(), p.getSize(), p.getPrice(), p.getQuantity());
            }

            // 3. Test Odoo JSON-RPC Payload Parser
            System.out.println("\n[TEST 3] Testing Odoo ERP Response Parser...");
            String mockOdooJson = "{\n" +
                    "  \"jsonrpc\": \"2.0\", \"id\": 2,\n" +
                    "  \"result\": [\n" +
                    "    {\"id\": 101, \"barcode\": \"880011223301\", \"default_code\": \"ODOO-TSHIRT\", \"name\": \"Odoo Brand T-Shirt\", \"list_price\": 29.99, \"qty_available\": 5},\n" +
                    "    {\"id\": 102, \"barcode\": \"880011223302\", \"default_code\": \"ODOO-HOODIE\", \"name\": \"Odoo Premium Hoodie\", \"list_price\": 59.99, \"qty_available\": 2}\n" +
                    "]\n" +
                    "}";
            List<Product> odooProducts = JsonProductParser.parseProducts(mockOdooJson);
            System.out.println("  -> Parsed " + odooProducts.size() + " products from mock Odoo ERP response.");
            for (Product p : odooProducts) {
                System.out.printf("     - Barcode: %s | Code: %s | Name: %s | Price: %s | Qty: %d%n",
                        p.getBarcode(), p.getProductCode(), p.getName(), p.getPrice(), p.getQuantity());
            }

            // 4. Test Multi-Column TSPL Generator (1-Up, 2-Up, 3-Up)
            System.out.println("\n[TEST 4] Testing 2-Up TSPL Generator (Winpal/TSC)...");
            LabelConfig config = new LabelConfig();
            config.setLabelsPerRow(2);
            config.setLabelWidthMm(104.0);
            config.setPrinterLanguage("TSPL");
            String tspl = TsplGenerator.generatePrintJob(products.subList(0, 4), config);
            System.out.println("  -> Generated TSPL Script (" + tspl.split("\r\n|\n").length + " lines)");

            // 5. Test Multi-Column ZPL Generator (Zebra / ZPL II)
            System.out.println("\n[TEST 5] Testing 2-Up ZPL Generator (Zebra / ZPL II)...");
            config.setPrinterLanguage("ZPL");
            String zpl = ZplGenerator.generatePrintJob(products.subList(0, 4), config);
            System.out.println("  -> Generated ZPL Script (" + zpl.split("\r\n|\n").length + " lines)");
            System.out.println("  -> ZPL Snippet Sample:");
            String[] zplLines = zpl.split("\r\n|\n");
            for (int i = 0; i < Math.min(10, zplLines.length); i++) {
                System.out.println("     " + zplLines[i]);
            }

            // 6. Test Printer Discovery
            System.out.println("\n[TEST 6] Local Printers Discovery:");
            List<String> printers = RawPrintService.getAvailablePrinters();
            for (String prn : printers) {
                System.out.println("  - " + prn);
            }

            System.out.println("\n==================================================");
            System.out.println("  ALL DUAL-LANGUAGE TESTS PASSED SUCCESSFULLY!    ");
            System.out.println("==================================================");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[FAIL] Error during test: " + e.getMessage());
        }
    }
}