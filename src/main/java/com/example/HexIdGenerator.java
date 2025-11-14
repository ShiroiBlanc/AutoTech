package com.example;

import java.security.SecureRandom;

public class HexIdGenerator {
    private static final SecureRandom random = new SecureRandom();
    private static final String HEX_CHARS = "0123456789ABCDEF";
    
    /**
     * Generates a random hexadecimal ID with the specified prefix and length
     * @param prefix Prefix for the ID (e.g., "CUST", "BOOK", "PART")
     * @param length Length of the hex portion (default 8)
     * @return Generated ID (e.g., "CUST-A1B2C3D4")
     */
    public static String generate(String prefix, int length) {
        StringBuilder hexId = new StringBuilder();
        for (int i = 0; i < length; i++) {
            hexId.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
        }
        return prefix + "-" + hexId.toString();
    }
    
    /**
     * Generates a hex ID with default length of 8 characters
     */
    public static String generate(String prefix) {
        return generate(prefix, 8);
    }
    
    /**
     * Generates hex IDs for different modules
     */
    public static String generateCustomerId() {
        return generate("CUST", 8);
    }
    
    public static String generateVehicleId() {
        return generate("VEH", 8);
    }
    
    public static String generateBookingId() {
        return generate("BOOK", 8);
    }
    
    public static String generatePartId() {
        return generate("PART", 8);
    }
    
    public static String generateBillId() {
        return generate("BILL", 8);
    }
    
    public static String generateMechanicId() {
        return generate("MECH", 8);
    }
    
    public static String generateInvoiceId() {
        return generate("INV", 8);
    }
}
