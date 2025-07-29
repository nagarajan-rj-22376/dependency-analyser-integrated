package com.example.depanalysis.util;

import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * ASM-based utility for analyzing JAR files by examining bytecode directly.
 * Much faster and more accurate than decompilation approach.
 */
@Component
public class JarBytecodeAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(JarBytecodeAnalyzer.class);
    
    /**
     * Analyzes all JAR files in the project using ASM bytecode analysis.
     * 
     * @param projectDir the project directory to scan for JAR files
     * @return analysis results containing method calls, annotations, and dependencies
     */
    public JarAnalysisResult analyzeJarsInProject(Path projectDir) {
        logger.info("Starting ASM-based JAR analysis for project: {}", projectDir);
        
        JarAnalysisResult result = new JarAnalysisResult();
        
        try {
            List<Path> jarFiles = findJarFiles(projectDir);
            logger.info("Found {} JAR files to analyze", jarFiles.size());
            
            for (Path jarFile : jarFiles) {
                try {
                    analyzeJar(jarFile, result);
                    logger.debug("Analyzed JAR: {}", jarFile.getFileName());
                } catch (Exception e) {
                    logger.warn("Failed to analyze JAR {}: {}", jarFile, e.getMessage());
                }
            }
            
        } catch (IOException e) {
            logger.error("Error scanning for JAR files: {}", e.getMessage());
        }
        
        logger.info("JAR analysis completed. Found {} method calls, {} deprecated annotations", 
                   result.getMethodCalls().size(), result.getDeprecatedElements().size());
        return result;
    }
    
    private List<Path> findJarFiles(Path projectDir) throws IOException {
        return Files.walk(projectDir)
                .filter(path -> path.toString().endsWith(".jar"))
                .filter(path -> !path.toString().contains("target/test-classes"))
                .filter(path -> !path.getFileName().toString().startsWith("._")) // Filter out macOS metadata files
                .filter(path -> !path.getFileName().toString().startsWith("_"))  // Filter out JARs starting with underscore
                .filter(path -> !path.getFileName().toString().startsWith("."))  // Filter out other hidden files
                .collect(Collectors.toList());
    }
    
    private void analyzeJar(Path jarPath, JarAnalysisResult result) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            
            List<String> classFiles = jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .filter(entry -> !entry.getName().contains("$")) // Skip inner classes for now
                    .map(JarEntry::getName)
                    .collect(Collectors.toList());
            
            // Limit classes to analyze for performance
            int maxClasses = Math.min(classFiles.size(), 100);
            
            for (int i = 0; i < maxClasses; i++) {
                String classFile = classFiles.get(i);
                try {
                    analyzeClassFile(jarFile, classFile, jarPath.toString(), result);
                } catch (Exception e) {
                    logger.debug("Failed to analyze class {}: {}", classFile, e.getMessage());
                }
            }
            
        } catch (IOException e) {
            logger.warn("Error reading JAR file {}: {}", jarPath, e.getMessage());
        }
    }
    
    private void analyzeClassFile(JarFile jarFile, String classFileName, String jarPath, JarAnalysisResult result) {
        try {
            JarEntry entry = jarFile.getJarEntry(classFileName);
            if (entry == null) return;
            
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                ClassReader classReader = new ClassReader(inputStream);
                
                // Create visitor to analyze the class
                AnalysisClassVisitor visitor = new AnalysisClassVisitor(jarPath, classFileName, result);
                classReader.accept(visitor, ClassReader.SKIP_DEBUG);
            }
            
        } catch (IOException e) {
            logger.debug("Error reading class file {}: {}", classFileName, e.getMessage());
        }
    }
    
    /**
     * ASM ClassVisitor that analyzes bytecode for method calls and annotations.
     */
    private static class AnalysisClassVisitor extends ClassVisitor {
        private final String jarPath;
        private final String classFileName;
        private final JarAnalysisResult result;
        private String currentClassName;
        
        public AnalysisClassVisitor(String jarPath, String classFileName, JarAnalysisResult result) {
            super(Opcodes.ASM9);
            this.jarPath = jarPath;
            this.classFileName = classFileName;
            this.result = result;
        }
        
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.currentClassName = name.replace('/', '.');
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            // Check for @Deprecated annotation
            if ("Ljava/lang/Deprecated;".equals(descriptor)) {
                result.addDeprecatedElement(currentClassName, "class", jarPath + ":" + classFileName);
            }
            return super.visitAnnotation(descriptor, visible);
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new AnalysisMethodVisitor(currentClassName, name, descriptor, jarPath, classFileName, result);
        }
        
        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            return new AnalysisFieldVisitor(currentClassName, name, jarPath, classFileName, result);
        }
    }
    
    /**
     * ASM MethodVisitor that analyzes method calls within methods.
     */
    private static class AnalysisMethodVisitor extends MethodVisitor {
        private final String ownerClass;
        private final String methodName;
        private final String methodDescriptor;
        private final String jarPath;
        private final String classFileName;
        private final JarAnalysisResult result;
        
        public AnalysisMethodVisitor(String ownerClass, String methodName, String methodDescriptor, 
                                   String jarPath, String classFileName, JarAnalysisResult result) {
            super(Opcodes.ASM9);
            this.ownerClass = ownerClass;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.jarPath = jarPath;
            this.classFileName = classFileName;
            this.result = result;
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            // Check for @Deprecated annotation on methods
            if ("Ljava/lang/Deprecated;".equals(descriptor)) {
                result.addDeprecatedElement(ownerClass, methodName, jarPath + ":" + classFileName);
            }
            return super.visitAnnotation(descriptor, visible);
        }
        
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            // Record method calls
            String targetClass = owner.replace('/', '.');
            String location = jarPath + ":" + classFileName + ":" + ownerClass + "." + methodName;
            result.addMethodCall(targetClass, name, descriptor, location);
            
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
        
        @Override
        public void visitTypeInsn(int opcode, String type) {
            // Record class instantiations (NEW instructions)
            if (opcode == Opcodes.NEW) {
                String className = type.replace('/', '.');
                String location = jarPath + ":" + classFileName + ":" + ownerClass + "." + methodName;
                result.addMethodCall(className, "<init>", "()V", location);
            }
            super.visitTypeInsn(opcode, type);
        }
    }
    
    /**
     * ASM FieldVisitor that analyzes field annotations.
     */
    private static class AnalysisFieldVisitor extends FieldVisitor {
        private final String ownerClass;
        private final String fieldName;
        private final String jarPath;
        private final String classFileName;
        private final JarAnalysisResult result;
        
        public AnalysisFieldVisitor(String ownerClass, String fieldName, String jarPath, String classFileName, JarAnalysisResult result) {
            super(Opcodes.ASM9);
            this.ownerClass = ownerClass;
            this.fieldName = fieldName;
            this.jarPath = jarPath;
            this.classFileName = classFileName;
            this.result = result;
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            // Check for @Deprecated annotation on fields
            if ("Ljava/lang/Deprecated;".equals(descriptor)) {
                result.addDeprecatedElement(ownerClass, fieldName, jarPath + ":" + classFileName);
            }
            return super.visitAnnotation(descriptor, visible);
        }
    }
    
    /**
     * Result object containing analysis data from JAR files.
     */
    public static class JarAnalysisResult {
        private final List<MethodCall> methodCalls = new ArrayList<>();
        private final List<DeprecatedElement> deprecatedElements = new ArrayList<>();
        
        public void addMethodCall(String targetClass, String methodName, String descriptor, String location) {
            methodCalls.add(new MethodCall(targetClass, methodName, descriptor, location));
        }
        
        public void addDeprecatedElement(String className, String elementName, String location) {
            deprecatedElements.add(new DeprecatedElement(className, elementName, location));
        }
        
        public List<MethodCall> getMethodCalls() {
            return methodCalls;
        }
        
        public List<DeprecatedElement> getDeprecatedElements() {
            return deprecatedElements;
        }
        
        public static class MethodCall {
            private final String targetClass;
            private final String methodName;
            private final String descriptor;
            private final String location;
            
            public MethodCall(String targetClass, String methodName, String descriptor, String location) {
                this.targetClass = targetClass;
                this.methodName = methodName;
                this.descriptor = descriptor;
                this.location = location;
            }
            
            public String getTargetClass() { return targetClass; }
            public String getMethodName() { return methodName; }
            public String getDescriptor() { return descriptor; }
            public String getLocation() { return location; }
        }
        
        public static class DeprecatedElement {
            private final String className;
            private final String elementName;
            private final String location;
            
            public DeprecatedElement(String className, String elementName, String location) {
                this.className = className;
                this.elementName = elementName;
                this.location = location;
            }
            
            public String getClassName() { return className; }
            public String getElementName() { return elementName; }
            public String getLocation() { return location; }
        }
    }
}
