package com.easyprufung.backend.Project.Models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Social {
    private String platform;
    private Boolean status;


    public Social() {
    }
}
