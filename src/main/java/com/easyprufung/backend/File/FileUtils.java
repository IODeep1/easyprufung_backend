package com.easyprufung.backend.File;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static List<File> getAllFilesInDirectory(String directoryPath) {
        List<File> fileList = new ArrayList<>();
        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory path: " + directoryPath);
        }

        // Recursively add files to the list
        addFilesRecursively(directory, fileList);
        return fileList;
    }

    private static void addFilesRecursively(File directory, List<File> fileList) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recurse into the subdirectory
                    addFilesRecursively(file, fileList);
                } else {
                    // Add file to the list
                    fileList.add(file);
                }
            }
        }
    }

    // Method to read file content as a Base64-encoded string
    public static String encodeFileToBase64(File file) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return java.util.Base64.getEncoder().encodeToString(fileContent);
    }
}