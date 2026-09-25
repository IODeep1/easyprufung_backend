package com.easyprufung.backend.Project.Service;
import com.easyprufung.backend.Project.Models.Domain;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class DomainCheckerService {

    // Method to check if a domain is registered (DNS resolution)
    public boolean isDomainRegistered(String domain) {
        try {
            InetAddress inetAddress = InetAddress.getByName(domain);
            return inetAddress != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Method to check multiple domains concurrently
    public List<Domain> checkDomains(List<String> domains) throws InterruptedException, ExecutionException {
        // Thread pool with a fixed number of threads (for efficient resource usage)
        ExecutorService executorService = Executors.newFixedThreadPool(10); // Adjust pool size as needed

        // List to hold Future objects representing each domain check task
        List<Callable<Map.Entry<String, Boolean>>> tasks = new ArrayList<>();

        // Creating tasks for each domain check
        for (String domain : domains) {
            tasks.add(() -> {
                boolean isRegistered = isDomainRegistered(domain);
                return new AbstractMap.SimpleEntry<>(domain, isRegistered);
            });
        }

        // Invoke all tasks concurrently
        List<Future<Map.Entry<String, Boolean>>> results = executorService.invokeAll(tasks);

        // Prepare the result map with domain names and their registration status
        List<Domain> domainStatusMap = new ArrayList<>();
        for (Future<Map.Entry<String, Boolean>> future : results) {
            Map.Entry<String, Boolean> entry = future.get(); // Get result of each task
            domainStatusMap.add(new Domain(entry.getKey(), entry.getValue()));
        }
        // Shut down the executor service
        executorService.shutdown();

        return domainStatusMap;
    }

}
