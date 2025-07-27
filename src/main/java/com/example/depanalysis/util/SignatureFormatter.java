package com.example.depanalysis.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to convert ASM bytecode signatures to human-readable Java method signatures
 */
public class SignatureFormatter {
    
    private static final Map<String, String> PRIMITIVE_TYPES = new HashMap<>();
    static {
        PRIMITIVE_TYPES.put("Z", "boolean");
        PRIMITIVE_TYPES.put("B", "byte");
        PRIMITIVE_TYPES.put("C", "char");
        PRIMITIVE_TYPES.put("S", "short");
        PRIMITIVE_TYPES.put("I", "int");
        PRIMITIVE_TYPES.put("J", "long");
        PRIMITIVE_TYPES.put("F", "float");
        PRIMITIVE_TYPES.put("D", "double");
        PRIMITIVE_TYPES.put("V", "void");
    }
    
    /**
     * Convert ASM method signature to human-readable format
     * Example: "parseBase64Binary(Ljava/lang/String;)[B" -> "byte[] parseBase64Binary(String)"
     */
    public static String formatMethodSignature(String methodName, String asmSignature) {
        if (asmSignature == null || asmSignature.isEmpty()) {
            return methodName + "()";
        }
        
        try {
            // Pattern to match method signature: (parameters)returnType
            Pattern pattern = Pattern.compile("^\\((.*)\\)(.+)$");
            Matcher matcher = pattern.matcher(asmSignature);
            
            if (!matcher.matches()) {
                return methodName + "(" + asmSignature + ")";
            }
            
            String parametersString = matcher.group(1);
            String returnTypeString = matcher.group(2);
            
            // Parse parameters
            String[] parameters = parseParameters(parametersString);
            String returnType = parseType(returnTypeString);
            
            // Format as Java method signature
            StringBuilder result = new StringBuilder();
            result.append(returnType).append(" ").append(methodName).append("(");
            
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) result.append(", ");
                result.append(parameters[i]);
            }
            
            result.append(")");
            return result.toString();
            
        } catch (Exception e) {
            // Fallback to original if parsing fails
            return methodName + "(" + asmSignature + ")";
        }
    }
    
    /**
     * Parse parameter types from ASM signature
     */
    private static String[] parseParameters(String parametersString) {
        if (parametersString.isEmpty()) {
            return new String[0];
        }
        
        java.util.List<String> params = new java.util.ArrayList<>();
        int i = 0;
        
        while (i < parametersString.length()) {
            String type = parseTypeAtPosition(parametersString, i);
            params.add(type);
            i += getTypeLength(parametersString, i);
        }
        
        return params.toArray(new String[0]);
    }
    
    /**
     * Parse a single type from ASM signature
     */
    private static String parseType(String typeString) {
        return parseTypeAtPosition(typeString, 0);
    }
    
    /**
     * Parse type at specific position in the signature
     */
    private static String parseTypeAtPosition(String signature, int position) {
        if (position >= signature.length()) {
            return "Object";
        }
        
        char typeChar = signature.charAt(position);
        
        // Check for array
        int arrayDimensions = 0;
        while (typeChar == '[') {
            arrayDimensions++;
            position++;
            if (position >= signature.length()) break;
            typeChar = signature.charAt(position);
        }
        
        String baseType;
        
        // Primitive types
        if (PRIMITIVE_TYPES.containsKey(String.valueOf(typeChar))) {
            baseType = PRIMITIVE_TYPES.get(String.valueOf(typeChar));
        }
        // Object types
        else if (typeChar == 'L') {
            int endIndex = signature.indexOf(';', position);
            if (endIndex == -1) endIndex = signature.length();
            
            String className = signature.substring(position + 1, endIndex);
            baseType = formatClassName(className);
        }
        // Fallback
        else {
            baseType = "Object";
        }
        
        // Add array brackets
        StringBuilder result = new StringBuilder(baseType);
        for (int i = 0; i < arrayDimensions; i++) {
            result.append("[]");
        }
        
        return result.toString();
    }
    
    /**
     * Get the length of type descriptor starting at position
     */
    private static int getTypeLength(String signature, int position) {
        if (position >= signature.length()) return 0;
        
        char typeChar = signature.charAt(position);
        int length = 0;
        
        // Count array dimensions
        while (typeChar == '[') {
            length++;
            position++;
            if (position >= signature.length()) return length;
            typeChar = signature.charAt(position);
        }
        
        // Primitive types are single character
        if (PRIMITIVE_TYPES.containsKey(String.valueOf(typeChar))) {
            return length + 1;
        }
        // Object types end with semicolon
        else if (typeChar == 'L') {
            int endIndex = signature.indexOf(';', position);
            if (endIndex == -1) endIndex = signature.length() - 1;
            return length + (endIndex - position) + 1;
        }
        
        return length + 1;
    }
    
    /**
     * Format class name from internal format to readable format
     */
    public static String formatClassName(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return "Object";
        }
        
        // Convert internal format (com/example/MyClass) to dot format (com.example.MyClass)
        String className = internalName.replace('/', '.');
        
        // Get simple name for common classes
        if (className.startsWith("java.lang.")) {
            return className.substring("java.lang.".length());
        }
        if (className.startsWith("java.util.")) {
            String simpleName = className.substring("java.util.".length());
            // Keep common util classes short
            if (simpleName.equals("List") || simpleName.equals("Map") || 
                simpleName.equals("Set") || simpleName.equals("Collection")) {
                return simpleName;
            }
        }
        
        // For other classes, show the full name but make it readable
        return className;
    }
    
    /**
     * Format a complete method signature with class name
     */
    public static String formatFullMethodSignature(String className, String methodName, String asmSignature) {
        String formattedClassName = formatClassName(className.replace('.', '/'));
        String methodSig = formatMethodSignature(methodName, asmSignature);
        
        return formattedClassName + "." + methodSig;
    }
    
    /**
     * Extract method name and signature from ASM format
     * Example: "parseBase64Binary(Ljava/lang/String;)[B" -> ["parseBase64Binary", "(Ljava/lang/String;)[B"]
     */
    public static String[] extractMethodAndSignature(String asmFormat) {
        if (asmFormat == null || asmFormat.isEmpty()) {
            return new String[]{"unknown", ""};
        }
        
        int openParen = asmFormat.indexOf('(');
        if (openParen == -1) {
            return new String[]{asmFormat, ""};
        }
        
        String methodName = asmFormat.substring(0, openParen);
        String signature = asmFormat.substring(openParen);
        
        return new String[]{methodName, signature};
    }
}
