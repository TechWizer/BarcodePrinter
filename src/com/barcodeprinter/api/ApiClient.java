package com.barcodeprinter.api;

import com.barcodeprinter.model.Product;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Universal API Client supporting standard REST APIs (Laravel, WooCommerce, Shopify, ERPNext)
 * and Odoo ERP External JSON-RPC API.
 */
public class ApiClient {

    /**
     * Fetches products from any REST API endpoint.
     */
    public static List<Product> fetchRestProducts(String endpointUrl, String authType, String tokenOrUser, String password, int timeoutSec) throws Exception {
        if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("API URL cannot be empty.");
        }

        URL url = new URL(endpointUrl.trim());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "BarcodePrinter/1.0");
        conn.setConnectTimeout(timeoutSec * 1000);
        conn.setReadTimeout(timeoutSec * 1000);

        // Apply Authentication Headers
        if ("Bearer Token".equalsIgnoreCase(authType) && tokenOrUser != null && !tokenOrUser.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + tokenOrUser.trim());
        } else if ("API Key (Header)".equalsIgnoreCase(authType) && tokenOrUser != null && !tokenOrUser.trim().isEmpty()) {
            conn.setRequestProperty("X-API-KEY", tokenOrUser.trim());
            conn.setRequestProperty("Authorization", tokenOrUser.trim());
        } else if ("Basic Auth".equalsIgnoreCase(authType) && tokenOrUser != null) {
            String credentials = tokenOrUser.trim() + ":" + (password != null ? password : "");
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encoded);
        }

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String errorMsg = readStream(conn.getErrorStream());
            throw new IOException("HTTP Error " + code + ": " + (errorMsg.isEmpty() ? conn.getResponseMessage() : errorMsg));
        }

        String jsonResponse = readStream(conn.getInputStream());
        return JsonProductParser.parseProducts(jsonResponse);
    }

    /**
     * Fetches products directly from Odoo ERP using Odoo JSON-RPC API.
     */
    public static List<Product> fetchOdooProducts(String odooBaseUrl, String db, String user, String password, int limit, int timeoutSec) throws Exception {
        if (odooBaseUrl == null || odooBaseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Odoo Server URL cannot be empty (e.g. http://localhost:8069).");
        }
        if (db == null || db.trim().isEmpty()) {
            throw new IllegalArgumentException("Odoo Database Name is required.");
        }
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("Odoo Username is required.");
        }

        String rpcUrl = odooBaseUrl.trim();
        if (!rpcUrl.endsWith("/jsonrpc")) {
            if (rpcUrl.endsWith("/")) rpcUrl = rpcUrl + "jsonrpc";
            else rpcUrl = rpcUrl + "/jsonrpc";
        }

        // 1. Authenticate with Odoo
        String authPayload = String.format(
                "{\"jsonrpc\": \"2.0\", \"method\": \"call\", \"params\": {\"service\": \"common\", \"method\": \"authenticate\", \"args\": [\"%s\", \"%s\", \"%s\", {}]}, \"id\": 1}",
                escapeJson(db), escapeJson(user), escapeJson(password != null ? password : ""));

        String authResponse = executeHttpPost(rpcUrl, authPayload, timeoutSec);

        // Extract UID (integer)
        int uid = parseOdooUid(authResponse);
        if (uid <= 0) {
            throw new IOException("Odoo Authentication failed! Please check your Database name, Username, and Password / API Key.");
        }

        // 2. Query product.product records
        int recordLimit = (limit > 0) ? limit : 500;
        String queryPayload = String.format(
                "{\"jsonrpc\": \"2.0\", \"method\": \"call\", \"params\": {\"service\": \"object\", \"method\": \"execute_kw\", \"args\": [\"%s\", %d, \"%s\", \"product.product\", \"search_read\", [[]], {\"fields\": [\"barcode\", \"default_code\", \"name\", \"list_price\", \"qty_available\"], \"limit\": %d}]}, \"id\": 2}",
                escapeJson(db), uid, escapeJson(password != null ? password : ""), recordLimit);

        String queryResponse = executeHttpPost(rpcUrl, queryPayload, timeoutSec);

        return JsonProductParser.parseProducts(queryResponse);
    }

    private static String executeHttpPost(String endpointUrl, String jsonBody, int timeoutSec) throws Exception {
        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(timeoutSec * 1000);
        conn.setReadTimeout(timeoutSec * 1000);
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String err = readStream(conn.getErrorStream());
            throw new IOException("HTTP Error " + code + ": " + (err.isEmpty() ? conn.getResponseMessage() : err));
        }

        return readStream(conn.getInputStream());
    }

    private static int parseOdooUid(String authResponse) {
        try {
            if (authResponse == null || !authResponse.contains("\"result\"")) return -1;
            int idx = authResponse.indexOf("\"result\":");
            if (idx != -1) {
                String sub = authResponse.substring(idx + 9).trim();
                if (sub.startsWith("false") || sub.startsWith("null")) return -1;
                StringBuilder digits = new StringBuilder();
                for (int i = 0; i < sub.length(); i++) {
                    char c = sub.charAt(i);
                    if (Character.isDigit(c)) digits.append(c);
                    else if (digits.length() > 0) break;
                }
                if (digits.length() > 0) return Integer.parseInt(digits.toString());
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
