package com.barcodeprinter.util;

import com.barcodeprinter.model.LabelConfig;

import java.util.prefs.Preferences;

/**
 * Manages persistent user preferences (Printers, Labels per row, Margins, Dimensions, and ERP/API configurations).
 * Uses standard Java Preferences API.
 */
public class SettingsManager {

    private static final Preferences prefs = Preferences.userNodeForPackage(SettingsManager.class);

    // Printer Keys
    private static final String KEY_PRINTER_MODE = "printer_mode"; // "USB" or "NETWORK"
    private static final String KEY_PRINTER_NAME = "printer_name";
    private static final String KEY_NET_IP = "net_ip";
    private static final String KEY_NET_PORT = "net_port";

    // Layout & Margin Keys
    private static final String KEY_LABELS_PER_ROW = "labels_per_row";
    private static final String KEY_LABEL_WIDTH = "label_width";
    private static final String KEY_LABEL_HEIGHT = "label_height";
    private static final String KEY_GAP = "label_gap";
    private static final String KEY_H_GAP = "label_h_gap";
    private static final String KEY_DENSITY = "print_density";
    private static final String KEY_SPEED = "print_speed";
    private static final String KEY_LEFT_X = "left_x";
    private static final String KEY_TOP_Y = "top_y";
    private static final String KEY_RIGHT_X = "right_x";
    private static final String KEY_CURRENCY = "currency_symbol";
    private static final String KEY_SHOW_SIZE = "show_size";
    private static final String KEY_SHOW_PRICE = "show_price";
    private static final String KEY_SHOW_BC_TEXT = "show_barcode_text";

    // API / ERP Integration Keys
    private static final String KEY_API_TYPE = "api_type"; // "REST" or "ODOO"
    private static final String KEY_API_URL = "api_url";
    private static final String KEY_API_AUTH_TYPE = "api_auth_type";
    private static final String KEY_API_TOKEN = "api_token";
    private static final String KEY_API_USER = "api_user";
    private static final String KEY_API_PASS = "api_pass";
    private static final String KEY_ODOO_DB = "odoo_db";

    public static String getPrinterMode() {
        return prefs.get(KEY_PRINTER_MODE, "USB");
    }

    public static void setPrinterMode(String mode) {
        prefs.put(KEY_PRINTER_MODE, mode != null ? mode : "USB");
    }

    public static String getPrinterName(String fallback) {
        return prefs.get(KEY_PRINTER_NAME, fallback != null ? fallback : "");
    }

    public static void setPrinterName(String name) {
        if (name != null) {
            prefs.put(KEY_PRINTER_NAME, name);
        }
    }

    public static String getNetworkIp() {
        return prefs.get(KEY_NET_IP, "192.168.1.200");
    }

    public static void setNetworkIp(String ip) {
        if (ip != null) {
            prefs.put(KEY_NET_IP, ip.trim());
        }
    }

    public static int getNetworkPort() {
        return prefs.getInt(KEY_NET_PORT, 9100);
    }

    public static void setNetworkPort(int port) {
        prefs.putInt(KEY_NET_PORT, port > 0 ? port : 9100);
    }

    public static void loadConfig(LabelConfig config) {
        if (config == null) return;
        config.setLabelsPerRow(prefs.getInt(KEY_LABELS_PER_ROW, config.getLabelsPerRow()));
        config.setLabelWidthMm(prefs.getDouble(KEY_LABEL_WIDTH, config.getLabelWidthMm()));
        config.setLabelHeightMm(prefs.getDouble(KEY_LABEL_HEIGHT, config.getLabelHeightMm()));
        config.setGapMm(prefs.getDouble(KEY_GAP, config.getGapMm()));
        config.setHorizontalGapMm(prefs.getDouble(KEY_H_GAP, config.getHorizontalGapMm()));
        config.setDarkness(prefs.getInt(KEY_DENSITY, config.getDarkness()));
        config.setPrintSpeed(prefs.getInt(KEY_SPEED, config.getPrintSpeed()));
        config.setLeftLabelX(prefs.getInt(KEY_LEFT_X, config.getLeftLabelX()));
        config.setTopMarginY(prefs.getInt(KEY_TOP_Y, config.getTopMarginY()));
        config.setRightLabelX(prefs.getInt(KEY_RIGHT_X, config.getRightLabelX()));
        config.setCurrencySymbol(prefs.get(KEY_CURRENCY, config.getCurrencySymbol()));
        config.setShowSize(prefs.getBoolean(KEY_SHOW_SIZE, config.isShowSize()));
        config.setShowPrice(prefs.getBoolean(KEY_SHOW_PRICE, config.isShowPrice()));
        config.setShowBarcodeText(prefs.getBoolean(KEY_SHOW_BC_TEXT, config.isShowBarcodeText()));
    }

    public static void saveConfig(LabelConfig config) {
        if (config == null) return;
        prefs.putInt(KEY_LABELS_PER_ROW, config.getLabelsPerRow());
        prefs.putDouble(KEY_LABEL_WIDTH, config.getLabelWidthMm());
        prefs.putDouble(KEY_LABEL_HEIGHT, config.getLabelHeightMm());
        prefs.putDouble(KEY_GAP, config.getGapMm());
        prefs.putDouble(KEY_H_GAP, config.getHorizontalGapMm());
        prefs.putInt(KEY_DENSITY, config.getDarkness());
        prefs.putInt(KEY_SPEED, config.getPrintSpeed());
        prefs.putInt(KEY_LEFT_X, config.getLeftLabelX());
        prefs.putInt(KEY_TOP_Y, config.getTopMarginY());
        prefs.putInt(KEY_RIGHT_X, config.getRightLabelX());
        prefs.put(KEY_CURRENCY, config.getCurrencySymbol());
        prefs.putBoolean(KEY_SHOW_SIZE, config.isShowSize());
        prefs.putBoolean(KEY_SHOW_PRICE, config.isShowPrice());
        prefs.putBoolean(KEY_SHOW_BC_TEXT, config.isShowBarcodeText());
    }

    // API / ERP Preferences
    public static String getApiType() {
        return prefs.get(KEY_API_TYPE, "REST");
    }

    public static void setApiType(String type) {
        prefs.put(KEY_API_TYPE, type != null ? type : "REST");
    }

    public static String getApiUrl() {
        return prefs.get(KEY_API_URL, "https://api.example.com/products");
    }

    public static void setApiUrl(String url) {
        prefs.put(KEY_API_URL, url != null ? url.trim() : "");
    }

    public static String getApiAuthType() {
        return prefs.get(KEY_API_AUTH_TYPE, "Bearer Token");
    }

    public static void setApiAuthType(String authType) {
        prefs.put(KEY_API_AUTH_TYPE, authType != null ? authType : "Bearer Token");
    }

    public static String getApiToken() {
        return prefs.get(KEY_API_TOKEN, "");
    }

    public static void setApiToken(String token) {
        prefs.put(KEY_API_TOKEN, token != null ? token.trim() : "");
    }

    public static String getApiUser() {
        return prefs.get(KEY_API_USER, "");
    }

    public static void setApiUser(String user) {
        prefs.put(KEY_API_USER, user != null ? user.trim() : "");
    }

    public static String getApiPassword() {
        return prefs.get(KEY_API_PASS, "");
    }

    public static void setApiPassword(String pass) {
        prefs.put(KEY_API_PASS, pass != null ? pass : "");
    }

    public static String getOdooDb() {
        return prefs.get(KEY_ODOO_DB, "odoo_db");
    }

    public static void setOdooDb(String db) {
        prefs.put(KEY_ODOO_DB, db != null ? db.trim() : "");
    }
}
