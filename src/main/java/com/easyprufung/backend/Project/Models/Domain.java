package com.easyprufung.backend.Project.Models;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Domain {
    private String name;
    private Boolean status;


    public Domain() {
    }
}
