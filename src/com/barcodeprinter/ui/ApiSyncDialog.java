package com.barcodeprinter.ui;

import com.barcodeprinter.api.ApiClient;
import com.barcodeprinter.model.Product;
import com.barcodeprinter.util.SettingsManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Modern modal dialog to sync and load products from REST APIs or Odoo ERP directly.
 * Clean, perfectly aligned layout with no stretched inputs or missing font glyphs.
 */
public class ApiSyncDialog extends JDialog {

    private final Frame parent;
    private List<Product> fetchedProducts = null;

    // Mode Selector
    private JRadioButton restRadio;
    private JRadioButton odooRadio;

    // REST Components
    private JTextField restUrlField;
    private JComboBox<String> authTypeCombo;
    private JTextField tokenField;
    private JTextField userField;
    private JPasswordField passField;
    private JPanel authCards;
    private CardLayout cardLayout;

    // Odoo Components
    private JTextField odooUrlField;
    private JTextField odooDbField;
    private JTextField odooUserField;
    private JPasswordField odooPassField;
    private JSpinner odooLimitSpinner;

    private JPanel configCardPanel;
    private CardLayout configCardLayout;

    private JLabel statusLabel;
    private JButton testBtn;
    private JButton loadBtn;

    public ApiSyncDialog(Frame parent) {
        super(parent, "Connect & Sync Products from Web / ERP API", true);
        this.parent = parent;
        if (parent != null && parent.getIconImages() != null && !parent.getIconImages().isEmpty()) {
            setIconImages(parent.getIconImages());
        }
        setSize(720, 540);
        setMinimumSize(new Dimension(680, 500));
        setLocationRelativeTo(parent);

        initUI();
        loadSavedConfig();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(new Color(245, 247, 250));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));

        // 1. Top System Type Selector
        JPanel topTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        topTypePanel.setBackground(Color.WHITE);
        topTypePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                new EmptyBorder(4, 10, 4, 10)
        ));

        JLabel modeLbl = new JLabel("System Type:");
        modeLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

        restRadio = new JRadioButton("Standard REST API (JSON / Web ERP)", true);
        restRadio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        restRadio.setOpaque(false);

        odooRadio = new JRadioButton("Odoo ERP (External JSON-RPC API)", false);
        odooRadio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        odooRadio.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(restRadio);
        bg.add(odooRadio);

        topTypePanel.add(modeLbl);
        topTypePanel.add(restRadio);
        topTypePanel.add(odooRadio);

        root.add(topTypePanel, BorderLayout.NORTH);

        // 2. Center Card Panel
        configCardLayout = new CardLayout();
        configCardPanel = new JPanel(configCardLayout);
        configCardPanel.setOpaque(false);

        configCardPanel.add(createRestPanel(), "REST");
        configCardPanel.add(createOdooPanel(), "ODOO");

        ActionListener typeToggle = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (restRadio.isSelected()) {
                    configCardLayout.show(configCardPanel, "REST");
                } else {
                    configCardLayout.show(configCardPanel, "ODOO");
                }
            }
        };
        restRadio.addActionListener(typeToggle);
        odooRadio.addActionListener(typeToggle);

        root.add(configCardPanel, BorderLayout.CENTER);

        // 3. Bottom Action & Status Panel
        root.add(createBottomPanel(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createRestPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: URL Label & Input
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
        JLabel urlLbl = new JLabel("REST API Endpoint URL (GET):");
        urlLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(urlLbl, gbc);

        gbc.gridy = 1;
        restUrlField = new JTextField("https://api.example.com/api/products");
        restUrlField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        restUrlField.setPreferredSize(new Dimension(500, 32));
        form.add(restUrlField, gbc);

        // Row 2: Auth Type Label & Dropdown
        gbc.gridy = 2;
        JLabel authLbl = new JLabel("Authentication Type:");
        authLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(authLbl, gbc);

        gbc.gridy = 3;
        String[] authTypes = {"Bearer Token", "API Key (Header)", "Basic Auth", "None"};
        authTypeCombo = new JComboBox<String>(authTypes);
        authTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        authTypeCombo.setPreferredSize(new Dimension(300, 32));
        form.add(authTypeCombo, gbc);

        // Row 3: Auth Credential Fields (CardLayout)
        gbc.gridy = 4;
        cardLayout = new CardLayout();
        authCards = new JPanel(cardLayout);
        authCards.setOpaque(false);

        // Token Panel
        tokenField = new JTextField();
        tokenField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tokenField.setPreferredSize(new Dimension(400, 32));
        JPanel tokenPanel = new JPanel(new BorderLayout(8, 4));
        tokenPanel.setOpaque(false);
        JLabel tokenLbl = new JLabel("Token / Key:");
        tokenLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tokenPanel.add(tokenLbl, BorderLayout.WEST);
        tokenPanel.add(tokenField, BorderLayout.CENTER);

        // Basic Auth Panel
        userField = new JTextField();
        userField.setPreferredSize(new Dimension(160, 32));
        passField = new JPasswordField();
        passField.setPreferredSize(new Dimension(160, 32));

        JPanel basicPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        basicPanel.setOpaque(false);
        JLabel uLbl = new JLabel("Username:");
        uLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        basicPanel.add(uLbl);
        basicPanel.add(userField);
        JLabel pLbl = new JLabel("Password:");
        pLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        basicPanel.add(pLbl);
        basicPanel.add(passField);

        // None Panel
        JPanel nonePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        nonePanel.setOpaque(false);
        JLabel noneLbl = new JLabel("No authentication header required for this endpoint.");
        noneLbl.setForeground(new Color(100, 116, 139));
        nonePanel.add(noneLbl);

        authCards.add(tokenPanel, "TOKEN");
        authCards.add(basicPanel, "BASIC");
        authCards.add(nonePanel, "NONE");

        authTypeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sel = (String) authTypeCombo.getSelectedItem();
                if ("Basic Auth".equals(sel)) {
                    cardLayout.show(authCards, "BASIC");
                } else if ("None".equals(sel)) {
                    cardLayout.show(authCards, "NONE");
                } else {
                    cardLayout.show(authCards, "TOKEN");
                }
            }
        });

        form.add(authCards, gbc);

        // Vertical Glue to keep controls on top
        gbc.gridy = 5; gbc.weighty = 1.0;
        form.add(Box.createVerticalGlue(), gbc);

        wrapper.add(form, BorderLayout.CENTER);

        JLabel tip = new JLabel("<html><span style='color:#64748B;'>Auto-matches JSON fields: <b>barcode</b>, <b>product_code</b>, <b>name</b>, <b>size</b>, <b>price</b>, <b>quantity</b></span></html>");
        wrapper.add(tip, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel createOdooPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        odooUrlField = new JTextField("http://localhost:8069");
        odooUrlField.setPreferredSize(new Dimension(240, 32));

        odooDbField = new JTextField("odoo_db");
        odooDbField.setPreferredSize(new Dimension(240, 32));

        odooUserField = new JTextField("admin");
        odooUserField.setPreferredSize(new Dimension(240, 32));

        odooPassField = new JPasswordField();
        odooPassField.setPreferredSize(new Dimension(240, 32));

        odooLimitSpinner = new JSpinner(new SpinnerNumberModel(500, 10, 5000, 50));
        odooLimitSpinner.setPreferredSize(new Dimension(100, 32));

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Odoo Server URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(odooUrlField, gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        form.add(new JLabel("Database Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(odooDbField, gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        form.add(new JLabel("Username / Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(odooUserField, gbc);

        // Row 3
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        form.add(new JLabel("Password / API Key:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(odooPassField, gbc);

        // Row 4
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        form.add(new JLabel("Max Records to Fetch:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(odooLimitSpinner, gbc);

        // Glue
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.weighty = 1.0;
        form.add(Box.createVerticalGlue(), gbc);

        wrapper.add(form, BorderLayout.CENTER);

        JLabel tip = new JLabel("<html><span style='color:#64748B;'>Connects via Odoo External API to query 'product.product' records.</span></html>");
        wrapper.add(tip, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);

        statusLabel = new JLabel("Ready. Enter details and click 'Test Connection' or 'Fetch & Load Products'.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(51, 65, 85));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        testBtn = new MainFrame.ModernButton("Test Connection", new Color(71, 85, 105), Color.WHITE);
        testBtn.setPreferredSize(new Dimension(140, 36));
        testBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onTestConnection();
            }
        });

        loadBtn = new MainFrame.ModernButton("Fetch & Load Products", new Color(5, 150, 105), Color.WHITE);
        loadBtn.setPreferredSize(new Dimension(180, 36));
        loadBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onFetchAndLoad();
            }
        });

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(85, 36));
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        btnPanel.add(testBtn);
        btnPanel.add(loadBtn);
        btnPanel.add(cancelBtn);

        bottom.add(statusLabel, BorderLayout.NORTH);
        bottom.add(btnPanel, BorderLayout.SOUTH);

        return bottom;
    }

    private void loadSavedConfig() {
        String type = SettingsManager.getApiType();
        if ("ODOO".equalsIgnoreCase(type)) {
            odooRadio.setSelected(true);
            configCardLayout.show(configCardPanel, "ODOO");
        } else {
            restRadio.setSelected(true);
            configCardLayout.show(configCardPanel, "REST");
        }

        restUrlField.setText(SettingsManager.getApiUrl());
        authTypeCombo.setSelectedItem(SettingsManager.getApiAuthType());
        tokenField.setText(SettingsManager.getApiToken());
        userField.setText(SettingsManager.getApiUser());
        passField.setText(SettingsManager.getApiPassword());

        odooUrlField.setText(SettingsManager.getApiUrl());
        odooDbField.setText(SettingsManager.getOdooDb());
        odooUserField.setText(SettingsManager.getApiUser());
        odooPassField.setText(SettingsManager.getApiPassword());
    }

    private void saveCurrentConfig() {
        boolean isRest = restRadio.isSelected();
        SettingsManager.setApiType(isRest ? "REST" : "ODOO");

        if (isRest) {
            SettingsManager.setApiUrl(restUrlField.getText().trim());
            SettingsManager.setApiAuthType((String) authTypeCombo.getSelectedItem());
            SettingsManager.setApiToken(tokenField.getText().trim());
            SettingsManager.setApiUser(userField.getText().trim());
            SettingsManager.setApiPassword(new String(passField.getPassword()));
        } else {
            SettingsManager.setApiUrl(odooUrlField.getText().trim());
            SettingsManager.setOdooDb(odooDbField.getText().trim());
            SettingsManager.setApiUser(odooUserField.getText().trim());
            SettingsManager.setApiPassword(new String(odooPassField.getPassword()));
        }
    }

    private void onTestConnection() {
        statusLabel.setText("Connecting to API server...");
        setBusy(true);

        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() throws Exception {
                return executeFetch();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    List<Product> result = get();
                    statusLabel.setText("Connection Successful! Found " + result.size() + " product items.");
                    JOptionPane.showMessageDialog(ApiSyncDialog.this,
                            "Connection Successful!\n\nSuccessfully connected to API and retrieved " + result.size() + " products.",
                            "API Connection OK",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    statusLabel.setText("Connection failed: " + msg);
                    JOptionPane.showMessageDialog(ApiSyncDialog.this,
                            "Connection Failed!\n\n" + msg + "\n\nPlease verify URL, Network connection, and Authentication credentials.",
                            "API Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void onFetchAndLoad() {
        statusLabel.setText("Fetching products from API...");
        setBusy(true);

        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() throws Exception {
                return executeFetch();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    List<Product> result = get();
                    saveCurrentConfig();
                    fetchedProducts = result;
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    statusLabel.setText("Fetch failed: " + msg);
                    JOptionPane.showMessageDialog(ApiSyncDialog.this,
                            "Fetch Failed!\n\n" + msg,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private List<Product> executeFetch() throws Exception {
        if (restRadio.isSelected()) {
            String url = restUrlField.getText().trim();
            String authType = (String) authTypeCombo.getSelectedItem();
            String tokenOrUser = "Basic Auth".equals(authType) ? userField.getText().trim() : tokenField.getText().trim();
            String pass = new String(passField.getPassword());
            return ApiClient.fetchRestProducts(url, authType, tokenOrUser, pass, 8);
        } else {
            String url = odooUrlField.getText().trim();
            String db = odooDbField.getText().trim();
            String user = odooUserField.getText().trim();
            String pass = new String(odooPassField.getPassword());
            int limit = (Integer) odooLimitSpinner.getValue();
            return ApiClient.fetchOdooProducts(url, db, user, pass, limit, 10);
        }
    }

    private void setBusy(boolean busy) {
        testBtn.setEnabled(!busy);
        loadBtn.setEnabled(!busy);
    }

    public List<Product> getFetchedProducts() {
        return fetchedProducts;
    }
}
