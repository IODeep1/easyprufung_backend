package com.easyprufung.backend.Project.Utility;

import com.easyprufung.backend.Project.Service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service

public class TerminalCommandService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    public String executeCommand(String command, String projects) {
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(projects));
        // Add npm's directory to the PATH if needed
        String path = System.getenv("PATH");
        processBuilder.environment().put("PATH", path + ":/usr/local/bin"); // Modify with your npm location

        processBuilder.command("bash", "-c", command);

        try {
            Process process = processBuilder.start();
            // Capture normal output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Capture error output
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int exitCode = process.waitFor(); // Wait for the process to complete
            if (exitCode != 0) {
                return "Error executing command: Exit code " + exitCode + "\nError Output:\n" + errorOutput.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing command: " + e.getMessage();
        }

        return output.toString();
    }

    public String executeBuildCommand(String command, String projects) {
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(projects));
        // Add npm's directory to the PATH if needed
        String path = System.getenv("PATH");
        processBuilder.environment().put("PATH", path + ":/usr/local/bin"); // Modify with your npm location

        processBuilder.command("bash", "-c", command);

        try {
            Process process = processBuilder.start();
            // Capture normal output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Capture error output
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int exitCode = process.waitFor(); // Wait for the process to complete
            if (exitCode != 0) {
                return ExtractBuildError(output.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing command: " + e.getMessage();
        }

        return "";
    }

    public Boolean executeAddDomainCommand(String path, String domain, String projectId, String email) {
        try {
            String nginxConfig = "server {  \n" +
                    "    listen 80;  \n" +
                    "    server_name $domain www.$domain;  \n" +
                    "  \n" +
                    "    location / {  \n" +
                    "        # Serve content from the corresponding subdomain directory\n" +
                    "        root /home/easyprufung/server/projects/$projectId/landingpage/build;\n" +
                    "        # Try to serve the requested file, if not found, return 404\n" +
                    "        try_files $uri $uri/ =404;\n" +
                    "    }  \n" +
                    "}";
            nginxConfig = nginxConfig.replace("$domain", domain).replace("$projectId", projectId);
            var configFileName = removeTLD(domain)+".conf";
            if(configFileName.equals("easyprufung.conf") || configFileName.equals("easyprufung-sub.conf")){
                logger.error("trying to add easyprufung conf");
                return false;
            }
            var tempConfigPath= path+ "/" + configFileName;
            Files.writeString(Paths.get(tempConfigPath), nginxConfig);
            String[] cmd = { "sudo", "mv", tempConfigPath, "/etc/nginx/conf.d/"+configFileName };

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                var result = ReloadNginxConfig();
                if (result) {
                    return RunCertBot(domain, email);
                }
            } else {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                logger.error("Move domain error:"+ errorOutput);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Boolean executeDeleteDomainCommand(String domain) {
        try {

            var configFileName = removeTLD(domain)+".conf";
            if(configFileName.equals("easyprufung.conf") || configFileName.equals("easyprufung-sub.conf")){
                logger.error("trying to remove easyprufung conf");
                return false;
            }
            String[] cmd = { "sudo", "rm", "/etc/nginx/conf.d/"+configFileName };
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return ReloadNginxConfig();
            } else {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                logger.error("Delete domain error:"+ errorOutput);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    Boolean ReloadNginxConfig() {
        try{
            String[] reloadCmd = { "sudo", "systemctl", "reload", "nginx" };
            ProcessBuilder reloadProcessBuilder = new ProcessBuilder(reloadCmd);
            Process reloadProcess = reloadProcessBuilder.start();
            int exitCode = reloadProcess.waitFor();

            if (exitCode == 0) {
                return true;
            } else {
                String errorOutput = new String(reloadProcess.getErrorStream().readAllBytes());
                logger.error("Reload Nginx error :"+ errorOutput);
            }
        }
        catch (Exception e){
            logger.error(e.getMessage());
        }
        return false;
    }

    Boolean RunCertBot(String domain, String email) {
        try{
            String[] certbotCmd = {
                    "sudo", "certbot", "--nginx",
                    "-d", domain,
                    "-d", "www."+domain,
                    "--non-interactive",
                    "--agree-tos",
                    "-m", email
            };
            ProcessBuilder certbotProcessBuilder = new ProcessBuilder(certbotCmd);
            //certbotProcessBuilder.inheritIO(); // ensures output/errors are shown in your app logs
            Process certbotProcess = certbotProcessBuilder.start();
            int certbotExit = certbotProcess.waitFor();

            if (certbotExit == 0) {
                return  true;
            } else {
                String errorOutput = new String(certbotProcess.getErrorStream().readAllBytes());
                logger.error("Run CertBot error :"+errorOutput);
            }
        }
        catch (Exception e){
            logger.error(e.getMessage());
        }
        return  false;
    }

    String removeTLD(String domain) {
        String[] parts = domain.split("\\.");
        if (parts.length > 1) {
            return parts[0];
        }
        return domain;
    }

    String ExtractBuildError(String input){
        String regex = "(?s)Failed to compile\\.\\s*(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String result = matcher.group(1).trim();
            return result;
        } else {
            return "";
        }
    }
}
