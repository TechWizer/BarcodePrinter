package com.barcodeprinter.model;

/**
 * Configuration settings for Winpal thermal label printing and TSPL generation.
 * Supports dynamic labels per row (1-Up Single, 2-Up Dual, 3-Up Triple, etc.)
 * and user-adjustable Left Margin (X) and Top Margin (Y).
 */
public class LabelConfig {
    // Printer Command Language: "TSPL" (Winpal, TSC, 4BARCODE) or "ZPL" (Zebra, ZDesigner)
    private String printerLanguage = "TSPL";

    // Label Roll Layout
    private int labelsPerRow = 2;     // 1 = Single, 2 = Dual (2-Up), 3 = Triple (3-Up), 4 = Quad

    // Physical dimensions (in millimeters)
    private double labelWidthMm = 104.0;    // Total roll width
    private double labelHeightMm = 25.0;   // Label height
    private double gapMm = 2.0;            // Vertical gap between rows
    private double horizontalGapMm = 3.0;  // Horizontal gap between side-by-side labels

    // Printer hardware settings
    private int dpi = 203;                 // 203 DPI = 8 dots/mm (Standard Winpal/TSC)
    private int printSpeed = 4;             // 2-6 inches per second
    private int darkness = 8;               // 1-15 density level

    // User-adjustable Margin Offsets (in dots at 203 DPI, 8 dots = 1mm)
    private int leftLabelX = 0;            // Left margin offset (0 = perfectly centered in slot)
    private int topMarginY = 28;           // Top margin offset (~3.5mm)
    private int rightLabelX = 435;         // Fallback for legacy 2-up

    // Component Y positions (calculated relative to topMarginY)
    private int titleY = 28;
    private int barcodeY = 64;
    private int footerY = 148;

    // Barcode dimensions
    private int barcodeHeight = 50;
    private int barcodeNarrow = 2;
    private int barcodeWide = 2;
    private String barcodeType = "128"; // Code 128

    // Content Display Options
    private String currencySymbol = "Rs";
    private boolean showPrice = true;
    private boolean showSize = true;
    private boolean showBarcodeText = true;

    // TSPL Fonts
    private String titleFont = "3";  // TSPL font 3 (Standard readable)
    private String footerFont = "2"; // TSPL font 2 (Compact readable)

    public LabelConfig() {
        recalculateYPositions();
    }

    public void recalculateYPositions() {
        this.titleY = this.topMarginY;
        this.barcodeY = this.topMarginY + 36;
        int textOffset = this.showBarcodeText ? 30 : 10;
        this.footerY = this.barcodeY + this.barcodeHeight + textOffset;
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

    public int getLeftLabelX() {
        return leftLabelX;
    }

    public void setLeftLabelX(int leftLabelX) {
        this.leftLabelX = Math.max(0, leftLabelX);
    }

    public int getTopMarginY() {
        return topMarginY;
    }

    public void setTopMarginY(int topMarginY) {
        this.topMarginY = Math.max(0, topMarginY);
        recalculateYPositions();
    }

    public int getRightLabelX() {
        return rightLabelX;
    }

    public void setRightLabelX(int rightLabelX) {
        this.rightLabelX = rightLabelX;
    }

    public int getTitleY() {
        return titleY;
    }

    public void setTitleY(int titleY) {
        this.titleY = titleY;
    }

    public int getBarcodeY() {
        return barcodeY;
    }

    public void setBarcodeY(int barcodeY) {
        this.barcodeY = barcodeY;
    }

    public int getFooterY() {
        return footerY;
    }

    public void setFooterY(int footerY) {
        this.footerY = footerY;
    }

    public int getBarcodeHeight() {
        return barcodeHeight;
    }

    public void setBarcodeHeight(int barcodeHeight) {
        this.barcodeHeight = barcodeHeight;
        recalculateYPositions();
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
    public String getCurrencySymbol() {
        return currencySymbol;
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
        recalculateYPositions();
    }

    public String getTitleFont() {
        return titleFont;
    }

    public void setTitleFont(String titleFont) {
        this.titleFont = titleFont;
    }

    public String getFooterFont() {
        return footerFont;
    }

    public void setFooterFont(String footerFont) {
        this.footerFont = footerFont;
    }
}
