package net.felix.features.information.hover;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

/**
 * Liest Text und Komponenten aus HoverEvents.
 */
public final class HoverEventReader {

	private HoverEventReader() {
	}

	/**
	 * Recursively searches for a HoverEvent in a Text component and its siblings
	 */
	public static HoverEvent findHoverEventInText(Component text) {
		if (text == null) {
			return null;
		}
		
		// Check the main text's style
		if (text.getStyle() != null) {
			HoverEvent hoverEvent = text.getStyle().getHoverEvent();
			if (hoverEvent != null) {
				// Accept both SHOW_TEXT and SHOW_ITEM
				if (hoverEvent.action() == HoverEvent.Action.SHOW_TEXT || hoverEvent.action() == HoverEvent.Action.SHOW_ITEM) {
					return hoverEvent;
				}
			}
		}
		
		// Recursively check siblings
		for (Component sibling : text.getSiblings()) {
			HoverEvent siblingHoverEvent = findHoverEventInText(sibling);
			if (siblingHoverEvent != null) {
				return siblingHoverEvent;
			}
		}
		
		return null;
	}

	/**
	 * Extracts the Text from a HoverEvent using multiple approaches
	 * Note: In Minecraft 1.21.7, the text is not stored in the HoverEvent itself,
	 * but is generated dynamically. We try to extract it from the ShowText object.
	 */
	public static Component extractHoverTextFromEvent(HoverEvent hoverEvent, Component message) {
		// Try to extract from HoverEvent first (this is what we want - the actual tooltip text)
		Component result = extractHoverTextFromEvent(hoverEvent);
		if (result != null) {
			return result;
		}
		
		// Fallback: If we can't extract from HoverEvent, try to extract from the message itself
		// (but this is NOT what we want - we want the tooltip text, not the chat message text)
		if (message != null) {
			Component textWithHover = findTextComponentWithHoverEvent(message, hoverEvent);
			if (textWithHover != null) {
				return textWithHover.copy();
			}
		}
		
		return null;
	}

	/**
	 * Finds the Text component in a message that has the specified HoverEvent
	 */
	private static Component findTextComponentWithHoverEvent(Component message, HoverEvent targetHoverEvent) {
		if (message == null || targetHoverEvent == null) {
			return null;
		}
		
		// Check if this text component has the hover event
		if (message.getStyle() != null && message.getStyle().getHoverEvent() == targetHoverEvent) {
			return message;
		}
		
		// Recursively check siblings
		for (Component sibling : message.getSiblings()) {
			Component result = findTextComponentWithHoverEvent(sibling, targetHoverEvent);
			if (result != null) {
				return result;
			}
		}
		
		return null;
	}

	/**
	 * Extracts the Text from a HoverEvent using multiple approaches
	 * Note: In Minecraft 1.21.7, the text is not stored in the HoverEvent itself,
	 * but is generated dynamically. We try to extract it from the ShowText object.
	 */
	public static Component extractHoverTextFromEvent(HoverEvent hoverEvent) {
		if (hoverEvent == null) {
			return null;
		}
		// Only handle SHOW_TEXT for now
		if (hoverEvent.action() != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}

		Component direct = readTextFromHoverEvent(hoverEvent);
		if (direct != null) {
			return direct;
		}

		// ShowText-Objekt ermitteln und den Text daraus lesen
		Object showTextValue = getShowTextValue(hoverEvent);
		if (showTextValue == null) {
			return null;
		}
		// Ist der Wert das HoverEvent selbst, wird er direkt als ShowText ausgelesen
		if (showTextValue != hoverEvent && showTextValue instanceof HoverEvent nestedEvent) {
			Component recursiveResult = extractHoverTextFromEvent(nestedEvent);
			if (recursiveResult != null) {
				return recursiveResult;
			}
		}
		try {
			return readTextFromShowText(showTextValue);
		} catch (Exception e) {
			// Ignore
			return null;
		}
	}

	// ------------------------------------------------------------------ Text direkt aus dem HoverEvent

	/** Versucht getValue(Action), getValue(), value() und danach alle Instanzfelder des HoverEvents. */
	private static Component readTextFromHoverEvent(HoverEvent hoverEvent) {
		Component result = invokeDeclaredForComponent(HoverEvent.class, "getValue", hoverEvent,
			new Class<?>[] {HoverEvent.Action.class}, HoverEvent.Action.SHOW_TEXT);
		if (result == null) {
			result = invokeDeclaredForComponent(HoverEvent.class, "getValue", hoverEvent, new Class<?>[0]);
		}
		if (result == null) {
			result = invokeDeclaredForComponent(HoverEvent.class, "value", hoverEvent, new Class<?>[0]);
		}
		if (result == null) {
			result = readComponentFromHoverEventFields(hoverEvent);
		}
		return result;
	}

	/** Erster Component-Wert der Instanzfelder vom Typ Component oder Object. */
	private static Component readComponentFromHoverEventFields(HoverEvent hoverEvent) {
		try {
			for (Field field : HoverEvent.class.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				Class<?> fieldType = field.getType();
				if (fieldType == Component.class || fieldType == Object.class) {
					try {
						field.setAccessible(true);
						Object value = field.get(hoverEvent);
						if (value instanceof Component component) {
							return component;
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		return null;
	}

	/** Ruft eine deklarierte Methode auf; liefert das Ergebnis nur, wenn es ein Component ist. */
	private static Component invokeDeclaredForComponent(Class<?> type, String methodName, Object target,
														Class<?>[] parameterTypes, Object... args) {
		try {
			Method method = type.getDeclaredMethod(methodName, parameterTypes);
			method.setAccessible(true);
			Object value = method.invoke(target, args);
			if (value instanceof Component component) {
				return component;
			}
		} catch (Exception e) {
			// Ignore
		}
		return null;
	}

	private static Object invokeDeclared(Class<?> type, String methodName, Object target) throws Exception {
		Method method = type.getDeclaredMethod(methodName);
		method.setAccessible(true);
		return method.invoke(target);
	}

	// ------------------------------------------------------------------ Text aus dem ShowText-Objekt

	/**
	 * Liest den Text aus dem ShowText-Objekt. Die Versuche laufen in fester Reihenfolge, der erste Treffer gewinnt:
	 * Record-Komponenten, Text-Felder, bekannte Accessoren, alle Felder, alle parameterlosen Methoden.
	 */
	private static Component readTextFromShowText(Object showTextValue) {
		Class<?> showTextClass = showTextValue.getClass();

		Component result = readTextFromRecordComponents(showTextClass, showTextValue);
		if (result == null) {
			result = readTextFromTextLikeFields(showTextClass, showTextValue);
		}
		if (result == null) {
			result = invokeDeclaredForComponent(showTextClass, "method_10893", showTextValue, new Class<?>[0]);
		}
		if (result == null) {
			result = invokeDeclaredForComponent(showTextClass, "value", showTextValue, new Class<?>[0]);
		}
		if (result == null) {
			result = readTextViaTextAccessors(showTextClass, showTextValue);
		}

		Field[] fields = showTextClass.getDeclaredFields();
		if (result == null) {
			result = readComponentFromAnyField(fields, showTextValue);
		}
		if (result == null) {
			result = readTextFromOtherFields(fields, showTextValue);
		}

		Method[] declaredMethods = showTextClass.getDeclaredMethods();
		if (result == null) {
			result = readTextFromMethods(declaredMethods, showTextValue);
		}
		if (result == null) {
			result = readTextFromInheritedMethods(showTextClass, declaredMethods, showTextValue);
		}
		return result;
	}

	/** Frisch kopierter Text; schlägt das Kopieren fehl, wird das Original zurückgegeben. */
	private static Component copyOrOriginal(Component text) {
		try {
			return text.copy();
		} catch (Exception e) {
			return text;
		}
	}

	private static Component readTextFromRecordComponents(Class<?> showTextClass, Object showTextValue) {
		if (!showTextClass.isRecord()) {
			return null;
		}
		try {
			for (RecordComponent component : showTextClass.getRecordComponents()) {
				try {
					Object value = component.getAccessor().invoke(showTextValue);
					if (value instanceof Component text) {
						return copyOrOriginal(text);
					}
				} catch (Exception e) {
					// Ignore
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		return null;
	}

	/** Felder vom Typ Component, Object oder "...Text..." (außer String). */
	private static Component readTextFromTextLikeFields(Class<?> showTextClass, Object showTextValue) {
		try {
			for (Field field : showTextClass.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				Class<?> fieldType = field.getType();
				if (fieldType == Component.class || fieldType == Object.class || isTextNamedType(fieldType)) {
					try {
						field.setAccessible(true);
						Object value = field.get(showTextValue);
						if (value instanceof Component text) {
							return copyOrOriginal(text);
						}
					} catch (Exception e) {
						// Continue to next field
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		return null;
	}

	private static boolean isTextNamedType(Class<?> fieldType) {
		return fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class;
	}

	/** text(), sonst getText() (getText() nur, wenn text() eine Exception wirft). */
	private static Component readTextViaTextAccessors(Class<?> showTextClass, Object showTextValue) {
		try {
			Object textValue = invokeDeclared(showTextClass, "text", showTextValue);
			if (textValue instanceof Component text) {
				return text;
			}
		} catch (Exception e) {
			try {
				Object textValue = invokeDeclared(showTextClass, "getText", showTextValue);
				if (textValue instanceof Component text) {
					return text;
				}
			} catch (Exception e2) {
				// Ignore
			}
		}
		return null;
	}

	/** Alle Instanzfelder: erster Component-Wert (kopiert). */
	private static Component readComponentFromAnyField(Field[] fields, Object showTextValue) {
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			try {
				field.setAccessible(true);
				Object value = field.get(showTextValue);
				if (value instanceof Component text) {
					return copyOrOriginal(text);
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return null;
	}

	/** Übrige Referenzfelder: Component, String, Liste von Components oder ein verschachteltes Objekt mit Component-Feld. */
	private static Component readTextFromOtherFields(Field[] fields, Object showTextValue) {
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			Class<?> fieldType = field.getType();
			// Component- und Text-Felder wurden bereits geprüft
			if (fieldType == Component.class || isTextNamedType(fieldType)) {
				continue;
			}
			if (fieldType.isPrimitive()) {
				continue;
			}
			try {
				field.setAccessible(true);
				Object value = field.get(showTextValue);
				Component text = componentFromValue(value);
				if (text != null) {
					return text;
				}
				text = readComponentFromNestedObject(value);
				if (text != null) {
					return text;
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return null;
	}

	/** Sucht in einem verschachtelten (nicht-java.*) Objekt nach einem Feld vom Typ Component. */
	private static Component readComponentFromNestedObject(Object value) {
		if (value == null || value instanceof String || value instanceof List
			|| value.getClass().isPrimitive() || value.getClass().getName().startsWith("java.")) {
			return null;
		}
		try {
			for (Field nestedField : value.getClass().getDeclaredFields()) {
				if (Modifier.isStatic(nestedField.getModifiers())) {
					continue;
				}
				if (nestedField.getType() == Component.class) {
					try {
						nestedField.setAccessible(true);
						Object nestedValue = nestedField.get(value);
						if (nestedValue instanceof Component text) {
							return text;
						}
					} catch (Exception e) {
						// Continue
					}
				}
			}
		} catch (Exception e) {
			// Continue
		}
		return null;
	}

	/** Alle parameterlosen, nicht-statischen Methoden der Klasse selbst. */
	private static Component readTextFromMethods(Method[] methods, Object showTextValue) {
		for (Method method : methods) {
			Component text = invokeForComponentValue(method, showTextValue);
			if (text != null) {
				return text;
			}
		}
		return null;
	}

	/** Geerbte Methoden, die nicht schon in den deklarierten Methoden geprüft wurden. */
	private static Component readTextFromInheritedMethods(Class<?> showTextClass, Method[] declaredMethods,
														  Object showTextValue) {
		for (Method method : showTextClass.getMethods()) {
			if (isDeclaredIn(declaredMethods, method)) {
				continue;
			}
			Component text = invokeForComponentValue(method, showTextValue);
			if (text != null) {
				return text;
			}
		}
		return null;
	}

	private static boolean isDeclaredIn(Method[] declaredMethods, Method method) {
		for (Method declaredMethod : declaredMethods) {
			if (declaredMethod.getName().equals(method.getName())
				&& Arrays.equals(declaredMethod.getParameterTypes(), method.getParameterTypes())) {
				return true;
			}
		}
		return false;
	}

	/** Ruft eine parameterlose, nicht-statische Methode mit Referenz-Rückgabetyp auf und wandelt das Ergebnis in Text um. */
	private static Component invokeForComponentValue(Method method, Object target) {
		if (Modifier.isStatic(method.getModifiers())) {
			return null;
		}
		if (method.getParameterCount() != 0 || method.getReturnType().isPrimitive() || method.getReturnType() == Void.TYPE) {
			return null;
		}
		try {
			method.setAccessible(true);
			return componentFromValue(method.invoke(target));
		} catch (Exception e) {
			// Ignore
			return null;
		}
	}

	/**
	 * Wandelt einen Wert in Text um: Component direkt, String als Literal (Aktionsnamen und Leerstrings
	 * ausgenommen), Liste von Components zusammengefügt.
	 */
	private static Component componentFromValue(Object value) {
		if (value instanceof Component text) {
			return text;
		}
		if (value instanceof String stringValue) {
			if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>")
				&& !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
				return Component.literal(stringValue);
			}
			return null;
		}
		if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Component) {
			MutableComponent combined = Component.empty();
			for (Object item : list) {
				if (item instanceof Component component) {
					combined.append(component);
				}
			}
			return combined;
		}
		return null;
	}

	// ------------------------------------------------------------------ ShowText-Objekt finden

	/**
	 * Gets the ShowText value from a HoverEvent
	 * Tries various methods to extract the ShowText instance
	 */
	private static Object getShowTextValue(HoverEvent hoverEvent) {
		try {
			// In Minecraft 1.21.7 ist HoverEvent ein sealed interface: bei SHOW_TEXT ist das HoverEvent selbst das ShowText-Objekt
			if (hoverEvent.action() == HoverEvent.Action.SHOW_TEXT && isShowTextEvent(hoverEvent)) {
				return hoverEvent;
			}

			Object value = readValueMethod(hoverEvent);
			if (value != null) {
				return value;
			}

			value = findShowTextInFields(hoverEvent);
			if (value != null) {
				return value;
			}
			return findShowTextViaMethods(hoverEvent);
		} catch (Exception e) {
			// Ignore
		}
		return null;
	}

	/** Das HoverEvent selbst ist ein ShowText (nach Klassenname, innerer Klasse mit Text-Konstruktor oder Klasse aus net.minecraft.text). */
	private static boolean isShowTextEvent(HoverEvent hoverEvent) {
		if (isShowTextLike(hoverEvent)) {
			return true;
		}
		try {
			Class<?> showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
			return showTextClass.isInstance(hoverEvent);
		} catch (Exception e) {
			// Ignore
			return false;
		}
	}

	/** value() des HoverEvents (für Records), falls vorhanden und nicht null. */
	private static Object readValueMethod(HoverEvent hoverEvent) {
		try {
			return invokeDeclared(HoverEvent.class, "value", hoverEvent);
		} catch (Exception e) {
			// Ignore
			return null;
		}
	}

	/** Instanzfelder, deren Wert wie ein ShowText aussieht. */
	private static Object findShowTextInFields(HoverEvent hoverEvent) {
		for (Field field : HoverEvent.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			Class<?> fieldType = field.getType();
			if (fieldType.getName().contains("ShowText") || fieldType == Object.class || !fieldType.isPrimitive()) {
				try {
					field.setAccessible(true);
					Object value = field.get(hoverEvent);
					if (value != null && isShowTextLike(value)) {
						return value;
					}
				} catch (Exception e) {
					// Ignore
				}
			}
		}
		return null;
	}

	/** Erst Methoden mit ShowText-Rückgabetyp, dann alle parameterlosen Methoden mit ShowText-ähnlichem Ergebnis. */
	private static Object findShowTextViaMethods(HoverEvent hoverEvent) {
		Method[] methods = HoverEvent.class.getDeclaredMethods();

		for (Method method : methods) {
			if (Modifier.isStatic(method.getModifiers()) || method.getName().equals("getAction")) {
				continue;
			}
			if (method.getReturnType().getName().contains("ShowText")) {
				try {
					method.setAccessible(true);
					Object value = method.invoke(hoverEvent);
					if (value != null && value.getClass().getName().contains("ShowText")) {
						return value;
					}
				} catch (Exception e) {
					// Ignore
				}
			}
		}

		for (Method method : methods) {
			if (Modifier.isStatic(method.getModifiers()) || method.getName().equals("getAction")) {
				continue;
			}
			if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
				try {
					method.setAccessible(true);
					Object value = method.invoke(hoverEvent);
					if (value != null && isShowTextLike(value)) {
						return value;
					}
				} catch (Exception e) {
					// Ignore
				}
			}
		}
		return null;
	}

	/**
	 * Ein ShowText-Objekt hat "ShowText" im Klassennamen, oder (bei obfuskierten Klassen) ist eine innere
	 * HoverEvent-Klasse, die ein HoverEvent ist und einen Konstruktor mit einem Component-Parameter hat
	 * (Action hat das nicht).
	 */
	private static boolean isShowTextLike(Object value) {
		Class<?> valueClass = value.getClass();
		if (valueClass.getName().contains("ShowText")) {
			return true;
		}
		Class<?> enclosingClass = valueClass.getEnclosingClass();
		boolean innerOfHoverEvent = enclosingClass == HoverEvent.class
			|| (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"));
		return innerOfHoverEvent && HoverEvent.class.isAssignableFrom(valueClass) && hasComponentConstructor(valueClass);
	}

	private static boolean hasComponentConstructor(Class<?> type) {
		try {
			for (Constructor<?> constructor : type.getDeclaredConstructors()) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0] == Component.class) {
					return true;
				}
			}
		} catch (Exception e) {
			// Continue
		}
		return false;
	}
}
