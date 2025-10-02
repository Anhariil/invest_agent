package ru.invest.agent.api.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import ru.invest.agent.api.BaseTest;
import ru.invest.agent.api.utils.JsonConverter;
import ru.invest.agent.api.utils.JsonFile;

import java.util.List;
import java.util.stream.DoubleStream;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class AdvancedPredictionServiceTest extends BaseTest {

    AdvancedPredictionService apService = new AdvancedPredictionService();

    @Test
    public void should_pass_when_start() {
        String path = testPath + "/priceList.json";
        List<Double> list = JsonConverter.convertJsonToObject(JsonFile.readJson(path));
        int n = 10;

        double[] source = list.stream().limit(n)
                .flatMapToDouble(DoubleStream::of)
                .toArray();

        double[] expected = list.stream().skip(n).limit(n)
                .flatMapToDouble(DoubleStream::of)
                .toArray();

        double[] result = apService.predictWithAdaptiveModel(source, source.length);

        assertNotNull(result);
        assertEquals(source.length, result.length);

    }

//    @Test
//    public void should_fail_when_one_of_massive_is_null(){
////        assertFalse(apService.predictWithAdaptiveModel( new double[], null)>0);
//    }
}
