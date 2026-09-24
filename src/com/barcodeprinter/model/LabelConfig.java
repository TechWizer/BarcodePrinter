package com.barcodeprinter.model;

/**
 * Configuration settings for barcode label printing (TSPL & ZPL).
 * Supports Millimeter (mm) calibration for Left Margin and Top Margin,
 * dynamic labels per row (1-Up, 2-Up, 3-Up), and multi-client presets.
 */
public class LabelConfig {
    // Printer Command Language: "TSPL" (Winpal, TSC, 4BARCODE) or "ZPL" (Zebra, ZDesigner)
    private String printerLanguage = "TSPL";

    // Label Roll Layout
    private int labelsPerRow = 2;          // 1 = Single, 2 = Dual (2-Up), 3 = Triple (3-Up)

    // Physical dimensions (in millimeters)
    private double labelWidthMm = 104.0;    // Total roll width in mm
    private double labelHeightMm = 25.0;   // Label height in mm
    private double gapMm = 3.0;            // Vertical gap between rows in mm (Standard 3.0mm)
    private double horizontalGapMm = 2.0;  // Horizontal gap between side-by-side labels in mm

    // Printer hardware settings
    private int dpi = 203;                 // 203 DPI = 8 dots/mm (Standard Winpal/TSC/Zebra)
    private int printSpeed = 4;             // 2-6 inches per second
    private int darkness = 8;               // 1-15 density level

    // User-adjustable Margins (in Millimeters - mm)
    private double leftMarginMm = 7.25;     // Left physical margin offset (7.25mm = 58 dots at 203 DPI)
    private double topMarginMm = 2.0;      // Top margin offset (3.5mm = 28 dots at 203 DPI)

    // Barcode dimensions
    private int barcodeHeight = 42;
    private int barcodeNarrow = 2;
    private int barcodeWide = 2;
    private String barcodeType = "128";    // Code 128

    // Content Display Options
    private String currencySymbol = "Rs";
    private boolean showPrice = true;
    private boolean showSize = true;
    private boolean showBarcodeText = true;
    private boolean showProductCode = true;

    public LabelConfig() {
    }

    public int getDotsPerMm() {
        return (int) Math.round(dpi / 25.4); // 8 dots/mm for 203 DPI
    }

    // --- Millimeter Margin Accessors ---

    public double getLeftMarginMm() {
        return leftMarginMm;
    }

    public void setLeftMarginMm(double leftMarginMm) {
        this.leftMarginMm = Math.max(0.0, leftMarginMm);
    }

    public double getTopMarginMm() {
        return topMarginMm;
    }

    public void setTopMarginMm(double topMarginMm) {
        this.topMarginMm = Math.max(0.0, topMarginMm);
    }

    // --- Dot Conversion Helpers (for TSPL/ZPL & Preview Rendering) ---

        private int rightLabelX = 435;

    public int getRightLabelX() {
        return rightLabelX;
    }

    public void setRightLabelX(int rightLabelX) {
        this.rightLabelX = rightLabelX;
    }
    public int getLeftLabelX() {
        return (int) Math.round(leftMarginMm * getDotsPerMm());
    }

    public void setLeftLabelX(int dots) {
        this.leftMarginMm = (double) dots / getDotsPerMm();
    }

    public int getTopMarginY() {
        return (int) Math.round(topMarginMm * getDotsPerMm());
    }

    public void setTopMarginY(int dots) {
        this.topMarginMm = (double) dots / getDotsPerMm();
    }

    // --- General Accessors ---

    public String getPrinterLanguage() {
        return printerLanguage != null ? printerLanguage : "TSPL";
    }

    public void setPrinterLanguage(String printerLanguage) {
        if ("ZPL".equalsIgnoreCase(printerLanguage)) {
            this.printerLanguage = "ZPL";
        } else {
            this.printerLanguage = "TSPL";
        }
    }

    public boolean isZpl() {
        return "ZPL".equalsIgnoreCase(printerLanguage);
    }

    public boolean isTspl() {
        return !"ZPL".equalsIgnoreCase(printerLanguage);
    }

    public int getLabelsPerRow() {
        return labelsPerRow;
    }

    public void setLabelsPerRow(int labelsPerRow) {
        this.labelsPerRow = Math.max(1, Math.min(6, labelsPerRow));
    }

    public double getLabelWidthMm() {
        return labelWidthMm;
    }

    public void setLabelWidthMm(double labelWidthMm) {
        this.labelWidthMm = labelWidthMm;
    }

    public double getLabelHeightMm() {
        return labelHeightMm;
    }

    public void setLabelHeightMm(double labelHeightMm) {
        this.labelHeightMm = labelHeightMm;
    }

    public double getGapMm() {
        return gapMm;
    }

    public void setGapMm(double gapMm) {
        this.gapMm = gapMm;
    }

    public double getHorizontalGapMm() {
        return horizontalGapMm;
    }

    public void setHorizontalGapMm(double horizontalGapMm) {
        this.horizontalGapMm = horizontalGapMm;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public int getPrintSpeed() {
        return printSpeed;
    }

    public void setPrintSpeed(int printSpeed) {
        this.printSpeed = printSpeed;
    }

    public int getDarkness() {
        return darkness;
    }

    public void setDarkness(int darkness) {
        this.darkness = darkness;
    }

    public int getBarcodeHeight() {
        return barcodeHeight;
    }

    public void setBarcodeHeight(int barcodeHeight) {
        this.barcodeHeight = barcodeHeight;
    }

    public int getBarcodeNarrow() {
        return barcodeNarrow;
    }

    public void setBarcodeNarrow(int barcodeNarrow) {
        this.barcodeNarrow = barcodeNarrow;
    }

    public int getBarcodeWide() {
        return barcodeWide;
    }

    public void setBarcodeWide(int barcodeWide) {
        this.barcodeWide = barcodeWide;
    }

    public String getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(String barcodeType) {
        this.barcodeType = barcodeType;
    }

    public String getCurrencySymbol() {
        return currencySymbol != null ? currencySymbol : "Rs";
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public boolean isShowPrice() {
        return showPrice;
    }

    public void setShowPrice(boolean showPrice) {
        this.showPrice = showPrice;
    }

    public boolean isShowSize() {
        return showSize;
    }

    public void setShowSize(boolean showSize) {
        this.showSize = showSize;
    }

    public boolean isShowBarcodeText() {
        return showBarcodeText;
    }

    public void setShowBarcodeText(boolean showBarcodeText) {
        this.showBarcodeText = showBarcodeText;
    }

    public boolean isShowProductCode() {
        return showProductCode;
    }

    public void setShowProductCode(boolean showProductCode) {
        this.showProductCode = showProductCode;
    }
}