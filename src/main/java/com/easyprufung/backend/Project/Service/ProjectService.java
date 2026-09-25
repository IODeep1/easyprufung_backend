package com.easyprufung.backend.Project.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.easyprufung.backend.AI.Models.ChatRequest;
import com.easyprufung.backend.AI.Models.CustomChatResponse;
import com.easyprufung.backend.AI.Service.OpenAIService;
import com.easyprufung.backend.Mixpanel.Service.MixpanelService;
import com.easyprufung.backend.Project.Constants.ProjectConstants;
import com.easyprufung.backend.Project.ContactForm;
import com.easyprufung.backend.Project.DTO.*;
import com.easyprufung.backend.Project.Helper.DomainHelper;
import com.easyprufung.backend.Project.Helper.IconHelper;
import com.easyprufung.backend.Project.Helper.ProjectHelper;
import com.easyprufung.backend.Project.Models.Domain;
import com.easyprufung.backend.Project.Models.Social;
import com.easyprufung.backend.Project.Project;
import com.easyprufung.backend.Project.ProjectRepository.ContactFormsRepository;
import com.easyprufung.backend.Project.ProjectRepository.ProjectsRepository;
import com.easyprufung.backend.Project.ProjectRepository.UpvotesRepository;
import com.easyprufung.backend.Project.ProjectRepository.WaitlistsRepository;
import com.easyprufung.backend.Project.Upvote;
import com.easyprufung.backend.Project.Utility.TerminalCommandService;
import com.easyprufung.backend.Project.Waitlist;
import com.easyprufung.backend.User.Service.SubscriptionService;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.User;
import com.easyprufung.backend.Utils.Constants;
import org.dom4j.rule.Mode;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.URL;

@Service
public class ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Autowired
    ProjectsRepository projectRepository;

    @Autowired
    WaitlistsRepository waitlistsRepository;

    @Autowired
    OpenAIService openAIService;

    @Autowired
    UserService userService;

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    MixpanelService mixpanelService;

    @Autowired
    DomainCheckerService domainCheckerService;

    @Autowired
    SocialMediaCheckerService socialMediaCheckerService;

    @Autowired
    private TerminalCommandService terminalCommandService;

    @Autowired
    private ContactFormsRepository contactFormsRepository;

    @Autowired
    private UpvotesRepository upvotesRepository;

    //Methods
    public ProjectDTO createProject(ProjectDTO projectDTO, User user)  {
        try {
            String uuid = UUID.randomUUID().toString();
            projectDTO.setUuid(uuid);
            String projectPath = ProjectHelper.createUniqueDirectory(Constants.projectsectory, ProjectHelper.encodeUUIDToShortString(uuid));
            projectDTO.setPath(projectPath);
            String chatDirectoryPath = ProjectHelper.createProjectSubDirectory(projectPath, ProjectConstants.chatDir);
            ProjectHelper.createFile(chatDirectoryPath, ProjectConstants.projectValidationFileName);
            var chatRequest = openAIService.getProjectValidation(projectDTO.getPrompt());
            ObjectMapper objectMapper = new ObjectMapper();
            String chatMessagesAsJson = objectMapper.writeValueAsString(chatRequest);
            ProjectHelper.saveFileContent(chatMessagesAsJson,chatDirectoryPath , ProjectConstants.projectValidationFileName);
            var assistantContent= chatRequest.getMessages().stream().filter(item-> ( item.getRole().equals("assistant"))).reduce((first, second) -> second);
            Object assistantContentAsObject = null;
            if(assistantContent.isPresent())
            {
                var content = assistantContent.get().getContent().stream().findFirst();
                if(content.isPresent()){
                    var customChatResponse = objectMapper.readValue(content.get().getText(), CustomChatResponse.class);
                    assistantContentAsObject = customChatResponse.getSystem_content();
                    ModelMapper modelMapper = new ModelMapper();
                    var projectValidation = modelMapper.map(assistantContentAsObject, ProjectValidation.class);
                    projectDTO.setDescription(projectValidation.getDescription().getLongDescription());
                    projectDTO.setShortDescription(projectValidation.getDescription().getShortDescription());
                    projectDTO.setLandingPageDescription(projectValidation.getDescription().getLandingPageDescription());
                }
            }
            projectDTO.setIsApproved(true);
            projectDTO.setUpvote(1);
            projectDTO.setSource("internal");
            Project project = saveProject(projectDTO);
            user.addProject(project);
            userService.updateUser(user);
            ModelMapper modelMapper = new ModelMapper();
            projectDTO = modelMapper.map(project, ProjectDTO.class);
            return projectDTO;
        }
        catch (Exception exc){
            logger.error(exc.getMessage());
        }
        return null;
    }

    public ProjectDTO createExternalProject(ProjectDTO projectDTO, User user)  {
        try {
            String uuid = UUID.randomUUID().toString();
            projectDTO.setUuid(uuid);
            String projectPath = ProjectHelper.createUniqueDirectory(Constants.projectsectory, ProjectHelper.encodeUUIDToShortString(uuid));
            projectDTO.setPath(projectPath);
            projectDTO.setIsApproved(false);
            projectDTO.setUpvote(1);
            projectDTO.setSource("external");
            Project project = saveProject(projectDTO);
            user.addProject(project);
            userService.updateUser(user);
            ModelMapper modelMapper = new ModelMapper();
            projectDTO = modelMapper.map(project, ProjectDTO.class);
            return projectDTO;
        }
        catch (Exception exc){
            logger.error(exc.getMessage());
        }
        return null;
    }

    public Project saveProject(ProjectDTO projectDTO) {
        ModelMapper modelMapper = new ModelMapper();
        Date date = new Date();
        if(projectDTO.getCreatedDate() == null)
            projectDTO.setCreatedDate(new Timestamp(date.getTime()));
        projectDTO.setUpdatedDate(new Timestamp(date.getTime()));
        Project project = modelMapper.map(projectDTO, Project.class);
        Project savedProject = projectRepository.save(project);
        return projectRepository.save(savedProject);
    }

    public Boolean deleteProject(Project projectToDelete, User user) {
        try {
            user.getProjects().removeIf(project -> project.getUuid() == projectToDelete.getUuid());
            userService.updateUser(user);
            if(projectToDelete.getLogoUrl() != null && projectToDelete.getLogoUrl().length() > 0) {
                IconHelper.deleteLogo(projectToDelete.getUuid());
            }
            return true;
        }
        catch (Exception e){
            logger.error(e.getMessage());
        }
        return false;
    }

    public Project saveProject(Project project) {
        Date date = new Date();
        if(project.getCreatedDate() == null)
            project.setCreatedDate(new Timestamp(date.getTime()));
        project.setUpdatedDate(new Timestamp(date.getTime()));
        return projectRepository.save(project);
    }

    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    public ProjectDTO updateProject(ProjectDTO projectDTO, Project project) {
        try {
            project.setIsPublic(projectDTO.getIsPublic());
            project.setCountry(projectDTO.getCountry());
            project.setLatitude(projectDTO.getLatitude());
            project.setLongitude(projectDTO.getLongitude());
            project =  saveProject(project);
            ModelMapper modelMapper = new ModelMapper();
            projectDTO= modelMapper.map(project, ProjectDTO.class);
        }
        catch (Exception e){
            logger.error(e.getMessage());
        }
        return projectDTO;
    }

    public ProjectDTO getProjectByUUID(Project project) throws JsonProcessingException {
        try{
            if(project.getTempUrl() != null && project.getTempUrl().length() > 0 && (project.getIsWebsiteUp() == null || !project.getIsWebsiteUp())){
                Boolean isUp = DomainHelper.isWebsiteUp(project.getTempUrl());
                project.setIsWebsiteUp(isUp);
                project = saveProject(project);
            }
            if(project.getDnsVerificationToken() == null)
            {
                project.setDnsVerificationToken("verify-"+DomainHelper.GenerateDNSToken());
                project = saveProject(project);
            }
            if(project.getUpvote() == null)
            {
                project.setUpvote(1);
                project = saveProject(project);
            }
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        ModelMapper modelMapper = new ModelMapper();
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        return projectDTO;
    }

    public void saveProjectDescription(ProjectDTO projectDTO){
        var project= projectRepository.findByUUID(projectDTO.getUuid());
        project.setDescription(projectDTO.getDescription());
        saveProject(project);
    }

    public ProjectDTO getProjectValidation(Project project) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        var chatHistoryAsJson = ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.chatDir).toString(),ProjectConstants.projectValidationFileName);
        ChatRequest chatRequest = objectMapper.readValue(chatHistoryAsJson, ChatRequest.class);

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
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        projectDTO.setValidationData(assistantContentAsObject);
        return projectDTO;
    }

    public ProjectDTO saveProjectName(ProjectDTO projectDTO){
        var project= projectRepository.findByUUID(projectDTO.getUuid());
        project.setName(projectDTO.getName());
        project = saveProject(project);
        ModelMapper modelMapper = new ModelMapper();
        projectDTO = modelMapper.map(project, ProjectDTO.class);
        return  projectDTO;
    }

    public ProjectDTO getProjectName(Project project) throws JsonProcessingException {
        ChatRequest chatRequest = null;
        ObjectMapper objectMapper = new ObjectMapper();
        var chatHistoryAsJson = ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.chatDir).toString(),ProjectConstants.projectNameFileName);
        if(chatHistoryAsJson != "" && chatHistoryAsJson.length() >0)
            chatRequest = objectMapper.readValue(chatHistoryAsJson, ChatRequest.class);

        Object assistantContentAsObject = null;
        if(chatRequest == null)
        {
            chatRequest = openAIService.getProjectName(project.getDescription());
            String chatMessagesAsJson = objectMapper.writeValueAsString(chatRequest);
            ProjectHelper.saveFileContent(chatMessagesAsJson,Paths.get(project.getPath(),ProjectConstants.chatDir).toString() , ProjectConstants.projectNameFileName);
        }
        var assistantContent= chatRequest.getMessages().stream().filter(item-> ( item.getRole().equals("assistant"))).reduce((first, second) -> second);
        var content = assistantContent.get().getContent().stream().findFirst();
        if(content.isPresent()){
            var customChatResponse = objectMapper.readValue(content.get().getText(), CustomChatResponse.class);
            assistantContentAsObject = customChatResponse.getSystem_content();
            if(project.getName() == null){
                ArrayList<String> list = (ArrayList<String>) assistantContentAsObject;
                project.setName(list.get(0));
                project = saveProject(project);
            }
        }
        ModelMapper modelMapper = new ModelMapper();
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        projectDTO.setNameData(assistantContentAsObject);
        return projectDTO;
    }

    public List<Domain> checkDomainsAvailability(String projectName) {
        try {
            projectName = projectName.replaceAll("\\s+", "");
            var domains = DomainHelper.generateDomains(projectName.toLowerCase());
            return domainCheckerService.checkDomains(domains);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<Social> checkSocialsAvailability(String projectName) {
        try {
            projectName = projectName.replaceAll("\\s+", "");
            return socialMediaCheckerService.checkSocialMediaProfiles(projectName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public ProjectDTO saveProjectLogo(ProjectDTO projectDTO){
        var project= projectRepository.findByUUID(projectDTO.getUuid());
        project.setIconConfiguration(projectDTO.getIconConfiguration());
        String logoPath = IconHelper.saveLogoContent(projectDTO.getUuid(), projectDTO.getSvgIcon());
        project.setLogoUrl(logoPath);
        project = saveProject(project);
        ModelMapper modelMapper = new ModelMapper();
        projectDTO = modelMapper.map(project, ProjectDTO.class);
        return  projectDTO;
    }

    public ProjectDTO deleteProjectLogo(Project project){
        IconHelper.deleteLogo(project.getUuid());
        project.setIconConfiguration(null);
        project.setLogoUrl(null);
        project = saveProject(project);
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(project, ProjectDTO.class);
    }

    public ProjectDTO getProjectLogoSvgContent(Project project) throws JsonProcessingException {
        /*ChatRequest chatRequest = null;

        ObjectMapper objectMapper = new ObjectMapper();
        var chatHistoryAsJson = ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.chatDir).toString(),ProjectConstants.projectIconFileName);
        if(chatHistoryAsJson != "" && chatHistoryAsJson.length() >0)
            chatRequest = objectMapper.readValue(chatHistoryAsJson, ChatRequest.class);

        Object assistantContentAsObject = null;
        if(chatRequest == null)
        {
            chatRequest = openAIService.getProjectIcon(project.getPrompt());
            String chatMessagesAsJson = objectMapper.writeValueAsString(chatRequest);
            ProjectHelper.saveFileContent(chatMessagesAsJson,Paths.get(project.getPath(),ProjectConstants.chatDir).toString() , ProjectConstants.projectIconFileName);
        }
        var assistantContent= chatRequest.getMessages().stream().filter(item-> ( item.getRole().equals("assistant"))).reduce((first, second) -> second);
        var content = assistantContent.get().getContent().stream().findFirst();
        if(content.isPresent()){
            var fixedJson = IconHelper.fixNewlinesInJson(content.get().getText());
            var customChatResponse = objectMapper.readValue(fixedJson, CustomChatResponse.class);
            assistantContentAsObject = customChatResponse.getSystem_content();
            if(project.getSvgIcon() == null){
                project.setSvgIcon(assistantContentAsObject.toString());
                project = saveProject(project);
            }
        }*/
        ModelMapper modelMapper = new ModelMapper();
        var projectDTO = modelMapper.map(project, ProjectDTO.class);

        if(project.getLogoUrl().isEmpty())
            return projectDTO;
        var iconData = IconHelper.getSvgContent(IconHelper.geLogoContent(project.getUuid()));
        projectDTO.setIconData(iconData);
        return projectDTO;
    }


    public ProjectDTO createProjectLandingPage(Project project, String templateName, List<String> features) throws JsonProcessingException {
        ChatRequest chatRequest = null;
        project.setIsBuilding(true);
        project = saveProject(project);
        ObjectMapper objectMapper = new ObjectMapper();
        /*if(templateName.equals("landing-page-empty")) {
            templateName = openAIService.getProjectSelectedTemplate(project.getDescription());
        }*/

        //Create landing page dir
        createLandingPageDirectory(project.getPath(), templateName);

        var templateContent = ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.landingPageDir,"/build/").toString() , ProjectConstants.indexHtmlFileName);
        var chatRequestStringPair = openAIService.createProjectLandingPage(project.getDescription(), templateContent, templateName, project.getName(), project.getIconConfiguration(), project.getLandingPageDescription(), features, project.getUuid(), project.getPath());
        chatRequest = chatRequestStringPair.getFirst();
        String chatMessagesAsJson = objectMapper.writeValueAsString(chatRequest);
        ProjectHelper.saveFileContent(chatMessagesAsJson,Paths.get(project.getPath(),ProjectConstants.chatDir).toString() , ProjectConstants.projectLandingPageFileName);

        var assistantContent= chatRequest.getMessages().stream().filter(item-> ( item.getRole().equals("assistant"))).reduce((first, second) -> second);
        var content = assistantContent.get().getContent().stream().findFirst();
        if(content.isPresent()){
            var layoutContent = chatRequestStringPair.getSecond();
            String encodedUuid=ProjectHelper.encodeUUIDToShortString(project.getUuid());
            String logoPath = "https://app.easyprufung.com/resources/logo/" +encodedUuid+".svg";
            layoutContent = layoutContent.replace("/img/logo.svg",logoPath);
            ProjectHelper.saveFileContent(layoutContent,Paths.get(project.getPath(),ProjectConstants.landingPageDir,"/build/").toString() , ProjectConstants.indexHtmlFileName);
            ProjectHelper.saveFileContent(IconHelper.geLogoContent(project.getUuid()),Paths.get(project.getPath(),ProjectConstants.landingPageDir,"/build/img").toString() , ProjectConstants.logoFileName);

            //run npm i
            //Path landingPageDirPath = Paths.get(project.getPath(), ProjectConstants.landingPageDir);
            //String installOutput = terminalCommandService.executeCommand("npm install --legacy-peer-deps", landingPageDirPath.toAbsolutePath().toString());

            // Run "npm run build" if "npm i" succeeds
                /*if (!installOutput.contains("Error")) {
                    String buildOutput = terminalCommandService.executeBuildCommand("npm run build", landingPageDirPath.toAbsolutePath().toString());
                    if(buildOutput.length() >0){
                        project.setBuildError(buildOutput);
                    }
                    //delete modules
                    ProjectHelper.deleteDirectory(project.getPath(), ProjectConstants.landingPageDir+"/node_modules");
                }*/
        }
        if(activeProfile.equals("debug")){
            project.setIsWebsiteUp(true);
            project.setTempUrl("http://localhost:8081");
        }else {
            String landingpageUrl = "https://"+ProjectHelper.encodeUUIDToShortString(project.getUuid())+".easyprufung.com";
            Boolean isUp = DomainHelper.isWebsiteUp(landingpageUrl);
            project.setIsWebsiteUp(isUp);
            project.setTempUrl(landingpageUrl);
        }
        project.setIsBuilding(false);
        project = saveProject(project);

        ModelMapper modelMapper = new ModelMapper();
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        return projectDTO;
    }

    public ProjectDTO getProjectLandingPage(Project project){
        var content= ProjectHelper.getFileContent(Paths.get(project.getPath(),ProjectConstants.landingPageDir,"/build/").toString() , ProjectConstants.indexHtmlFileName);
        ModelMapper modelMapper = new ModelMapper();
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        projectDTO.setIndexHtmlContent(content);
        return projectDTO;
    }

    public Boolean saveProjectLandingPage(ProjectDTO projectDTO){
        ProjectHelper.saveFileContent(projectDTO.getIndexHtmlContent(), Paths.get(projectDTO.getPath(),ProjectConstants.landingPageDir,"/build/").toString() , ProjectConstants.indexHtmlFileName);
        return true;
    }

    public Path downloadProjectLandingPageCode(Project project, String type)
    {
        try {
            Path zipPath = ProjectHelper.zipDirectory(Paths.get(project.getPath(), ProjectConstants.landingPageDir,(type.equals("build")?"build":"")));
            return zipPath;
        }
        catch (Exception ex){

        }
        return null;
    }

    public ProjectDTO deleteProjectLandingPage(Project project) throws IOException {
        ProjectHelper.deleteDirectory(project.getPath(), ProjectConstants.landingPageDir);
        ProjectHelper.deleteFile(Paths.get(project.getPath(),ProjectConstants.chatDir).toString() , ProjectConstants.projectLandingPageFileName);
        project.setTempUrl("");
        project = saveProject(project);
        ModelMapper modelMapper = new ModelMapper();
        var projectDTO = modelMapper.map(project, ProjectDTO.class);
        return projectDTO;
    }


    void createLandingPageDirectory(String projectPath, String template){
        try {
            ProjectHelper.createProjectSubDirectory(projectPath, ProjectConstants.landingPageDir + "/build/img");

            if (!template.equals("landing-page-empty")) {
                Resource resource = new ClassPathResource("templates/" + template + ".html");
                Path destPath = Path.of(projectPath, ProjectConstants.landingPageDir + File.separator + "build" + File.separator + "index.html");
                Files.createDirectories(destPath.getParent());
                try (InputStream is = resource.getInputStream()) {
                    Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    void createLandingPageTemplate(String projectPath, String template){
       try {
           Path landingPageDirectoryPath = Paths.get(ProjectHelper.createProjectSubDirectory(projectPath, ProjectConstants.landingPageDir));
           Resource resource = new ClassPathResource("templates/"+template+".zip");
           try (InputStream inputStream = resource.getInputStream(); ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
               ZipEntry entry;

               while ((entry = zipInputStream.getNextEntry()) != null) {
                   try {
                       Path entryPath = landingPageDirectoryPath.resolve(entry.getName());
                       if (entry.isDirectory()) {
                           Files.createDirectories(entryPath);
                       } else {
                           Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                       }
                       zipInputStream.closeEntry();
                   }
                   catch (Exception e){
                   }
               }
           }
           catch (IOException e) {
               logger.error(e.getMessage());
           }
       }
       catch (IOException e) {
           logger.error(e.getMessage());
       }
    }

    public boolean  addDomain(Project project, DomainDTO domainDTO, String userEmail)  {
        try {
            String path = Paths.get(project.getPath()).toAbsolutePath().toString();
            var result = terminalCommandService.executeAddDomainCommand(path, domainDTO.getDomain(), ProjectHelper.encodeUUIDToShortString(project.getUuid()),userEmail);
            if(result) {
                project.setUrl("https://"+domainDTO.getDomain());
                updateProject(project);
                return true;
            }
            else
                return false;
        }
        catch (Exception exc){
            logger.error(exc.getMessage());
        }
        return false;
    }

    public boolean deleteDomain(Project project)  {
        try {
            URL url = new URL(project.getUrl());
            String domain = url.getHost();
           var result = terminalCommandService.executeDeleteDomainCommand(domain);
            if(result) {
                project.setUrl("");
                updateProject(project);
                return true;
            }
            else
                return false;
        }
        catch (Exception exc){
            logger.error(exc.getMessage());
        }
        return false;
    }

    public boolean addUserToWaitList(WaitlistDTO waitlistDTO)  {
        try {
            var project = projectRepository.findByUUID(waitlistDTO.getProjectId());
            Waitlist waitlist = new Waitlist();
            waitlist.setEmail(waitlistDTO.getEmail());
            Date date = new Date();
            waitlist.setJoinedDate(new Timestamp(date.getTime()));
            waitlist = waitlistsRepository.save(waitlist);
            project.addWaitList(waitlist);
            updateProject(project);
            return true;
        }
        catch (Exception exc){
            logger.error(exc.getMessage());
        }
        return false;
    }

    public boolean addContactForm(ContactFormDTO contactFormDTO)  {
        try {
            var project = projectRepository.findByUUID(contactFormDTO.getProjectId());
            ContactForm contactForm = new ContactForm();
            contactForm.setName(contactFormDTO.getName());
            contactForm.setEmail(contactFormDTO.getEmail());
            contactForm.setMessage(contactFormDTO.getMessage());
            Date date = new Date();
            contactForm.setSubmittedDate(new Timestamp(date.getTime()));
            contactForm = contactFormsRepository.save(contactForm);
            project.addContactForm(contactForm);
            updateProject(project);
            return true;
        }
        catch (Exception exc){
            logger.error(exc.getMessage());
        }
        return false;
    }



    public List<Project> getProjects() {
        return projectRepository.findAll();
    }
    public Page<Project> getProjects(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    public List<PublicProjectDTO> getPublicProjects() {
        try {
            ModelMapper modelMapper = new ModelMapper();
            var projects =  projectRepository.findAllPublicProjects();
            Type listType = new TypeToken<List<PublicProjectDTO>>() {}.getType();
            return modelMapper.map(projects, listType);
        }
        catch (Exception exc){
            logger.error(exc.getMessage());
        }
        return new ArrayList<>();
    }

    public long getProjectsCount() {
        return projectRepository.count();
    }

    public boolean upvoteProject(User user, String uuid) {
        Project project = projectRepository.findByUUID(uuid);
        if (upvotesRepository.existsByUserAndProject(user, project)) {
            // Already upvoted
            return false;
        }
        Upvote upvote = new Upvote();
        upvote.setUser(user);
        upvote.setProject(project);
        project.setUpvote(project.getUpvote() +1);
        projectRepository.save(project);
        Date date = new Date();
        upvote.setCreatedDate(new Timestamp(date.getTime()));
        upvotesRepository.save(upvote);
        return true;
    }

    public long getProjectUpvotes(Project project) {
        return upvotesRepository.countByProject(project);
    }
}

