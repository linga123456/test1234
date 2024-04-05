import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ServiceOrchestration {

    // Simulated Service1 API call
    public Map<String, List<String>> callService1() {
        // Return map of markets and their inventories for which service2 calls need to be made
        Map<String, List<String>> marketInventories = new HashMap<>();
        marketInventories.put("market1", Arrays.asList("inventory1", "inventory2"));
        marketInventories.put("market2", Arrays.asList("inventory3"));
        marketInventories.put("market3", Arrays.asList("inventory4", "inventory5", "inventory6"));
        return marketInventories;
    }

    // Simulated Service2 API call
    public List<TargetedAvailability> callService2(String market, String inventory) {
        // This method should call the actual service2 API and return its list of TargetedAvailability objects
        // For demonstration, I'm returning a mocked list of TargetedAvailability objects
        return Arrays.asList(
                new TargetedAvailability("object1", market, inventory),
                new TargetedAvailability("object2", market, inventory)
        );
    }

    // Simulated Service3 API call
    public String callService3(List<List<TargetedAvailability>> service2Responses) {
        // This method should call the actual service3 API and return its response
        // For demonstration, I'm returning a mocked response
        return "service3_response_" + service2Responses.stream()
                .flatMap(List::stream)
                .map(TargetedAvailability::toString)
                .collect(Collectors.joining(","));
    }

    public void orchestrateServices() {
        ExecutorService executor = Executors.newFixedThreadPool(10); // Create a thread pool

        // Get map of markets and their inventories from Service1
        Map<String, List<String>> marketInventories = callService1();

        // Make asynchronous calls to Service2 for each inventory within each market
        Map<String, List<CompletableFuture<List<TargetedAvailability>>>> marketService2Futures = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : marketInventories.entrySet()) {
            String market = entry.getKey();
            List<CompletableFuture<List<TargetedAvailability>>> futures = entry.getValue().stream()
                    .map(inventory -> CompletableFuture.supplyAsync(() -> callService2(market, inventory), executor))
                    .collect(Collectors.toList());
            marketService2Futures.put(market, futures);
        }

        // Wait for all Service2 calls to complete for each market
        Map<String, CompletableFuture<Void>> marketAllService2Futures = new HashMap<>();
        for (Map.Entry<String, List<CompletableFuture<List<TargetedAvailability>>>> entry : marketService2Futures.entrySet()) {
            String market = entry.getKey();
            List<CompletableFuture<List<TargetedAvailability>>> futures = entry.getValue();
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            marketAllService2Futures.put(market, allFutures);
        }

        // Handle the results and call Service3 once per market
        for (Map.Entry<String, CompletableFuture<Void>> entry : marketAllService2Futures.entrySet()) {
            String market = entry.getKey();
            CompletableFuture<Void> allFutures = entry.getValue();

            allFutures.thenAcceptAsync(
                    v -> {
                        List<List<TargetedAvailability>> service2Responses = marketService2Futures.get(market).stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList());

                        String service3Response = callService3(service2Responses);
                        System.out.println("Service3 response for market " + market + ": " + service3Response);
                    },
                    executor
            );
        }

        // Wait for all tasks to complete
        marketAllService2Futures.values().forEach(CompletableFuture::join);
        executor.shutdown(); // Shut down the executor
    }

    public static void main(String[] args) {
        ServiceOrchestration orchestration = new ServiceOrchestration();
        orchestration.orchestrateServices();
    }
}
