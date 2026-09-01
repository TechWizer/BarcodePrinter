package com.barcodeprinter.generator;

import com.barcodeprinter.model.LabelConfig;
import com.barcodeprinter.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates ZPL II (Zebra Programming Language) scripts for Zebra, ZDesigner,
 * Honeywell, and other ZPL-compatible thermal label printers.
 * Matches the perfected retail template layout:
 * - [Barcode + Size] combined group centered horizontally across each sticker
 * - Barcode Digits centered directly beneath barcode bars
 * - Reduced Currency Symbol + Large Bold Price
 * - Product Name centered at bottom (with 2-line wrapping for long titles)
 */
public class ZplGenerator {

    /**
     * Generates a complete ZPL print job script for the given list of products.
     */
    public static String generatePrintJob(List<Product> products, LabelConfig config) {
        if (products == null || products.isEmpty()) {
            return "";
        }

        List<Product> individualLabels = new ArrayList<Product>();
        for (Product p : products) {
            if (p.isSelected() && p.getQuantity() > 0) {
                for (int i = 0; i < p.getQuantity(); i++) {
                    individualLabels.add(p);
                }
            }
        }

        if (individualLabels.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int n = Math.max(1, config.getLabelsPerRow());
        int dotsPerMm = (int) Math.round(config.getDpi() / 25.4); // 8 dots/mm for 203 DPI
        int totalWidthDots = (int) Math.round(config.getLabelWidthMm() * dotsPerMm);
        int totalHeightDots = (int) Math.round(config.getLabelHeightMm() * dotsPerMm);
        int slotWidthDots = totalWidthDots / n;
        int leftMarginDots = config.getLeftLabelX();

        // Iterate row by row (chunk of N labels)
        for (int i = 0; i < individualLabels.size(); i += n) {
            sb.append("^XA\r\n");
            sb.append(String.format("^PW%d\r\n", totalWidthDots));
            sb.append(String.format("^LL%d\r\n", totalHeightDots));
            sb.append(String.format("^PR%d,%d\r\n", config.getPrintSpeed(), config.getPrintSpeed()));
            sb.append(String.format("^MD%d\r\n", config.getDarkness()));
            sb.append("^LH0,0\r\n\r\n");

            for (int col = 0; col < n; col++) {
                int itemIdx = i + col;
                if (itemIdx < individualLabels.size()) {
                    Product p = individualLabels.get(itemIdx);
                    int slotStartX = leftMarginDots + col * slotWidthDots;
                    int slotCenterX = leftMarginDots + (int) Math.round((col + 0.5) * slotWidthDots);
                    appendLabelZpl(sb, p, slotStartX, slotCenterX, slotWidthDots, config, "Col " + (col + 1));
                }
            }

            sb.append("^PQ1,0,1,Y\r\n");
            sb.append("^XZ\r\n\r\n");
        }

        return sb.toString();
    }

    /**
     * Generates ZPL script for a single test row.
     */
    public static String generateTestRow(List<Product> rowProducts, LabelConfig config) {
        StringBuilder sb = new StringBuilder();
        int n = Math.max(1, config.getLabelsPerRow());
        int dotsPerMm = (int) Math.round(config.getDpi() / 25.4);
        int totalWidthDots = (int) Math.round(config.getLabelWidthMm() * dotsPerMm);
        int totalHeightDots = (int) Math.round(config.getLabelHeightMm() * dotsPerMm);
        int slotWidthDots = totalWidthDots / n;
        int leftMarginDots = config.getLeftLabelX();

        sb.append("^XA\r\n");
        sb.append(String.format("^PW%d\r\n", totalWidthDots));
        sb.append(String.format("^LL%d\r\n", totalHeightDots));
        sb.append(String.format("^PR%d,%d\r\n", config.getPrintSpeed(), config.getPrintSpeed()));
        sb.append(String.format("^MD%d\r\n", config.getDarkness()));
        sb.append("^LH0,0\r\n\r\n");

        if (rowProducts != null) {
            for (int col = 0; col < Math.min(n, rowProducts.size()); col++) {
                Product p = rowProducts.get(col);
                if (p != null) {
                    int slotStartX = leftMarginDots + col * slotWidthDots;
                    int slotCenterX = leftMarginDots + (int) Math.round((col + 0.5) * slotWidthDots);
                    appendLabelZpl(sb, p, slotStartX, slotCenterX, slotWidthDots, config, "Col " + (col + 1));
                }
            }
        }

        sb.append("^PQ1,0,1,Y\r\n");
        sb.append("^XZ\r\n");
        return sb.toString();
    }

    private static void appendLabelZpl(StringBuilder sb, Product p, int slotStartX, int slotCenterX, int slotWidthDots, LabelConfig config, String side) {
        sb.append("^FX --- ").append(side).append(" Label: ").append(sanitizeComment(p.getName())).append(" ---^\r\n");

        int bcY = Math.max(24, config.getTopMarginY() + 4);
        int barcodeHeight = Math.min(42, config.getBarcodeHeight());

        // 1. Calculate Combined Width of [ Barcode + Size ] to center the entire group
        String barcode = sanitizeText(p.getBarcode());
        int approxBcWidth = 180;
        if (!barcode.isEmpty()) {
            approxBcWidth = estimateBarcodeWidthDots(barcode, config.getBarcodeNarrow());
        }

        String sizeStr = "";
        int sizeWidth = 0;
        int gapBetweenBcAndSize = 20;
        boolean hasSize = config.isShowSize() && p.getSize() != null && !p.getSize().trim().isEmpty();
        if (hasSize) {
            sizeStr = sanitizeText(p.getSize().trim());
            int sizePitch = 15; // ZPL ^A0N,22,15
            sizeWidth = sizeStr.length() * sizePitch;
        }

        int totalGroupWidth = approxBcWidth + (hasSize ? (gapBetweenBcAndSize + sizeWidth) : 0);
        int groupStartX = Math.max(slotStartX + 16, slotCenterX - (totalGroupWidth / 2));
        int bcX = groupStartX;

        // Output Barcode Graphic
        int bcNumberY = bcY + barcodeHeight + 4;
        if (!barcode.isEmpty()) {
            sb.append(String.format("^BY%d,2,%d\r\n", config.getBarcodeNarrow(), barcodeHeight));
            sb.append(String.format("^FO%d,%d^BCN,%d,N,N,N^FD%s^FS\r\n",
                    bcX, bcY, barcodeHeight, barcode));

            // Human-Readable Digits (Centered directly beneath barcode bars)
            if (config.isShowBarcodeText()) {
                int numPitch = 12; // ZPL ^A0N,18,12
                int approxNumWidth = barcode.length() * numPitch;
                int centeredNumX = bcX + Math.max(0, (approxBcWidth - approxNumWidth) / 2);

                sb.append(String.format("^FO%d,%d^A0N,18,12^FD%s^FS\r\n",
                        centeredNumX, bcNumberY, barcode));
            }
        }

        // Output Size (Positioned right beside the barcode in the centered group)
        if (hasSize) {
            int sizeX = bcX + approxBcWidth + gapBetweenBcAndSize;
            int sizeY = bcY + 8;

            sb.append(String.format("^FO%d,%d^A0N,24,18^FD%s^FS\r\n",
                    sizeX, sizeY, sizeStr));
        }

        // Expanded gap after barcode numbers before Price
        int curY = (config.isShowBarcodeText() && !barcode.isEmpty()) ? (bcNumberY + 28) : (bcY + barcodeHeight + 20);

        // 3. Price: Reduced Currency Symbol (Font ^A0N,16,12) + Large Bold Amount (Font ^A0N,30,22)
        if (config.isShowPrice() && p.getPrice() != null && !p.getPrice().trim().isEmpty()) {
            String[] priceParts = TsplGenerator.splitCurrencyAndAmount(p.getPrice(), config.getCurrencySymbol());
            String currStr = sanitizeText(priceParts[0]);
            String amtStr = sanitizeText(priceParts[1]);

            int currPitch = 12;
            int amtPitch = 20;
            int currWidth = currStr.isEmpty() ? 0 : (currStr.length() * currPitch);
            int amtWidth = amtStr.length() * amtPitch;
            int gapCurrAmt = (!currStr.isEmpty() && !amtStr.isEmpty()) ? 6 : 0;

            int totalPriceWidth = currWidth + gapCurrAmt + amtWidth;
            int priceStartX = Math.max(slotStartX + 16, slotCenterX - (totalPriceWidth / 2));

            // Currency symbol
            if (!currStr.isEmpty()) {
                int currX = priceStartX;
                int currY = curY + 8;
                sb.append(String.format("^FO%d,%d^A0N,18,13^FD%s^FS\r\n",
                        currX, currY, currStr));
            }

            // Large Bold Numeric Price
            int amtX = priceStartX + currWidth + gapCurrAmt;
            sb.append(String.format("^FO%d,%d^A0N,32,24^FD%s^FS\r\n",
                    amtX, curY, amtStr));

            curY += 36; // Generous extra gap between price and product name
        }

        // 4. Centered Product Title & Code (Regular Clean Font, supports automatic 2-line wrapping)
        String titleText = "";
        if (p.getProductCode() != null && !p.getProductCode().isEmpty()) {
            titleText = p.getProductCode() + " " + (p.getName() != null ? p.getName() : "");
        } else {
            titleText = p.getName() != null ? p.getName() : "";
        }
        titleText = sanitizeText(titleText);

        if (!titleText.isEmpty()) {
            int maxCharsPerLine = Math.max(16, (slotWidthDots - 36) / 13);
            List<String> lines = TsplGenerator.wrapText(titleText, maxCharsPerLine);
            for (String line : lines) {
                int lineWidth = line.length() * 13;
                int lineX = Math.max(slotStartX + 16, slotCenterX - (lineWidth / 2));

                sb.append(String.format("^FO%d,%d^A0N,19,13^FD%s^FS\r\n",
                        lineX, curY, line));
                curY += 22;
            }
        }
    }

    private static int estimateBarcodeWidthDots(String barcode, int narrow) {
        if (barcode == null || barcode.isEmpty()) return 180;
        int len = barcode.length();
        boolean numericOnly = barcode.matches("\\d+");
        int dataModules = numericOnly ? (((len + 1) / 2) * 11) : (len * 11);
        int totalModules = dataModules + (3 * 11) + 2;
        return totalModules * narrow;
    }

    private static String sanitizeText(String input) {
        if (input == null) return "";
        return input.replace("^", "").replace("~", "").replace("\r", "").replace("\n", " ").trim();
    }

    private static String sanitizeComment(String input) {
        if (input == null) return "";
        return input.replace("^", "").replace("\r", "").replace("\n", " ").trim();
    }
}