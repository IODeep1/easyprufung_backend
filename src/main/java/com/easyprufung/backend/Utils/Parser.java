package com.easyprufung.backend.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Parser {
    public static boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }


    public static String removeLinesContainingBackticks(String input) {
        // Split the input string into lines
        List<String> lines = Arrays.asList(input.split("\n"));

        // Filter out lines that contain "```"
        List<String> filteredLines = lines.stream()
                .filter(line -> !line.contains("```"))
                .collect(Collectors.toList());

        // Join the filtered lines back into a single string
        return String.join("\n", filteredLines);
    }

    public static String RemoveAllCharsBefore(String input, String word)
    {
        int index = input.indexOf(word);
        if (index != -1) {
            return input.substring(index);
        } else {
            return input;
        }
    }

    public static Boolean validatePassword(String password) {
        if (password.length() < 8
                || !Pattern.compile("[A-Z]").matcher(password).find()
                || !Pattern.compile("[a-z]").matcher(password).find()
                || !Pattern.compile("[0-9]").matcher(password).find()
                ||!Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find()){
            return  false;
        }

        return true;
    }
}
