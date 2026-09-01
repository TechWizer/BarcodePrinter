package com.barcodeprinter.ui;

import com.barcodeprinter.model.LabelConfig;
import com.barcodeprinter.model.Product;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Visual preview component that dynamically adapts to 1-Up (Single), 2-Up (Dual), 3-Up (Triple), etc.
 * Supports real-time visualization with perfectly centered Title, Barcode, and Footer.
 */
public class LabelPreviewPanel extends JPanel {

    private final List<Product> currentRowProducts = new ArrayList<Product>();
    private LabelConfig config;
    private int currentRow = 1;
    private int totalRows = 1;
    private double zoom = 1.3;

    public LabelPreviewPanel(LabelConfig config) {
        this.config = config != null ? config : new LabelConfig();
        setBackground(new Color(240, 243, 246));
        setPreferredSize(new Dimension(620, 220));
    }

    public void setPreviewRow(List<Product> productsInRow, int row, int total) {
        this.currentRowProducts.clear();
        if (productsInRow != null) {
            this.currentRowProducts.addAll(productsInRow);
        }
        this.currentRow = row;
        this.totalRows = Math.max(1, total);
        repaint();
    }

    public void setConfig(LabelConfig config) {
        this.config = config;
        repaint();
    }

    public void setZoom(double zoom) {
        this.zoom = Math.max(0.5, Math.min(2.5, zoom));
        repaint();
    }

    public double getZoom() {
        return zoom;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int panelW = getWidth();
        int panelH = getHeight();

        double pxPerMm = 4.2 * zoom;
        int totalWidthPx = (int) (config.getLabelWidthMm() * pxPerMm);
        int totalHeightPx = (int) (config.getLabelHeightMm() * pxPerMm);

        int startX = (panelW - totalWidthPx) / 2;
        int startY = (panelH - totalHeightPx) / 2;

        // Paper Backing Liner
        g2.setColor(new Color(228, 232, 235));
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
        g2.fill(new RoundRectangle2D.Double(x + 2, y + 2, w, h, 8, 8));

        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 8, 8));

        g2.setColor(new Color(185, 195, 205));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 8, 8));

        if (p == null) {
            g2.setColor(new Color(170, 180, 190));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            String msg = "(" + placeholder + " Empty)";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, x + (w - fm.stringWidth(msg)) / 2, y + (h / 2));
            return;
        }

        // Dynamic top margin scaling
        double dotToPx = (zoom / 3.0);
        int topOffset = (int) (config.getTopMarginY() * dotToPx);
        int paddingX = Math.max(6, (int) (8 * (zoom / 1.3)));
        int curY = y + topOffset + 10;

        // 1. Calculate Combined [ Barcode + Size ] Width to Center the Group
        String barcode = p.getBarcode() != null ? p.getBarcode() : "";
        int bcH = (int) (config.getBarcodeHeight() * (zoom / 3.4));
        int bcW = Math.min((int) (w * 0.55), (int) (125 * (zoom / 1.3)));

        String sizeStr = (config.isShowSize() && p.getSize() != null && !p.getSize().trim().isEmpty()) ? p.getSize().trim() : "";
        g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (13 * (zoom / 1.3))));
        FontMetrics fmSize = g2.getFontMetrics();
        int sizeW = !sizeStr.isEmpty() ? fmSize.stringWidth(sizeStr) : 0;
        int gapBcSize = !sizeStr.isEmpty() ? (int) (10 * (zoom / 1.3)) : 0;

        int totalGroupW = bcW + gapBcSize + sizeW;
        int groupLeftX = x + Math.max(paddingX, (w - totalGroupW) / 2);
        int bcLeftX = groupLeftX;

        // Draw Barcode Simulation
        drawBarcodeSimulation(g2, bcLeftX, curY, bcW, bcH, barcode);

        // Draw Size
        if (!sizeStr.isEmpty()) {
            g2.setColor(new Color(20, 110, 190));
            int sizeX = bcLeftX + bcW + gapBcSize;
            int sizeY = curY + (bcH / 2) + 6;
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
        curY += 8; // Extra vertical gap
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
            curY += (int) (17 * (zoom / 1.3)); // Generous gap after price before product name
        } else {
            curY += 6;
        }

        // 5. Product Name & Code (Regular Clean Font, Centered, auto-wrapping up to 2 lines)
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
