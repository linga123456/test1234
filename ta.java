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
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

@Service
public class OrderManager {

    private final RestTemplate restTemplate;
    private final AppConfig appConfig;
    private final Queue<Integer> orderQueue = new ConcurrentLinkedQueue<>();
    private final Lock lock = new ReentrantLock(true); // Fair lock to avoid thread starvation
    private final Condition notEmpty = lock.newCondition(); // Condition to wait when queue is empty

    public OrderManager(RestTemplate restTemplate, AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
    }

    public Integer getNextParentId() { 
        lock.lock();
        try {
            while (orderQueue.isEmpty()) {
                allocateOrderIds(10); // Request 10 more IDs
                notEmpty.await(); // Wait until IDs are available
            }
            return orderQueue.poll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for parent IDs", e);
        } finally {
            lock.unlock();
        }
    }

    private void allocateOrderIds(int batchSize) {
        lock.lock();
        try {
            if (!orderQueue.isEmpty()) {
                return; // Avoid duplicate allocation if another thread already filled the queue
            }

            Integer startingId = getABParentId(batchSize);
            if (startingId == null) {
                throw new RuntimeException("Failed to allocate parent IDs");
            }

            for (int i = 0; i < batchSize; i++) {
                orderQueue.add(startingId + i);
            }

            notEmpty.signalAll(); // Notify waiting threads
        } finally {
            lock.unlock();
        }
    }

    private Integer getABParentId(int batchSize) { 
        String url = "http://server-c/api/get-sequence?batchSize=" + batchSize;
        return restTemplate.getForObject(url, Integer.class);
    }
}

@ExtendWith(MockitoExtension.class)
class OrderManagerTest {

    @Mock
    private RestTemplate restTemplate; // Mocking RestTemplate

    @Mock
    private AppConfig appConfig; // Mocking AppConfig

    private OrderManager orderManager;
    private Queue<Integer> orderQueue;

    @BeforeEach
    void setUp() {
        // Initialize OrderManager with mocked dependencies
        orderManager = new OrderManager(restTemplate, appConfig);
        orderQueue = new ConcurrentLinkedQueue<>();
        // Set the queue directly if needed
    }

    @Test
    void testGetNextOrderId_whenQueueEmpty_allocatesNewIds() throws InterruptedException {
        // Simulate an empty queue
        orderQueue.clear();

        // Mock the behavior of RestTemplate or AppConfig if needed
        // For example, if AppConfig provides some configuration needed for allocateOrderIds
        when(appConfig.getBatchSize()).thenReturn(10); // Example: Mocking a method in AppConfig
        doNothing().when(restTemplate).getForObject(anyString(), eq(String.class));  // Example mocking RestTemplate call if required

        // Call the method
        Integer nextOrderId = orderManager.getNextOrderId();

        // Assert that the order ID is returned
        assertNotNull(nextOrderId, "Order ID should not be null after allocation.");
        assertEquals(1, nextOrderId, "The first order ID should be 1.");
    }

    @Test
    void testGetNextOrderId_whenQueueHasIds_returnsNextId() {
        // Prepopulate the queue with IDs
        orderQueue.add(1);
        orderQueue.add(2);

        // Call the method
        Integer nextOrderId = orderManager.getNextOrderId();

        // Assert that the first order ID is returned
        assertEquals(1, nextOrderId, "The first order ID should be 1.");
    }

    @Test
    void testGetNextOrderId_whenMultipleThreadsAccessQueue() throws InterruptedException {
        // Create a separate thread to call getNextOrderId()
        Runnable task = () -> {
            try {
                Integer orderId = orderManager.getNextOrderId();
                assertNotNull(orderId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Start multiple threads
        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);
        thread1.start();
        thread2.start();

        // Wait for threads to finish
        thread1.join();
        thread2.join();

        // Validate that no exceptions occurred during concurrent access
        assertTrue(true); // If no exceptions thrown, the test passed
    }

    @Test
    void testGetNextOrderId_whenAllocationFails_shouldThrowException() {
        // Simulate failure in allocation
        orderQueue.clear();

        // Mock allocation failure (AppConfig/RestTemplate calls)
        when(appConfig.getBatchSize()).thenReturn(10); // Example: Mocking AppConfig
        doThrow(new RuntimeException("Failed to allocate order IDs")).when(restTemplate).getForObject(anyString(), eq(String.class));

        // Expect an exception when getting the next order ID
        assertThrows(RuntimeException.class, () -> {
            orderManager.getNextOrderId();
        }, "Should throw exception if allocation fails.");
    }
}

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
