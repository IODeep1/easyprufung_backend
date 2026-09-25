package com.easyprufung.backend.Project.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.commons.lang.Pair;
import com.easyprufung.backend.AI.Models.ChatRequest;
import com.easyprufung.backend.AI.Models.CustomChatResponse;
import com.easyprufung.backend.AI.Service.OpenAIService;
import com.easyprufung.backend.Project.Constants.ProjectConstants;
import com.easyprufung.backend.Project.DTO.ProjectDTO;
import com.easyprufung.backend.Project.Helper.DomainHelper;
import com.easyprufung.backend.Project.Helper.IconHelper;
import com.easyprufung.backend.Project.Helper.ProjectHelper;
import com.easyprufung.backend.Project.Models.ChatMessage;
import com.easyprufung.backend.Project.ProjectRepository.ProjectsRepository;
import com.easyprufung.backend.Project.Utility.TerminalCommandService;
import com.easyprufung.backend.Utils.Parser;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    @Autowired
    ProjectsRepository projectRepository;

    @Autowired
    OpenAIService openAIService;

    @Autowired
    ProjectService projectService;

    @Autowired
    private TerminalCommandService terminalCommandService;


    public ChatRequest getProjectChatHistory(String uuid, String location) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        var chatHistoryAsJson = ProjectHelper.getFileContent(Paths.get("projects", ProjectHelper.encodeUUIDToShortString(uuid) ,ProjectConstants.chatDir).toString(),location +".txt");
        ChatRequest chatRequest = objectMapper.readValue(chatHistoryAsJson, ChatRequest.class);
        return  chatRequest;
    }

    public ProjectDTO updateProjectValidation(ChatMessage chatMessage, String uuid) throws JsonProcessingException {
        var project= projectRepository.findByUUID(uuid);
        ObjectMapper objectMapper = new ObjectMapper();
        var chatHistoryAsJson = ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.chatDir).toString(),ProjectConstants.projectValidationFileName);
        ChatRequest chatRequest = objectMapper.readValue(chatHistoryAsJson, ChatRequest.class);

        chatRequest = openAIService.getUpdatedProjectValidation(chatMessage.getContent(),chatRequest);
        String chatMessagesAsJson = objectMapper.writeValueAsString(chatRequest);
        ProjectHelper.saveFileContent(chatMessagesAsJson,Paths.get(project.getPath(),ProjectConstants.chatDir).toString() , ProjectConstants.projectValidationFileName);

        var assistantContent= chatRequest.getMessages().stream().filter(item-> ( item.getRole().equals("assistant"))).reduce((first, second) -> second);
        Object assistantContentAsObject = null;
        if(assistantContent.isPresent())
        {
            var content = assistantContent.get().getContent().stream().findFirst();
            if(content.isPresent()){
                var customChatResponse = objectMapper.readValue(content.get().getText(), CustomChatResponse.class);
                assistantContentAsObject = customChatResponse.getSystem_content();
            }
        }
        ModelMapper modelMapper = new ModelMapper();
        projectService.saveProject(project);
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        projectDTO.setValidationData(assistantContentAsObject);
        return projectDTO;
    }


    public ProjectDTO updateProjectName(ChatMessage chatMessage, String uuid) throws JsonProcessingException {
        var project= projectRepository.findByUUID(uuid);
        ObjectMapper objectMapper = new ObjectMapper();
        var chatHistoryAsJson = ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.chatDir).toString(),ProjectConstants.projectNameFileName);
        ChatRequest chatRequest = objectMapper.readValue(chatHistoryAsJson, ChatRequest.class);

        chatRequest = openAIService.getUpdatedProjectName(chatMessage.getContent(),chatRequest);
        String chatMessagesAsJson = objectMapper.writeValueAsString(chatRequest);
        ProjectHelper.saveFileContent(chatMessagesAsJson,Paths.get(project.getPath(),ProjectConstants.chatDir).toString() , ProjectConstants.projectNameFileName);

        var assistantContent= chatRequest.getMessages().stream().filter(item-> ( item.getRole().equals("assistant"))).reduce((first, second) -> second);
        Object assistantContentAsObject = null;
        if(assistantContent.isPresent())
        {
            var content = assistantContent.get().getContent().stream().findFirst();
            if(content.isPresent()){
                var customChatResponse = objectMapper.readValue(content.get().getText(), CustomChatResponse.class);
                assistantContentAsObject = customChatResponse.getSystem_content();
            }
        }
        ModelMapper modelMapper = new ModelMapper();
        projectService.saveProject(project);
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        projectDTO.setNameData(assistantContentAsObject);
        return projectDTO;
    }

    public ProjectDTO updateProjectIcon(ChatMessage chatMessage, String uuid) throws JsonProcessingException {
        var project= projectRepository.findByUUID(uuid);
        ObjectMapper objectMapper = new ObjectMapper();
        var chatHistoryAsJson = ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.chatDir).toString(),ProjectConstants.projectIconFileName);
        ChatRequest chatRequest = objectMapper.readValue(chatHistoryAsJson, ChatRequest.class);

        chatRequest = openAIService.getUpdatedProjectIcon(chatMessage.getContent(),chatRequest);
        String chatMessagesAsJson = objectMapper.writeValueAsString(chatRequest);
        ProjectHelper.saveFileContent(chatMessagesAsJson,Paths.get(project.getPath(),ProjectConstants.chatDir).toString() , ProjectConstants.projectIconFileName);

        var assistantContent= chatRequest.getMessages().stream().filter(item-> ( item.getRole().equals("assistant"))).reduce((first, second) -> second);
        Object assistantContentAsObject = null;
        if(assistantContent.isPresent())
        {
            var content = assistantContent.get().getContent().stream().findFirst();
            if(content.isPresent()){
                var fixedJson = IconHelper.fixNewlinesInJson(content.get().getText());
                var customChatResponse = objectMapper.readValue(fixedJson, CustomChatResponse.class);
                assistantContentAsObject = customChatResponse.getSystem_content();
            }
        }
        ModelMapper modelMapper = new ModelMapper();
        projectService.saveProject(project);
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        var iconData = IconHelper.getSvgContent(assistantContentAsObject.toString());
        projectDTO.setIconData(iconData);
        return projectDTO;
    }

    public ProjectDTO updateProjectLandingPage(ChatMessage chatMessage, String uuid) throws JsonProcessingException {

        var project= projectRepository.findByUUID(uuid);
        project.setIsBuilding(true);
        project = projectService.saveProject(project);

        ObjectMapper objectMapper = new ObjectMapper();
        var chatHistoryAsJson = ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.chatDir).toString(),ProjectConstants.projectLandingPageFileName);

        ChatRequest chatRequest = objectMapper.readValue(chatHistoryAsJson, ChatRequest.class);
        Pair<ChatRequest, String> chatRequestStringPair = null;
        if(chatHistoryAsJson.contains("export default Layout;")){
            chatRequestStringPair = openAIService.getUpdatedProjectLandingPage(chatMessage.getContent(), chatRequest);
            chatRequest = chatRequestStringPair.getFirst();
        }
        else
        {
            var content= ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.landingPageDir,"/build/").toString() , ProjectConstants.indexHtmlFileName);
            chatRequestStringPair = openAIService.getUpdatedProjectLandingPageNew(chatMessage.getContent(), content, project.getPath());
            var chatRequestResult = chatRequestStringPair.getFirst();
            for (var mesage : chatRequestResult.getMessages()) {
                chatRequest.getMessages().add(mesage);
            }
        }
        String chatMessagesAsJson = objectMapper.writeValueAsString(chatRequest);
        ProjectHelper.saveFileContent(chatMessagesAsJson,Paths.get(project.getPath(),ProjectConstants.chatDir).toString() , ProjectConstants.projectLandingPageFileName);

        if(chatHistoryAsJson.contains("export default Layout;")){
            var layoutContent =  chatRequestStringPair.getSecond();
            ProjectHelper.saveFileContent(layoutContent,Paths.get(project.getPath(),ProjectConstants.landingPageDir,"/src/").toString() , ProjectConstants.layoutFileName);

            //run npm i
            Path landingPageDirPath = Paths.get(project.getPath(), ProjectConstants.landingPageDir);
            String installOutput = terminalCommandService.executeCommand("npm install --legacy-peer-deps", landingPageDirPath.toAbsolutePath().toString());

            // Run "npm run build" if "npm i" succeeds
            if (!installOutput.contains("Error")) {
                String buildOutput = terminalCommandService.executeBuildCommand("npm run build", landingPageDirPath.toAbsolutePath().toString());
                if(buildOutput.length() >0){
                    project.setBuildError(buildOutput);
                }
                //delete modules
                ProjectHelper.deleteDirectory(project.getPath(), ProjectConstants.landingPageDir+"/node_modules");
            }
        }
        else {
            var layoutContent =  chatRequestStringPair.getSecond();
            ProjectHelper.saveFileContent(layoutContent,Paths.get(project.getPath(),ProjectConstants.landingPageDir,"/build/").toString() , ProjectConstants.indexHtmlFileName);
        }
        Boolean isUp = DomainHelper.isWebsiteUp(project.getTempUrl());
        project.setIsWebsiteUp(isUp);

        ModelMapper modelMapper = new ModelMapper();
        project.setIsBuilding(false);
        projectService.saveProject(project);
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        return projectDTO;
    }

}
