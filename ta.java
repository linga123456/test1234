import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
public class WorkbenchController {

    @Autowired
    private WorkbenchService workbenchService;
import java.util.List;
import java.util.stream.Collectors;

List<OrderReqBatch> updatedOrderReqBatches = listOfOrderReqBatches.stream()
    .map(batch -> {
        List<OrderRequest> updatedOrderRequests = batch.getOrderRequests().stream()
            .map(orderRequest -> {
                List<Bid> filteredBids = orderRequest.getBids().stream()
                    .filter(bid -> !rejectedBidIdList.contains(bid.getBidId())) // Filter out bids with rejected bidId
                    .collect(Collectors.toList());
                orderRequest.setBids(filteredBids); // Update the order request with filtered bids
                return orderRequest;
            })
            .collect(Collectors.toList());
        batch.setOrderRequests(updatedOrderRequests); // Update the batch with filtered order requests
        return batch;
    })
    .collect(Collectors.toList());

System.out.println(updatedOrderReqBatches); // Output the updated structure

    @GetMapping("/processSecurities")
    public Map<String, Object> processSecurities() throws InterruptedException, ExecutionException {
        // Step 1: Divide the securities by market
        Map<String, List<String>> securitiesByMarket = divideSecuritiesByMarket();

        // Step 2: Load taList for each security under each market
        Map<String, Map<String, List<String>>> taListByMarket = loadTaListForSecurities(securitiesByMarket);

        // Step 3: Call AutoBorrow API for each market
        Map<String, List<String>> responseByMarket = new ConcurrentHashMap<>();
        for (Map.Entry<String, Map<String, List<String>>> entry : taListByMarket.entrySet()) {
            String market = entry.getKey();
            Map<String, List<String>> taListForMarket = entry.getValue();

            List<String> response = workbenchService.callAutoBorrowApi(market, taListForMarket);
            responseByMarket.put(market, response);
        }

        // Aggregate response and send it to UI
        Map<String, Object> aggregatedResponse = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : responseByMarket.entrySet()) {
            aggregatedResponse.put(entry.getKey(), entry.getValue());
        }

        return aggregatedResponse;
    }

    private Map<String, List<String>> divideSecuritiesByMarket() {
        // Mocked data. Replace with actual data source.
        Map<String, List<String>> securitiesByMarket = new HashMap<>();
        securitiesByMarket.put("Market1", Arrays.asList("Sec1", "Sec2"));
        securitiesByMarket.put("Market2", Arrays.asList("Sec3", "Sec4"));
        // ... Add more markets and securities

        return securitiesByMarket;
    }

    private Map<String, Map<String, List<String>>> loadTaListForSecurities(Map<String, List<String>> securitiesByMarket) throws InterruptedException, ExecutionException {
        Map<String, Map<String, List<String>>> taListByMarket = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(10); // Thread pool size

        for (Map.Entry<String, List<String>> entry : securitiesByMarket.entrySet()) {
            String market = entry.getKey();
            List<String> securities = entry.getValue();
            Map<String, List<String>> taListForMarket = new ConcurrentHashMap<>();

            List<CompletableFuture<Void>> futures = securities.stream()
                    .map(security -> CompletableFuture.runAsync(() -> {
                        try {
                            List<String> taList = workbenchService.loadTaList(market, security);
                            taListForMarket.put(security, taList);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            taListByMarket.put(market, taListForMarket);
        }

        return taListByMarket;
    }
}

@Service
class WorkbenchService {

    @Autowired
    private RadService radService;

    @Async
    public List<String> loadTaList(String market, String security) {
        // Call Rad service API to load taList for the security
        List<String> taList = radService.getTaList(market, security);
        
        // Preprocess taList
        // Example: taList = preprocessTaList(taList);
        
        return taList;
    }

    public List<String> callAutoBorrowApi(String market, Map<String, List<String>> taListForMarket) {
        // Call AutoBorrow API for the market with taListForMarket and return the response
        List<String> response = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : taListForMarket.entrySet()) {
            String security = entry.getKey();
            List<String> taList = entry.getValue();
            
            // Call AutoBorrow API with taList and add response to the list
            // Example: String borrowResponse = autoBorrowApi.call(market, security, taList);
            // response.add(borrowResponse);
        }

        return response;
    }
}

@Service
class RadService {

    public List<String> getTaList(String market, String security) {
        // Implementation to call Rad service API and get taList for the security
        // Return mock data for demonstration
        return Arrays.asList("TA1", "TA2", "TA3");
    }
}
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class ApiClientWithHttpURLConnection {

    public static void main(String[] args) throws Exception {
        String url = "https://api.example.com/token"; // Your API URL
        String certPath = "path/to/cert.pem";  // Path to certificate
        String keyPath = "path/to/key.pem";  // Path to private key
        String caCertPath = "path/to/ca.pem";  // Path to CA certificate for verification

        // Load the SSL context with your cert, key, and custom CA
        SSLContext sslContext = createCustomSSLContext(certPath, keyPath, caCertPath);

        // Create URL object
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        // Set up SSL context
        con.setSSLSocketFactory(sslContext.getSocketFactory());

        // Proceed with the request setup and execution...
    }

    // Method to create SSLContext with a custom CA
    private static SSLContext createCustomSSLContext(String certPath, String keyPath, String caCertPath) throws Exception {
        // Load client certificate and private key (as done before)
        SSLContext sslContext = createSSLContext(certPath, keyPath);  // Use the existing method to load cert & key

        // Load CA certificate from PEM file
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        FileInputStream caCertInputStream = new FileInputStream(caCertPath);
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(caCertInputStream);
        caCertInputStream.close();
import java.util.concurrent.ConcurrentLinkedQueue;

public class OrderSequenceManager {
    private final ConcurrentLinkedQueue<Integer> orderQueue = new ConcurrentLinkedQueue<>();
    
    // Simulated external Server C sequence management (thread-safe)
    private int currentServerCSequence = 1000; // Initial sequence in Server C

    // Synchronized method to get the next sequence and update Server C in one atomic operation
    private synchronized int getCurrentServerCSequence(int batchSize) {
        int startId = currentServerCSequence; // Capture the current sequence
        currentServerCSequence += batchSize; // Increment it by batch size
        return startId; // Return the previous sequence as the starting ID
    }

    // Method to allocate order IDs
    public synchronized void allocateOrderIds(int batchSize) {
        int startId = getCurrentServerCSequence(batchSize);
        for (int i = startId; i < startId + batchSize; i++) {
            orderQueue.add(i);
        }
        System.out.println("Allocated Order IDs: " + startId + " to " + (startId + batchSize - 1));
    }

    // Method to get the next available order ID
    public synchronized Integer getNextOrderId() {
        return orderQueue.poll(); // Retrieves and removes the head of the queue
    }

    // Example test
    public static void main(String[] args) {
        OrderSequenceManager manager = new OrderSequenceManager();

        // Simulate two batch requests
        manager.allocateOrderIds(24); // First request for 24 orders
        manager.allocateOrderIds(2);  // Second request for 2 orders

        // Process orders sequentially
        for (int i = 0; i < 26; i++) {
            System.out.println("Processing Order ID: " + manager.getNextOrderId());
        }
    }
}

        // Create a KeyStore and load 
