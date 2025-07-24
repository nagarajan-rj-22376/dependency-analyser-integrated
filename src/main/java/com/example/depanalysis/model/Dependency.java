package com.example.depanalysis.model;

public class Dependency {
    private String ecosystem;
    private String name;
    private String version;

    public Dependency(String ecosystem, String name, String version) {
        this.ecosystem = ecosystem;
        this.name = name;
        this.version = version;
    }

    public String getEcosystem() { return ecosystem; }
    public String getName() { return name; }
    public String getVersion() { return version; }
}