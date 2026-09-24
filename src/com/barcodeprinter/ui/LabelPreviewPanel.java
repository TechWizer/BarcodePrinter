package com.barcodeprinter.ui;

import com.barcodeprinter.model.LabelConfig;
import com.barcodeprinter.model.Product;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern real-time visual canvas renderer for printed barcode labels.
 * Accurately simulates:
 * - 1-Up, 2-Up, 3-Up column multi-label layout
 * - Exact physical millimeter proportions and DPI scaling
 * - Centered barcodes, clean sizes, large bold prices, and auto-wrapped product names
 * - Compact tag mode for 31mm x 15mm small tags
 */
public class LabelPreviewPanel extends JPanel {

    private LabelConfig config;
    private final List<Product> currentRowProducts = new ArrayList<Product>();
    private int currentRow = 1;
    private int totalRows = 1;
    private double zoom = 1.3;

    public LabelPreviewPanel(LabelConfig config) {
        this.config = config != null ? config : new LabelConfig();
        setBackground(new Color(245, 247, 250));
        setPreferredSize(new Dimension(560, 360));
    }

    public void setConfig(LabelConfig config) {
        this.config = config;
        repaint();
    }

    public void setZoom(double zoom) {
        this.zoom = Math.max(0.5, Math.min(2.5, zoom));
        repaint();
    }

    public void setPreviewRow(List<Product> rowProducts, int currentRow, int totalRows) {
        setRowData(rowProducts, currentRow, totalRows);
    }

    public void setRowData(List<Product> rowProducts, int currentRow, int totalRows) {
        this.currentRowProducts.clear();
        if (rowProducts != null) {
            this.currentRowProducts.addAll(rowProducts);
        }
        this.currentRow = Math.max(1, currentRow);
        this.totalRows = Math.max(1, totalRows);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        // 1. Draw subtle blueprint/grid background
        g2.setColor(new Color(236, 240, 245));
        for (int x = 0; x < panelWidth; x += 20) {
            g2.drawLine(x, 0, x, panelHeight);
        }
        for (int y = 0; y < panelHeight; y += 20) {
            g2.drawLine(0, y, panelWidth, y);
        }

        // 2. Physical conversions
        double pxPerMm = (config.getDpi() / 25.4) * (zoom / 2.6);
        int totalWidthPx = (int) (config.getLabelWidthMm() * pxPerMm);
        int totalHeightPx = (int) (config.getLabelHeightMm() * pxPerMm);

        int startX = Math.max(25, (panelWidth - totalWidthPx) / 2);
        int startY = Math.max(35, (panelHeight - totalHeightPx) / 2);

        // Draw outer backing paper liner (Glassine liner)
        g2.setColor(new Color(220, 226, 232));
        g2.fillRect(startX - 15, startY - 10, totalWidthPx + 30, totalHeightPx + 20);
        g2.setColor(new Color(200, 205, 210));
        g2.drawRect(startX - 15, startY - 10, totalWidthPx + 30, totalHeightPx + 20);

        int n = Math.max(1, config.getLabelsPerRow());
        int hGapPx = (n > 1) ? (int) (config.getHorizontalGapMm() * pxPerMm) : 0;
        int singleLabelW = (totalWidthPx - ((n - 1) * hGapPx)) / n;

        // Draw each column label in the current row
        for (int col = 0; col < n; col++) {
            int labelX = startX + col * (singleLabelW + hGapPx);
            Product p = (col < currentRowProducts.size()) ? currentRowProducts.get(col) : null;
            drawSingleLabel(g2, labelX, startY, singleLabelW, totalHeightPx, p, "Label " + (col + 1));
        }

        // Info Banner
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(new Color(90, 100, 110));
        String rowType = (n == 1) ? "Single (1-Up)" : ((n == 2) ? "Dual (2-Up)" : ((n == 3) ? "Triple (3-Up)" : (n + "-Up")));
        String rowInfo = String.format("Sheet Preview (%s): Row %d of %d  |  Roll Width: %.0fmm x %.0fmm  |  Margin: Left %ddot, Top %ddot",
                rowType, currentRow, totalRows, config.getLabelWidthMm(), config.getLabelHeightMm(), config.getLeftLabelX(), config.getTopMarginY());
        g2.drawString(rowInfo, 18, 22);

        g2.dispose();
    }

    private void drawSingleLabel(Graphics2D g2, int x, int y, int w, int h, Product p, String placeholder) {
        g2.setColor(new Color(0, 0, 0, 25));
        g2.fill(new RoundRectangle2D.Double(x + 2, y + 2, w, h, 6, 6));

        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 6, 6));

        g2.setColor(new Color(185, 195, 205));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 6, 6));

        if (p == null) {
            g2.setColor(new Color(170, 180, 190));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            String msg = "(" + placeholder + " Empty)";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, x + (w - fm.stringWidth(msg)) / 2, y + (h / 2));
            return;
        }

        // Check compact mode (<= 18mm height, e.g. 31x15mm tags)
        if (config.getLabelHeightMm() <= 18.0) {
            drawCompactLabel(g2, x, y, w, h, p);
            return;
        }

        // Dynamic top margin scaling
        double dotToPx = (zoom / 3.0);
        int topOffset = (int) (config.getTopMarginY() * dotToPx);
        int paddingX = Math.max(6, (int) (8 * (zoom / 1.3)));
        int curY = y + topOffset + 10;

        // 1. Calculate Combined [ Barcode + Size ] Width to Center the Group
        String barcode = p.getBarcode() != null ? p.getBarcode() : "";
        String sizeStr = (config.isShowSize() && p.getSize() != null) ? p.getSize().trim() : "";
        boolean hasSize = !sizeStr.isEmpty();

        int bcW = Math.max(60, Math.min((int) (w * 0.72), (barcode.length() * 11) + 40));
        int bcH = Math.max(20, Math.min(36, (int) (h * 0.28)));

        int sizeW = 0;
        int gapBcSize = 12;
        if (hasSize) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (13 * (zoom / 1.3))));
            FontMetrics fmSize = g2.getFontMetrics();
            sizeW = fmSize.stringWidth(sizeStr);
        }

        int totalGroupW = bcW + gapBcSize + sizeW;
        int groupLeftX = x + Math.max(paddingX, (w - totalGroupW) / 2);
        int bcLeftX = groupLeftX;

        // Draw Barcode Simulation
        if (!barcode.isEmpty()) {
            drawBarcodeSimulation(g2, bcLeftX, curY, bcW, bcH, barcode);
        }

        // 2. Draw Size (Right beside Barcode)
        if (hasSize) {
            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (13 * (zoom / 1.3))));
            FontMetrics fmSize = g2.getFontMetrics();
            int sizeX = bcLeftX + bcW + gapBcSize;
            int sizeY = curY + (bcH / 2) + (fmSize.getAscent() / 2) - 1;
            g2.drawString(sizeStr, sizeX, sizeY);
        }

        // 3. Human Readable Barcode Number (Centered under barcode)
        curY += bcH + 11;
        if (config.isShowBarcodeText() && !barcode.isEmpty()) {
            g2.setColor(new Color(50, 55, 65));
            g2.setFont(new Font("Monospaced", Font.BOLD, (int) (9.5 * (zoom / 1.3))));
            FontMetrics fmBc = g2.getFontMetrics();
            int bcTextX = bcLeftX + Math.max(0, (bcW - fmBc.stringWidth(barcode)) / 2);
            g2.drawString(barcode, bcTextX, curY);
            curY += 12;
        } else {
            curY += 4;
        }

        // 4. Price (Small Currency Symbol + Large Bold Numeric Amount - Centered)
        curY += 8;
        if (config.isShowPrice() && p.getPrice() != null && !p.getPrice().trim().isEmpty()) {
            String[] priceParts = com.barcodeprinter.generator.TsplGenerator.splitCurrencyAndAmount(p.getPrice(), config.getCurrencySymbol());
            String currStr = priceParts[0];
            String amtStr = priceParts[1];

            Font fontCurr = new Font("Segoe UI", Font.BOLD, (int) (10.0 * (zoom / 1.3)));
            Font fontAmt = new Font("Segoe UI", Font.BOLD, (int) (15.0 * (zoom / 1.3)));

            g2.setFont(fontCurr);
            FontMetrics fmCurr = g2.getFontMetrics();
            int currW = !currStr.isEmpty() ? fmCurr.stringWidth(currStr) : 0;

            g2.setFont(fontAmt);
            FontMetrics fmAmt = g2.getFontMetrics();
            int amtW = fmAmt.stringWidth(amtStr);
            int gapCurrAmt = !currStr.isEmpty() ? (int) (4 * (zoom / 1.3)) : 0;

            int totalPriceW = currW + gapCurrAmt + amtW;
            int priceStartX = x + (w - totalPriceW) / 2;

            g2.setColor(Color.BLACK);
            if (!currStr.isEmpty()) {
                g2.setFont(fontCurr);
                g2.drawString(currStr, priceStartX, curY + 2);
            }

            g2.setFont(fontAmt);
            g2.drawString(amtStr, priceStartX + currW + gapCurrAmt, curY + 2);
            curY += (int) (17 * (zoom / 1.3));
        } else {
            curY += 6;
        }

        // 5. Product Name & Code
        String title = "";
        if (p.getProductCode() != null && !p.getProductCode().isEmpty()) {
            title = p.getProductCode() + " " + (p.getName() != null ? p.getName() : "");
        } else {
            title = p.getName() != null ? p.getName() : "";
        }

        if (!title.isEmpty()) {
            g2.setColor(new Color(40, 45, 55));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, (int) (10.0 * (zoom / 1.3))));
            FontMetrics fmTitle = g2.getFontMetrics();
            int maxChars = Math.max(16, (w - (paddingX * 2)) / 7);
            java.util.List<String> lines = com.barcodeprinter.generator.TsplGenerator.wrapText(title, maxChars);
            for (String line : lines) {
                int lineX = x + (w - fmTitle.stringWidth(line)) / 2;
                g2.drawString(line, lineX, Math.min(y + h - 4, curY + 2));
                curY += 12;
            }
        }
    }

    private void drawCompactLabel(Graphics2D g2, int x, int y, int w, int h, Product p) {
        double sx = (double) w / 240.0;
        double sy = (double) h / 120.0;

        String barcode = p.getBarcode() != null ? p.getBarcode().trim() : "";
        String sizeStr = (config.isShowSize() && p.getSize() != null) ? p.getSize().trim() : "";
        String priceStr = (config.isShowPrice() && p.getPrice() != null) ? p.getPrice().trim() : "";
        String currConfig = config.getCurrencySymbol() != null ? config.getCurrencySymbol().trim() : "";
        String currStr = currConfig;
        String amtStr = priceStr;

        if (!priceStr.isEmpty()) {
            String[] priceParts = com.barcodeprinter.generator.TsplGenerator.splitCurrencyAndAmount(priceStr, currConfig);
            if (!priceParts[0].isEmpty()) {
                currStr = priceParts[0];
            }
            amtStr = priceParts[1].isEmpty() ? priceStr : priceParts[1];
        }

        String fullPrice = "";
        if (!amtStr.isEmpty()) {
            fullPrice = currStr.isEmpty() ? amtStr : (currStr + " " + amtStr);
        }

        String title = "";
        String code = p.getProductCode() != null ? p.getProductCode().trim() : "";
        String name = p.getName() != null ? p.getName().trim() : "";
        if (!code.isEmpty() && !name.isEmpty()) {
            title = code + " - " + name;
        } else if (!code.isEmpty()) {
            title = code;
        } else {
            title = name;
        }

        int leftX = x + (int) (15 * sx);
        int rightMarginX = x + w - (int) (15 * sx);

        // 1. Barcode (Top Left, Y=10..38)
        if (!barcode.isEmpty()) {
            int bcY = y + (int) (10 * sy);
            int bcW = (int) (155 * sx);
            int bcH = (int) (28 * sy);
            drawBarcodeSimulation(g2, leftX, bcY, bcW, bcH, barcode);
        }

        // 2. Size (Top Right, aligned with Barcode, Y=28)
        if (!sizeStr.isEmpty()) {
            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("Segoe UI", Font.BOLD, (int) Math.max(10, 20 * sy)));
            FontMetrics fmSize = g2.getFontMetrics();
            int sizeX = rightMarginX - fmSize.stringWidth(sizeStr);
            int sizeY = y + (int) (28 * sy);
            g2.drawString(sizeStr, sizeX, sizeY);
        }

        // 3. Barcode Digits (Middle Left, directly under barcode, Y=58)
        if (config.isShowBarcodeText() && !barcode.isEmpty()) {
            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("Monospaced", Font.BOLD, (int) Math.max(9, 13 * sy)));
            int numY = y + (int) (58 * sy);
            g2.drawString(barcode, leftX, numY);
        }

        // 4. Combined Price with Currency (Middle Right, under Size, Y=58)
        if (!fullPrice.isEmpty()) {
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, (int) Math.max(11, 20 * sy)));
            FontMetrics fmPrice = g2.getFontMetrics();
            int priceX = rightMarginX - fmPrice.stringWidth(fullPrice);
            int priceY = y + (int) (58 * sy);
            g2.drawString(fullPrice, priceX, priceY);
        }

        // 5. Product Title / Code (Bottom Row - Bold font across full width, Y=98)
        if (!title.isEmpty()) {
            g2.setColor(new Color(15, 23, 42));
            g2.setFont(new Font("Segoe UI", Font.BOLD, (int) Math.max(9, 14 * sy)));
            FontMetrics fmTitle = g2.getFontMetrics();
            int maxW = w - (int) (30 * sx);
            String displayTitle = title;
            if (fmTitle.stringWidth(displayTitle) > maxW) {
                while (displayTitle.length() > 5 && fmTitle.stringWidth(displayTitle + "...") > maxW) {
                    displayTitle = displayTitle.substring(0, displayTitle.length() - 1);
                }
                displayTitle = displayTitle + "...";
            }
            int titleY = y + (int) (98 * sy);
            g2.drawString(displayTitle, leftX, titleY);
        }
    }

    private void drawBarcodeSimulation(Graphics2D g2, int x, int y, int w, int h, String code) {
        g2.setColor(Color.BLACK);
        if (code == null || code.isEmpty()) {
            code = "123456789";
        }

        int numBars = 32;
        double barSlotW = (double) w / (numBars * 1.6);

        double curX = x + 2;
        for (int i = 0; i < numBars; i++) {
            char ch = code.charAt(i % code.length());
            int pattern = (ch * 31 + i * 17) % 5;
            double barWidth = (pattern == 0 || pattern == 2) ? barSlotW * 1.8 : barSlotW * 0.9;
            boolean isBlack = (i % 2 == 0) || (pattern > 2);

            if (isBlack && (curX + barWidth) <= (x + w - 2)) {
                g2.fillRect((int) curX, y, (int) Math.max(1, barWidth), h);
            }
            curX += barWidth + (barSlotW * 0.5);
            if (curX >= x + w - 4) break;
        }
    }
}
