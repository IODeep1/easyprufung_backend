package com.easyprufung.backend.Project.Models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LandingPage {

    private String templateName;
    private List<String> features;
    private String projectUuid;


    public LandingPage() {
    }
}
