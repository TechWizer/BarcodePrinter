package com.barcodeprinter.parser;

import com.barcodeprinter.model.Product;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Universal parser for Excel (.xlsx) and Delimited files (.csv, .tsv, .txt).
 * Automatically detects: Barcode, Product Code, Product Name, Size, Price, QTY.
 */
public class ExcelParser {

    /**
     * Parses a file (XLSX, CSV, TSV) and returns a list of Products.
     */
    public static List<Product> parseFile(File file) throws Exception {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".xlsx")) {
            return parseXlsx(file);
        } else {
            return parseCsv(file);
        }
    }

    // ==========================================
    // XLSX (OpenXML) Built-in Parser
    // ==========================================

    private static List<Product> parseXlsx(File file) throws Exception {
        ZipFile zip = new ZipFile(file);
        try {
            // 1. Read shared strings if available
            List<String> sharedStrings = new ArrayList<String>();
            ZipEntry sstEntry = zip.getEntry("xl/sharedStrings.xml");
            if (sstEntry != null) {
                InputStream is = zip.getInputStream(sstEntry);
                sharedStrings = parseSharedStrings(is);
                is.close();
            }

            // 2. Read first worksheet
            ZipEntry sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml");
            if (sheetEntry == null) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("xl/worksheets/sheet") && entry.getName().endsWith(".xml")) {
                        sheetEntry = entry;
                        break;
                    }
                }
            }

            if (sheetEntry == null) {
                throw new IOException("No worksheet found in Excel file: " + file.getName());
            }

            InputStream sheetIs = zip.getInputStream(sheetEntry);
            List<List<String>> rows = parseSheetXml(sheetIs, sharedStrings);
            sheetIs.close();

            return convertRowsToProducts(rows);
        } finally {
            zip.close();
        }
    }

    private static List<String> parseSharedStrings(InputStream is) throws Exception {
        List<String> list = new ArrayList<String>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);

        NodeList siNodes = doc.getElementsByTagName("si");
        for (int i = 0; i < siNodes.getLength(); i++) {
            Element si = (Element) siNodes.item(i);
            NodeList tNodes = si.getElementsByTagName("t");
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < tNodes.getLength(); j++) {
                sb.append(tNodes.item(j).getTextContent());
            }
            list.add(sb.toString());
        }
        return list;
    }

    private static List<List<String>> parseSheetXml(InputStream is, List<String> sharedStrings) throws Exception {
        List<List<String>> rows = new ArrayList<List<String>>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);

        NodeList rowNodes = doc.getElementsByTagName("row");
        Pattern colPattern = Pattern.compile("^([A-Z]+)(\\d+)$");

        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element rowEl = (Element) rowNodes.item(i);
            NodeList cNodes = rowEl.getElementsByTagName("c");

            Map<Integer, String> cellMap = new HashMap<Integer, String>();
            int maxCol = -1;

            for (int j = 0; j < cNodes.getLength(); j++) {
                Element cEl = (Element) cNodes.item(j);
                String cellRef = cEl.getAttribute("r");
                String cellType = cEl.getAttribute("t");

                int colIdx = j;
                if (cellRef != null && !cellRef.isEmpty()) {
                    Matcher m = colPattern.matcher(cellRef);
                    if (m.find()) {
                        colIdx = columnNameToIndex(m.group(1));
                    }
                }
                maxCol = Math.max(maxCol, colIdx);

                String value = "";
                NodeList vList = cEl.getElementsByTagName("v");
                if (vList.getLength() > 0) {
                    String rawVal = vList.item(0).getTextContent();
                    if ("s".equals(cellType)) {
                        try {
                            int sIndex = Integer.parseInt(rawVal);
                            if (sIndex >= 0 && sIndex < sharedStrings.size()) {
                                value = sharedStrings.get(sIndex);
                            }
                        } catch (NumberFormatException ignored) {
                            value = rawVal;
                        }
                    } else {
                        value = rawVal;
                    }
                } else {
                    NodeList isList = cEl.getElementsByTagName("t");
                    if (isList.getLength() > 0) {
                        value = isList.item(0).getTextContent();
                    }
                }

                cellMap.put(colIdx, value != null ? value.trim() : "");
            }

            if (maxCol >= 0) {
                List<String> rowData = new ArrayList<String>();
                for (int col = 0; col <= maxCol; col++) {
                    rowData.add(cellMap.containsKey(col) ? cellMap.get(col) : "");
                }
                rows.add(rowData);
            }
        }
        return rows;
    }

    private static int columnNameToIndex(String colName) {
        int index = 0;
        for (int i = 0; i < colName.length(); i++) {
            index = index * 26 + (colName.charAt(i) - 'A' + 1);
        }
        return index - 1;
    }

    // ==========================================
    // CSV / TSV Parser
    // ==========================================

    private static List<Product> parseCsv(File file) throws Exception {
        List<List<String>> rows = new ArrayList<List<String>>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        String line;
        char delimiter = detectDelimiter(file);

        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            List<String> tokens = parseCsvLine(line, delimiter);
            rows.add(tokens);
        }
        br.close();

        return convertRowsToProducts(rows);
    }

    private static char detectDelimiter(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine != null) {
                int commas = countOccurrences(firstLine, ',');
                int tabs = countOccurrences(firstLine, '\t');
                int semicolons = countOccurrences(firstLine, ';');
                if (tabs > commas && tabs > semicolons) return '\t';
                if (semicolons > commas && semicolons > tabs) return ';';
            }
        } catch (Exception ignored) {}
        return ',';
    }

    private static int countOccurrences(String str, char ch) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == ch) count++;
        }
        return count;
    }

    private static List<String> parseCsvLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }

    // ==========================================
    // Product Conversion & Header Auto-Detection
    // ==========================================

    private static List<Product> convertRowsToProducts(List<List<String>> rows) {
        List<Product> products = new ArrayList<Product>();
        if (rows == null || rows.isEmpty()) return products;

        int headerRowIndex = -1;
        int barcodeCol = -1;
        int codeCol = -1;
        int nameCol = -1;
        int sizeCol = -1;
        int priceCol = -1;
        int qtyCol = -1;

        // Search for header row in top 5 rows
        for (int i = 0; i < Math.min(5, rows.size()); i++) {
            List<String> row = rows.get(i);
            int b = -1, c = -1, n = -1, s = -1, p = -1, q = -1;

            for (int col = 0; col < row.size(); col++) {
                String val = row.get(col).toLowerCase().trim();
                if (val.isEmpty()) continue;

                if (b == -1 && (val.equals("barcode") || val.contains("bar_code") || val.contains("barcode") || val.contains("ean") || val.contains("upc"))) {
                    b = col;
                } else if (c == -1 && (val.contains("product code") || val.contains("product_code") || val.contains("item code") || val.contains("item_code") || val.contains("sku") || val.equals("code") || val.contains("style"))) {
                    c = col;
                } else if (n == -1 && (val.contains("product name") || val.contains("product_name") || val.contains("name") || val.contains("title") || val.contains("item") || val.contains("desc"))) {
                    n = col;
                } else if (s == -1 && (val.contains("size") || val.contains("variant") || val.contains("dimension") || val.contains("type"))) {
                    s = col;
                } else if (p == -1 && (val.contains("price") || val.contains("cost") || val.contains("mrp") || val.contains("amount") || val.contains("rate"))) {
                    p = col;
                } else if (q == -1 && (val.contains("qty") || val.contains("quantity") || val.contains("count"))) {
                    q = col;
                }
            }

            if (b != -1 || n != -1 || c != -1) {
                headerRowIndex = i;
                barcodeCol = b;
                codeCol = c;
                nameCol = n;
                sizeCol = s;
                priceCol = p;
                qtyCol = q;
                break;
            }
        }

        // If no header found, default to template order: 0=Barcode, 1=Code, 2=Name, 3=Size, 4=Price, 5=QTY
        if (headerRowIndex == -1) {
            headerRowIndex = -1;
            barcodeCol = 0;
            codeCol = 1;
            nameCol = 2;
            sizeCol = 3;
            priceCol = 4;
            qtyCol = 5;
        }

        int startRow = headerRowIndex + 1;
        for (int i = startRow; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isRowEmpty(row)) continue;

            String barcode = cleanBarcode(getCell(row, barcodeCol));
            String code = getCell(row, codeCol);
            String name = getCell(row, nameCol);
            String size = getCell(row, sizeCol);
            String price = cleanPrice(getCell(row, priceCol));
            int qty = 1;

            if (qtyCol != -1) {
                String qtyStr = getCell(row, qtyCol);
                try {
                    qty = Math.max(1, (int) Double.parseDouble(qtyStr));
                } catch (Exception ignored) {}
            }

            if (barcode.isEmpty() && name.isEmpty() && code.isEmpty()) continue;

            products.add(new Product(barcode, code, name, size, price, qty));
        }

        return products;
    }

    private static String getCell(List<String> row, int col) {
        if (col >= 0 && col < row.size()) {
            return row.get(col).trim();
        }
        return "";
    }

    private static boolean isRowEmpty(List<String> row) {
        for (String s : row) {
            if (!s.trim().isEmpty()) return false;
        }
        return true;
    }

    private static String cleanPrice(String val) {
        if (val == null) return "";
        try {
            if (val.contains("E") || val.contains("e")) {
                double d = Double.parseDouble(val);
                return String.format("%.2f", d);
            }
        } catch (Exception ignored) {}
        return val.trim();
    }

    private static String cleanBarcode(String val) {
        if (val == null) return "";
        try {
            if (val.contains("E") || val.contains("e")) {
                double d = Double.parseDouble(val);
                long l = (long) d;
                return String.valueOf(l);
            }
            if (val.endsWith(".0")) {
                return val.substring(0, val.length() - 2);
            }
        } catch (Exception ignored) {}
        return val.trim();
    }
}
