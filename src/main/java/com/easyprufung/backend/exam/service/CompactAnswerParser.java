package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.dto.AnswerPayload;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CompactAnswerParser {
    private static final Pattern ITEM = Pattern.compile("^\\s*(\\d+)\\s*([A-Za-z0-9_+\\-]+)\\s*$");

    public List<AnswerPayload> parse(String compactAnswers) {
        if (compactAnswers == null || compactAnswers.isBlank()) return List.of();
        List<AnswerPayload> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String item : compactAnswers.split("[,;]")) {
            Matcher matcher = ITEM.matcher(item);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid compact answer '" + item.trim()
                        + "'. Expected format such as 1f, 2d, 3g");
            }
            String number = matcher.group(1);
            if (!seen.add(number)) throw new IllegalArgumentException("Duplicate answer for question " + number);
            result.add(new AnswerPayload(number, List.of(matcher.group(2)), null));
        }
        return result;
    }
}
