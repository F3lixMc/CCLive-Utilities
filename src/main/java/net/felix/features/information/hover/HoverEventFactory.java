package net.felix.features.information.hover;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Erzeugt und ersetzt HoverEvents in Chatnachrichten.
 */
public final class HoverEventFactory {

	private HoverEventFactory() {
	}

	/**
	 * Creates a HoverEvent for aspect information
	 * Uses the same approach as TestUtility.createHoverEvent
	 */
	public static HoverEvent createHoverEventForAspect(Component hoverText) {
		if (hoverText == null) {
			return null;
		}
		
		// Use createHoverEventDirect which has more comprehensive logic
		HoverEvent result = createHoverEventDirect(hoverText);
		if (result != null) {
			return result;
		}
		
		// Try HoverEvent.showText() static factory method first
		try {
			java.lang.reflect.Method showTextMethod = HoverEvent.class.getDeclaredMethod("showText", Component.class);
			showTextMethod.setAccessible(true);
			HoverEvent result2 = (HoverEvent) showTextMethod.invoke(null, hoverText);
			return result2;
		} catch (Exception e) {
			// Ignore
		}
		
		// Try all static methods in HoverEvent
		try {
			for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0] == Component.class) {
						try {
							method.setAccessible(true);
							HoverEvent result3 = (HoverEvent) method.invoke(null, hoverText);
							return result3;
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try using HoverEvent$ShowText inner class
		try {
			Class<?> showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
			
			// Try ShowText constructors
			java.lang.reflect.Constructor<?>[] constructors = showTextClass.getDeclaredConstructors();
			for (java.lang.reflect.Constructor<?> constructor : constructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0] == Component.class) {
					try {
						constructor.setAccessible(true);
						Object showTextInstance = constructor.newInstance(hoverText);
						
						// Check if ShowText is assignable to HoverEvent (sealed interface/class pattern)
						if (HoverEvent.class.isAssignableFrom(showTextInstance.getClass())) {
							return (HoverEvent) showTextInstance;
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

	/**
	 * Creates a HoverEvent directly using the constructor
	 * In Minecraft 1.21.7, the constructor works at runtime even if not visible at compile time
	 */
	private static HoverEvent createHoverEventDirect(Component hoverText) {
		if (hoverText == null) {
			return null;
		}
		
		// First, try to find ShowText class dynamically by searching inner classes
		Class<?> showTextClass = null;
		try {
			// Try direct name first (for non-obfuscated environments)
			showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
		} catch (ClassNotFoundException e) {
			// If direct name fails, search inner classes dynamically
			Class<?>[] innerClasses = HoverEvent.class.getDeclaredClasses();
			for (Class<?> innerClass : innerClasses) {
				// Check if this inner class is assignable to HoverEvent or has a constructor that takes Text
				java.lang.reflect.Constructor<?>[] constructors = innerClass.getDeclaredConstructors();
				for (java.lang.reflect.Constructor<?> constructor : constructors) {
					Class<?>[] paramTypes = constructor.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0] == Component.class) {
						// This looks like ShowText!
						showTextClass = innerClass;
						break;
					}
				}
				if (showTextClass != null) break;
			}
		}
		
		// Try using ShowText inner class if we found it
		if (showTextClass != null) {
			try {
			
			// Check for static factory methods in ShowText
			for (java.lang.reflect.Method method : showTextClass.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0] == Component.class) {
						try {
							method.setAccessible(true);
							HoverEvent result = (HoverEvent) method.invoke(null, hoverText);
							return result;
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			}
			
			// Try constructors in ShowText
			java.lang.reflect.Constructor<?>[] constructors = showTextClass.getDeclaredConstructors();
			
			for (java.lang.reflect.Constructor<?> constructor : constructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0] == Component.class) {
					try {
						constructor.setAccessible(true);
						Object showTextInstance = constructor.newInstance(hoverText);
						
						// Check if ShowText is directly assignable to HoverEvent (sealed interface/class pattern)
						if (HoverEvent.class.isAssignableFrom(showTextInstance.getClass())) {
							return (HoverEvent) showTextInstance;
						}
						
						// Check interfaces
						Class<?>[] interfaces = showTextClass.getInterfaces();
						for (Class<?> iface : interfaces) {
							if (iface == HoverEvent.class || HoverEvent.class.isAssignableFrom(iface)) {
								return (HoverEvent) showTextInstance;
							}
						}
						
						// Check superclass
						Class<?> superclass = showTextClass.getSuperclass();
						if (superclass != null) {
							if (superclass == HoverEvent.class || HoverEvent.class.isAssignableFrom(superclass)) {
								return (HoverEvent) showTextInstance;
							}
						}
						
						// Check if ShowText has a method to create HoverEvent
						for (java.lang.reflect.Method method : showTextClass.getDeclaredMethods()) {
							if (method.getReturnType() == HoverEvent.class) {
								try {
									method.setAccessible(true);
									HoverEvent result = (HoverEvent) method.invoke(showTextInstance);
									return result;
								} catch (Exception e) {
									// Ignore
								}
							}
						}
						
						// Check if ShowText implements an interface or extends a class that has a method to create HoverEvent
						Class<?>[] showTextInterfaces = showTextClass.getInterfaces();
						for (Class<?> iface : showTextInterfaces) {
							for (java.lang.reflect.Method method : iface.getMethods()) {
								if (method.getReturnType() == HoverEvent.class) {
									try {
										HoverEvent result = (HoverEvent) method.invoke(showTextInstance);
										return result;
									} catch (Exception e) {
										// Ignore
									}
								}
							}
						}
						
						// Try to find a static method in HoverEvent that takes ShowText or Object
						for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
							if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && 
							    method.getReturnType() == HoverEvent.class) {
								Class<?>[] methodParamTypes = method.getParameterTypes();
								if (methodParamTypes.length == 1) {
									if (methodParamTypes[0].isAssignableFrom(showTextInstance.getClass()) || 
									    methodParamTypes[0] == Object.class ||
									    methodParamTypes[0] == HoverEvent.Action.class) {
										try {
											method.setAccessible(true);
											Object param = methodParamTypes[0] == HoverEvent.Action.class ? HoverEvent.Action.SHOW_TEXT : showTextInstance;
											HoverEvent result = (HoverEvent) method.invoke(null, param);
											return result;
										} catch (Exception e) {
											// Ignore
										}
									}
								} else if (methodParamTypes.length == 2 && 
								          methodParamTypes[0] == HoverEvent.Action.class &&
								          (methodParamTypes[1].isAssignableFrom(showTextInstance.getClass()) || methodParamTypes[1] == Object.class)) {
									try {
										method.setAccessible(true);
										HoverEvent result = (HoverEvent) method.invoke(null, HoverEvent.Action.SHOW_TEXT, showTextInstance);
										return result;
									} catch (Exception e) {
										// Ignore
									}
								}
							}
						}
						
						// Now try to create HoverEvent with ShowText instance using reflection
						java.lang.reflect.Constructor<?>[] hoverEventConstructors = HoverEvent.class.getDeclaredConstructors();
						for (java.lang.reflect.Constructor<?> hoverEventConstructor : hoverEventConstructors) {
							Class<?>[] hoverEventParamTypes = hoverEventConstructor.getParameterTypes();
							if (hoverEventParamTypes.length == 2 && 
							    hoverEventParamTypes[0] == HoverEvent.Action.class &&
							    hoverEventParamTypes[1].isAssignableFrom(showTextInstance.getClass())) {
								try {
									hoverEventConstructor.setAccessible(true);
									HoverEvent result = (HoverEvent) hoverEventConstructor.newInstance(HoverEvent.Action.SHOW_TEXT, showTextInstance);
									return result;
								} catch (Exception e) {
									// Ignore
								}
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
		
		// Fallback: Try using reflection on HoverEvent directly
		HoverEvent result = createHoverEventWithReflection(hoverText);
		return result;
	}

	/**
	 * Creates a HoverEvent using reflection as fallback
	 * In 1.21.7, HoverEvent can be created with: new HoverEvent(Action, Text)
	 * We use reflection since the constructor may not be visible at compile time
	 */
	private static HoverEvent createHoverEventWithReflection(Component hoverText) {
		
		java.lang.reflect.Constructor<?>[] declaredConstructors = HoverEvent.class.getDeclaredConstructors();
		java.lang.reflect.Constructor<?>[] publicConstructors = HoverEvent.class.getConstructors();
		
		try {
			// Try to find and call the constructor
			for (java.lang.reflect.Constructor<?> constructor : declaredConstructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class) {
					try {
						constructor.setAccessible(true);
						HoverEvent result = (HoverEvent) constructor.newInstance(HoverEvent.Action.SHOW_TEXT, hoverText);
						return result;
					} catch (Exception e) {
						// Silent error handling
					}
				}
			}
			
			// Also try public constructors
			for (java.lang.reflect.Constructor<?> constructor : publicConstructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class) {
					try {
						HoverEvent result = (HoverEvent) constructor.newInstance(HoverEvent.Action.SHOW_TEXT, hoverText);
						return result;
					} catch (Exception e) {
						// Silent error handling
					}
				}
			}
			
			// Try static factory methods
			for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class) {
						try {
							method.setAccessible(true);
							HoverEvent result = (HoverEvent) method.invoke(null, HoverEvent.Action.SHOW_TEXT, hoverText);
							return result;
						} catch (Exception e) {
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
		return null;
	}

	/**
	 * Recursively modifies a Text component to replace hover events while preserving all formatting.
	 * Only replaces hover events where they exist (SHOW_TEXT action).
	 */
	public static Component modifyTextWithHoverEvent(Component text, HoverEvent newHoverEvent) {
		if (text == null) return text;
		
		// Copy the text to preserve its structure - copy() returns MutableComponent which has setStyle()
		net.minecraft.network.chat.MutableComponent newText = text.copy();
		
		// Check if this text component has a hover event that needs to be replaced
		Style currentStyle = text.getStyle();
		if (currentStyle != null && currentStyle.getHoverEvent() != null) {
			// This text has a hover event - replace it with the new one
			Style newStyle = currentStyle.withHoverEvent(newHoverEvent);
			newText.setStyle(newStyle);
		}
		// If no hover event, the style is already preserved by copy()
		
		// Recursively process all siblings to preserve their formatting
		newText.getSiblings().clear(); // Clear existing siblings to avoid duplication
		for (Component sibling : text.getSiblings()) {
			Component modifiedSibling = modifyTextWithHoverEvent(sibling, newHoverEvent);
			newText.getSiblings().add(modifiedSibling);
		}
		
		return newText;
	}
}
