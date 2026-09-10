package com.invasion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void returnsValidNumericValues() throws IOException {
        Config config = load("count=42\nscale=1.5\n");

        assertEquals(42, config.getPropertyValueInt("count", 7));
        assertEquals(1.5F, config.getPropertyValueFloat("scale", 2.0F));
    }

    @Test
    void replacesInvalidIntegerWithDefault() throws IOException {
        Config config = load("count=invalid\n");

        assertEquals(7, config.getPropertyValueInt("count", 7));
        assertEquals(7, config.getPropertyValueInt("count", 9));
    }

    @Test
    void replacesInvalidAndNonFiniteFloatsWithDefault() throws IOException {
        Config config = load("invalid=broken\nnan=NaN\ninfinity=Infinity\n");

        assertEquals(2.0F, config.getPropertyValueFloat("invalid", 2.0F));
        assertEquals(3.0F, config.getPropertyValueFloat("nan", 3.0F));
        assertEquals(4.0F, config.getPropertyValueFloat("infinity", 4.0F));
        assertEquals(2.0F, config.getPropertyValueFloat("invalid", 5.0F));
        assertEquals(3.0F, config.getPropertyValueFloat("nan", 6.0F));
        assertEquals(4.0F, config.getPropertyValueFloat("infinity", 7.0F));
    }

    private Config load(String contents) throws IOException {
        Path file = temporaryDirectory.resolve("invasion_config.cfg");
        Files.writeString(file, contents);
        Config config = new Config();
        config.loadConfig(file.toFile());
        return config;
    }
}
