package ru.invest.agent.api.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest()
@ActiveProfiles("test")
public class JsonFileWriterTest {

    @Value("${file.path}")
    private String testPath;

    @Test
    public void should_pass_write_file() throws IOException {
        String testJson = "{\"test\":\"value\"}";
        String filePath = testPath + "test.json";

        // Выполнение
        boolean result = JsonFileWriter.saveJson(testJson, filePath);
        assertTrue(result);

        Path path = Path.of(filePath);
        assertTrue(Files.exists(path));
        assertEquals(testJson, Files.readString(path));
    }

    @Test
    public void should_pass_write_file_when_not_json_type() throws IOException {
        String testJson = "{\"test\":\"value\"}";
        String filePath = testPath + "test";

        // Выполнение
        boolean result = JsonFileWriter.saveJson(testJson, filePath);
        assertTrue(result);

        Path path = Path.of(filePath);
        assertTrue(Files.exists(path));
        assertEquals(testJson, Files.readString(path));
    }

    @Test
    public void should_fail_when_directory_not_exist() throws IOException {
        String testJson = "{\"test\":\"value\"}";
        String filePath = testPath + "some/wrong/path" + "test.json";

        // Выполнение
        boolean result = JsonFileWriter.saveJson(testJson, filePath);
        assertFalse(result);

        Path path = Path.of(filePath);
        assertFalse(Files.exists(path));
    }
}
