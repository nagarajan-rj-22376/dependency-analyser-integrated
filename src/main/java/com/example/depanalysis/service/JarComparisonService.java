package com.example.depanalysis.service;

import com.example.depanalysis.dto.JarComparisonResult;
import com.example.depanalysis.util.JarBytecodeAnalyzer;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@Service
public class JarComparisonService {

    private static final Logger logger = LoggerFactory.getLogger(JarComparisonService.class);
    private final JarBytecodeAnalyzer jarAnalyzer;

    public JarComparisonService(JarBytecodeAnalyzer jarAnalyzer) {
        this.jarAnalyzer = jarAnalyzer;
    }

    public JarComparisonResult compareJars(Path oldJarPath, Path newJarPath, String oldJarName, String newJarName) throws IOException {
        logger.info("Starting JAR comparison between {} and {}", oldJarName, newJarName);

        // Check if files are identical
        if (areFilesIdentical(oldJarPath, newJarPath)) {
            logger.info("Detected identical JAR files - creating identity comparison result");
            return createIdenticalFilesResult(oldJarName, newJarName);
        }

        JarComparisonResult result = new JarComparisonResult(oldJarName, newJarName);

        // Analyze both JARs
        JarAnalysisData oldJarData = analyzeJar(oldJarPath);
        JarAnalysisData newJarData = analyzeJar(newJarPath);

        logger.info("Old JAR analysis: {} classes, {} packages", oldJarData.classes.size(), oldJarData.packages.size());
        logger.info("New JAR analysis: {} classes, {} packages", newJarData.classes.size(), newJarData.packages.size());

        // Perform comparison at different levels
        result.setSummary(createComparisonSummary(oldJarData, newJarData));
        result.setProjectLevel(createProjectLevelChanges(oldJarData, newJarData));
        result.setPackageLevel(createPackageLevelChanges(oldJarData, newJarData));
        result.setClassLevel(createClassLevelChanges(oldJarData, newJarData));
        result.setMethodLevel(createMethodLevelChanges(oldJarData, newJarData));

        logger.info("JAR comparison completed successfully");
        logger.info("Results: {} package changes, {} class changes", 
                   result.getPackageLevel().size(), result.getClassLevel().size());
        return result;
    }

    private JarAnalysisData analyzeJar(Path jarPath) throws IOException {
        JarAnalysisData data = new JarAnalysisData();
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // Extract manifest
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                data.manifestAttributes = new HashMap<>();
                manifest.getMainAttributes().forEach((key, value) -> 
                    data.manifestAttributes.put(key.toString(), value.toString()));
            }

            // Analyze classes and packages
            jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().endsWith(".class"))
                .filter(entry -> !entry.getName().contains("$")) // Skip inner classes for now
                .forEach(entry -> {
                    String className = entry.getName().replace('/', '.').replace(".class", "");
                    String packageName = getPackageName(className);
                    
                    data.classes.add(className);
                    data.packages.add(packageName);
                    
                    // Analyze class structure using bytecode analysis
                    try {
                        analyzeClassStructure(jarFile, entry, data);
                    } catch (Exception e) {
                        logger.warn("Failed to analyze class {}: {}", className, e.getMessage());
                    }
                });
        }

        return data;
    }

    private void analyzeClassStructure(JarFile jarFile, JarEntry entry, JarAnalysisData data) throws IOException {
        String className = entry.getName().replace('/', '.').replace(".class", "");
        
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            ClassStructure classStructure = new ClassStructure();
            classStructure.accessModifier = "public"; // Default
            
            // Use ASM to analyze the class bytecode
            try {
                ClassReader classReader = new ClassReader(inputStream);
                ClassAnalysisVisitor visitor = new ClassAnalysisVisitor(classStructure);
                classReader.accept(visitor, 0);
            } catch (Exception e) {
                logger.warn("Failed to analyze class structure for {}: {}", className, e.getMessage());
                // Fallback to basic structure
            }
            
            data.classStructures.put(className, classStructure);
        }
    }
    
    // ASM visitor to extract class details
    private static class ClassAnalysisVisitor extends ClassVisitor {
        private final ClassStructure classStructure;
        
        public ClassAnalysisVisitor(ClassStructure classStructure) {
            super(Opcodes.ASM9);
            this.classStructure = classStructure;
        }
        
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            // Set access modifier
            if ((access & Opcodes.ACC_PUBLIC) != 0) {
                classStructure.accessModifier = "public";
            } else if ((access & Opcodes.ACC_PROTECTED) != 0) {
                classStructure.accessModifier = "protected";
            } else if ((access & Opcodes.ACC_PRIVATE) != 0) {
                classStructure.accessModifier = "private";
            } else {
                classStructure.accessModifier = "package-private";
            }
            
            // Set interfaces
            if (interfaces != null) {
                for (String iface : interfaces) {
                    classStructure.interfaces.add(iface.replace('/', '.'));
                }
            }
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // Create method signature
            String accessModifier = "";
            if ((access & Opcodes.ACC_PUBLIC) != 0) accessModifier = "public";
            else if ((access & Opcodes.ACC_PROTECTED) != 0) accessModifier = "protected";
            else if ((access & Opcodes.ACC_PRIVATE) != 0) accessModifier = "private";
            else accessModifier = "package-private";
            
            String methodSignature = String.format("%s %s%s", accessModifier, name, descriptor);
            classStructure.methods.add(methodSignature);
            
            // Create bytecode analyzer for this method
            MethodBytecode methodBytecode = new MethodBytecode(methodSignature);
            classStructure.methodBytecodes.put(methodSignature, methodBytecode);
            
            // Return custom method visitor to capture bytecode instructions
            return new BytecodeCapturingMethodVisitor(methodBytecode);
        }
    }
    
    // Custom MethodVisitor to capture bytecode instructions
    private static class BytecodeCapturingMethodVisitor extends MethodVisitor {
        private final MethodBytecode methodBytecode;
        private int instructionCount = 0;
        
        public BytecodeCapturingMethodVisitor(MethodBytecode methodBytecode) {
            super(Opcodes.ASM9);
            this.methodBytecode = methodBytecode;
        }
        
        @Override
        public void visitInsn(int opcode) {
            String instruction = String.format("  %03d: %s", instructionCount++, getOpcodeName(opcode));
            methodBytecode.instructions.add(instruction);
        }
        
        @Override
        public void visitIntInsn(int opcode, int operand) {
            String instruction = String.format("  %03d: %s %d", instructionCount++, getOpcodeName(opcode), operand);
            methodBytecode.instructions.add(instruction);
        }
        
        @Override
        public void visitVarInsn(int opcode, int var) {
            String instruction = String.format("  %03d: %s %d", instructionCount++, getOpcodeName(opcode), var);
            methodBytecode.instructions.add(instruction);
        }
        
        @Override
        public void visitTypeInsn(int opcode, String type) {
            String instruction = String.format("  %03d: %s %s", instructionCount++, getOpcodeName(opcode), type);
            methodBytecode.instructions.add(instruction);
        }
        
        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            String instruction = String.format("  %03d: %s %s.%s:%s", instructionCount++, getOpcodeName(opcode), owner, name, descriptor);
            methodBytecode.instructions.add(instruction);
        }
        
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            String instruction = String.format("  %03d: %s %s.%s%s", instructionCount++, getOpcodeName(opcode), owner, name, descriptor);
            methodBytecode.instructions.add(instruction);
        }
        
        @Override
        public void visitLdcInsn(Object value) {
            String instruction = String.format("  %03d: LDC %s", instructionCount++, value);
            methodBytecode.instructions.add(instruction);
        }
        
        @Override
        public void visitEnd() {
            // Generate human-readable ASM code
            StringBuilder asmCode = new StringBuilder();
            asmCode.append("// Method: ").append(methodBytecode.methodSignature).append("\n");
            for (String inst : methodBytecode.instructions) {
                asmCode.append(inst).append("\n");
            }
            methodBytecode.asmCode = asmCode.toString();
        }
        
        private String getOpcodeName(int opcode) {
            switch (opcode) {
                case Opcodes.ALOAD: return "ALOAD";
                case Opcodes.ASTORE: return "ASTORE";
                case Opcodes.ILOAD: return "ILOAD";
                case Opcodes.ISTORE: return "ISTORE";
                case Opcodes.INVOKESPECIAL: return "INVOKESPECIAL";
                case Opcodes.INVOKEVIRTUAL: return "INVOKEVIRTUAL";
                case Opcodes.INVOKESTATIC: return "INVOKESTATIC";
                case Opcodes.INVOKEINTERFACE: return "INVOKEINTERFACE";
                case Opcodes.RETURN: return "RETURN";
                case Opcodes.IRETURN: return "IRETURN";
                case Opcodes.ARETURN: return "ARETURN";
                case Opcodes.NEW: return "NEW";
                case Opcodes.DUP: return "DUP";
                case Opcodes.GETFIELD: return "GETFIELD";
                case Opcodes.PUTFIELD: return "PUTFIELD";
                case Opcodes.GETSTATIC: return "GETSTATIC";
                case Opcodes.PUTSTATIC: return "PUTSTATIC";
                case Opcodes.CHECKCAST: return "CHECKCAST";
                case Opcodes.INSTANCEOF: return "INSTANCEOF";
                case Opcodes.IADD: return "IADD";
                case Opcodes.ISUB: return "ISUB";
                case Opcodes.IMUL: return "IMUL";
                case Opcodes.IDIV: return "IDIV";
                case Opcodes.IREM: return "IREM";
                case Opcodes.ICONST_0: return "ICONST_0";
                case Opcodes.ICONST_1: return "ICONST_1";
                case Opcodes.ICONST_2: return "ICONST_2";
                case Opcodes.ICONST_3: return "ICONST_3";
                case Opcodes.ICONST_4: return "ICONST_4";
                case Opcodes.ICONST_5: return "ICONST_5";
                case Opcodes.ICONST_M1: return "ICONST_M1";
                case Opcodes.BIPUSH: return "BIPUSH";
                case Opcodes.SIPUSH: return "SIPUSH";
                default: return "OPCODE_" + opcode;
            }
        }
    }

    private String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "(default)";
    }

    private JarComparisonResult.ComparisonSummary createComparisonSummary(JarAnalysisData oldJar, JarAnalysisData newJar) {
        JarComparisonResult.ComparisonSummary summary = new JarComparisonResult.ComparisonSummary();
        
        // Package analysis
        Set<String> addedPackages = new HashSet<>(newJar.packages);
        addedPackages.removeAll(oldJar.packages);
        
        Set<String> removedPackages = new HashSet<>(oldJar.packages);
        removedPackages.removeAll(newJar.packages);
        
        Set<String> commonPackages = new HashSet<>(oldJar.packages);
        commonPackages.retainAll(newJar.packages);
        
        // Class analysis
        Set<String> addedClasses = new HashSet<>(newJar.classes);
        addedClasses.removeAll(oldJar.classes);
        
        Set<String> removedClasses = new HashSet<>(oldJar.classes);
        removedClasses.removeAll(newJar.classes);
        
        Set<String> commonClasses = new HashSet<>(oldJar.classes);
        commonClasses.retainAll(newJar.classes);
        
        // Count modified packages (packages that exist in both but have different classes)
        int packagesModified = 0;
        for (String packageName : commonPackages) {
            Set<String> oldClassesInPackage = oldJar.classes.stream()
                .filter(className -> getPackageName(className).equals(packageName))
                .collect(Collectors.toSet());
            Set<String> newClassesInPackage = newJar.classes.stream()
                .filter(className -> getPackageName(className).equals(packageName))
                .collect(Collectors.toSet());
            
            if (!oldClassesInPackage.equals(newClassesInPackage)) {
                packagesModified++;
            }
        }

        // Set package summary
        summary.setTotalPackages(newJar.packages.size());
        summary.setPackagesAdded(addedPackages.size());
        summary.setPackagesRemoved(removedPackages.size());
        summary.setPackagesModified(packagesModified);
        
        // Set class summary 
        summary.setTotalClasses(newJar.classes.size());
        summary.setClassesAdded(addedClasses.size());
        summary.setClassesRemoved(removedClasses.size());
        summary.setClassesModified(0); // Would require detailed bytecode analysis
        
        // Set method summary (placeholder values - would require detailed analysis)
        summary.setTotalMethods(0);
        summary.setMethodsAdded(0);
        summary.setMethodsRemoved(0);
        summary.setMethodsModified(0);

        return summary;
    }

    private JarComparisonResult.ProjectLevelChanges createProjectLevelChanges(JarAnalysisData oldJar, JarAnalysisData newJar) {
        JarComparisonResult.ProjectLevelChanges changes = new JarComparisonResult.ProjectLevelChanges();
        
        // Compare manifest attributes
        Map<String, String> manifestChanges = new HashMap<>();
        
        if (oldJar.manifestAttributes != null && newJar.manifestAttributes != null) {
            // Find changed attributes
            for (Map.Entry<String, String> entry : newJar.manifestAttributes.entrySet()) {
                String key = entry.getKey();
                String newValue = entry.getValue();
                String oldValue = oldJar.manifestAttributes.get(key);
                
                if (oldValue == null) {
                    manifestChanges.put(key, "ADDED: " + newValue);
                } else if (!oldValue.equals(newValue)) {
                    manifestChanges.put(key, "CHANGED: " + oldValue + " -> " + newValue);
                }
            }
            
            // Find removed attributes
            for (String key : oldJar.manifestAttributes.keySet()) {
                if (!newJar.manifestAttributes.containsKey(key)) {
                    manifestChanges.put(key, "REMOVED: " + oldJar.manifestAttributes.get(key));
                }
            }
        }
        
        changes.setMetadataChanges(manifestChanges);
        changes.setManifestChanges(!manifestChanges.isEmpty() ? "Manifest modified" : "No manifest changes");
        changes.setDependencyChanges(new ArrayList<>()); // Would require more analysis

        return changes;
    }

    private List<JarComparisonResult.PackageLevelChanges> createPackageLevelChanges(JarAnalysisData oldJar, JarAnalysisData newJar) {
        List<JarComparisonResult.PackageLevelChanges> changes = new ArrayList<>();
        
        Set<String> allPackages = new HashSet<>();
        allPackages.addAll(oldJar.packages);
        allPackages.addAll(newJar.packages);
        
        for (String packageName : allPackages) {
            boolean inOld = oldJar.packages.contains(packageName);
            boolean inNew = newJar.packages.contains(packageName);
            
            JarComparisonResult.PackageLevelChanges packageChange = new JarComparisonResult.PackageLevelChanges();
            packageChange.setPackageName(packageName);
            
            if (!inOld && inNew) {
                packageChange.setChangeType("ADDED");
            } else if (inOld && !inNew) {
                packageChange.setChangeType("REMOVED");
            } else if (inOld && inNew) {
                // Package exists in both - check if classes in package changed
                Set<String> oldClassesInPackage = oldJar.classes.stream()
                    .filter(className -> getPackageName(className).equals(packageName))
                    .collect(Collectors.toSet());
                Set<String> newClassesInPackage = newJar.classes.stream()
                    .filter(className -> getPackageName(className).equals(packageName))
                    .collect(Collectors.toSet());
                
                if (!oldClassesInPackage.equals(newClassesInPackage)) {
                    packageChange.setChangeType("MODIFIED");
                } else {
                    packageChange.setChangeType("UNCHANGED");
                }
            }
            
            // Count classes in this package
            long classCount = newJar.classes.stream()
                .filter(className -> getPackageName(className).equals(packageName))
                .count();
            packageChange.setClassCount((int) classCount);
            
            // Only add if there's actually a change
            if (!"UNCHANGED".equals(packageChange.getChangeType())) {
                changes.add(packageChange);
            }
        }
        
        return changes;
    }

    private List<JarComparisonResult.ClassLevelChanges> createClassLevelChanges(JarAnalysisData oldJar, JarAnalysisData newJar) {
        List<JarComparisonResult.ClassLevelChanges> changes = new ArrayList<>();
        
        Set<String> allClasses = new HashSet<>();
        allClasses.addAll(oldJar.classes);
        allClasses.addAll(newJar.classes);
        
        for (String className : allClasses) {
            boolean inOld = oldJar.classes.contains(className);
            boolean inNew = newJar.classes.contains(className);
            
            JarComparisonResult.ClassLevelChanges classChange = new JarComparisonResult.ClassLevelChanges();
            classChange.setClassName(className);
            classChange.setPackageName(getPackageName(className));
            
            if (!inOld && inNew) {
                classChange.setChangeType("ADDED");
                populateAddedClassDetails(classChange, newJar, className);
            } else if (inOld && !inNew) {
                classChange.setChangeType("REMOVED");
                populateRemovedClassDetails(classChange, oldJar, className);
            } else if (inOld && inNew) {
                // Class exists in both - compare detailed structure
                if (compareClassStructures(oldJar, newJar, className, classChange)) {
                    classChange.setChangeType("MODIFIED");
                } else {
                    classChange.setChangeType("UNCHANGED");
                }
            }
            
            // Only add classes that have changes
            if (!"UNCHANGED".equals(classChange.getChangeType())) {
                changes.add(classChange);
            }
            
            // Only add if there's actually a change
            if (!"UNCHANGED".equals(classChange.getChangeType())) {
                changes.add(classChange);
            }
        }
        
        return changes;
    }

    private List<JarComparisonResult.MethodLevelChanges> createMethodLevelChanges(JarAnalysisData oldJar, JarAnalysisData newJar) {
        List<JarComparisonResult.MethodLevelChanges> changes = new ArrayList<>();
        
        // For now, return empty list as method-level analysis requires more detailed ASM work
        // In a full implementation, you would compare method signatures, access modifiers, etc.
        
        return changes;
    }

    private boolean areFilesIdentical(Path file1, Path file2) throws IOException {
        // First check file sizes
        if (Files.size(file1) != Files.size(file2)) {
            return false;
        }
        
        // Check if files are exactly the same (byte comparison)
        byte[] file1Bytes = Files.readAllBytes(file1);
        byte[] file2Bytes = Files.readAllBytes(file2);
        
        return java.util.Arrays.equals(file1Bytes, file2Bytes);
    }
    
    private JarComparisonResult createIdenticalFilesResult(String oldJarName, String newJarName) {
        JarComparisonResult result = new JarComparisonResult(oldJarName, newJarName);
        
        // Create a summary indicating no changes
        JarComparisonResult.ComparisonSummary summary = new JarComparisonResult.ComparisonSummary();
        summary.setTotalPackages(0);
        summary.setPackagesAdded(0);
        summary.setPackagesRemoved(0);
        summary.setPackagesModified(0);
        summary.setTotalClasses(0);
        summary.setClassesAdded(0);
        summary.setClassesRemoved(0);
        summary.setClassesModified(0);
        summary.setTotalMethods(0);
        summary.setMethodsAdded(0);
        summary.setMethodsRemoved(0);
        summary.setMethodsModified(0);
        
        result.setSummary(summary);
        result.setProjectLevel(new JarComparisonResult.ProjectLevelChanges());
        result.setPackageLevel(new ArrayList<>());
        result.setClassLevel(new ArrayList<>());
        result.setMethodLevel(new ArrayList<>());
        
        logger.info("Files are identical - no differences found");
        
        return result;
    }

    private void populateAddedClassDetails(JarComparisonResult.ClassLevelChanges classChange, JarAnalysisData jarData, String className) {
        ClassStructure classStructure = jarData.classStructures.get(className);
        classChange.setAddedMethods(new ArrayList<>());
        classChange.setRemovedMethods(new ArrayList<>());
        classChange.setModifiedMethods(new ArrayList<>());
        classChange.setInterfaceChanges(new ArrayList<>());
        
        if (classStructure != null) {
            if (classStructure.methods != null) {
                classChange.setAddedMethods(new ArrayList<>(classStructure.methods));
            }
            
            // Set access modifier
            if (classStructure.accessModifier != null) {
                classChange.setAccessModifier(classStructure.accessModifier);
            }
            
            // Set interfaces
            if (classStructure.interfaces != null && !classStructure.interfaces.isEmpty()) {
                classChange.setInterfaceChanges(new ArrayList<>(classStructure.interfaces));
            }
            
            // Set method bytecodes
            if (classStructure.methodBytecodes != null && !classStructure.methodBytecodes.isEmpty()) {
                Map<String, String> bytecodes = new HashMap<>();
                for (Map.Entry<String, MethodBytecode> entry : classStructure.methodBytecodes.entrySet()) {
                    if (entry.getValue().asmCode != null) {
                        bytecodes.put(entry.getKey(), entry.getValue().asmCode);
                    }
                }
                classChange.setMethodBytecodes(bytecodes);
            }
        }
        
        // Log for debugging
        logger.debug("Added class {}: {} methods, access: {}, bytecodes: {}", className, 
            classChange.getAddedMethods() != null ? classChange.getAddedMethods().size() : 0,
            classChange.getAccessModifier(),
            classChange.getMethodBytecodes() != null ? classChange.getMethodBytecodes().size() : 0);
    }
    
    private void populateRemovedClassDetails(JarComparisonResult.ClassLevelChanges classChange, JarAnalysisData jarData, String className) {
        ClassStructure classStructure = jarData.classStructures.get(className);
        classChange.setAddedMethods(new ArrayList<>());
        classChange.setRemovedMethods(new ArrayList<>());
        classChange.setModifiedMethods(new ArrayList<>());
        classChange.setInterfaceChanges(new ArrayList<>());
        
        if (classStructure != null) {
            if (classStructure.methods != null) {
                classChange.setRemovedMethods(new ArrayList<>(classStructure.methods));
            }
            
            // Set access modifier
            if (classStructure.accessModifier != null) {
                classChange.setAccessModifier(classStructure.accessModifier);
            }
            
            // Set interfaces
            if (classStructure.interfaces != null && !classStructure.interfaces.isEmpty()) {
                classChange.setInterfaceChanges(new ArrayList<>(classStructure.interfaces));
            }
            
            // Set method bytecodes
            if (classStructure.methodBytecodes != null && !classStructure.methodBytecodes.isEmpty()) {
                Map<String, String> bytecodes = new HashMap<>();
                for (Map.Entry<String, MethodBytecode> entry : classStructure.methodBytecodes.entrySet()) {
                    if (entry.getValue().asmCode != null) {
                        bytecodes.put(entry.getKey(), entry.getValue().asmCode);
                    }
                }
                classChange.setMethodBytecodes(bytecodes);
            }
        }
        
        // Log for debugging
        logger.debug("Removed class {}: {} methods, access: {}, bytecodes: {}", className, 
            classChange.getRemovedMethods() != null ? classChange.getRemovedMethods().size() : 0,
            classChange.getAccessModifier(),
            classChange.getMethodBytecodes() != null ? classChange.getMethodBytecodes().size() : 0);
    }
    
    private boolean compareClassStructures(JarAnalysisData oldJar, JarAnalysisData newJar, String className, JarComparisonResult.ClassLevelChanges classChange) {
        ClassStructure oldStructure = oldJar.classStructures.get(className);
        ClassStructure newStructure = newJar.classStructures.get(className);
        
        // Initialize lists
        classChange.setAddedMethods(new ArrayList<>());
        classChange.setRemovedMethods(new ArrayList<>());
        classChange.setModifiedMethods(new ArrayList<>());
        classChange.setInterfaceChanges(new ArrayList<>());
        
        boolean hasChanges = false;
        
        if (oldStructure != null && newStructure != null) {
            // Compare methods
            List<String> oldMethods = oldStructure.methods != null ? oldStructure.methods : new ArrayList<>();
            List<String> newMethods = newStructure.methods != null ? newStructure.methods : new ArrayList<>();
            
            // Find added methods
            for (String method : newMethods) {
                if (!oldMethods.contains(method)) {
                    classChange.getAddedMethods().add(method);
                    hasChanges = true;
                }
            }
            
            // Find removed methods
            for (String method : oldMethods) {
                if (!newMethods.contains(method)) {
                    classChange.getRemovedMethods().add(method);
                    hasChanges = true;
                }
            }
            
            // Compare access modifiers
            if (oldStructure.accessModifier != null && newStructure.accessModifier != null) {
                if (!oldStructure.accessModifier.equals(newStructure.accessModifier)) {
                    classChange.setAccessModifierChange(String.format("%s -> %s", 
                        oldStructure.accessModifier, newStructure.accessModifier));
                    hasChanges = true;
                }
            }
            
            // Compare interfaces
            List<String> oldInterfaces = oldStructure.interfaces != null ? oldStructure.interfaces : new ArrayList<>();
            List<String> newInterfaces = newStructure.interfaces != null ? newStructure.interfaces : new ArrayList<>();
            
            if (!oldInterfaces.equals(newInterfaces)) {
                List<String> interfaceChanges = new ArrayList<>();
                for (String iface : newInterfaces) {
                    if (!oldInterfaces.contains(iface)) {
                        interfaceChanges.add("+ " + iface);
                    }
                }
                for (String iface : oldInterfaces) {
                    if (!newInterfaces.contains(iface)) {
                        interfaceChanges.add("- " + iface);
                    }
                }
                classChange.setInterfaceChanges(interfaceChanges);
                hasChanges = true;
            }
        }
        
        logger.debug("Class {} comparison: {} added methods, {} removed methods, hasChanges={}", 
            className, classChange.getAddedMethods().size(), classChange.getRemovedMethods().size(), hasChanges);
        
        return hasChanges;
    }

    // Helper classes for internal data storage
    private static class JarAnalysisData {
        Set<String> classes = new HashSet<>();
        Set<String> packages = new HashSet<>();
        Map<String, String> manifestAttributes = new HashMap<>();
        Map<String, ClassStructure> classStructures = new HashMap<>();
    }

    private static class ClassStructure {
        List<String> methods;
        String accessModifier;
        List<String> interfaces;
        Map<String, MethodBytecode> methodBytecodes; // Store bytecode for each method
        
        public ClassStructure() {
            this.methods = new ArrayList<>();
            this.interfaces = new ArrayList<>();
            this.methodBytecodes = new HashMap<>();
        }
    }
    
    private static class MethodBytecode {
        String methodSignature;
        List<String> instructions; // ASM bytecode instructions
        String asmCode; // Human-readable ASM representation
        
        public MethodBytecode(String methodSignature) {
            this.methodSignature = methodSignature;
            this.instructions = new ArrayList<>();
        }
    }
}
