package com.barcodeprinter.model;

/**
 * Represents a single product item with barcode, product code, name, size, price, and print quantity.
 */
public class Product {
    private String barcode;
    private String productCode;
    private String name;
    private String size;
    private String price;
    private int quantity;
    private boolean selected;

    public Product() {
        this("", "", "", "", "", 1);
    }

    public Product(String barcode, String productCode, String name, String size, String price) {
        this(barcode, productCode, name, size, price, 1);
    }

    public Product(String barcode, String productCode, String name, String size, String price, int quantity) {
        this.barcode = barcode != null ? barcode.trim() : "";
        this.productCode = productCode != null ? productCode.trim() : "";
        this.name = name != null ? name.trim() : "";
        this.size = size != null ? size.trim() : "";
        this.price = price != null ? price.trim() : "";
        this.quantity = Math.max(1, quantity);
        this.selected = true;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "Product{" +
                "barcode='" + barcode + '\'' +
                ", productCode='" + productCode + '\'' +
                ", name='" + name + '\'' +
                ", size='" + size + '\'' +
                ", price='" + price + '\'' +
                ", quantity=" + quantity +
                ", selected=" + selected +
                '}';
    }
}
