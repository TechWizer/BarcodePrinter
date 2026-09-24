package com.barcodeprinter.generator;

import com.barcodeprinter.model.LabelConfig;
import com.barcodeprinter.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates Zebra ZPL II command scripts for thermal barcode printers (Zebra, ZDesigner, Honeywell, etc.).
 * Supports:
 * - Standard spacious retail layout (25mm+ height)
 * - Exact calibrated compact 31mm x 15mm tag layout matching physical printer specifications.
 * - Hardware Gap / Web tracking (^MNY) and Tear-Off mode (^MMT) to prevent empty label skipping.
 */
public class ZplGenerator {

    /**
     * Generates a complete ZPL II print job script for the given list of products.
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

        for (int i = 0; i < individualLabels.size(); i += n) {
            sb.append("^XA\r\n");
            sb.append("^MMT\r\n"); // Media Mode: Tear-off
            sb.append("^MNY\r\n"); // Media Tracking: Gap/Web Sensing (Stops printer skipping empty labels!)
            sb.append(String.format("^PW%d\r\n", totalWidthDots));
            sb.append(String.format("^LL%d\r\n", totalHeightDots));
            sb.append("^LS0\r\n\r\n");

            for (int col = 0; col < n; col++) {
                int itemIdx = i + col;
                if (itemIdx < individualLabels.size()) {
                    Product p = individualLabels.get(itemIdx);
                    int slotStartX = leftMarginDots + col * slotWidthDots;
                    int slotCenterX = leftMarginDots + (int) Math.round((col + 0.5) * slotWidthDots);
                    appendLabelZpl(sb, p, slotStartX, slotCenterX, slotWidthDots, totalHeightDots, config, "Col " + (col + 1));
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
        sb.append("^MMT\r\n");
        sb.append("^MNY\r\n");
        sb.append(String.format("^PW%d\r\n", totalWidthDots));
        sb.append(String.format("^LL%d\r\n", totalHeightDots));
        sb.append("^LS0\r\n\r\n");

        if (rowProducts != null) {
            for (int col = 0; col < Math.min(n, rowProducts.size()); col++) {
                Product p = rowProducts.get(col);
                if (p != null) {
                    int slotStartX = leftMarginDots + col * slotWidthDots;
                    int slotCenterX = leftMarginDots + (int) Math.round((col + 0.5) * slotWidthDots);
                    appendLabelZpl(sb, p, slotStartX, slotCenterX, slotWidthDots, totalHeightDots, config, "Col " + (col + 1));
                }
            }
        }

        sb.append("^PQ1,0,1,Y\r\n");
        sb.append("^XZ\r\n");
        return sb.toString();
    }

    private static void appendLabelZpl(StringBuilder sb, Product p, int slotStartX, int slotCenterX, int slotWidthDots, int totalHeightDots, LabelConfig config, String side) {
        // Compact mode for 31mm x 15mm small tags (Height <= 18mm)
        if (config.getLabelHeightMm() <= 18.0) {
            appendCompactLabelZpl(sb, p, slotStartX, slotWidthDots, totalHeightDots, config, side);
            return;
        }

        sb.append("^FX --- ").append(side).append(" Label: ").append(sanitizeComment(p.getName())).append(" ---\r\n");
        sb.append(String.format("^PR%d,%d\r\n", config.getPrintSpeed(), config.getPrintSpeed()));
        sb.append(String.format("^MD%d\r\n", config.getDarkness()));
        sb.append("^LH0,0\r\n\r\n");

        int bcY = Math.max(24, config.getTopMarginY() + 4);
        int barcodeHeight = Math.min(42, config.getBarcodeHeight());

        // 1. Calculate Combined Width of [ Barcode + Size ] to center the entire group
        String barcode = sanitizeText(p.getBarcode());
        int approxBcWidth = 180;
        if (!barcode.isEmpty()) {
            approxBcWidth = estimateBarcodeWidthDots(barcode, config.getBarcodeNarrow());
        }

        String sizeStr = (config.isShowSize() && p.getSize() != null) ? sanitizeText(p.getSize().trim()) : "";
        boolean hasSize = !sizeStr.isEmpty();
        int sizeWidth = 0;
        int gapBetweenBcAndSize = 12;

        if (hasSize) {
            sizeWidth = sizeStr.length() * 18 + 6;
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

            curY += 36;
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

    /**
     * Exact 3-zone calibrated compact 31mm x 15mm tag layout:
     * - Row 1 (Top): Barcode on Left (^FO15,8) | Size on Top-Right (^FT{sizeX},28)
     * - Row 2 (Middle Left): Crisp Legible Barcode Digits (^FT15,54 ^A0N,18,13)
     * - Row 2 (Middle-Lower Right): Big Bold Right-Aligned Price (^FT{priceX},66 ^A0N,24,19)
     * - Row 3 (Bottom): Product Code & Name in BOLD (^FT15,102 ^A0N,18,14)
     */
    private static void appendCompactLabelZpl(StringBuilder sb, Product p, int slotStartX, int slotWidthDots, int totalHeightDots, LabelConfig config, String side) {
        int leftX = slotStartX + 15;
        int rightMarginX = slotStartX + Math.max(228, slotWidthDots - 12);

        String barcode = sanitizeText(p.getBarcode());
        String sizeStr = (config.isShowSize() && p.getSize() != null) ? sanitizeText(p.getSize().trim()) : "";
        String priceStr = (config.isShowPrice() && p.getPrice() != null) ? sanitizeText(p.getPrice().trim()) : "";
        String currConfig = config.getCurrencySymbol() != null ? config.getCurrencySymbol().trim() : "";
        String currStr = sanitizeText(currConfig);
        String amtStr = priceStr;

        if (!priceStr.isEmpty()) {
            String[] priceParts = TsplGenerator.splitCurrencyAndAmount(priceStr, currConfig);
            if (!priceParts[0].isEmpty()) {
                currStr = sanitizeText(priceParts[0]);
            }
            amtStr = sanitizeText(priceParts[1].isEmpty() ? priceStr : priceParts[1]);
        }

        // Full Price String with Currency (e.g. "AED 45.00" or "Rs. 2,450.00")
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
        title = sanitizeText(title);

        // 1. Barcode (Top Left, Y=8, Height=28)
        int bcH = Math.min(28, Math.max(20, config.getBarcodeHeight()));
        if (!barcode.isEmpty()) {
            sb.append(String.format("^FO%d,8\r\n", leftX));
            sb.append(String.format("^BY1,30,%d\r\n", bcH));
            sb.append(String.format("^BCN,%d,N,N,N\r\n", bcH));
            sb.append(String.format("^FD%s^FS\r\n\r\n", barcode));
        }

        // 2. Size (Top Right, Y=28)
        if (!sizeStr.isEmpty()) {
            int sizeWidth = sizeStr.length() * 16;
            int sizeX = Math.max(slotStartX + 175, rightMarginX - sizeWidth);
            sb.append(String.format("^FT%d,28\r\n", sizeX));
            sb.append("^A0N,22,18\r\n");
            sb.append(String.format("^FD%s^FS\r\n\r\n", sizeStr));
        }

        // 3. Barcode Digits (Middle Left, crisp & bold font ^A0N,18,13, Y=54)
        if (config.isShowBarcodeText() && !barcode.isEmpty()) {
            sb.append(String.format("^FT%d,54\r\n", leftX));
            sb.append("^A0N,18,13\r\n");
            sb.append(String.format("^FD%s^FS\r\n\r\n", barcode));
        }

        // 4. Large Bold Price (Lowered to Y=66 and right-aligned to right margin)
        if (!fullPrice.isEmpty()) {
            int charW = 15; // Width per char in font ^A0N,24,19
            int priceWidth = fullPrice.length() * charW;
            int priceX = Math.max(slotStartX + 85, rightMarginX - priceWidth);
            sb.append(String.format("^FT%d,66\r\n", priceX));
            sb.append("^A0N,24,19\r\n");
            sb.append(String.format("^FD%s^FS\r\n\r\n", fullPrice));
        }

        // 5. Product Title / Code (Bottom Row - Bold font ^A0N,18,14 across full width, Y=102)
        if (!title.isEmpty()) {
            int maxChars = Math.max(16, (slotWidthDots - 25) / 11);
            if (title.length() > maxChars) {
                title = title.substring(0, maxChars - 2) + "..";
            }
            sb.append(String.format("^FT%d,102\r\n", leftX));
            sb.append("^A0N,18,14\r\n");
            sb.append(String.format("^FD%s^FS\r\n\r\n", title));
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
