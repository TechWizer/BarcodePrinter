package com.barcodeprinter.api;

import com.barcodeprinter.model.Product;

import java.util.*;

/**
 * Universal zero-dependency JSON parser for REST API and ERP payloads (Odoo, Laravel, Shopify, WooCommerce).
 * Seamlessly handles top-level arrays [...], wrapped objects {"result": [...]}, {"data": [...]}, {"products": [...]}.
 */
public class JsonProductParser {

    /**
     * Parses any JSON payload and extracts all Product entities.
     */
    public static List<Product> parseProducts(String json) throws Exception {
        List<Product> products = new ArrayList<Product>();
        if (json == null || json.trim().isEmpty()) return products;

        json = json.trim();
        List<Map<String, String>> records = extractAllRecordObjects(json);

        for (Map<String, String> map : records) {
            String barcode = findField(map, "barcode", "bar_code", "ean", "upc", "sku", "code128");
            String code = findField(map, "product_code", "default_code", "item_code", "code", "sku", "art_no", "model");
            String name = findField(map, "product_name", "display_name", "name", "title", "description", "item");
            String size = findField(map, "size", "variant", "dimension", "type");
            String price = findField(map, "price", "list_price", "unit_price", "mrp", "cost", "amount", "rate");
            String qtyStr = findField(map, "qty", "quantity", "qty_available", "stock", "count");

            int qty = 1;
            if (!qtyStr.isEmpty()) {
                try {
                    qty = Math.max(1, (int) Double.parseDouble(qtyStr));
                } catch (Exception ignored) {}
            }

            // Only add if it contains product identifiers
            if (!name.isEmpty() || !barcode.isEmpty() || !code.isEmpty()) {
                products.add(new Product(barcode, code, name, size, price, qty));
            }
        }

        return products;
    }

    private static String findField(Map<String, String> map, String... possibleKeys) {
        for (String key : possibleKeys) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue() != null ? entry.getValue().trim() : "";
                }
            }
        }
        return "";
    }

    /**
     * Finds and extracts all distinct JSON object blocks `{ ... }` from anywhere in the JSON payload.
     */
    private static List<Map<String, String>> extractAllRecordObjects(String json) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        int len = json.length();
        boolean inQuotes = false;
        boolean escape = false;

        StringBuilder objBuffer = new StringBuilder();
        int braceDepth = 0;

        for (int i = 0; i < len; i++) {
            char c = json.charAt(i);

            if (escape) {
                if (braceDepth > 0) objBuffer.append(c);
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                if (braceDepth > 0) objBuffer.append(c);
                continue;
            }

            if (c == '\"') {
                inQuotes = !inQuotes;
                if (braceDepth > 0) objBuffer.append(c);
                continue;
            }

            if (!inQuotes) {
                if (c == '{') {
                    braceDepth++;
                    if (braceDepth == 1) {
                        objBuffer.setLength(0);
                    } else if (braceDepth == 2) {
                        // Found nested item inside an outer wrapper (e.g. inside {"result": [ {item1}, {item2} ]})
                        objBuffer.setLength(0);
                    }
                    objBuffer.append(c);
                } else if (c == '}') {
                    objBuffer.append(c);
                    if (braceDepth == 2) {
                        // Completed a nested item
                        Map<String, String> parsed = parseSingleJsonObject(objBuffer.toString());
                        if (!parsed.isEmpty() && isProductRecord(parsed)) {
                            list.add(parsed);
                        }
                        objBuffer.setLength(0);
                    } else if (braceDepth == 1) {
                        // Completed top-level object
                        Map<String, String> parsed = parseSingleJsonObject(objBuffer.toString());
                        if (!parsed.isEmpty() && isProductRecord(parsed)) {
                            list.add(parsed);
                        }
                    }
                    braceDepth--;
                } else if (braceDepth > 0) {
                    objBuffer.append(c);
                }
            } else {
                if (braceDepth > 0) objBuffer.append(c);
            }
        }

        return list;
    }

    private static boolean isProductRecord(Map<String, String> map) {
        return map.containsKey("barcode") || map.containsKey("bar_code") ||
                map.containsKey("default_code") || map.containsKey("product_code") ||
                map.containsKey("name") || map.containsKey("display_name") ||
                map.containsKey("list_price") || map.containsKey("price");
    }

    private static Map<String, String> parseSingleJsonObject(String jsonBlock) {
        Map<String, String> map = new HashMap<String, String>();
        if (jsonBlock == null || jsonBlock.length() < 2) return map;

        String content = jsonBlock.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);

        List<String> pairs = splitJsonKeyValuePairs(content);
        for (String pair : pairs) {
            int colonIdx = findColonSeparator(pair);
            if (colonIdx != -1) {
                String key = cleanJsonToken(pair.substring(0, colonIdx));
                String value = cleanJsonToken(pair.substring(colonIdx + 1));
                if (!key.isEmpty()) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private static List<String> splitJsonKeyValuePairs(String s) {
        List<String> list = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        boolean escape = false;
        int depth = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                sb.append(c);
                continue;
            }
            if (c == '\"') {
                inQuotes = !inQuotes;
                sb.append(c);
                continue;
            }
            if (!inQuotes) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ',' && depth == 0) {
                    list.add(sb.toString().trim());
                    sb.setLength(0);
                    continue;
                }
            }
            sb.append(c);
        }
        if (sb.length() > 0 && !sb.toString().trim().isEmpty()) {
            list.add(sb.toString().trim());
        }
        return list;
    }

    private static int findColonSeparator(String s) {
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\"') inQuotes = !inQuotes;
            else if (c == ':' && !inQuotes) return i;
        }
        return -1;
    }

    private static String cleanJsonToken(String token) {
        if (token == null) return "";
        token = token.trim();
        if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
            token = token.substring(1, token.length() - 1);
        }
        return token.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\/", "/").trim();
    }
}
