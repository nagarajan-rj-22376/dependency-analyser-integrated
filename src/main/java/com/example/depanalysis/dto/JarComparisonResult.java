package com.example.depanalysis.dto;

import java.util.List;
import java.util.Map;

public class JarComparisonResult {
    private String oldJarName;
    private String newJarName;
    private ComparisonSummary summary;
    private ProjectLevelChanges projectLevel;
    private List<PackageLevelChanges> packageLevel;
    private List<ClassLevelChanges> classLevel;
    private List<MethodLevelChanges> methodLevel;

    public JarComparisonResult() {}

    public JarComparisonResult(String oldJarName, String newJarName) {
        this.oldJarName = oldJarName;
        this.newJarName = newJarName;
    }

    // Getters and setters
    public String getOldJarName() { return oldJarName; }
    public void setOldJarName(String oldJarName) { this.oldJarName = oldJarName; }

    public String getNewJarName() { return newJarName; }
    public void setNewJarName(String newJarName) { this.newJarName = newJarName; }

    public ComparisonSummary getSummary() { return summary; }
    public void setSummary(ComparisonSummary summary) { this.summary = summary; }

    public ProjectLevelChanges getProjectLevel() { return projectLevel; }
    public void setProjectLevel(ProjectLevelChanges projectLevel) { this.projectLevel = projectLevel; }

    public List<PackageLevelChanges> getPackageLevel() { return packageLevel; }
    public void setPackageLevel(List<PackageLevelChanges> packageLevel) { this.packageLevel = packageLevel; }

    public List<ClassLevelChanges> getClassLevel() { return classLevel; }
    public void setClassLevel(List<ClassLevelChanges> classLevel) { this.classLevel = classLevel; }

    public List<MethodLevelChanges> getMethodLevel() { return methodLevel; }
    public void setMethodLevel(List<MethodLevelChanges> methodLevel) { this.methodLevel = methodLevel; }

    public static class ComparisonSummary {
        private int totalPackages;
        private int packagesAdded;
        private int packagesRemoved;
        private int packagesModified;
        private int totalClasses;
        private int classesAdded;
        private int classesRemoved;
        private int classesModified;
        private int totalMethods;
        private int methodsAdded;
        private int methodsRemoved;
        private int methodsModified;

        // Getters and setters
        public int getTotalPackages() { return totalPackages; }
        public void setTotalPackages(int totalPackages) { this.totalPackages = totalPackages; }

        public int getPackagesAdded() { return packagesAdded; }
        public void setPackagesAdded(int packagesAdded) { this.packagesAdded = packagesAdded; }

        public int getPackagesRemoved() { return packagesRemoved; }
        public void setPackagesRemoved(int packagesRemoved) { this.packagesRemoved = packagesRemoved; }

        public int getPackagesModified() { return packagesModified; }
        public void setPackagesModified(int packagesModified) { this.packagesModified = packagesModified; }

        public int getTotalClasses() { return totalClasses; }
        public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

        public int getClassesAdded() { return classesAdded; }
        public void setClassesAdded(int classesAdded) { this.classesAdded = classesAdded; }

        public int getClassesRemoved() { return classesRemoved; }
        public void setClassesRemoved(int classesRemoved) { this.classesRemoved = classesRemoved; }

        public int getClassesModified() { return classesModified; }
        public void setClassesModified(int classesModified) { this.classesModified = classesModified; }

        public int getTotalMethods() { return totalMethods; }
        public void setTotalMethods(int totalMethods) { this.totalMethods = totalMethods; }

        public int getMethodsAdded() { return methodsAdded; }
        public void setMethodsAdded(int methodsAdded) { this.methodsAdded = methodsAdded; }

        public int getMethodsRemoved() { return methodsRemoved; }
        public void setMethodsRemoved(int methodsRemoved) { this.methodsRemoved = methodsRemoved; }

        public int getMethodsModified() { return methodsModified; }
        public void setMethodsModified(int methodsModified) { this.methodsModified = methodsModified; }
    }

    public static class ProjectLevelChanges {
        private String manifestChanges;
        private Map<String, String> metadataChanges;
        private List<String> dependencyChanges;

        // Getters and setters
        public String getManifestChanges() { return manifestChanges; }
        public void setManifestChanges(String manifestChanges) { this.manifestChanges = manifestChanges; }

        public Map<String, String> getMetadataChanges() { return metadataChanges; }
        public void setMetadataChanges(Map<String, String> metadataChanges) { this.metadataChanges = metadataChanges; }

        public List<String> getDependencyChanges() { return dependencyChanges; }
        public void setDependencyChanges(List<String> dependencyChanges) { this.dependencyChanges = dependencyChanges; }
    }

    public static class PackageLevelChanges {
        private String packageName;
        private String changeType; // ADDED, REMOVED, MODIFIED
        private int classCount;
        private List<String> addedClasses;
        private List<String> removedClasses;

        // Getters and setters
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }

        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }

        public int getClassCount() { return classCount; }
        public void setClassCount(int classCount) { this.classCount = classCount; }

        public List<String> getAddedClasses() { return addedClasses; }
        public void setAddedClasses(List<String> addedClasses) { this.addedClasses = addedClasses; }

        public List<String> getRemovedClasses() { return removedClasses; }
        public void setRemovedClasses(List<String> removedClasses) { this.removedClasses = removedClasses; }
    }

    public static class ClassLevelChanges {
        private String className;
        private String changeType; // ADDED, REMOVED, MODIFIED
        private String packageName;
        private String accessModifier; // For single class access modifier (for added/removed classes)
        private List<String> addedMethods;
        private List<String> removedMethods;
        private List<String> modifiedMethods;
        private String accessModifierChange; // For access modifier changes (for modified classes)
        private List<String> interfaceChanges;
        private Map<String, String> methodBytecodes; // Method signature -> ASM bytecode

        // Getters and setters
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }

        public String getAccessModifier() { return accessModifier; }
        public void setAccessModifier(String accessModifier) { this.accessModifier = accessModifier; }

        public List<String> getAddedMethods() { return addedMethods; }
        public void setAddedMethods(List<String> addedMethods) { this.addedMethods = addedMethods; }

        public List<String> getRemovedMethods() { return removedMethods; }
        public void setRemovedMethods(List<String> removedMethods) { this.removedMethods = removedMethods; }

        public List<String> getModifiedMethods() { return modifiedMethods; }
        public void setModifiedMethods(List<String> modifiedMethods) { this.modifiedMethods = modifiedMethods; }

        public String getAccessModifierChange() { return accessModifierChange; }
        public void setAccessModifierChange(String accessModifierChange) { this.accessModifierChange = accessModifierChange; }

        public Map<String, String> getMethodBytecodes() { return methodBytecodes; }
        public void setMethodBytecodes(Map<String, String> methodBytecodes) { this.methodBytecodes = methodBytecodes; }

        public List<String> getInterfaceChanges() { return interfaceChanges; }
        public void setInterfaceChanges(List<String> interfaceChanges) { this.interfaceChanges = interfaceChanges; }
    }

    public static class MethodLevelChanges {
        private String className;
        private String methodName;
        private String changeType; // ADDED, REMOVED, MODIFIED
        private String oldSignature;
        private String newSignature;
        private String accessModifierChange;
        private String returnTypeChange;
        private List<String> parameterChanges;

        // Getters and setters
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }

        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }

        public String getOldSignature() { return oldSignature; }
        public void setOldSignature(String oldSignature) { this.oldSignature = oldSignature; }

        public String getNewSignature() { return newSignature; }
        public void setNewSignature(String newSignature) { this.newSignature = newSignature; }

        public String getAccessModifierChange() { return accessModifierChange; }
        public void setAccessModifierChange(String accessModifierChange) { this.accessModifierChange = accessModifierChange; }

        public String getReturnTypeChange() { return returnTypeChange; }
        public void setReturnTypeChange(String returnTypeChange) { this.returnTypeChange = returnTypeChange; }

        public List<String> getParameterChanges() { return parameterChanges; }
        public void setParameterChanges(List<String> parameterChanges) { this.parameterChanges = parameterChanges; }
    }
}
