package com.barcodeprinter.ui;

import com.barcodeprinter.generator.TsplGenerator;
import com.barcodeprinter.generator.ZplGenerator;
import com.barcodeprinter.model.LabelConfig;
import com.barcodeprinter.model.Product;
import com.barcodeprinter.parser.ExcelParser;
import com.barcodeprinter.service.NetworkPrintService;
import com.barcodeprinter.service.RawPrintService;
import com.barcodeprinter.util.SettingsManager;
import com.barcodeprinter.util.TemplateGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window: Winpal Barcode Lable Printer.
 * Supports configurable 1-Up (Single), 2-Up (Dual), 3-Up (Triple), etc.
 */
public class MainFrame extends JFrame {

    private final List<Product> products = new ArrayList<Product>();
    private final LabelConfig config = new LabelConfig();
    private boolean isUpdatingTable = false;
    private boolean isUpdatingPrinters = false;

    // UI Components
    private JTable productTable;
    private DefaultTableModel tableModel;
    private LabelPreviewPanel previewPanel;
    private JTabbedPane rightTabbedPane;
    private JTextArea tsplTextArea;
    private JLabel statusLabel;
    private JLabel statsLabel;

    // Navigation
    private int currentPreviewRow = 0;
    private JLabel rowIndicatorLabel;
    private JButton prevRowBtn;
    private JButton nextRowBtn;

    // Bottom Bar Printer Selection
    private JRadioButton usbRadio;
    private JRadioButton netRadio;
    private JComboBox<String> printerCombo;
    private JComboBox<String> cmbLanguage;
    private JTextField netIpField;
    private JTextField netPortField;

    // Settings Tab Components
    private JComboBox<String> defaultPrinterCombo;
    private JRadioButton defaultUsbRadio;
    private JRadioButton defaultNetRadio;
    private JTextField defaultNetIpField;
    private JTextField defaultNetPortField;

    private JComboBox<String> labelsPerRowCombo;
    private JComboBox<String> presetCombo;
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private JSpinner gapSpinner;
    private JSpinner hGapSpinner;
    private JSpinner densitySpinner;
    private JSpinner speedSpinner;
    private JSpinner leftXSpinner;
    private JSpinner topYSpinner;
    private JTextField currencyField;
    private JCheckBox showSizeChk;
    private JCheckBox showPriceChk;
    private JCheckBox showBarcodeTextChk;

    public MainFrame() {
        // Set top bar name
        setTitle("Winpal Barcode Lable Printer");
        setSize(1280, 800);
        setMinimumSize(new Dimension(1050, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. Default Open Maximized Window
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Load Persistent Configuration
        SettingsManager.loadConfig(config);

        initUI();

        // Populate and select saved printer state
        loadSavedPrinterState();

        // Initial preview & stats update
        updateStats();
        updatePreview();
        updateTsplScript();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(245, 247, 250));

        // 1. Top Colorful Toolbar
        root.add(createTopToolbar(), BorderLayout.NORTH);

        // 2. Center Split View (Left: Table, Right: Tabs)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(650);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        splitPane.setLeftComponent(createTableSection());
        rightTabbedPane = createRightTabbedSection();
        splitPane.setRightComponent(rightTabbedPane);
        root.add(splitPane, BorderLayout.CENTER);

        // 3. Bottom Control & Print Bar
        root.add(createBottomBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // =========================================================================
    // 1. TOP TOOLBAR
    // =========================================================================
    private JToolBar createTopToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(Color.WHITE);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 230)),
                new EmptyBorder(8, 12, 8, 12)
        ));

        // Import Button (Royal Blue)
        ModernButton importBtn = new ModernButton("Import Excel / CSV", new Color(29, 78, 216), Color.WHITE);
        importBtn.setToolTipText("Import product rows from .xlsx, .xls, .csv, or .tsv");
        importBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onImportFile();
            }
        });

        // Load from API / ERP Button (Vibrant Cyan #0891B2)
        ModernButton apiBtn = new ModernButton("Sync from API / ERP", new Color(8, 145, 178), Color.WHITE);
        apiBtn.setToolTipText("Sync products live from REST API, Odoo ERP, WooCommerce, Shopify, or Laravel");
        apiBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onLoadFromApi();
            }
        });

        // Download Template Button (Teal / Emerald)
        ModernButton templateBtn = new ModernButton("Download Template", new Color(13, 148, 136), Color.WHITE);
        templateBtn.setToolTipText("Download official Excel / CSV template for importing products");
        templateBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onDownloadTemplate();
            }
        });

        // Load Sample Data Button (Vibrant Violet #7C3AED)
        ModernButton sampleBtn = new ModernButton("Load Sample Data", new Color(124, 58, 237), Color.WHITE);
        sampleBtn.setToolTipText("Load demo product items to test label preview and printing");
        sampleBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadSampleData();
            }
        });

        // + Add Product (Vibrant Sky Blue #0284C7)
        ModernButton addRowBtn = new ModernButton("+ Add Product", new Color(2, 132, 199), Color.WHITE);
        addRowBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onAddRow();
            }
        });

        // - Remove Selected (Vibrant Orange #EA580C)
        ModernButton delRowBtn = new ModernButton("- Remove Selected", new Color(234, 88, 12), Color.WHITE);
        delRowBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onDeleteSelectedRows();
            }
        });

        // [x] Select All (Vibrant Indigo #6366F1)
        ModernButton selectAllBtn = new ModernButton("[x] Select All", new Color(99, 102, 241), Color.WHITE);
        selectAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSelectAll(true);
            }
        });

        // [ ] Deselect All (Slate #64748B)
        ModernButton deselectAllBtn = new ModernButton("[ ] Deselect All", new Color(100, 116, 139), Color.WHITE);
        deselectAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSelectAll(false);
            }
        });

        // Clear Table (Crimson Red #DC2626)
        ModernButton clearAllBtn = new ModernButton("Clear Table", new Color(220, 38, 38), Color.WHITE);
        clearAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClearAll();
            }
        });

        toolBar.add(importBtn);
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.add(apiBtn);
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.add(templateBtn);
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.add(sampleBtn);
        toolBar.add(Box.createHorizontalStrut(16));
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalStrut(16));
        toolBar.add(addRowBtn);
        toolBar.add(Box.createHorizontalStrut(6));
        toolBar.add(delRowBtn);
        toolBar.add(Box.createHorizontalStrut(6));
        toolBar.add(selectAllBtn);
        toolBar.add(Box.createHorizontalStrut(6));
        toolBar.add(deselectAllBtn);
        toolBar.add(Box.createHorizontalStrut(16));
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalStrut(16));
        toolBar.add(clearAllBtn);

        return toolBar;
    }

    // =========================================================================
    // 2. PRODUCT TABLE (LEFT PANEL - USER COLUMN ORDER)
    // Order: Print, Barcode, Product Code, Product Name, Size, Price, QTY
    // =========================================================================
    private JPanel createTableSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 6));
        panel.setBackground(new Color(245, 247, 250));

        // Header Title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel title = new JLabel("Products to Print");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statsLabel = new JLabel("0 Products | 0 Labels");
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statsLabel.setForeground(new Color(100, 110, 125));

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(statsLabel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table Model with 7 Columns: Print, Barcode, Product Code, Product Name, Size, Price, QTY
        String[] columns = {"Print", "Barcode", "Product Code", "Product Name", "Size", "Price", "QTY"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 6) return Integer.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        productTable = new JTable(tableModel);
        productTable.setRowHeight(26);
        productTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        productTable.setSelectionBackground(new Color(224, 238, 255));
        productTable.setSelectionForeground(Color.BLACK);

        productTable.getColumnModel().getColumn(0).setPreferredWidth(45);  // Print
        productTable.getColumnModel().getColumn(1).setPreferredWidth(125); // Barcode
        productTable.getColumnModel().getColumn(2).setPreferredWidth(95);  // Product Code
        productTable.getColumnModel().getColumn(3).setPreferredWidth(170); // Product Name
        productTable.getColumnModel().getColumn(4).setPreferredWidth(55);  // Size
        productTable.getColumnModel().getColumn(5).setPreferredWidth(65);  // Price
        productTable.getColumnModel().getColumn(6).setPreferredWidth(50);  // QTY

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        productTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        productTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        productTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        productTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        productTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (isUpdatingTable) return;
                syncProductsFromTable();
                updatePreview();
                updateTsplScript();
                updateStats();
            }
        });

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // 3. RIGHT TABBED SECTION (PREVIEW, TSPL, SETTINGS)
    // =========================================================================
    private JTabbedPane createRightTabbedSection() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));

        String tabTitle = getTabPreviewTitle();
        tabbedPane.addTab(tabTitle, createPreviewTab());
        tabbedPane.addTab("TSPL Script Viewer", createTsplScriptTab());
        tabbedPane.addTab("Printer & Label Settings", createSettingsTab());

        return tabbedPane;
    }

    private String getTabPreviewTitle() {
        int n = config.getLabelsPerRow();
        return "Label Preview (" + (n == 1 ? "1-Up Single" : (n == 2 ? "2-Up Dual" : (n == 3 ? "3-Up Triple" : n + "-Up"))) + ")";
    }

    private JPanel createPreviewTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.WHITE);

        previewPanel = new LabelPreviewPanel(config);
        JScrollPane previewScroll = new JScrollPane(previewPanel);
        previewScroll.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 235)));
        panel.add(previewScroll, BorderLayout.CENTER);

        // Preview Controls
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        navPanel.setOpaque(false);

        JButton firstBtn = new JButton("<< First");
        prevRowBtn = new JButton("< Previous Row");
        rowIndicatorLabel = new JLabel("Row 1 of 1");
        rowIndicatorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nextRowBtn = new JButton("Next Row >");
        JButton lastBtn = new JButton("Last >>");

        firstBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentPreviewRow = 0;
                updatePreview();
            }
        });
        prevRowBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentPreviewRow > 0) {
                    currentPreviewRow--;
                    updatePreview();
                }
            }
        });
        nextRowBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int totalRows = calculateTotalRows();
                if (currentPreviewRow < totalRows - 1) {
                    currentPreviewRow++;
                    updatePreview();
                }
            }
        });
        lastBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int totalRows = calculateTotalRows();
                currentPreviewRow = Math.max(0, totalRows - 1);
                updatePreview();
            }
        });

        JLabel zoomLbl = new JLabel("Zoom:");
        JSlider zoomSlider = new JSlider(50, 200, 130);
        zoomSlider.setPreferredSize(new Dimension(100, 20));
        zoomSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                previewPanel.setZoom(zoomSlider.getValue() / 100.0);
            }
        });

        navPanel.add(firstBtn);
        navPanel.add(prevRowBtn);
        navPanel.add(rowIndicatorLabel);
        navPanel.add(nextRowBtn);
        navPanel.add(lastBtn);
        navPanel.add(Box.createHorizontalStrut(15));
        navPanel.add(zoomLbl);
        navPanel.add(zoomSlider);

        panel.add(navPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTsplScriptTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.WHITE);

        tsplTextArea = new JTextArea();
        tsplTextArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        tsplTextArea.setEditable(false);
        tsplTextArea.setBackground(new Color(248, 250, 252));
        tsplTextArea.setForeground(new Color(30, 41, 59));
        tsplTextArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(tsplTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230)));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        ModernButton copyBtn = new ModernButton("Copy TSPL Code", new Color(241, 245, 249), new Color(30, 41, 59), new Color(203, 213, 225));
        copyBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StringSelection sel = new StringSelection(tsplTextArea.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                JOptionPane.showMessageDialog(MainFrame.this, "TSPL commands copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        ModernButton saveBtn = new ModernButton("Export to .txt / .tspl", new Color(241, 245, 249), new Color(30, 41, 59), new Color(203, 213, 225));
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onExportTsplFile();
            }
        });

        actions.add(copyBtn);
        actions.add(saveBtn);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSettingsTab() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 12));
        rootPanel.setBorder(new EmptyBorder(16, 16, 16, 16));
        rootPanel.setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Section 1: Default Target Printer (Persistent Settings)
        JPanel printerSec = new JPanel(new GridLayout(0, 2, 10, 8));
        printerSec.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                " Default Target Printer (Saved for Next Launch) ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(30, 41, 59)
        ));
        printerSec.setOpaque(false);

        defaultUsbRadio = new JRadioButton("USB / Windows Spooler", true);
        defaultNetRadio = new JRadioButton("Network / LAN (TCP Socket)", false);
        ButtonGroup defaultBg = new ButtonGroup();
        defaultBg.add(defaultUsbRadio);
        defaultBg.add(defaultNetRadio);

        defaultPrinterCombo = new JComboBox<String>();
        defaultNetIpField = new JTextField(SettingsManager.getNetworkIp());
        defaultNetPortField = new JTextField(String.valueOf(SettingsManager.getNetworkPort()));

        ActionListener defRadioToggle = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean isUsb = defaultUsbRadio.isSelected();
                defaultPrinterCombo.setEnabled(isUsb);
                defaultNetIpField.setEnabled(!isUsb);
                defaultNetPortField.setEnabled(!isUsb);
            }
        };
        defaultUsbRadio.addActionListener(defRadioToggle);
        defaultNetRadio.addActionListener(defRadioToggle);

        cmbLanguage = new JComboBox<String>(new String[]{
                "TSPL (Winpal, TSC, 4BARCODE, Xprinter)",
                "ZPL (Zebra, ZDesigner, Honeywell)"
        });
        cmbLanguage.setSelectedIndex(config.isZpl() ? 1 : 0);
        cmbLanguage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncConfigFromUI();
            }
        });

        printerSec.add(new JLabel("Printer Command Language:"));
        printerSec.add(cmbLanguage);

        printerSec.add(new JLabel("Default Connection Mode:"));
        JPanel radioBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        radioBox.setOpaque(false);
        radioBox.add(defaultUsbRadio);
        radioBox.add(defaultNetRadio);
        printerSec.add(radioBox);

        printerSec.add(new JLabel("Default USB Printer:"));
        printerSec.add(defaultPrinterCombo);

        printerSec.add(new JLabel("Default Network IP & Port:"));
        JPanel ipPortBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        ipPortBox.setOpaque(false);
        defaultNetIpField.setPreferredSize(new Dimension(140, 26));
        defaultNetPortField.setPreferredSize(new Dimension(60, 26));
        ipPortBox.add(defaultNetIpField);
        ipPortBox.add(new JLabel(":"));
        ipPortBox.add(defaultNetPortField);
        printerSec.add(ipPortBox);

        contentPanel.add(printerSec);
        contentPanel.add(Box.createVerticalStrut(12));

        // Section 2: Multi-Client Label Roll & Paper Layout Configuration
        JPanel layoutSec = new JPanel(new GridLayout(0, 2, 10, 8));
        layoutSec.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                " Label Roll Configuration & Multi-Customer Presets ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(30, 41, 59)
        ));
        layoutSec.setOpaque(false);

        // Presets
        String[] presets = {
                "-- Select Standard Preset --",
                "1-Up Single Label (50mm x 25mm)",
                "1-Up Single Shipping Label (100mm x 50mm)",
                "2-Up Dual Labels (104mm x 25mm) - Winpal Default",
                "3-Up Triple Labels (104mm x 25mm)"
        };
        presetCombo = new JComboBox<String>(presets);
        presetCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applyPreset(presetCombo.getSelectedIndex());
            }
        });

        // Labels per row dropdown
        String[] rowCounts = {"1 Label per Row (Single 1-Up)", "2 Labels per Row (Dual 2-Up)", "3 Labels per Row (Triple 3-Up)", "4 Labels per Row (Quad 4-Up)"};
        labelsPerRowCombo = new JComboBox<String>(rowCounts);
        int currentN = Math.max(1, Math.min(4, config.getLabelsPerRow()));
        labelsPerRowCombo.setSelectedIndex(currentN - 1);
        labelsPerRowCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int chosen = labelsPerRowCombo.getSelectedIndex() + 1;
                config.setLabelsPerRow(chosen);
                if (rightTabbedPane != null) {
                    rightTabbedPane.setTitleAt(0, getTabPreviewTitle());
                }
                updatePreview();
                updateTsplScript();
                updateStats();
            }
        });

        widthSpinner = new JSpinner(new SpinnerNumberModel(config.getLabelWidthMm(), 20.0, 150.0, 1.0));
        heightSpinner = new JSpinner(new SpinnerNumberModel(config.getLabelHeightMm(), 10.0, 150.0, 1.0));
        gapSpinner = new JSpinner(new SpinnerNumberModel(config.getGapMm(), 0.0, 10.0, 0.5));
        hGapSpinner = new JSpinner(new SpinnerNumberModel(config.getHorizontalGapMm(), 0.0, 10.0, 0.5));
        densitySpinner = new JSpinner(new SpinnerNumberModel(config.getDarkness(), 1, 15, 1));
        speedSpinner = new JSpinner(new SpinnerNumberModel(config.getPrintSpeed(), 1, 8, 1));
        leftXSpinner = new JSpinner(new SpinnerNumberModel(config.getLeftLabelX(), 0, 150, 2));
        topYSpinner = new JSpinner(new SpinnerNumberModel(config.getTopMarginY(), 0, 150, 2));
        currencyField = new JTextField(config.getCurrencySymbol());

        showSizeChk = new JCheckBox("Include Size in Label Header (value only, e.g. M)", config.isShowSize());
        showPriceChk = new JCheckBox("Include Price in Label Header", config.isShowPrice());
        showBarcodeTextChk = new JCheckBox("Show Human-Readable Numbers below Barcode", config.isShowBarcodeText());

        // Live Real-Time Update Listeners
        javax.swing.event.ChangeListener liveUpdateListener = new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                syncConfigFromUI();
                if (previewPanel != null) previewPanel.setConfig(config);
                updatePreview();
                updateTsplScript();
                updateStats();
            }
        };

        widthSpinner.addChangeListener(liveUpdateListener);
        heightSpinner.addChangeListener(liveUpdateListener);
        gapSpinner.addChangeListener(liveUpdateListener);
        hGapSpinner.addChangeListener(liveUpdateListener);
        densitySpinner.addChangeListener(liveUpdateListener);
        speedSpinner.addChangeListener(liveUpdateListener);
        leftXSpinner.addChangeListener(liveUpdateListener);
        topYSpinner.addChangeListener(liveUpdateListener);

        ActionListener toggleListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncConfigFromUI();
                if (previewPanel != null) previewPanel.setConfig(config);
                updatePreview();
                updateTsplScript();
                updateStats();
            }
        };
        showSizeChk.addActionListener(toggleListener);
        showPriceChk.addActionListener(toggleListener);
        showBarcodeTextChk.addActionListener(toggleListener);

        layoutSec.add(new JLabel("Quick Paper Preset:"));
        layoutSec.add(presetCombo);

        layoutSec.add(new JLabel("Labels per Row (Columns):"));
        layoutSec.add(labelsPerRowCombo);

        layoutSec.add(new JLabel("Total Roll Width (mm):"));
        layoutSec.add(widthSpinner);

        layoutSec.add(new JLabel("Label Height (mm):"));
        layoutSec.add(heightSpinner);

        layoutSec.add(new JLabel("Vertical Row Gap (mm):"));
        layoutSec.add(gapSpinner);

        layoutSec.add(new JLabel("Horizontal Gap Between Labels (mm):"));
        layoutSec.add(hGapSpinner);

        layoutSec.add(new JLabel("Print Darkness / Density (1-15):"));
        layoutSec.add(densitySpinner);

        layoutSec.add(new JLabel("Print Speed (inches/sec):"));
        layoutSec.add(speedSpinner);

        layoutSec.add(new JLabel("Left Margin Offset (dots):"));
        layoutSec.add(leftXSpinner);

        layoutSec.add(new JLabel("Top Margin Offset (dots):"));
        layoutSec.add(topYSpinner);

        layoutSec.add(new JLabel("Currency Symbol:"));
        layoutSec.add(currencyField);

        layoutSec.add(showSizeChk);
        layoutSec.add(showPriceChk);
        layoutSec.add(showBarcodeTextChk);
        layoutSec.add(new JLabel(""));

        contentPanel.add(layoutSec);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        rootPanel.add(scrollPane, BorderLayout.CENTER);

        ModernButton applyBtn = new ModernButton("Save All Settings as Default", new Color(29, 78, 216), Color.WHITE);
        applyBtn.setPreferredSize(new Dimension(220, 38));
        applyBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applyAndSaveSettings();
            }
        });

        rootPanel.add(applyBtn, BorderLayout.SOUTH);

        return rootPanel;
    }

    private void applyPreset(int index) {
        if (index == 1) {
            // 1-Up Single 50x25mm
            labelsPerRowCombo.setSelectedIndex(0); // 1
            widthSpinner.setValue(50.0);
            heightSpinner.setValue(25.0);
            gapSpinner.setValue(2.0);
            hGapSpinner.setValue(0.0);
            leftXSpinner.setValue(0);
            topYSpinner.setValue(28);
        } else if (index == 2) {
            // 1-Up Single Shipping 100x50mm
            labelsPerRowCombo.setSelectedIndex(0); // 1
            widthSpinner.setValue(100.0);
            heightSpinner.setValue(50.0);
            gapSpinner.setValue(2.0);
            hGapSpinner.setValue(0.0);
            leftXSpinner.setValue(0);
            topYSpinner.setValue(28);
        } else if (index == 3) {
            // 2-Up Dual 104x25mm
            labelsPerRowCombo.setSelectedIndex(1); // 2
            widthSpinner.setValue(104.0);
            heightSpinner.setValue(25.0);
            gapSpinner.setValue(2.0);
            hGapSpinner.setValue(2.5);
            leftXSpinner.setValue(0);
            topYSpinner.setValue(28);
        } else if (index == 4) {
            // 3-Up Triple 104x25mm
            labelsPerRowCombo.setSelectedIndex(2); // 3
            widthSpinner.setValue(104.0);
            heightSpinner.setValue(25.0);
            gapSpinner.setValue(2.0);
            hGapSpinner.setValue(2.0);
            leftXSpinner.setValue(0);
            topYSpinner.setValue(28);
        }
    }

    // =========================================================================
    // 4. BOTTOM BAR (PRINTER TARGET & PRINT ACTIONS)
    // =========================================================================
    private JPanel createBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setBackground(new Color(240, 243, 248));
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 222, 230)),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel printerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        printerPanel.setOpaque(false);

        usbRadio = new JRadioButton("USB / Windows Spooler", true);
        netRadio = new JRadioButton("Network / LAN (TCP Socket)", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(usbRadio);
        bg.add(netRadio);

        printerCombo = new JComboBox<String>();
        printerCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sel = (String) printerCombo.getSelectedItem();
                if (sel != null && cmbLanguage != null) {
                    String lower = sel.toLowerCase();
                    if (lower.contains("zdesigner") || lower.contains("zebra") || lower.contains("gk888") || lower.contains("zd888") || lower.contains("tlp") || lower.contains("gx")) {
                        if (cmbLanguage.getSelectedIndex() != 1) {
                            cmbLanguage.setSelectedIndex(1);
                        }
                    } else if (lower.contains("winpal") || lower.contains("4barcode") || lower.contains("tsc") || lower.contains("xprinter") || lower.contains("gprinter")) {
                        if (cmbLanguage.getSelectedIndex() != 0) {
                            cmbLanguage.setSelectedIndex(0);
                        }
                    }
                }
            }
        });
        printerCombo.setPreferredSize(new Dimension(240, 30));
        printerCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isUpdatingPrinters) return;
                if (printerCombo.getSelectedItem() != null) {
                    String sel = String.valueOf(printerCombo.getSelectedItem());
                    SettingsManager.setPrinterName(sel);
                    if (defaultPrinterCombo != null) {
                        isUpdatingPrinters = true;
                        defaultPrinterCombo.setSelectedItem(sel);
                        isUpdatingPrinters = false;
                    }
                }
            }
        });

        JButton refreshPrintersBtn = new JButton("Refresh");
        refreshPrintersBtn.setToolTipText("Refresh Windows printers list");
        refreshPrintersBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshPrinterList();
            }
        });

        netIpField = new JTextField(SettingsManager.getNetworkIp(), 12);
        netPortField = new JTextField(String.valueOf(SettingsManager.getNetworkPort()), 5);

        ActionListener radioToggle = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isUpdatingPrinters) return;
                boolean isUsb = usbRadio.isSelected();
                printerCombo.setEnabled(isUsb);
                refreshPrintersBtn.setEnabled(isUsb);
                netIpField.setEnabled(!isUsb);
                netPortField.setEnabled(!isUsb);

                SettingsManager.setPrinterMode(isUsb ? "USB" : "NETWORK");

                if (defaultUsbRadio != null && defaultNetRadio != null) {
                    isUpdatingPrinters = true;
                    defaultUsbRadio.setSelected(isUsb);
                    defaultNetRadio.setSelected(!isUsb);
                    defaultPrinterCombo.setEnabled(isUsb);
                    defaultNetIpField.setEnabled(!isUsb);
                    defaultNetPortField.setEnabled(!isUsb);
                    isUpdatingPrinters = false;
                }
            }
        };
        usbRadio.addActionListener(radioToggle);
        netRadio.addActionListener(radioToggle);

        printerPanel.add(new JLabel("Target Printer:"));
        printerPanel.add(usbRadio);
        printerPanel.add(printerCombo);
        printerPanel.add(refreshPrintersBtn);
        printerPanel.add(Box.createHorizontalStrut(10));
        printerPanel.add(netRadio);
        printerPanel.add(new JLabel("IP:"));
        printerPanel.add(netIpField);
        printerPanel.add(new JLabel("Port:"));
        printerPanel.add(netPortField);

        // Right side: Print Execution Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        ModernButton testPrintBtn = new ModernButton("Test Print 1 Row", new Color(51, 65, 85), Color.WHITE);
        testPrintBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        testPrintBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onTestPrintRow();
            }
        });

        ModernButton printAllBtn = new ModernButton("PRINT SELECTED LABELS", new Color(5, 150, 105), Color.WHITE);
        printAllBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        printAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onPrintAllSelected();
            }
        });

        actions.add(testPrintBtn);
        actions.add(printAllBtn);

        statusLabel = new JLabel("Ready. Import an Excel or CSV file to begin.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(75, 85, 99));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(printerPanel, BorderLayout.WEST);
        topRow.add(actions, BorderLayout.EAST);

        bottom.add(topRow, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);

        return bottom;
    }

    // =========================================================================
    // BUSINESS LOGIC & EVENT HANDLERS
    // =========================================================================

    private void onLoadFromApi() {
        ApiSyncDialog dialog = new ApiSyncDialog(this);
        dialog.setVisible(true);
        List<Product> fetched = dialog.getFetchedProducts();
        if (fetched != null && !fetched.isEmpty()) {
            populateTable(fetched);
            currentPreviewRow = 0;
            statusLabel.setText("Successfully loaded " + fetched.size() + " products from API / ERP.");
            JOptionPane.showMessageDialog(this, "Loaded " + fetched.size() + " products from API successfully!", "API Sync Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onDownloadTemplate() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Product Import Template");
        fc.setSelectedFile(new File("Barcode_Print_Template.xlsx"));
        fc.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));

        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            try {
                TemplateGenerator.generateTemplateFile(dest);
                statusLabel.setText("Template saved: " + dest.getName());
                int openChoice = JOptionPane.showConfirmDialog(
                        this,
                        "Template successfully created at:\n" + dest.getAbsolutePath() + "\n\nWould you like to open it now?",
                        "Template Created",
                        JOptionPane.YES_NO_OPTION
                );
                if (openChoice == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dest);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to save template: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onImportFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Excel or CSV Product File");
        fc.setFileFilter(new FileNameExtensionFilter("Excel & Delimited Files (*.xlsx, *.csv, *.tsv)", "xlsx", "csv", "tsv", "txt"));

        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fc.getSelectedFile();
            try {
                List<Product> imported = ExcelParser.parseFile(selectedFile);
                if (imported.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No valid product rows could be detected in the file.", "Empty File", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                populateTable(imported);
                currentPreviewRow = 0;
                statusLabel.setText("Successfully imported " + imported.size() + " products from " + selectedFile.getName());
                JOptionPane.showMessageDialog(this, "Imported " + imported.size() + " products successfully!", "Import Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error parsing file: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadSampleData() {
        List<Product> sample = new ArrayList<Product>();
        sample.add(new Product("750100990011", "TS-001", "Classic Crewneck T-Shirt", "M", "18.50", 2));
        sample.add(new Product("750100990012", "TS-002", "Classic Crewneck T-Shirt", "L", "18.50", 2));
        sample.add(new Product("750100990013", "TS-003", "Classic Crewneck T-Shirt", "XL", "18.50", 2));
        sample.add(new Product("750100990021", "JN-101", "Slim Fit Denim Jeans", "32", "45.00", 1));
        sample.add(new Product("750100990022", "JN-102", "Slim Fit Denim Jeans", "34", "45.00", 1));
        sample.add(new Product("750100990031", "HD-201", "Cotton Pullover Hoodie", "L", "38.00", 2));
        sample.add(new Product("750100990041", "SH-301", "Casual Chino Shorts", "32", "24.99", 2));
        sample.add(new Product("750100990051", "BT-401", "Leather Dress Belt", "Free", "16.50", 1));
        sample.add(new Product("750100990061", "FT-501", "Running Athletic Shoes", "42", "65.00", 1));
        sample.add(new Product("750100990062", "FT-502", "Running Athletic Shoes", "43", "65.00", 1));

        populateTable(sample);
        currentPreviewRow = 0;
        statusLabel.setText("Loaded 10 sample products for Winpal label demonstration.");
    }

    private void populateTable(List<Product> list) {
        isUpdatingTable = true;
        try {
            products.clear();
            products.addAll(list);

            tableModel.setRowCount(0);
            for (Product p : products) {
                tableModel.addRow(new Object[]{
                        p.isSelected(),
                        p.getBarcode(),
                        p.getProductCode(),
                        p.getName(),
                        p.getSize(),
                        p.getPrice(),
                        p.getQuantity()
                });
            }
        } finally {
            isUpdatingTable = false;
        }
        updateStats();
        updatePreview();
        updateTsplScript();
    }

    private void syncProductsFromTable() {
        products.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean sel = (Boolean) tableModel.getValueAt(i, 0);
            String barcode = String.valueOf(tableModel.getValueAt(i, 1));
            String code = String.valueOf(tableModel.getValueAt(i, 2));
            String name = String.valueOf(tableModel.getValueAt(i, 3));
            String size = String.valueOf(tableModel.getValueAt(i, 4));
            String price = String.valueOf(tableModel.getValueAt(i, 5));
            int qty = 1;
            try {
                qty = Integer.parseInt(String.valueOf(tableModel.getValueAt(i, 6)));
            } catch (Exception ignored) {}

            Product p = new Product(barcode, code, name, size, price, qty);
            p.setSelected(sel != null && sel);
            products.add(p);
        }
    }

    private void onAddRow() {
        tableModel.addRow(new Object[]{true, "123456789012", "PRD-001", "New Product", "M", "10.00", 1});
    }

    private void onDeleteSelectedRows() {
        int[] rows = productTable.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select rows to delete from the table.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (int i = rows.length - 1; i >= 0; i--) {
            tableModel.removeRow(rows[i]);
        }
    }

    private void setSelectAll(boolean select) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(select, i, 0);
        }
    }

    private void onClearAll() {
        int confirm = JOptionPane.showConfirmDialog(this, "Clear all products from the table?", "Confirm Clear", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            isUpdatingTable = true;
            try {
                tableModel.setRowCount(0);
                products.clear();
            } finally {
                isUpdatingTable = false;
            }
            updatePreview();
            updateTsplScript();
            updateStats();
        }
    }

    private void syncConfigFromUI() {
        if (labelsPerRowCombo != null) config.setLabelsPerRow(labelsPerRowCombo.getSelectedIndex() + 1);
        if (widthSpinner != null) config.setLabelWidthMm((Double) widthSpinner.getValue());
        if (heightSpinner != null) config.setLabelHeightMm((Double) heightSpinner.getValue());
        if (gapSpinner != null) config.setGapMm((Double) gapSpinner.getValue());
        if (hGapSpinner != null) config.setHorizontalGapMm((Double) hGapSpinner.getValue());
        if (densitySpinner != null) config.setDarkness((Integer) densitySpinner.getValue());
        if (speedSpinner != null) config.setPrintSpeed((Integer) speedSpinner.getValue());
        if (leftXSpinner != null) config.setLeftLabelX((Integer) leftXSpinner.getValue());
        if (topYSpinner != null) config.setTopMarginY((Integer) topYSpinner.getValue());
        if (currencyField != null) config.setCurrencySymbol(currencyField.getText().trim());
        if (showSizeChk != null) config.setShowSize(showSizeChk.isSelected());
        if (showPriceChk != null) config.setShowPrice(showPriceChk.isSelected());
        if (showBarcodeTextChk != null) config.setShowBarcodeText(showBarcodeTextChk.isSelected());
    }

    private void applyAndSaveSettings() {
        // 1. Sync from UI inputs
        syncConfigFromUI();
        SettingsManager.saveConfig(config);

        // 2. Save Default Printer Settings
        boolean isUsb = defaultUsbRadio.isSelected();
        SettingsManager.setPrinterMode(isUsb ? "USB" : "NETWORK");

        String chosenPrinter = defaultPrinterCombo.getSelectedItem() != null ?
                String.valueOf(defaultPrinterCombo.getSelectedItem()) : "";

        if (!chosenPrinter.isEmpty()) {
            SettingsManager.setPrinterName(chosenPrinter);
        }

        String chosenIp = defaultNetIpField.getText().trim();
        SettingsManager.setNetworkIp(chosenIp);

        int chosenPort = 9100;
        try {
            chosenPort = Integer.parseInt(defaultNetPortField.getText().trim());
            SettingsManager.setNetworkPort(chosenPort);
        } catch (Exception ignored) {}

        // 3. Immediately synchronize bottom bar target printer controls
        isUpdatingPrinters = true;
        try {
            usbRadio.setSelected(isUsb);
            netRadio.setSelected(!isUsb);
            printerCombo.setEnabled(isUsb);
            netIpField.setEnabled(!isUsb);
            netPortField.setEnabled(!isUsb);

            if (!chosenPrinter.isEmpty()) {
                printerCombo.setSelectedItem(chosenPrinter);
            }
            netIpField.setText(chosenIp);
            netPortField.setText(String.valueOf(chosenPort));
        } finally {
            isUpdatingPrinters = false;
        }

        if (rightTabbedPane != null) {
            rightTabbedPane.setTitleAt(0, getTabPreviewTitle());
        }

        previewPanel.setConfig(config);
        updatePreview();
        updateTsplScript();
        updateStats();
        JOptionPane.showMessageDialog(this, "Default settings and printer configuration saved successfully!", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadSavedPrinterState() {
        isUpdatingPrinters = true;
        try {
            refreshPrinterList();

            String savedMode = SettingsManager.getPrinterMode();
            boolean isUsb = !"NETWORK".equalsIgnoreCase(savedMode);

            usbRadio.setSelected(isUsb);
            netRadio.setSelected(!isUsb);
            defaultUsbRadio.setSelected(isUsb);
            defaultNetRadio.setSelected(!isUsb);

            printerCombo.setEnabled(isUsb);
            netIpField.setEnabled(!isUsb);
            netPortField.setEnabled(!isUsb);

            defaultPrinterCombo.setEnabled(isUsb);
            defaultNetIpField.setEnabled(!isUsb);
            defaultNetPortField.setEnabled(!isUsb);

            String savedPrinter = SettingsManager.getPrinterName(RawPrintService.getDefaultPrinterName());
            if (savedPrinter != null && !savedPrinter.isEmpty()) {
                printerCombo.setSelectedItem(savedPrinter);
                defaultPrinterCombo.setSelectedItem(savedPrinter);
            }

            netIpField.setText(SettingsManager.getNetworkIp());
            netPortField.setText(String.valueOf(SettingsManager.getNetworkPort()));
            defaultNetIpField.setText(SettingsManager.getNetworkIp());
            defaultNetPortField.setText(String.valueOf(SettingsManager.getNetworkPort()));
        } finally {
            isUpdatingPrinters = false;
        }
    }

    private void refreshPrinterList() {
        boolean prevFlag = isUpdatingPrinters;
        isUpdatingPrinters = true;
        try {
            Object currentSel = printerCombo.getSelectedItem();
            printerCombo.removeAllItems();
            defaultPrinterCombo.removeAllItems();

            List<String> list = RawPrintService.getAvailablePrinters();
            for (String p : list) {
                printerCombo.addItem(p);
                defaultPrinterCombo.addItem(p);
            }

            if (currentSel != null && list.contains(currentSel.toString())) {
                printerCombo.setSelectedItem(currentSel);
                defaultPrinterCombo.setSelectedItem(currentSel);
            } else {
                String savedPrinter = SettingsManager.getPrinterName(RawPrintService.getDefaultPrinterName());
                if (savedPrinter != null && list.contains(savedPrinter)) {
                    printerCombo.setSelectedItem(savedPrinter);
                    defaultPrinterCombo.setSelectedItem(savedPrinter);
                }
            }
        } finally {
            isUpdatingPrinters = prevFlag;
        }
    }

    private List<Product> getExpandedSelectedProducts() {
        List<Product> list = new ArrayList<Product>();
        for (Product p : products) {
            if (p.isSelected() && p.getQuantity() > 0) {
                for (int i = 0; i < p.getQuantity(); i++) {
                    list.add(p);
                }
            }
        }
        return list;
    }

    private int calculateTotalRows() {
        List<Product> expanded = getExpandedSelectedProducts();
        int n = Math.max(1, config.getLabelsPerRow());
        return (int) Math.ceil(expanded.size() / (double) n);
    }

    private void updatePreview() {
        List<Product> expanded = getExpandedSelectedProducts();
        int n = Math.max(1, config.getLabelsPerRow());
        int totalRows = Math.max(1, (int) Math.ceil(expanded.size() / (double) n));

        if (currentPreviewRow >= totalRows) {
            currentPreviewRow = Math.max(0, totalRows - 1);
        }

        int startIdx = currentPreviewRow * n;
        List<Product> rowItems = new ArrayList<Product>();
        for (int i = 0; i < n; i++) {
            int idx = startIdx + i;
            if (idx < expanded.size()) {
                rowItems.add(expanded.get(idx));
            }
        }

        previewPanel.setPreviewRow(rowItems, currentPreviewRow + 1, totalRows);
        rowIndicatorLabel.setText(String.format("Row %d of %d", currentPreviewRow + 1, totalRows));
        prevRowBtn.setEnabled(currentPreviewRow > 0);
        nextRowBtn.setEnabled(currentPreviewRow < totalRows - 1);
    }

    private void updateTsplScript() {
        String script = config.isZpl()
                ? ZplGenerator.generatePrintJob(products, config)
                : TsplGenerator.generatePrintJob(products, config);
        tsplTextArea.setText(script);
        tsplTextArea.setCaretPosition(0);
    }

    private void updateStats() {
        int totalProducts = products.size();
        int totalLabels = 0;
        for (Product p : products) {
            if (p.isSelected()) {
                totalLabels += p.getQuantity();
            }
        }
        int n = Math.max(1, config.getLabelsPerRow());
        int totalRows = (int) Math.ceil(totalLabels / (double) n);
        statsLabel.setText(String.format("%d Products  |  %d Labels to Print  |  %d Physical Rows (%d label%s/row)",
                totalProducts, totalLabels, totalRows, n, (n > 1 ? "s" : "")));
    }

    private void onExportTsplFile() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(config.isZpl() ? "barcode_print_job.zpl" : "barcode_print_job.tspl"));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                FileWriter fw = new FileWriter(fc.getSelectedFile());
                fw.write(tsplTextArea.getText());
                fw.close();
                JOptionPane.showMessageDialog(this, "TSPL script saved successfully to " + fc.getSelectedFile().getName(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onTestPrintRow() {
        syncConfigFromUI();
        List<Product> expanded = getExpandedSelectedProducts();
        if (expanded.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No products in table to test print. Please add or import products first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int n = Math.max(1, config.getLabelsPerRow());
        int startIdx = currentPreviewRow * n;
        List<Product> rowItems = new ArrayList<Product>();
        for (int i = 0; i < n; i++) {
            int idx = startIdx + i;
            if (idx < expanded.size()) {
                rowItems.add(expanded.get(idx));
            }
        }

        String testTspl = config.isZpl()
                ? ZplGenerator.generateTestRow(rowItems, config)
                : TsplGenerator.generateTestRow(rowItems, config);
        executePrint(testTspl, "Test Row Print");
    }

    private void onPrintAllSelected() {
        syncConfigFromUI();
        List<Product> expanded = getExpandedSelectedProducts();
        if (expanded.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one product with quantity > 0 to print.", "No Labels Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int n = Math.max(1, config.getLabelsPerRow());
        int totalLabels = expanded.size();
        int totalRows = (int) Math.ceil(totalLabels / (double) n);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Are you ready to send %d labels (%d physical rows at %d per row) to the Winpal printer?", totalLabels, totalRows, n),
                "Confirm Print Job",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            String fullJob = config.isZpl()
                    ? ZplGenerator.generatePrintJob(products, config)
                    : TsplGenerator.generatePrintJob(products, config);
            executePrint(fullJob, "Batch Label Print (" + totalLabels + " labels)");
        }
    }

    private void executePrint(final String tsplCode, final String jobName) {
        statusLabel.setText("Sending print job to printer...");
        final boolean isUsb = usbRadio.isSelected();
        final String selectedPrinter = (String) printerCombo.getSelectedItem();
        final String ip = netIpField.getText().trim();
        int p = 9100;
        try {
            p = Integer.parseInt(netPortField.getText().trim());
        } catch (Exception ignored) {}
        final int port = p;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (isUsb) {
                    RawPrintService.printRaw(selectedPrinter, tsplCode, jobName);
                } else {
                    NetworkPrintService.printViaSocket(ip, port, tsplCode, 4000);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("[SUCCESS] Print job successfully dispatched: " + jobName);
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Print job dispatched successfully to " + (isUsb ? selectedPrinter : (ip + ":" + port)) + "!",
                            "Print Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    statusLabel.setText("[ERROR] Print failed: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Printing failed: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()) +
                                    "\n\nTips:\n- Make sure printer is turned on and connected via USB/LAN.\n- Verify printer name or IP address.",
                            "Print Failure",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =========================================================================
    // CUSTOM MODERN BUTTON COMPONENT (Vibrant, High-Contrast & Smooth)
    // =========================================================================
    public static class ModernButton extends JButton {
        private final Color normalBg;
        private final Color hoverBg;
        private final Color pressedBg;
        private final Color normalFg;
        private Color borderColor;
        private int cornerRadius = 6;

        public ModernButton(String text, Color bg, Color fg) {
            this(text, bg, fg, null);
        }

        public ModernButton(String text, Color bg, Color fg, Color border) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = brighten(bg, 1.12f);
            this.pressedBg = bg.darker();
            this.normalFg = fg;
            this.borderColor = border;

            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setForeground(fg);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 15, 8, 15));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (!isEnabled()) {
                g2.setColor(new Color(226, 232, 240));
                g2.fill(new RoundRectangle2D.Double(0, 0, w, h, cornerRadius, cornerRadius));
                g2.setColor(new Color(148, 163, 184));
            } else {
                if (getModel().isPressed()) {
                    g2.setColor(pressedBg);
                } else if (getModel().isRollover()) {
                    g2.setColor(hoverBg);
                } else {
                    g2.setColor(normalBg);
                }
                g2.fill(new RoundRectangle2D.Double(0, 0, w, h, cornerRadius, cornerRadius));

                if (borderColor != null) {
                    g2.setColor(borderColor);
                    g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));
                }

                g2.setColor(normalFg);
            }

            // Draw centered text
            FontMetrics fm = g2.getFontMetrics(getFont());
            int textX = (w - fm.stringWidth(getText())) / 2;
            int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.setFont(getFont());
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }

        private static Color brighten(Color c, float factor) {
            int r = Math.min(255, (int) (c.getRed() * factor));
            int g = Math.min(255, (int) (c.getGreen() * factor));
            int b = Math.min(255, (int) (c.getBlue() * factor));
            return new Color(r, g, b);
        }
    }
}
