package com.easyprufung.backend.Project.WebSocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.easyprufung.backend.Project.Constants.ProjectConstants;
import com.easyprufung.backend.Project.Helper.ProjectHelper;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.User;
import com.easyprufung.backend.Utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.file.Paths;

@Component
public class LandingPageWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserService userService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String authorization = json.get("authorization").asText();
            String projectUuid = json.get("projectUuid").asText();

            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorization);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(projectUuid))
                    .findFirst();
            if(!project.isPresent()){
                session.sendMessage(new TextMessage("[EASYPRUFUNG_DONE]"));
                return;
            }
            Thread.sleep(5000);
            var content= "";
            while (!content.contains("</html>")){
                Thread.sleep(10000); // simulate delay
                session.sendMessage(new TextMessage(content));
                content= ProjectHelper.getFileContent(Paths.get(project.get().getPath(), ProjectConstants.landingPageDir,"/build/").toString() , ProjectConstants.indexHtmlFileName);
            }
            session.sendMessage(new TextMessage("[EASYPRUFUNG_DONE]"));
        }
        catch (Exception e) {

        }

    }
}