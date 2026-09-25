package com.easyprufung.backend.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final Path rootStorageLocation;
    private final Path resourcesStorageLocation;
    private final Path logoStorageLocation;
    @Autowired
    public FileService(Environment env) {
        this.rootStorageLocation = Paths.get(env.getProperty("app.file.working-dir", "./projects"))
                .toAbsolutePath().normalize();
        this.resourcesStorageLocation = Paths.get(env.getProperty("app.file.resources-dir", "./resources"))
                .toAbsolutePath().normalize();
        this.logoStorageLocation = Paths.get("./resources/logo")
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.rootStorageLocation);
            Files.createDirectories(this.resourcesStorageLocation);
            Files.createDirectories(this.logoStorageLocation);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            throw new RuntimeException(
                    "Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
}