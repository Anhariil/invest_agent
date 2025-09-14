package ru.invest.agent.api.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest()
@ActiveProfiles("test")
public class JsonConverterTest {

    @Test
    public void should_pass_for_string(){
        String testString = "привет мир";
        String testJsonString = '"' + testString + '"';

        String outputJson = JsonConverter.convertObjectToJson(testString);
        assertNotNull(outputJson);
        assertEquals(testJsonString,outputJson);

        String outputString = JsonConverter.convertJsonToObject(outputJson, new TypeReference<>() {});
        assertNotNull(outputString);
        assertEquals(testString, outputString);

        outputString = JsonConverter.convertJsonToObject(outputJson);
        assertNotNull(outputString);
        assertEquals( testString, outputString);
    }

    @Test
    public void should_pass_for_list_of_string(){
        String testString1 = "привет мир";
        String testString2 = "прощай мир";
        String testJsonString = "[\"" + testString1 + "\",\"" + testString2 + "\"]";

        String outputJson = JsonConverter.convertObjectToJson(List.of(testString1,testString2));
        assertNotNull(outputJson);
        assertEquals(testJsonString,outputJson);

        List<String> outputString = JsonConverter.convertJsonToObject(outputJson, new TypeReference<>() {});
        assertNotNull(outputString);
        assertEquals(List.of(testString1,testString2), outputString);

        outputString = JsonConverter.convertJsonToObject(outputJson);
        assertNotNull(outputString);
        assertEquals( List.of(testString1,testString2), outputString);
    }
}
