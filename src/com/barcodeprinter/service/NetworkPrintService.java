package com.barcodeprinter.service;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Sends raw TSPL commands directly over TCP/IP sockets to Ethernet / Wi-Fi thermal printers.
 * Default port for TSPL/thermal receipt & label printers is 9100.
 */
public class NetworkPrintService {

    public static void printViaSocket(String host, int port, String rawData, int timeoutMs) throws Exception {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Printer IP address or hostname is required.");
        }
        if (port <= 0 || port > 65535) {
            port = 9100;
        }
        if (rawData == null || rawData.trim().isEmpty()) {
            throw new IllegalArgumentException("No print commands to send.");
        }

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host.trim(), port), timeoutMs > 0 ? timeoutMs : 4000);
            socket.setSoTimeout(timeoutMs > 0 ? timeoutMs : 4000);

            OutputStream out = socket.getOutputStream();
            byte[] bytes = rawData.getBytes(StandardCharsets.ISO_8859_1);
            out.write(bytes);
            out.flush();
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }
}
