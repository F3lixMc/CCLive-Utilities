package net.felix.features.information.hover;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
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
		
		HoverEvent.Action action = hoverEvent.action();
		
		// Only handle SHOW_TEXT for now
		if (action != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Try HoverEvent#getValue(Action) - the official API method
		try {
			java.lang.reflect.Method getValueMethod = HoverEvent.class.getDeclaredMethod("getValue", HoverEvent.Action.class);
			getValueMethod.setAccessible(true);
			Object value = getValueMethod.invoke(hoverEvent, HoverEvent.Action.SHOW_TEXT);
			if (value instanceof Component) {
				return (Component) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try method getValue() without parameters (might exist in some versions)
		try {
			java.lang.reflect.Method getValueMethod = HoverEvent.class.getDeclaredMethod("getValue");
			getValueMethod.setAccessible(true);
			Object value = getValueMethod.invoke(hoverEvent);
			if (value instanceof Component) {
				return (Component) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try method value() (for records)
		try {
			java.lang.reflect.Method valueMethod = HoverEvent.class.getDeclaredMethod("value");
			valueMethod.setAccessible(true);
			Object value = valueMethod.invoke(hoverEvent);
			if (value instanceof Component) {
				return (Component) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try reading all fields in HoverEvent
		try {
			java.lang.reflect.Field[] fields = HoverEvent.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				// Skip static fields
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				
				Class<?> fieldType = field.getType();
				if (fieldType == Component.class || fieldType == Object.class) {
					try {
						field.setAccessible(true);
						Object value = field.get(hoverEvent);
						if (value instanceof Component) {
							return (Component) value;
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try ShowText inner class - FIRST get the ShowText value
		Object showTextValue = getShowTextValue(hoverEvent);
		if (showTextValue != null) {
			// CRITICAL: If showTextValue IS the hoverEvent itself, we need to extract from it directly
			// Don't recurse, just process it as ShowText
			if (showTextValue == hoverEvent) {
				// Continue with extraction below
			} else if (showTextValue instanceof HoverEvent) {
				Component recursiveResult = extractHoverTextFromEvent((HoverEvent) showTextValue);
				if (recursiveResult != null) {
					return recursiveResult;
				}
			}
			
			try {
				Class<?> showTextClass = showTextValue.getClass();
				
				// CRITICAL: Check if ShowText is a Record and extract via Record components
				if (showTextClass.isRecord()) {
					try {
						java.lang.reflect.RecordComponent[] components = showTextClass.getRecordComponents();
						for (java.lang.reflect.RecordComponent component : components) {
							try {
								Object value = component.getAccessor().invoke(showTextValue);
								if (value instanceof Component) {
									// Found a Text object! Copy it to create a fresh instance
									try {
										Component textValue = (Component) value;
										Component copiedText = textValue.copy();
										return copiedText;
									} catch (Exception e) {
										// If copy fails, return the original
										return (Component) value;
									}
								}
							} catch (Exception e) {
								// Ignore
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
				
				// CRITICAL: Try to find the Component object and serialize/deserialize it
				// This is the key approach: Component → JSON → Text (to get a fresh Text instance)
				// We need to find the actual Text/Component object first, then serialize/deserialize it
				try {
					// Try to find any Text object in the ShowText instance
					java.lang.reflect.Field[] showTextFields = showTextClass.getDeclaredFields();
					for (java.lang.reflect.Field field : showTextFields) {
						if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
							continue;
						}
						
						Class<?> fieldType = field.getType();
						// Look for fields that might contain a Component (Text or Object that could be Text)
						if (fieldType == Component.class || fieldType == Object.class || 
						    (fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class)) {
							try {
								field.setAccessible(true);
								Object value = field.get(showTextValue);
								
								if (value instanceof Component) {
									// Found a Text object! Copy it to create a fresh instance
									try {
										Component textValue = (Component) value;
										Component copiedText = textValue.copy();
										return copiedText;
									} catch (Exception e) {
										// If copy fails, return the original
										return (Component) value;
									}
								}
							} catch (Exception e) {
								// Continue to next field
							}
						}
					}
				} catch (Exception e) {
					// Ignore
				}
				
				// CRITICAL: Try method_10893() first - this is the obfuscated method that returns the Text!
				try {
					java.lang.reflect.Method method10893 = showTextClass.getDeclaredMethod("method_10893");
					method10893.setAccessible(true);
					Object textValue = method10893.invoke(showTextValue);
					if (textValue instanceof Component) {
						return (Component) textValue;
					}
				} catch (Exception e) {
					// Ignore
				}
				
				// Try method value() on ShowText (for records)
				try {
					java.lang.reflect.Method valueMethod = showTextClass.getDeclaredMethod("value");
					valueMethod.setAccessible(true);
					Object textValue = valueMethod.invoke(showTextValue);
					if (textValue instanceof Component) {
						return (Component) textValue;
					}
				} catch (Exception e) {
					// Ignore
				}
				
				// Try method text() or getText() on ShowText
				try {
					java.lang.reflect.Method textMethod = showTextClass.getDeclaredMethod("text");
					textMethod.setAccessible(true);
					Object textValue = textMethod.invoke(showTextValue);
					if (textValue instanceof Component) {
						return (Component) textValue;
					}
				} catch (Exception e) {
					// Try getText()
					try {
						java.lang.reflect.Method getTextMethod = showTextClass.getDeclaredMethod("getText");
						getTextMethod.setAccessible(true);
						Object textValue = getTextMethod.invoke(showTextValue);
						if (textValue instanceof Component) {
							return (Component) textValue;
						}
					} catch (Exception e2) {
						// Ignore
					}
				}
				
				// Try all fields in ShowText (excluding static)
				java.lang.reflect.Field[] showTextFields = showTextClass.getDeclaredFields();
				
				// CRITICAL: Check ALL fields first, including ALL types, to find the Text object
				for (java.lang.reflect.Field field : showTextFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					
					try {
						field.setAccessible(true);
						Object value = field.get(showTextValue);
						
						// Check if it's Text directly
						if (value instanceof Component) {
							Component textValue = (Component) value;
							try {
								Component copiedText = textValue.copy();
								return copiedText;
							} catch (Exception e) {
								return textValue;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
				
				// First pass: Check fields with Text type (highest priority)
				for (java.lang.reflect.Field field : showTextFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					
					Class<?> fieldType = field.getType();
					if (fieldType == Component.class || (fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class)) {
						try {
							field.setAccessible(true);
							Object value = field.get(showTextValue);
							if (value instanceof Component) {
								return (Component) value;
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				
				// Second pass: Check ALL fields (including primitives for debugging, but only process non-primitives)
				// IMPORTANT: We need to check ALL 9 fields to find the Text!
				for (java.lang.reflect.Field field : showTextFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					
					Class<?> fieldType = field.getType();
					
					// Skip if already checked in first pass
					if (fieldType == Component.class || (fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class)) {
						continue;
					}
					
					// Check all non-primitive fields for Text
					if (!fieldType.isPrimitive()) {
						try {
							field.setAccessible(true);
							Object value = field.get(showTextValue);
							
							// Check if it's Text directly
							if (value instanceof Component) {
								return (Component) value;
							}
							
							// Check if it's a String (but skip action names)
							if (value instanceof String) {
								String stringValue = (String) value;
								if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>") && 
								    !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
									// Use as literal text
									return Component.literal(stringValue);
								}
							}
							
							// Check if it's a List of Text components
							if (value instanceof java.util.List) {
								java.util.List<?> list = (java.util.List<?>) value;
								if (!list.isEmpty() && list.get(0) instanceof Component) {
									// Combine all Text components into one
									net.minecraft.network.chat.MutableComponent combined = Component.empty();
									for (Object item : list) {
										if (item instanceof Component) {
											combined.append((Component) item);
										}
									}
									return combined;
								}
							}
							
							// Check if it's an object that might contain Text (recursive search)
							if (value != null && !(value instanceof String) && !(value instanceof java.util.List) && 
							    !value.getClass().isPrimitive() && !value.getClass().getName().startsWith("java.")) {
								// Try to find Text in nested object
								try {
									java.lang.reflect.Field[] nestedFields = value.getClass().getDeclaredFields();
									for (java.lang.reflect.Field nestedField : nestedFields) {
										if (java.lang.reflect.Modifier.isStatic(nestedField.getModifiers())) {
											continue;
										}
										if (nestedField.getType() == Component.class) {
											try {
												nestedField.setAccessible(true);
												Object nestedValue = nestedField.get(value);
												if (nestedValue instanceof Component) {
													return (Component) nestedValue;
												}
											} catch (Exception e) {
												// Continue
											}
										}
									}
								} catch (Exception e) {
									// Continue
								}
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				
				// Try all methods that might return Text (including inherited methods)
				java.lang.reflect.Method[] showTextMethods = showTextClass.getDeclaredMethods();
				java.lang.reflect.Method[] allMethods = showTextClass.getMethods();
				
				// Try declared methods first
				for (java.lang.reflect.Method method : showTextMethods) {
					if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
						continue;
					}
					// Check ALL methods that take no parameters and return non-primitive (including String)
					if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
						try {
							method.setAccessible(true);
							Object textValue = method.invoke(showTextValue);
							if (textValue instanceof Component) {
								return (Component) textValue;
							} else if (textValue instanceof String) {
								// Maybe the text is stored as a String? Use it as literal (but skip action names)
								String stringValue = (String) textValue;
								if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>") && 
								    !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
									// Use as literal text
									return Component.literal(stringValue);
								}
							} else if (textValue instanceof java.util.List) {
								// Maybe it's a list of Text components?
								java.util.List<?> list = (java.util.List<?>) textValue;
								if (!list.isEmpty() && list.get(0) instanceof Component) {
									// Combine all Text components into one
									net.minecraft.network.chat.MutableComponent combined = Component.empty();
									for (Object item : list) {
										if (item instanceof Component) {
											combined.append((Component) item);
										}
									}
									return combined;
								}
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				
				// Also try inherited methods
				for (java.lang.reflect.Method method : allMethods) {
					if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
						continue;
					}
					// Skip if already checked in declared methods
					boolean alreadyChecked = false;
					for (java.lang.reflect.Method declaredMethod : showTextMethods) {
						if (declaredMethod.getName().equals(method.getName()) && 
						    java.util.Arrays.equals(declaredMethod.getParameterTypes(), method.getParameterTypes())) {
							alreadyChecked = true;
							break;
						}
					}
					if (alreadyChecked) continue;
					
					// Check ALL methods that take no parameters and return non-primitive (including String)
					if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
						try {
							method.setAccessible(true);
							Object textValue = method.invoke(showTextValue);
							if (textValue instanceof Component) {
								return (Component) textValue;
							} else if (textValue instanceof String) {
								// Maybe the text is stored as a String? Use it as literal (but skip action names)
								String stringValue = (String) textValue;
								if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>") && 
								    !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
									// Use as literal text
									return Component.literal(stringValue);
								}
							} else if (textValue instanceof java.util.List) {
								// Maybe it's a list of Text components?
								java.util.List<?> list = (java.util.List<?>) textValue;
								if (!list.isEmpty() && list.get(0) instanceof Component) {
									// Combine all Text components into one
									net.minecraft.network.chat.MutableComponent combined = Component.empty();
									for (Object item : list) {
										if (item instanceof Component) {
											combined.append((Component) item);
										}
									}
									return combined;
								}
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		
		return null;
	}

	/**
	 * Gets the ShowText value from a HoverEvent
	 * Tries various methods to extract the ShowText instance
	 */
	private static Object getShowTextValue(HoverEvent hoverEvent) {
		try {
			// CRITICAL: In Minecraft 1.21.7, HoverEvent is a sealed interface/class
			// If the action is SHOW_TEXT, the HoverEvent itself IS the ShowText object!
			HoverEvent.Action action = hoverEvent.action();
			if (action == HoverEvent.Action.SHOW_TEXT) {
				// Check if the HoverEvent itself is a ShowText instance
				String className = hoverEvent.getClass().getName();
				if (className.contains("ShowText")) {
					return hoverEvent;
				}
				
				// Check if it's an inner class of HoverEvent and has a Text constructor (ShowText has this)
				Class<?> hoverEventClass = hoverEvent.getClass();
				Class<?> enclosingClass = hoverEventClass.getEnclosingClass();
				if (enclosingClass == HoverEvent.class || (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"))) {
					// It's an inner class of HoverEvent - check if it has a constructor that takes Text
					try {
						java.lang.reflect.Constructor<?>[] constructors = hoverEventClass.getDeclaredConstructors();
						for (java.lang.reflect.Constructor<?> constructor : constructors) {
							Class<?>[] paramTypes = constructor.getParameterTypes();
							if (paramTypes.length == 1 && paramTypes[0] == Component.class) {
								return hoverEvent;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
				
				// Try to cast it to ShowText by class name
				try {
					Class<?> showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
					if (showTextClass.isInstance(hoverEvent)) {
						return hoverEvent;
					}
				} catch (Exception e) {
					// Ignore
				}
			}
			
			// Try method value() first (for records)
			try {
				java.lang.reflect.Method valueMethod = HoverEvent.class.getDeclaredMethod("value");
				valueMethod.setAccessible(true);
				Object value = valueMethod.invoke(hoverEvent);
				if (value != null) {
					return value;
				}
			} catch (Exception e) {
				// Ignore
			}
			
			// Try all declared fields (excluding static)
			java.lang.reflect.Field[] fields = HoverEvent.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				// Skip static fields
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				
				Class<?> fieldType = field.getType();
				String fieldTypeName = fieldType.getName();
				
				if (fieldTypeName.contains("ShowText") || fieldType == Object.class || !fieldType.isPrimitive()) {
					try {
						field.setAccessible(true);
						Object value = field.get(hoverEvent);
						if (value != null) {
							// Check if it's a ShowText-like object
							String className = value.getClass().getName();
							
							boolean isShowText = false;
							if (className.contains("ShowText")) {
								isShowText = true;
							} else {
								// Check if it's an inner class of HoverEvent and assignable to HoverEvent with Text constructor
								Class<?> valueClass = value.getClass();
								Class<?> enclosingClass = valueClass.getEnclosingClass();
								if (enclosingClass == HoverEvent.class || (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"))) {
									if (HoverEvent.class.isAssignableFrom(valueClass)) {
										try {
											java.lang.reflect.Constructor<?>[] constructors = valueClass.getDeclaredConstructors();
											for (java.lang.reflect.Constructor<?> constructor : constructors) {
												Class<?>[] paramTypes = constructor.getParameterTypes();
												if (paramTypes.length == 1 && paramTypes[0] == Component.class) {
													isShowText = true;
													break;
												}
											}
										} catch (Exception e) {
											// Continue
										}
									}
								}
							}
							
							if (isShowText) {
								return value;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
			
			// Try all methods that might return the value
			java.lang.reflect.Method[] methods = HoverEvent.class.getDeclaredMethods();
			
			// First, try methods that return ShowText directly (by return type name)
			for (java.lang.reflect.Method method : methods) {
				// Skip static methods
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				// Skip getAction() - that returns Action, not ShowText!
				if (method.getName().equals("getAction")) {
					continue;
				}
				// Check if return type contains ShowText
				String returnTypeName = method.getReturnType().getName();
				if (returnTypeName.contains("ShowText")) {
					try {
						method.setAccessible(true);
						Object value = method.invoke(hoverEvent);
						if (value != null) {
							if (value.getClass().getName().contains("ShowText")) {
								return value;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
			
			// Then try all other methods that might return ShowText
			for (java.lang.reflect.Method method : methods) {
				// Skip static methods
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				// Skip getAction() - that returns Action, not ShowText!
				if (method.getName().equals("getAction")) {
					continue;
				}
				// Look for methods that take no parameters and return non-primitive
				if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
					try {
						method.setAccessible(true);
						Object value = method.invoke(hoverEvent);
						if (value != null) {
							// Check if it's a ShowText-like object
							String className = value.getClass().getName();
							
							// CRITICAL: Check if it contains "ShowText" OR if it's an inner class of HoverEvent that is assignable to HoverEvent
							// (ShowText is assignable to HoverEvent, but Action is NOT)
							boolean isShowText = false;
							
							if (className.contains("ShowText")) {
								isShowText = true;
							} else {
								// Check if it's an inner class of HoverEvent and assignable to HoverEvent
								// This handles obfuscated ShowText classes
								Class<?> valueClass = value.getClass();
								Class<?> enclosingClass = valueClass.getEnclosingClass();
								if (enclosingClass == HoverEvent.class || (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"))) {
									// It's an inner class of HoverEvent
									// Check if it's assignable to HoverEvent (ShowText is, Action is NOT)
									if (HoverEvent.class.isAssignableFrom(valueClass)) {
										// Check if it has a constructor that takes Text (ShowText has this, Action doesn't)
										try {
											java.lang.reflect.Constructor<?>[] constructors = valueClass.getDeclaredConstructors();
											for (java.lang.reflect.Constructor<?> constructor : constructors) {
												Class<?>[] paramTypes = constructor.getParameterTypes();
												if (paramTypes.length == 1 && paramTypes[0] == Component.class) {
													isShowText = true;
													break;
												}
											}
										} catch (Exception e) {
											// Continue
										}
									}
								}
							}
							
							if (isShowText) {
								return value;
							}
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
}
