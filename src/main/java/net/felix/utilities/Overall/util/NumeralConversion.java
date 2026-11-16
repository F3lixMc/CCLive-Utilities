package net.felix.utilities.Overall.util;

import java.util.Map;
import net.felix.utilities.Overall.glyphs.ZeichenUtility;

/**
 * Umwandlung von roemischen und chinesischen Zahlen.
 */
public final class NumeralConversion {

	private NumeralConversion() {
	}

	/**
	 * Converts an arabic number to roman numeral
	 */
	public static String convertToRomanNumeral(int number) {
		if (number < 1 || number > 8) {
			return String.valueOf(number); // Return as string if out of range
		}
		
		String[] romanNumerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
		return romanNumerals[number - 1];
	}

	/**
	 * Converts a roman numeral to arabic number
	 */
	public static int romanToArabic(String roman) {
		java.util.Map<String, Integer> romanMap = new java.util.HashMap<>();
		romanMap.put("I", 1);
		romanMap.put("II", 2);
		romanMap.put("III", 3);
		romanMap.put("IV", 4);
		romanMap.put("V", 5);
		romanMap.put("VI", 6);
		romanMap.put("VII", 7);
		romanMap.put("VIII", 8);
		
		return romanMap.getOrDefault(roman, -1);
	}

	/**
	 * Decodes a Chinese number from text using aincraft_bottom_font
	 */
	public static int decodeChineseNumber(String text) {
		try {
			Map<Character, Integer> chineseNumbers = ZeichenUtility.getAincraftBottomFontNumbers();
			StringBuilder numberStr = new StringBuilder();
			
			for (char c : text.toCharArray()) {
				Integer digit = chineseNumbers.get(c);
				if (digit != null) {
					numberStr.append(digit);
				}
			}
			
			if (numberStr.length() > 0) {
				return Integer.parseInt(numberStr.toString());
			}
		} catch (Exception e) {
			// Silent error handling
		}
		return -1;
	}
}
