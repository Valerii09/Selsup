package org.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final Semaphore semaphore;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();

        // Scheduled task to release permits at fixed intervals
        Runnable releaseTask = () -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
        };

        // Schedule the task based on the specified timeUnit
        switch (timeUnit) {
            case SECONDS:
                // Schedule task every second
                scheduleTask(releaseTask, 1, TimeUnit.SECONDS);
                break;
            case MINUTES:
                // Schedule task every minute
                scheduleTask(releaseTask, 1, TimeUnit.MINUTES);
                break;
            // Add more cases as needed
        }
    }

    private void scheduleTask(Runnable task, long period, TimeUnit timeUnit) {
        Thread scheduler = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(timeUnit.toMillis(period));
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        scheduler.setDaemon(true);
        scheduler.start();
    }

    public void createDocument(String apiUrl, Document document, String signature) {
        try {
            semaphore.acquire(); // Acquire permit, blocking if necessary
            System.out.println("Creating document: " + document);
            System.out.println("Signature: " + signature);

            String requestBody = objectMapper.writeValueAsString(document);

            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Signature", signature);
            httpPost.setEntity(new StringEntity(requestBody));

            HttpResponse response = httpClient.execute(httpPost);

            // Process response as needed

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            semaphore.release(); // Release permit when done
        }
    }

    // Internal class representing the document structure
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        // Constructors, getters, setters

        @Override
        public String toString() {
            // Implement toString method as needed
            // Convert the document to JSON using Jackson's ObjectMapper
            return "Document JSON String";
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private static class Description {
            private String participantInn;

            // Constructors, getters, setters
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;

            // Constructors, getters, setters
        }
    }


    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);

        // Create a sample document and signature
        Document document = new Document();
        String signature = "SampleSignature";

        // API endpoint URL
        String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        // Call the createDocument method multiple times
        for (int i = 0; i < 10; i++) {
            crptApi.createDocument(apiUrl, document, signature);
            try {
                Thread.sleep(200); // Sleep for 200 milliseconds between calls
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
