package net.felix.utilities.ItemViewer;

/**
 * Gemeinsame Vergleichslogik für Stat- und Ebenen-Filter
 */
public final class ComparisonUtils {
    
    private ComparisonUtils() {
    }
    
    public static String normalizeOperator(String operator) {
        if (operator == null || operator.isEmpty()) {
            return "=";
        }
        String op = operator.trim();
        if (op.equals("==")) {
            return "=";
        }
        return op;
    }
    
    public static boolean compareInt(int itemValue, int filterValue, String operator) {
        switch (normalizeOperator(operator)) {
            case ">":
                return itemValue > filterValue;
            case "<":
                return itemValue < filterValue;
            case ">=":
                return itemValue >= filterValue;
            case "<=":
                return itemValue <= filterValue;
            case "=":
                return itemValue == filterValue;
            default:
                return false;
        }
    }
    
    public static boolean compareDouble(double itemValue, double filterValue, String operator) {
        switch (normalizeOperator(operator)) {
            case ">":
                return itemValue > filterValue;
            case "<":
                return itemValue < filterValue;
            case ">=":
                return itemValue >= filterValue;
            case "<=":
                return itemValue <= filterValue;
            case "=":
                return Math.abs(itemValue - filterValue) < 0.0001;
            default:
                return false;
        }
    }
}
