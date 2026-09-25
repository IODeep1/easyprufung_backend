package com.easyprufung.backend.Project.Service;

import com.easyprufung.backend.Project.Models.Social;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class SocialMediaCheckerService {

    public boolean isProfileAvailable(String urlString, String socialApp) {
        try {
            // Create URL object
            URL url = new URL(urlString);
            // Open HTTP connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // Set timeout for the connection
            connection.connect();

            // Check HTTP response code
            int responseCode = connection.getResponseCode();

            switch (socialApp){
                case "youtube":
                    return responseCode == HttpURLConnection.HTTP_OK;
                case "instagram":{
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    // Read the content line by line
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return (response.toString().contains(urlString));
                }
                case "x":
                    return responseCode == HttpURLConnection.HTTP_OK;
                case "tiktok":
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    // Read the content line by line
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return (response.toString().contains("Couldn't find this account"));
            }
            // If we get a 404 (Not Found), the profile doesn't exist
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            // Handle error (e.g., network issues, invalid URL, etc.)
            return false;
        }
    }

    public List<Social> checkSocialMediaProfiles(String username) throws InterruptedException, ExecutionException {
        // Executor service to manage concurrent tasks
        ExecutorService executorService = Executors.newFixedThreadPool(4); // Pool with 4 threads for the 4 platforms

        // Create a list of callable tasks to check profiles on different platforms
        List<Callable<Map.Entry<String, Boolean>>> tasks = new ArrayList<>();

        // Instagram check
        tasks.add(() -> {
            String url = "https://www.instagram.com/" + username + "/";
            return new AbstractMap.SimpleEntry<>("Instagram", isProfileAvailable(url,"instagram"));
        });

        // TikTok check
        tasks.add(() -> {
            String url = "https://www.tiktok.com/@" + username.toLowerCase();
            return new AbstractMap.SimpleEntry<>("TikTok", isProfileAvailable(url,"tiktok"));
        });

        // YouTube check
        tasks.add(() -> {
            String url = "https://www.youtube.com/@" + username;
            return new AbstractMap.SimpleEntry<>("YouTube", isProfileAvailable(url,"youtube"));
        });

        // Add more platforms if needed (e.g., Twitter, Facebook, etc.)
        // Example for Twitter:
        tasks.add(() -> {
            String url = "https://x.com/" + username;
            return new AbstractMap.SimpleEntry<>("TwitterX", isProfileAvailable(url,"x"));
        });

        // Execute all tasks concurrently and get results
        List<Future<Map.Entry<String, Boolean>>> results = executorService.invokeAll(tasks);

        // Collect results into a map
        List<Social> platformStatus = new ArrayList<>();
        for (Future<Map.Entry<String, Boolean>> result : results) {
            Map.Entry<String, Boolean> entry = result.get();
            platformStatus.add(new Social(entry.getKey(), entry.getValue()));
        }

        // Shut down the executor service
        executorService.shutdown();

        return platformStatus;
    }
}
