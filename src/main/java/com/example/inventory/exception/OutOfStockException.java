package com.example.inventory.exception;

/** Thrown when a sale (or a stock write-off) asks for more units than are available. */
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String sku, int requested, int available) {
        super("Insufficient stock for SKU '" + sku + "': requested " + requested + ", available " + available);
    }
}
