package com.invasion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

public class Config {
    private Properties properties = new Properties();
    private Set<String> keys = Set.of();

    public void loadConfig(File configFile) {
        InvasionMod.log("Loading config");
        properties.clear();
        try {
            if (!configFile.exists()) {
                InvasionMod.LOGGER.debug("Config not found. Creating file '" + configFile.getName() + "' in minecraft directory");
                if (!configFile.createNewFile()) {
                    InvasionMod.LOGGER.debug("Unable to create new config file.");
                }
            } else {
                try (FileReader configRead = new FileReader(configFile)) {
                    properties.load(configRead);
                }
            }
        } catch (IOException e) {
            InvasionMod.LOGGER.error("Proceeding with default config", e);
        } finally {
            keys = properties.keySet().stream().map(i -> i.toString()).collect(Collectors.toSet());
        }
    }

    public void writeProperty(BufferedWriter writer, String key) throws IOException {
        writeProperty(writer, key, null);
    }

    public void writeProperty(BufferedWriter writer, String key, String comment) throws IOException {
        writeValue(writer, key, comment, properties.getProperty(key));
    }

    public void writeValue(BufferedWriter writer, String key, Object value) throws IOException {
        writeValue(writer, key, null, value);
    }

    public void writeValue(BufferedWriter writer, String key, String comment, Object value) throws IOException {
        if (comment != null) {
            writeLine(writer, "# " + comment);
        }
        writeLine(writer, key + "=" + value);
    }

    protected void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public Set<String> keySet() {
        return keys;
    }

    public String getProperty(String key, String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }

    public int getPropertyValueInt(String keyName, int defaultValue) {
        String property = properties.getProperty(keyName, null);
        if (property != null) {
            return Integer.parseInt(property);
        }

        properties.setProperty(keyName, Integer.toString(defaultValue));
        return defaultValue;
    }

    public float getPropertyValueFloat(String keyName, float defaultValue) {
        @Nullable String property = properties.getProperty(keyName, null);
        if (property != null) {
            return Float.parseFloat(property);
        }

        properties.setProperty(keyName, Float.toString(defaultValue));
        return defaultValue;
    }

    public boolean getPropertyValueBoolean(String keyName, boolean defaultValue) {
        @Nullable String property = properties.getProperty(keyName, null);
        if (property != null) {
            return Boolean.parseBoolean(property);
        }

        properties.setProperty(keyName, Boolean.toString(defaultValue));
        return defaultValue;
    }

    public String getPropertyValueString(String keyName, String defaultValue) {
        @Nullable String property = properties.getProperty(keyName, null);
        if (property != null) {
            return property;
        }

        properties.setProperty(keyName, defaultValue);
        return defaultValue;
    }
}