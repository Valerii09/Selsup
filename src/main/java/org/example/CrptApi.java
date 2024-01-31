package org.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
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
        // Define the structure of the document as per your requirements
        // ...

        @Override
        public String toString() {
            // Implement toString method as needed
            // Convert the document to JSON using Jackson's ObjectMapper
            return "Document JSON String";
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
