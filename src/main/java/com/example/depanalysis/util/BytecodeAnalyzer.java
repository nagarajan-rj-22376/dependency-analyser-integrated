package com.example.depanalysis.util;

import com.example.depanalysis.dto.DeprecationInfo;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BytecodeAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(BytecodeAnalyzer.class);

    public static class MethodCall {
        private String callerClass;
        private String callerMethod;
        private String targetClass;
        private String targetMethod;
        private String targetDescriptor;

        public MethodCall(String callerClass, String callerMethod, String targetClass, 
                         String targetMethod, String targetDescriptor) {
            this.callerClass = callerClass;
            this.callerMethod = callerMethod;
            this.targetClass = targetClass;
            this.targetMethod = targetMethod;
            this.targetDescriptor = targetDescriptor;
        }

        // Getters
        public String getCallerClass() { return callerClass; }
        public String getCallerMethod() { return callerMethod; }
        public String getTargetClass() { return targetClass; }
        public String getTargetMethod() { return targetMethod; }
        public String getTargetDescriptor() { return targetDescriptor; }
        public String getTargetSignature() { return targetMethod + targetDescriptor; }
        
        @Override
        public String toString() {
            return callerClass + "." + callerMethod + " -> " + targetClass + "." + targetMethod + targetDescriptor;
        }
    }

    public static List<MethodCall> analyzeJar(Path jarPath) {
        List<MethodCall> methodCalls = new ArrayList<>();
        
        logger.debug("Analyzing bytecode in JAR: {}", jarPath.getFileName());
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        ClassReader classReader = new ClassReader(is);
                        MethodCallVisitor visitor = new MethodCallVisitor();
                        classReader.accept(visitor, ClassReader.SKIP_DEBUG);
                        methodCalls.addAll(visitor.getMethodCalls());
                    } catch (Exception e) {
                        logger.warn("Error analyzing class {}: {}", entry.getName(), e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            logger.error("Error analyzing JAR file {}: {}", jarPath, e.getMessage(), e);
        }
        
        logger.debug("Found {} method calls in {}", methodCalls.size(), jarPath.getFileName());
        return methodCalls;
    }

    public static Set<String> getAvailableMethods(Path jarPath) {
        Set<String> methods = new HashSet<>();
        
        logger.debug("Extracting available methods from JAR: {}", jarPath.getFileName());
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        ClassReader classReader = new ClassReader(is);
                        MethodExtractorVisitor visitor = new MethodExtractorVisitor();
                        classReader.accept(visitor, ClassReader.SKIP_DEBUG);
                        methods.addAll(visitor.getMethods());
                    } catch (Exception e) {
                        logger.warn("Error extracting methods from class {}: {}", entry.getName(), e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            logger.error("Error extracting methods from JAR file {}: {}", jarPath, e.getMessage(), e);
        }
        
        logger.debug("Found {} available methods in {}", methods.size(), jarPath.getFileName());
        return methods;
    }

    private static class MethodCallVisitor extends ClassVisitor {
        private String currentClassName;
        private List<MethodCall> methodCalls = new ArrayList<>();

        public MethodCallVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.currentClassName = name.replace('/', '.');
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodCallMethodVisitor(currentClassName, name);
        }

        public List<MethodCall> getMethodCalls() {
            return methodCalls;
        }

        private class MethodCallMethodVisitor extends MethodVisitor {
            private String currentClassName;
            private String currentMethodName;

            public MethodCallMethodVisitor(String className, String methodName) {
                super(Opcodes.ASM9);
                this.currentClassName = className;
                this.currentMethodName = methodName;
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                String targetClass = owner.replace('/', '.');
                // Only track external method calls (not same class calls)
                if (!targetClass.equals(currentClassName) && !targetClass.startsWith("java.")) {
                    methodCalls.add(new MethodCall(currentClassName, currentMethodName, targetClass, name, descriptor));
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }
    }

    private static class MethodExtractorVisitor extends ClassVisitor {
        private String currentClassName;
        private Set<String> methods = new HashSet<>();

        public MethodExtractorVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.currentClassName = name.replace('/', '.');
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // Only include public and protected methods
            if ((access & Opcodes.ACC_PUBLIC) != 0 || (access & Opcodes.ACC_PROTECTED) != 0) {
                methods.add(currentClassName + "." + name + descriptor);
            }
            return null;
        }

        public Set<String> getMethods() {
            return methods;
        }
    }

    // ==== DEPRECATION DETECTION METHODS ====

    /**
     * Find all deprecated elements (methods, classes, fields) in a JAR file
     */
    public static List<DeprecationInfo> findDeprecatedElements(Path jarPath) {
        List<DeprecationInfo> deprecations = new ArrayList<>();
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        ClassReader classReader = new ClassReader(is);
                        DeprecationVisitor visitor = new DeprecationVisitor();
                        classReader.accept(visitor, 0);
                        deprecations.addAll(visitor.getDeprecations());
                    } catch (Exception e) {
                        logger.debug("Error analyzing class {} for deprecations: {}", entry.getName(), e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error reading JAR file {} for deprecation analysis: {}", jarPath, e.getMessage());
        }
        
        logger.info("Found {} deprecated elements in {}", deprecations.size(), jarPath.getFileName());
        return deprecations;
    }

    /**
     * Create a map of deprecated element name -> DeprecationInfo for fast lookup
     */
    public static Map<String, DeprecationInfo> getDeprecationMap(Path jarPath) {
        Map<String, DeprecationInfo> deprecationMap = new HashMap<>();
        List<DeprecationInfo> deprecations = findDeprecatedElements(jarPath);
        
        for (DeprecationInfo info : deprecations) {
            deprecationMap.put(info.getElementName(), info);
        }
        
        return deprecationMap;
    }

    /**
     * ASM Visitor to detect @Deprecated annotations and @deprecated Javadoc
     */
    private static class DeprecationVisitor extends ClassVisitor {
        private List<DeprecationInfo> deprecations = new ArrayList<>();
        private String currentClassName;
        private boolean classDeprecated = false;
        private DeprecationInfo classDeprecationInfo;

        public DeprecationVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.currentClassName = name.replace('/', '.');
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if ("Ljava/lang/Deprecated;".equals(descriptor)) {
                // Class-level deprecation
                classDeprecated = true;
                classDeprecationInfo = new DeprecationInfo(currentClassName, "CLASS");
                classDeprecationInfo.setSourceLocation(currentClassName);
                
                return new DeprecationAnnotationVisitor(classDeprecationInfo);
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // Only analyze public/protected methods
            if ((access & Opcodes.ACC_PUBLIC) != 0 || (access & Opcodes.ACC_PROTECTED) != 0) {
                return new DeprecationMethodVisitor(currentClassName, name, descriptor);
            }
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            // Only analyze public/protected fields
            if ((access & Opcodes.ACC_PUBLIC) != 0 || (access & Opcodes.ACC_PROTECTED) != 0) {
                return new DeprecationFieldVisitor(currentClassName, name, descriptor);
            }
            return null;
        }

        @Override
        public void visitEnd() {
            if (classDeprecated && classDeprecationInfo != null) {
                deprecations.add(classDeprecationInfo);
            }
        }

        public List<DeprecationInfo> getDeprecations() {
            return deprecations;
        }

        /**
         * Method visitor to detect deprecated methods
         */
        private class DeprecationMethodVisitor extends MethodVisitor {
            private String className;
            private String methodName;
            private String methodDescriptor;

            public DeprecationMethodVisitor(String className, String methodName, String methodDescriptor) {
                super(Opcodes.ASM9);
                this.className = className;
                this.methodName = methodName;
                this.methodDescriptor = methodDescriptor;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if ("Ljava/lang/Deprecated;".equals(descriptor)) {
                    String elementName = className + "." + methodName + methodDescriptor;
                    DeprecationInfo info = new DeprecationInfo(elementName, "METHOD");
                    info.setSourceLocation(className);
                    deprecations.add(info);
                    
                    return new DeprecationAnnotationVisitor(info);
                }
                return null;
            }
        }

        /**
         * Field visitor to detect deprecated fields
         */
        private class DeprecationFieldVisitor extends FieldVisitor {
            private String className;
            private String fieldName;
            private String fieldDescriptor;

            public DeprecationFieldVisitor(String className, String fieldName, String fieldDescriptor) {
                super(Opcodes.ASM9);
                this.className = className;
                this.fieldName = fieldName;
                this.fieldDescriptor = fieldDescriptor;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if ("Ljava/lang/Deprecated;".equals(descriptor)) {
                    String elementName = className + "." + fieldName;
                    DeprecationInfo info = new DeprecationInfo(elementName, "FIELD");
                    info.setSourceLocation(className);
                    deprecations.add(info);
                    
                    return new DeprecationAnnotationVisitor(info);
                }
                return null;
            }
        }
    }

    /**
     * Annotation visitor to extract @Deprecated annotation parameters
     */
    private static class DeprecationAnnotationVisitor extends AnnotationVisitor {
        private DeprecationInfo deprecationInfo;

        public DeprecationAnnotationVisitor(DeprecationInfo deprecationInfo) {
            super(Opcodes.ASM9);
            this.deprecationInfo = deprecationInfo;
        }

        @Override
        public void visit(String name, Object value) {
            if ("since".equals(name)) {
                deprecationInfo.setSince((String) value);
            }
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            if ("forRemoval".equals(name)) {
                deprecationInfo.setForRemoval(Boolean.parseBoolean(value));
            }
        }
    }
}
