package com.easyprufung.backend.Project.Helper;

import com.easyprufung.backend.Project.Constants.ProjectConstants;
import com.easyprufung.backend.Project.Utility.ImageUtils;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IconHelper {

    public static String getSvgContent(String svgData) {
        String svgContentRegex = "<svg.*?>(.*?)</svg>";
        Pattern svgPattern = Pattern.compile(svgContentRegex, Pattern.DOTALL);
        Matcher svgMatcher = svgPattern.matcher(svgData);

        if (svgMatcher.find()) {
            String svgContent = svgMatcher.group(1);
            // Now extract all <image ... /> tags from svgContent
            String imageTagRegex = "<image\\s+[^>]*?/>";
            Pattern imagePattern = Pattern.compile(imageTagRegex, Pattern.DOTALL);
            Matcher imageMatcher = imagePattern.matcher(svgContent);

            StringBuilder result = new StringBuilder();
            while (imageMatcher.find()) {
                result.append(imageMatcher.group()).append("\n");
            }

            if (result.length() > 0) {
                return result.toString();
            } else {
                return "<ellipse cx=\"12\" cy=\"5\" rx=\"9\" ry=\"3\" />\n" +
                        "  <path d=\"M3 5v14a9 3 0 0 0 18 0V5\" />";
            }
        } else {
            return "<ellipse cx=\"12\" cy=\"5\" rx=\"9\" ry=\"3\" />\n" +
                    "  <path d=\"M3 5v14a9 3 0 0 0 18 0V5\" />";
        }
    }
    public  static String saveLogoContent(String projectUuid, String svgData) {
        String encodedUuid=ProjectHelper.encodeUUIDToShortString(projectUuid);
        String svgPath = "/resources/logo/" +encodedUuid+".svg";
        ProjectHelper.createFile("./resources/logo", encodedUuid+".svg");
        ProjectHelper.saveFileContent(svgData, "./resources/logo", encodedUuid+".svg");
        return svgPath;
    }

    public  static String geLogoContent(String projectUuid) {
        String logoDir = "./resources/logo";
        String encodedUuid=ProjectHelper.encodeUUIDToShortString(projectUuid);
        return ProjectHelper.getFileContent(logoDir, encodedUuid+".svg");
    }

    public  static void deleteLogo(String projectUuid) {
        String logoDir = "./resources/logo";
        String encodedUuid=ProjectHelper.encodeUUIDToShortString(projectUuid);
        ProjectHelper.deleteFile(logoDir, encodedUuid+".svg");
    }


    public static String fixNewlinesInJson(String inputJson) {
        // Escape newlines inside the system_content field
        StringBuilder fixedJson = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < inputJson.length(); i++) {
            char c = inputJson.charAt(i);

            // Detect the beginning and end of strings (i.e., content inside quotes)
            if (c == '\"') {
                inString = !inString;
                fixedJson.append(c);
            } else if (inString && c == '\n') {
                // Replace newline with escaped \\n when inside a string
                fixedJson.append("\\n");
            } else {
                fixedJson.append(c);
            }
        }

        return fixedJson.toString();
    }
}
