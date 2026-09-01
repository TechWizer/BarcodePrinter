package com.barcodeprinter.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates official Excel (.xlsx) and CSV (.csv) templates for users to fill in their products.
 * Columns: Barcode, Product Code, Product Name, Size, Price, QTY.
 */
public class TemplateGenerator {

    /**
     * Creates an Excel template file based on file extension (.xlsx or .csv).
     */
    public static void generateTemplateFile(File targetFile) throws IOException {
        if (targetFile.getName().toLowerCase().endsWith(".xlsx")) {
            generateXlsxTemplate(targetFile);
        } else {
            generateCsvTemplate(targetFile);
        }
    }

    /**
     * Generates a standard CSV template.
     */
    public static void generateCsvTemplate(File targetFile) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8);
        writer.write("Barcode,Product Code,Product Name,Size,Price,QTY\r\n");
        writer.write("750100990011,TS-001,Classic Crewneck T-Shirt,M,18.50,2\r\n");
        writer.write("750100990012,TS-002,Classic Crewneck T-Shirt,L,18.50,2\r\n");
        writer.write("750100990021,JN-101,Slim Fit Denim Jeans,32,45.00,1\r\n");
        writer.write("750100990031,HD-201,Cotton Pullover Hoodie,L,38.00,2\r\n");
        writer.flush();
        writer.close();
    }

    /**
     * Generates a native OpenXML (.xlsx) Excel template in pure Java.
     */
    public static void generateXlsxTemplate(File targetFile) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(targetFile));

        // 1. [Content_Types].xml
        zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
        byte[] contentTypes = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\r\n" +
                "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\r\n" +
                "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\r\n" +
                "  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\r\n" +
                "  <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\r\n" +
                "</Types>").getBytes(StandardCharsets.UTF_8);
        zos.write(contentTypes);
        zos.closeEntry();

        // 2. _rels/.rels
        zos.putNextEntry(new ZipEntry("_rels/.rels"));
        byte[] rels = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\r\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\r\n" +
                "</Relationships>").getBytes(StandardCharsets.UTF_8);
        zos.write(rels);
        zos.closeEntry();

        // 3. xl/workbook.xml
        zos.putNextEntry(new ZipEntry("xl/workbook.xml"));
        byte[] wb = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\r\n" +
                "  <sheets>\r\n" +
                "    <sheet name=\"Products\" sheetId=\"1\" r:id=\"rId1\"/>\r\n" +
                "  </sheets>\r\n" +
                "</workbook>").getBytes(StandardCharsets.UTF_8);
        zos.write(wb);
        zos.closeEntry();

        // 4. xl/_rels/workbook.xml.rels
        zos.putNextEntry(new ZipEntry("xl/_rels/workbook.xml.rels"));
        byte[] wbRels = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\r\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\r\n" +
                "</Relationships>").getBytes(StandardCharsets.UTF_8);
        zos.write(wbRels);
        zos.closeEntry();

        // 5. xl/worksheets/sheet1.xml
        zos.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
        StringBuilder sheetXml = new StringBuilder();
        sheetXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n");
        sheetXml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\r\n");
        sheetXml.append("  <sheetData>\r\n");

        // Row 1: Header (Barcode, Product Code, Product Name, Size, Price, QTY)
        sheetXml.append("    <row r=\"1\">\r\n");
        sheetXml.append("      <c r=\"A1\" t=\"inlineStr\"><is><t>Barcode</t></is></c>\r\n");
        sheetXml.append("      <c r=\"B1\" t=\"inlineStr\"><is><t>Product Code</t></is></c>\r\n");
        sheetXml.append("      <c r=\"C1\" t=\"inlineStr\"><is><t>Product Name</t></is></c>\r\n");
        sheetXml.append("      <c r=\"D1\" t=\"inlineStr\"><is><t>Size</t></is></c>\r\n");
        sheetXml.append("      <c r=\"E1\" t=\"inlineStr\"><is><t>Price</t></is></c>\r\n");
        sheetXml.append("      <c r=\"F1\" t=\"inlineStr\"><is><t>QTY</t></is></c>\r\n");
        sheetXml.append("    </row>\r\n");

        // Row 2: Sample item 1
        sheetXml.append("    <row r=\"2\">\r\n");
        sheetXml.append("      <c r=\"A2\" t=\"inlineStr\"><is><t>750100990011</t></is></c>\r\n");
        sheetXml.append("      <c r=\"B2\" t=\"inlineStr\"><is><t>TS-001</t></is></c>\r\n");
        sheetXml.append("      <c r=\"C2\" t=\"inlineStr\"><is><t>Classic Crewneck T-Shirt</t></is></c>\r\n");
        sheetXml.append("      <c r=\"D2\" t=\"inlineStr\"><is><t>M</t></is></c>\r\n");
        sheetXml.append("      <c r=\"E2\" t=\"inlineStr\"><is><t>18.50</t></is></c>\r\n");
        sheetXml.append("      <c r=\"F2\"><v>2</v></c>\r\n");
        sheetXml.append("    </row>\r\n");

        // Row 3: Sample item 2
        sheetXml.append("    <row r=\"3\">\r\n");
        sheetXml.append("      <c r=\"A3\" t=\"inlineStr\"><is><t>750100990012</t></is></c>\r\n");
        sheetXml.append("      <c r=\"B3\" t=\"inlineStr\"><is><t>TS-002</t></is></c>\r\n");
        sheetXml.append("      <c r=\"C3\" t=\"inlineStr\"><is><t>Classic Crewneck T-Shirt</t></is></c>\r\n");
        sheetXml.append("      <c r=\"D3\" t=\"inlineStr\"><is><t>L</t></is></c>\r\n");
        sheetXml.append("      <c r=\"E3\" t=\"inlineStr\"><is><t>18.50</t></is></c>\r\n");
        sheetXml.append("      <c r=\"F3\"><v>2</v></c>\r\n");
        sheetXml.append("    </row>\r\n");

        // Row 4: Sample item 3
        sheetXml.append("    <row r=\"4\">\r\n");
        sheetXml.append("      <c r=\"A4\" t=\"inlineStr\"><is><t>750100990021</t></is></c>\r\n");
        sheetXml.append("      <c r=\"B4\" t=\"inlineStr\"><is><t>JN-101</t></is></c>\r\n");
        sheetXml.append("      <c r=\"C4\" t=\"inlineStr\"><is><t>Slim Fit Denim Jeans</t></is></c>\r\n");
        sheetXml.append("      <c r=\"D4\" t=\"inlineStr\"><is><t>32</t></is></c>\r\n");
        sheetXml.append("      <c r=\"E4\" t=\"inlineStr\"><is><t>45.00</t></is></c>\r\n");
        sheetXml.append("      <c r=\"F4\"><v>1</v></c>\r\n");
        sheetXml.append("    </row>\r\n");

        sheetXml.append("  </sheetData>\r\n");
        sheetXml.append("</worksheet>");

        zos.write(sheetXml.toString().getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        zos.finish();
        zos.close();
    }
}
