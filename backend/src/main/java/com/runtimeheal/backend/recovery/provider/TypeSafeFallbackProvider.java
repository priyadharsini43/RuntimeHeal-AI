package com.runtimeheal.backend.recovery.provider;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Generates type-safe default and fallback return values based on method return type metadata.
 */
@Component
public class TypeSafeFallbackProvider {

    private final Map<String, Supplier<Object>> customSuppliers = new ConcurrentHashMap<>();

    public TypeSafeFallbackProvider() {
        // Register known application fallbacks
        registerCustomFallback("com.runtimeheal.backend.sample.service.OrderProcessingService$CustomerProfile",
                () -> null);
    }

    public void registerCustomFallback(String className, Supplier<Object> supplier) {
        customSuppliers.put(className, supplier);
    }

    /**
     * Supplies a type-safe fallback value matching the specified return type name.
     */
    public Object getFallbackValue(String returnTypeName) {
        if (returnTypeName == null || returnTypeName.trim().isEmpty() ||
                "void".equalsIgnoreCase(returnTypeName) || "V".equals(returnTypeName)) {
            return null;
        }

        String type = returnTypeName.trim();

        // 1. Check custom registered suppliers
        if (customSuppliers.containsKey(type)) {
            return customSuppliers.get(type).get();
        }

        // 2. Primitives and Boxed Numerics
        switch (type) {
            case "int":
            case "java.lang.Integer":
            case "Integer":
                return 0;

            case "double":
            case "java.lang.Double":
            case "Double":
                return 0.0;

            case "long":
            case "java.lang.Long":
            case "Long":
                return 0L;

            case "float":
            case "java.lang.Float":
            case "Float":
                return 0.0f;

            case "boolean":
            case "java.lang.Boolean":
            case "Boolean":
                return Boolean.FALSE;

            case "short":
            case "java.lang.Short":
            case "Short":
                return (short) 0;

            case "byte":
            case "java.lang.Byte":
            case "Byte":
                return (byte) 0;

            case "char":
            case "java.lang.Character":
            case "Character":
                return '\0';

            case "java.lang.String":
            case "String":
                return "DEFAULT_FALLBACK";

            case "java.util.List":
            case "List":
                return Collections.emptyList();

            case "java.util.Map":
            case "Map":
                return Collections.emptyMap();

            case "java.util.Set":
            case "Set":
                return Collections.emptySet();

            case "java.util.Optional":
            case "Optional":
                return Optional.empty();

            default:
                // Default safe return for arbitrary reference types
                return null;
        }
    }
}
