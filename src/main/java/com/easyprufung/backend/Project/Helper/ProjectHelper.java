package com.easyprufung.backend.Project.Helper;

import com.easyprufung.backend.Utils.Constants;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ProjectHelper {
    private static final Logger logger = LoggerFactory.getLogger(ProjectHelper.class);

    public static void createFile(String directoryPath, String fileName){
        try {
            Path filePath = Paths.get(directoryPath,fileName).toAbsolutePath();
            Files.createFile(filePath);

        } catch (IOException e) {
        }
    }

    public static void deleteFile(String directoryPath, String fileName){
        try {
            Path filePath = Paths.get(directoryPath,fileName).toAbsolutePath();
            Files.delete(filePath);

        } catch (IOException e) {
        }
    }

    public static void saveFileContent(String content, String directoryPath, String fileName){
        FileWriter writer = null;
        try {
            String filePath = Paths.get(directoryPath,fileName).toAbsolutePath().toString();
            File file = new File(filePath);
            if (file.exists()) {
                boolean deleted = file.delete();
            }
            writer = new FileWriter(file);

            // Write content to the file
            writer.write(content);
            writer.flush();
        } catch (IOException e) {

        } finally {
            // Close FileWriter to release system resources
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {

                }
            }
        }
    }

    public static String getFileContent(String directoryPath, String fileName) {
        String contentToReturn = null;
        String filePath = Paths.get(directoryPath,fileName).toAbsolutePath().toString();
        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;

        try {
            File file = new File(filePath);

            if (!file.exists()) {
                throw new IOException("File not found: " + filePath);
            }

            reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }

        } catch (IOException e) {
           
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                   
                }
            }
        }
        contentToReturn = content.toString();
        if(filePath.contains("/chat/")){
            contentToReturn = contentToReturn.replace("\"max_tokens\"", "\"max_completion_tokens\"");
        }
        return contentToReturn;
    }

    public static Path zipDirectory(Path directoryPath) throws IOException {
        Path zipPath = Paths.get(directoryPath.toString() + ".zip");
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Files.walk(directoryPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(directoryPath.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        }
        catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return zipPath;
    }

    public static String createUniqueDirectory(String baseDir, String uuid) throws IOException {
        Path uniqueDirPath = Paths.get(baseDir, uuid);
        Files.createDirectories(uniqueDirPath);
        return uniqueDirPath.toString();
    }

    public static String createProjectSubDirectory(String baseDir, String directoryName) throws IOException {
        Path uniqueProjectPath = Paths.get(baseDir, directoryName);
        Files.createDirectories(uniqueProjectPath);
        return uniqueProjectPath.toString();
    }

    public static void deleteDirectory(String baseDir, String directoryName) {
        try {
            Path directoryPath = Paths.get(baseDir, directoryName);
            FileUtils.deleteDirectory(new File(directoryPath.toAbsolutePath().toString()));
            Files.delete(directoryPath);
        }catch (Exception exception){

        }
    }

    public static String encodeUUIDToShortString(String uuid) {
        UUID originalUUID = UUID.fromString(uuid);
        byte[] bytes = new byte[16];
        long mostSigBits = originalUUID.getMostSignificantBits();
        long leastSigBits = originalUUID.getLeastSignificantBits();

        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (mostSigBits >>> (56 - (i * 8)));
            bytes[i + 8] = (byte) (leastSigBits >>> (56 - (i * 8)));
        }

        // Use Base64 encoding (URL-safe, without padding)
        String base64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Remove non-alphanumeric characters and convert to lowercase
        String shortEncoded = base64Url.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        // Trim to maximum length of 15 characters
        return shortEncoded.length() > 15 ? shortEncoded.substring(0, 15) : shortEncoded;
    }

    public static String decodeShortStringToUUID(String shortEncoded) {
        // Ensure the input string is 15 characters or fewer
        if (shortEncoded.length() != 15) {
            throw new IllegalArgumentException("Input string must be 15 characters or less");
        }

        // Reverse the process by padding the string for Base64 URL decoding
        String base64Url = shortEncoded + "=="; // Adding padding to match original Base64 URL length

        // Decode the Base64 string back into bytes
        byte[] decodedBytes = Base64.getUrlDecoder().decode(base64Url);

        // Recreate the UUID from the decoded bytes
        long mostSigBits = 0;
        long leastSigBits = 0;

        for (int i = 0; i < 8; i++) {
            mostSigBits |= ((long) (decodedBytes[i] & 0xff)) << (56 - (i * 8));
            leastSigBits |= ((long) (decodedBytes[i + 8] & 0xff)) << (56 - ((i + 8) * 8));
        }

        return new UUID(mostSigBits, leastSigBits).toString();
    }
}
