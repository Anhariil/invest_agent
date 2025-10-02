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
public class AssessmentServiceTest extends BaseTest {

    AdvancedPredictionService apService = new AdvancedPredictionService();

    @Test
    public void should_pass_when_arrays_valid() {

        String path = testPath + "/priceList.json";
        List<Double> list = JsonConverter.convertJsonToObject(JsonFile.readJson(path));
        int n = 40;

        if (list.size() / 2 < n){
            n = list.size() / 2;
        }

        double[] source = list.stream().limit(n)
                .flatMapToDouble(DoubleStream::of)
                .toArray();

        double[] expected = list.stream().skip(n).limit(n)
                .flatMapToDouble(DoubleStream::of)
                .toArray();

        double[] result = apService.predictWithAdaptiveModel(source, source.length);

       int assessmentServiceResult = AssessmentService.getBestIntervalLength(expected, result);
       assertTrue(assessmentServiceResult > 0 && assessmentServiceResult <= expected.length);
        System.out.println(assessmentServiceResult + " " + expected.length );
        for (double value : result) {

            System.out.print(Math.round(value)  + " | ");
        }
        System.out.println();
        for (double v : expected) {
            System.out.print(v + " | ");

        }

    }
}
