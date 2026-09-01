package com.barcodeprinter.generator;

import com.barcodeprinter.model.LabelConfig;
import com.barcodeprinter.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates TSPL / TSPL2 command scripts for Winpal thermal printers.
 * Perfected Retail Template Layout:
 * - [Barcode + Size] combined group centered horizontally
 * - Barcode Digits centered directly beneath barcode bars
 * - Generous vertical spacing (gaps) between barcode, price, and product name
 * - Price rendered in larger, bold font ("3")
 * - Product Name centered at bottom (with auto-wrap for long titles into 2 lines)
 */
public class TsplGenerator {

    /**
     * Generates a complete TSPL print job script for the given list of products.
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

        // 1. Initial Setup Commands
        sb.append(String.format("SIZE %.0f mm, %.0f mm\r\n", config.getLabelWidthMm(), config.getLabelHeightMm()));
        sb.append(String.format("GAP %.0f mm, 0 mm\r\n", config.getGapMm()));
        sb.append("SPEED ").append(config.getPrintSpeed()).append("\r\n");
        sb.append("DENSITY ").append(config.getDarkness()).append("\r\n");
        sb.append("DIRECTION 1\r\n");
        sb.append("REFERENCE 0,0\r\n");
        sb.append("OFFSET 0 mm\r\n\r\n");

        int n = Math.max(1, config.getLabelsPerRow());
        int dotsPerMm = (int) Math.round(config.getDpi() / 25.4); // 8 dots/mm for 203 DPI
        int totalWidthDots = (int) Math.round(config.getLabelWidthMm() * dotsPerMm);
        int slotWidthDots = totalWidthDots / n;
        int leftMarginDots = config.getLeftLabelX();

        // 2. Iterate row by row (chunk of N labels)
        for (int i = 0; i < individualLabels.size(); i += n) {
            sb.append("; ====== ROW ").append((i / n) + 1).append(" ======\r\n");
            sb.append("CLS\r\n");

            for (int col = 0; col < n; col++) {
                int itemIdx = i + col;
                if (itemIdx < individualLabels.size()) {
                    Product p = individualLabels.get(itemIdx);
                    int slotStartX = leftMarginDots + col * slotWidthDots;
                    int slotCenterX = leftMarginDots + (int) Math.round((col + 0.5) * slotWidthDots);
                    appendLabelCommands(sb, p, slotStartX, slotCenterX, slotWidthDots, config, "Col " + (col + 1));
                }
            }

            sb.append("PRINT 1, 1\r\n\r\n");
        }

        return sb.toString();
    }

    /**
     * Generates TSPL script for a single test row.
     */
    public static String generateTestRow(List<Product> rowProducts, LabelConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SIZE %.0f mm, %.0f mm\r\n", config.getLabelWidthMm(), config.getLabelHeightMm()));
        sb.append(String.format("GAP %.0f mm, 0 mm\r\n", config.getGapMm()));
        sb.append("SPEED ").append(config.getPrintSpeed()).append("\r\n");
        sb.append("DENSITY ").append(config.getDarkness()).append("\r\n");
        sb.append("DIRECTION 1\r\n");
        sb.append("REFERENCE 0,0\r\n");
        sb.append("CLS\r\n");

        int n = Math.max(1, config.getLabelsPerRow());
        int dotsPerMm = (int) Math.round(config.getDpi() / 25.4);
        int totalWidthDots = (int) Math.round(config.getLabelWidthMm() * dotsPerMm);
        int slotWidthDots = totalWidthDots / n;
        int leftMarginDots = config.getLeftLabelX();

        if (rowProducts != null) {
            for (int col = 0; col < Math.min(n, rowProducts.size()); col++) {
                Product p = rowProducts.get(col);
                if (p != null) {
                    int slotStartX = leftMarginDots + col * slotWidthDots;
                    int slotCenterX = leftMarginDots + (int) Math.round((col + 0.5) * slotWidthDots);
                    appendLabelCommands(sb, p, slotStartX, slotCenterX, slotWidthDots, config, "Col " + (col + 1));
                }
            }
        }

        sb.append("PRINT 1, 1\r\n");
        return sb.toString();
    }

    private static void appendLabelCommands(StringBuilder sb, Product p, int slotStartX, int slotCenterX, int slotWidthDots, LabelConfig config, String side) {
        sb.append("; --- ").append(side).append(" Label: ").append(sanitizeComment(p.getName())).append(" ---\r\n");

        int bcY = Math.max(24, config.getTopMarginY() + 4);
        int barcodeHeight = Math.min(42, config.getBarcodeHeight());

        // 1. Calculate Combined Width of [ Barcode + Size ] to center the entire group
        String barcode = sanitizeText(p.getBarcode());
        int approxBcWidth = 180;
        if (!barcode.isEmpty()) {
            approxBcWidth = estimateBarcodeWidthDots(barcode, config.getBarcodeType(), config.getBarcodeNarrow(), config.getBarcodeWide());
        }

        String sizeStr = "";
        int sizeWidth = 0;
        int gapBetweenBcAndSize = 20;
        boolean hasSize = config.isShowSize() && p.getSize() != null && !p.getSize().trim().isEmpty();
        if (hasSize) {
            sizeStr = sanitizeText(p.getSize().trim());
            int sizePitch = getFontCharWidth("3", false); // 14 dots / char
            sizeWidth = sizeStr.length() * sizePitch;
        }

        int totalGroupWidth = approxBcWidth + (hasSize ? (gapBetweenBcAndSize + sizeWidth) : 0);
        int groupStartX = Math.max(slotStartX + 16, slotCenterX - (totalGroupWidth / 2));
        int bcX = groupStartX;

        // Output Barcode Graphic
        int bcNumberY = bcY + barcodeHeight + 4;
        if (!barcode.isEmpty()) {
            sb.append(String.format("BARCODE %d, %d, \"%s\", %d, 0, 0, %d, %d, \"%s\"\r\n",
                    bcX, bcY, config.getBarcodeType(),
                    barcodeHeight,
                    config.getBarcodeNarrow(), config.getBarcodeWide(), barcode));

            // Human-Readable Digits (Centered directly beneath barcode bars)
            if (config.isShowBarcodeText()) {
                int numPitch = getFontCharWidth("2", true); // 12 dots / digit
                int approxNumWidth = barcode.length() * numPitch;
                int centeredNumX = bcX + Math.max(0, (approxBcWidth - approxNumWidth) / 2);

                sb.append(String.format("TEXT %d, %d, \"2\", 0, 1, 1, \"%s\"\r\n",
                        centeredNumX, bcNumberY, barcode));
            }
        }

        // Output Size (Positioned right beside the barcode in the centered group)
        if (hasSize) {
            int sizeX = bcX + approxBcWidth + gapBetweenBcAndSize;
            int sizeY = bcY + 10; // Moved down slightly from barcode top edge

            sb.append(String.format("TEXT %d, %d, \"3\", 0, 1, 1, \"%s\"\r\n",
                    sizeX, sizeY, sizeStr));
        }

        // Expanded gap after barcode numbers before Price
        int curY = (config.isShowBarcodeText() && !barcode.isEmpty()) ? (bcNumberY + 28) : (bcY + barcodeHeight + 20);

        // 3. Price: Reduced Currency Symbol (Font 2) + Large Bold Numeric Amount (Font 4)
        if (config.isShowPrice() && p.getPrice() != null && !p.getPrice().trim().isEmpty()) {
            String[] priceParts = splitCurrencyAndAmount(p.getPrice(), config.getCurrencySymbol());
            String currStr = sanitizeText(priceParts[0]);
            String amtStr = sanitizeText(priceParts[1]);

            int currPitch = 13; // Font 2 pitch
            int amtPitch = 20;  // Font 4 pitch
            int currWidth = currStr.isEmpty() ? 0 : (currStr.length() * currPitch);
            int amtWidth = amtStr.length() * amtPitch;
            int gapCurrAmt = (!currStr.isEmpty() && !amtStr.isEmpty()) ? 6 : 0;

            int totalPriceWidth = currWidth + gapCurrAmt + amtWidth;
            int priceStartX = Math.max(slotStartX + 16, slotCenterX - (totalPriceWidth / 2));

            // Currency symbol (Smaller Font 2, aligned nicely with large numbers)
            if (!currStr.isEmpty()) {
                int currX = priceStartX;
                int currY = curY + 8;
                sb.append(String.format("TEXT %d, %d, \"2\", 0, 1, 1, \"%s\"\r\n",
                        currX, currY, currStr));
            }

            // Large Bold Numeric Price (Font 4 with 1-dot overstrike)
            int amtX = priceStartX + currWidth + gapCurrAmt;
            sb.append(String.format("TEXT %d, %d, \"4\", 0, 1, 1, \"%s\"\r\n",
                    amtX, curY, amtStr));
            sb.append(String.format("TEXT %d, %d, \"4\", 0, 1, 1, \"%s\"\r\n",
                    amtX + 1, curY, amtStr));

            curY += 36; // Generous extra gap between price and product name
        }

        // 4. Centered Product Title & Code (Regular Clean Font 2, supports automatic 2-line wrapping)
        String titleText = "";
        if (p.getProductCode() != null && !p.getProductCode().isEmpty()) {
            titleText = p.getProductCode() + " " + (p.getName() != null ? p.getName() : "");
        } else {
            titleText = p.getName() != null ? p.getName() : "";
        }
        titleText = sanitizeText(titleText);

        if (!titleText.isEmpty()) {
            int maxCharsPerLine = Math.max(16, (slotWidthDots - 36) / 13);
            List<String> lines = wrapText(titleText, maxCharsPerLine);
            for (String line : lines) {
                int lineWidth = line.length() * 13;
                int lineX = Math.max(slotStartX + 16, slotCenterX - (lineWidth / 2));

                // Clean, regular Font 2 (crisp and not overbold)
                sb.append(String.format("TEXT %d, %d, \"2\", 0, 1, 1, \"%s\"\r\n",
                        lineX, curY, line));
                curY += 22;
            }
        }
    }

    /**
     * Splits price string into currency symbol and numeric amount.
     */
    public static String[] splitCurrencyAndAmount(String priceStr, String defaultCurrency) {
        if (priceStr == null) priceStr = "";
        priceStr = priceStr.trim();
        if (priceStr.isEmpty()) {
            return new String[]{"", ""};
        }

        String curr = (defaultCurrency != null && !defaultCurrency.trim().isEmpty()) ? defaultCurrency.trim() : "Rs";
        String amt = priceStr;

        if (priceStr.startsWith("Rs.") || priceStr.startsWith("RS.") || priceStr.startsWith("rs.")) {
            curr = "Rs.";
            amt = priceStr.substring(3).trim();
        } else if (priceStr.startsWith("Rs") || priceStr.startsWith("RS") || priceStr.startsWith("rs")) {
            curr = "Rs";
            amt = priceStr.substring(2).trim();
        } else if (priceStr.startsWith("LKR") || priceStr.startsWith("lkr")) {
            curr = "LKR";
            amt = priceStr.substring(3).trim();
        } else if (priceStr.startsWith("$") || priceStr.startsWith("€") || priceStr.startsWith("£") || priceStr.startsWith("¥")) {
            curr = priceStr.substring(0, 1);
            amt = priceStr.substring(1).trim();
        } else if (defaultCurrency != null && !defaultCurrency.isEmpty() && priceStr.startsWith(defaultCurrency)) {
            curr = defaultCurrency;
            amt = priceStr.substring(defaultCurrency.length()).trim();
        }

        return new String[]{curr, amt};
    }

    /**
     * Splits long product names cleanly into up to 2 wrapped lines.
     */
    public static List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<String>();
        if (text == null || text.trim().isEmpty()) {
            return lines;
        }
        text = text.trim();
        if (text.length() <= maxChars) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxChars) {
                currentLine.append(" ").append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
                if (lines.size() >= 2) {
                    break;
                }
            }
        }
        if (currentLine.length() > 0 && lines.size() < 2) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    /**
     * Exact hardware character pitch (width in dots at 203 DPI) for TSPL bitmap fonts.
     */
    private static int getFontCharWidth(String font, boolean isDigitsOnly) {
        if ("1".equals(font)) return isDigitsOnly ? 8 : 9;
        if ("2".equals(font)) return isDigitsOnly ? 12 : 13; // TSPL font 2: 12 dots for digits, 13 dots for text
        if ("3".equals(font)) return isDigitsOnly ? 14 : 14; // TSPL font 3: 14 dots width
        if ("4".equals(font)) return 20;
        if ("5".equals(font)) return 28;
        return 13;
    }

    private static int estimateBarcodeWidthDots(String barcode, String type, int narrow, int wide) {
        if (barcode == null || barcode.isEmpty()) return 180;
        int len = barcode.length();
        if ("128".equalsIgnoreCase(type) || "CODE128".equalsIgnoreCase(type)) {
            boolean numericOnly = barcode.matches("\\d+");
            int dataModules = numericOnly ? (((len + 1) / 2) * 11) : (len * 11);
            int totalModules = dataModules + (3 * 11) + 2; // start, checksum, stop
            return totalModules * narrow;
        } else if ("EAN13".equalsIgnoreCase(type)) {
            return 95 * narrow;
        } else if ("39".equalsIgnoreCase(type) || "CODE39".equalsIgnoreCase(type)) {
            return (len + 2) * (3 * wide + 6 * narrow + 1);
        }
        return len * 14 * narrow;
    }

    private static String sanitizeText(String input) {
        if (input == null) return "";
        return input.replace("\"", "'").replace("\r", "").replace("\n", " ").trim();
    }

    private static String sanitizeComment(String input) {
        if (input == null) return "";
        return input.replace("\r", "").replace("\n", " ").trim();
    }
}
