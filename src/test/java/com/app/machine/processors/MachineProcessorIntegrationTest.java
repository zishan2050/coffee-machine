package com.app.machine.processors;

import com.app.machine.models.MachineInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MachineProcessorIntegrationTest {

    private MachineProcessor machineProcessor;
    private MachineInput machineInput;

    @Before
    public void setup() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        ClassLoader classLoader = getClass().getClassLoader();
        File machineInputFile = new File(classLoader.getResource("jsons/machine_input.json").getFile());
        machineInput = objectMapper.readValue(machineInputFile, MachineInput.class);
        machineProcessor = machineProcessor.getInstance();
        machineProcessor.init(machineInput.getMachine().getOutlets().getCountN(), machineInput.getMachine().getTotalItemsQuantity());
    }

    @Test
    public void testMachine() throws ExecutionException, InterruptedException {
        List<String> results = machineProcessor.process(machineInput.getMachine().getBeverages());
        MatcherAssert.assertThat(results, Matchers.anyOf(Matchers.containsInAnyOrder("hot_tea cannot be prepared because sugar_syrup is not sufficient", "hot_coffee is prepared", "black_tea cannot be prepared because hot_water is not sufficient", "green_tea is prepared"),
                Matchers.containsInAnyOrder("hot_tea cannot be prepared because hot_water is not sufficient", "hot_coffee is prepared", "black_tea cannot be prepared because sugar_syrup is not sufficient", "green_tea is prepared"),
                Matchers.containsInAnyOrder("hot_tea cannot be prepared because hot_water is not sufficient", "hot_coffee is prepared", "black_tea is prepared", "green_tea cannot be prepared because sugar_syrup is not sufficient"),
                Matchers.containsInAnyOrder("hot_coffee is prepared", "black_tea is prepared", "green_tea cannot be prepared because sugar_syrup is not sufficient", "hot_tea cannot be prepared because item hot_water is not sufficient"),
                Matchers.containsInAnyOrder("hot_tea cannot be prepared because hot_water is not sufficient", "hot_coffee is prepared", "black_tea cannot be prepared because sugar_syrup is not sufficient", "green_tea is prepared"),
                Matchers.containsInAnyOrder("hot_tea cannot be prepared because hot_water is not sufficient", "hot_coffee cannot be prepared because sugar_syrup is not sufficient", "black_tea is prepared", "green_tea is prepared"),
                Matchers.containsInAnyOrder("hot_tea cannot be prepared because hot_water is not sufficient", "hot_coffee cannot be prepared because sugar_syrup is not sufficient", "black_tea is prepared", "green_tea cannot be prepared because sugar_syrup is not sufficient"),
                Matchers.containsInAnyOrder("hot_tea is prepared", "hot_coffee cannot be prepared because hot_water is not sufficient", "black_tea is prepared", "green_tea cannot be prepared because hot_water is not sufficient"),
                Matchers.containsInAnyOrder("hot_tea is prepared", "hot_coffee cannot be prepared because sugar_syrup is not sufficient", "black_tea cannot be prepared because hot_water is not sufficient", "green_tea cannot be prepared because sugar_syrup is not sufficient"),
                Matchers.containsInAnyOrder("hot_tea is prepared", "hot_coffee is prepared", "black_tea cannot be prepared because hot_water is not sufficient", "green_tea cannot be prepared because sugar_syrup is not sufficient"),
                Matchers.containsInAnyOrder("hot_tea cannot be prepared because sugar_syrup is not sufficient", "hot_coffee cannot be prepared because hot_water is not sufficient", "black_tea is prepared", "green_tea cannot be prepared because hot_water is not sufficient")));
    }

    @Test
    public void testMachineItemsQuantity() {
        Map<String, Integer> itemQuantityMap = machineProcessor.getItemQuantityMap();
        MatcherAssert.assertThat(itemQuantityMap, Matchers.allOf(Matchers.hasEntry("hot_water", 500), Matchers.hasEntry("hot_milk", 500), Matchers.hasEntry("ginger_syrup", 100), Matchers.hasEntry("sugar_syrup", 100), Matchers.hasEntry("tea_leaves_syrup", 100)));
    }

    @Test
    public void testMachineItemsRunningLowOnQuantity() throws ExecutionException, InterruptedException {
        machineProcessor.process(machineInput.getMachine().getBeverages());
        List<String> lowRunningItems = machineProcessor.getLowRunningItems();
        MatcherAssert.assertThat(lowRunningItems.size(), Matchers.greaterThan(0));
    }

    @Test
    public void testRefillItemInMachine() {
        Integer updatedQuantity = machineProcessor.refillItem("sugar_syrup", 200);
        Map<String, Integer> itemQuantityMap = machineProcessor.getItemQuantityMap();
        MatcherAssert.assertThat(itemQuantityMap, Matchers.notNullValue());
        MatcherAssert.assertThat(itemQuantityMap, Matchers.allOf(Matchers.hasEntry("hot_water", 500), Matchers.hasEntry("hot_milk", 500), Matchers.hasEntry("ginger_syrup", 100), Matchers.hasEntry("sugar_syrup", updatedQuantity), Matchers.hasEntry("tea_leaves_syrup", 100)));
    }
}
