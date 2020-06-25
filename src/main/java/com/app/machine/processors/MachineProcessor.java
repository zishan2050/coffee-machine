package com.app.machine.processors;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class MachineProcessor {

    private ForkJoinPool forkJoinPool;
    private Map<String, AtomicInteger> itemQuantityMap;

    private MachineProcessor() {
    }

    public static MachineProcessor getInstance() {
        return MachineProcessorSingleton.machineProcessor;
    }

    public void init(Integer outletCount, Map<String, Integer> itemQtyMap) {
        try {
            //Initialize fork join pool with parallelism equivalent to outlet count
            this.forkJoinPool = new ForkJoinPool(outletCount);
            this.itemQuantityMap = new ConcurrentHashMap<String, AtomicInteger>(itemQtyMap.size());
            itemQtyMap.entrySet().forEach(entry -> this.itemQuantityMap.put(entry.getKey(), new AtomicInteger(entry.getValue())));
        } catch (Exception e) {
            log.info("Exception initializing the machine", e);
        }
    }

    public List<String> process(Map<String, Map<String, Integer>> beverages) throws ExecutionException, InterruptedException {
        try {
            return forkJoinPool.submit(() -> {
                //Submitting beverages in parallel
                return beverages.keySet().stream().parallel().map(key -> {
                    Map<String, Integer> beverageIngredients = beverages.get(key);
                    List<String> itemsUpdated = new ArrayList(beverageIngredients.keySet().size());
                    List<String> itemsNotUpdated = new ArrayList(beverageIngredients.keySet().size());
                    //Reducing item quantity if machine contains the required quantity
                    for (String beverageItem : beverageIngredients.keySet()) {
                        if (this.itemQuantityMap.containsKey(beverageItem)) {
                            this.itemQuantityMap.get(beverageItem).updateAndGet(qty -> {
                                int beverageItemQuantity = beverageIngredients.getOrDefault(beverageItem, 0);
                                if (qty >= beverageItemQuantity) {
                                    itemsUpdated.add(beverageItem);
                                    return qty - beverageItemQuantity;
                                } else {
                                    itemsNotUpdated.add(beverageItem);
                                    return qty;
                                }
                            });
                        }
                    }
                    //Check to return locked items in case beverage is not prepared due to insufficient quantity of any item
                    if (itemsNotUpdated.size() != 0) {
                        itemsUpdated.stream().filter(item -> this.itemQuantityMap.get(item) != null).forEach(item -> this.itemQuantityMap.get(item).addAndGet(beverageIngredients.get(item)));
                        return key + " cannot be prepared because " + itemsNotUpdated.get(0) + " is not sufficient";
                    }
                    return key + " is prepared";
                }).collect(Collectors.toList());
            }).get();
        } catch (Exception e) {
            log.info("Exception processing beverages", e);
            throw e;
        }
    }

    public Map<String, Integer> getItemQuantityMap() {
        return (itemQuantityMap == null) ? Collections.emptyMap() : this.itemQuantityMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }

    public List<String> getLowRunningItems() {
        try {
            return (itemQuantityMap == null) ? Collections.emptyList() : this.itemQuantityMap.keySet().stream().filter(key -> this.itemQuantityMap.get(key).get() == 0).collect(Collectors.toList());
        } catch (Exception e) {
            log.info("Exception fetching low running items", e);
            throw e;
        }
    }

    public Integer refillItem(String item, Integer quantity) {
        try {
            if (this.itemQuantityMap.containsKey(item)) {
                return this.itemQuantityMap.get(item).addAndGet(quantity);
            }
            return 0;
        } catch (Exception e) {
            log.info("Exception filling item", e);
            throw e;
        }
    }

    private static class MachineProcessorSingleton {
        private static final MachineProcessor machineProcessor = new MachineProcessor();
    }
}
