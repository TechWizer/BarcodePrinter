package com.barcodeprinter;

import com.barcodeprinter.ui.MainFrame;

import javax.swing.*;

/**
 * Main application entrypoint for the Winpal 2-Up Barcode Label Printer.
 */
public class BarcodePrinterApp {

    public static void main(String[] args) {
        // Set System Look and Feel for native OS appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Launch UI on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            }
        });
    }
}
