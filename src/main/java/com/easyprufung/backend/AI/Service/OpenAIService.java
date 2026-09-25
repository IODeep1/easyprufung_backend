package com.easyprufung.backend.AI.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.commons.lang.Pair;
import com.easyprufung.backend.AI.Helper.OpenAiAssistantHelper;
import com.easyprufung.backend.AI.Models.ChatRequest;
import com.easyprufung.backend.AI.Models.ChatResponse;
import com.easyprufung.backend.AI.Models.Message;
import com.easyprufung.backend.Project.Constants.ProjectConstants;
import com.easyprufung.backend.Project.Helper.ProjectHelper;
import com.easyprufung.backend.Utils.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment env;

    private static final String ENDPOINT  = "https://spaiprod.openai.azure.com/openai/deployments/gpt-4.1/chat/completions?api-version=2025-01-01-preview";

    public OpenAIService(@Value("${easyprufung.openai.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public ChatRequest getProjectValidation(String prompt){
        ChatRequest request = null;
        try {
            String systemPrompt = OpenAiAssistantHelper.GetProjectValidationSystemPrompt();
            String chatgptModel = env.getProperty("easyprufung.openai.model");
            double chatgptTemperature = Double.parseDouble(env.getProperty("easyprufung.openai.temperature"));
            int chatgptMaxTokens = Integer.parseInt(env.getProperty("easyprufung.openai.maxtokens"));
            request = new ChatRequest(chatgptModel,systemPrompt , prompt , chatgptTemperature, chatgptMaxTokens, false);
            ChatResponse response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
            String result =  response.getChoices().get(0).getMessage().getContent();
            result = Parser.removeLinesContainingBackticks(result);
            List<Message.Content> assitantcontent = new ArrayList<>();
            assitantcontent.add(new Message.Content("text", result));
            request.getMessages().add(new Message("assistant", assitantcontent));
            return request;
        }
        catch (Exception e) {
            logger.error(e.getMessage());

        }
        return request;
    }

    public ChatRequest getUpdatedProjectValidation(String prompt, ChatRequest request){
        try {
            List<Message.Content> usercontent = new ArrayList<>();
            usercontent.add(new Message.Content("text",prompt));
            request.getMessages().add(new Message("user", usercontent));
            ChatResponse response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
            String result =  response.getChoices().get(0).getMessage().getContent();
            result = Parser.removeLinesContainingBackticks(result);
            List<Message.Content> assitantcontent = new ArrayList<>();
            assitantcontent.add(new Message.Content("text", result));
            request.getMessages().add(new Message("assistant", assitantcontent));
            return request;
        }
        catch (Exception e) {
            logger.error(e.getMessage());

        }
        return request;
    }

    public ChatRequest getProjectName(String prompt){
        ChatRequest request = null;
        try {
            String systemPrompt = OpenAiAssistantHelper.GetProjectNameSystemPrompt();
            String chatgptModel = env.getProperty("easyprufung.openai.model");
            double chatgptTemperature = Double.parseDouble(env.getProperty("easyprufung.openai.temperature"));
            int chatgptMaxTokens = Integer.parseInt(env.getProperty("easyprufung.openai.maxtokens"));
            request = new ChatRequest(chatgptModel,systemPrompt , prompt , chatgptTemperature, chatgptMaxTokens, false);
            ChatResponse response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
            String result =  response.getChoices().get(0).getMessage().getContent();
            result = Parser.removeLinesContainingBackticks(result);
            List<Message.Content> assitantcontent = new ArrayList<>();
            assitantcontent.add(new Message.Content("text", result));
            request.getMessages().add(new Message("assistant", assitantcontent));
            return request;
        }
        catch (Exception e) {
            logger.error(e.getMessage());

        }
        return request;
    }

    public ChatRequest getUpdatedProjectName(String prompt, ChatRequest request){
        try {
            List<Message.Content> usercontent = new ArrayList<>();
            usercontent.add(new Message.Content("text",prompt));
            request.getMessages().add(new Message("user", usercontent));
            ChatResponse response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
            String result =  response.getChoices().get(0).getMessage().getContent();
            result = Parser.removeLinesContainingBackticks(result);
            List<Message.Content> assitantcontent = new ArrayList<>();
            assitantcontent.add(new Message.Content("text", result));
            request.getMessages().add(new Message("assistant", assitantcontent));
            return request;
        }
        catch (Exception e) {
            logger.error(e.getMessage());

        }
        return request;
    }

    public ChatRequest getProjectIcon(String prompt){
        ChatRequest request = null;
        try {
            String systemPrompt = OpenAiAssistantHelper.GetProjectIconSystemPrompt();
            String chatgptModel = env.getProperty("easyprufung.openai.model");
            double chatgptTemperature = Double.parseDouble(env.getProperty("easyprufung.openai.temperature"));
            int chatgptMaxTokens = Integer.parseInt(env.getProperty("easyprufung.openai.maxtokens"));
            request = new ChatRequest(chatgptModel,systemPrompt , prompt , chatgptTemperature, chatgptMaxTokens, false);
            ChatResponse response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
            String result =  response.getChoices().get(0).getMessage().getContent();
            result = Parser.removeLinesContainingBackticks(result);
            List<Message.Content> assitantcontent = new ArrayList<>();
            assitantcontent.add(new Message.Content("text", result));
            request.getMessages().add(new Message("assistant", assitantcontent));
            return request;
        }
        catch (Exception e) {
            logger.error(e.getMessage());

        }
        return request;
    }

    public ChatRequest getUpdatedProjectIcon(String prompt, ChatRequest request){
        try {
            List<Message.Content> usercontent = new ArrayList<>();
            usercontent.add(new Message.Content("text",prompt));
            request.getMessages().add(new Message("user", usercontent));
            ChatResponse response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
            String result =  response.getChoices().get(0).getMessage().getContent();
            result = Parser.removeLinesContainingBackticks(result);
            List<Message.Content> assitantcontent = new ArrayList<>();
            assitantcontent.add(new Message.Content("text", result));
            request.getMessages().add(new Message("assistant", assitantcontent));
            return request;
        }
        catch (Exception e) {
            logger.error(e.getMessage());

        }
        return request;
    }


    public String getProjectSelectedTemplate(String prompt){
        try {
            ChatRequest request = null;
            String chatgptModel = env.getProperty("easyprufung.openai.model");
            double chatgptTemperature = Double.parseDouble(env.getProperty("easyprufung.openai.temperature"));
            int chatgptMaxTokens = Integer.parseInt(env.getProperty("easyprufung.openai.maxtokens"));
            request = new ChatRequest(chatgptModel,OpenAiAssistantHelper.GetProjectTemplateSelectorSystemPrompt() , prompt , chatgptTemperature, chatgptMaxTokens, false);
            ChatResponse response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
            return response.getChoices().get(0).getMessage().getContent();
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
        return"landing-page-empty";
    }

    public Pair<ChatRequest, String> createProjectLandingPage(String prompt, String templateContent, String templateName, String appName, String svgContent, String landingPageDescription, List<String> features, String projectUuid, String projectPath){
        ChatRequest request = null;
        String finalResult = "";
        String chatgptModel = env.getProperty("easyprufung.openai.model");
        double chatgptTemperature = Double.parseDouble(env.getProperty("easyprufung.openai.temperature"));
        int chatgptMaxTokens = Integer.parseInt(env.getProperty("easyprufung.openai.maxtokens"));
        try {
            String systemPrompt = "";
            if(templateName.contains("landing-page-empty"))
                systemPrompt = OpenAiAssistantHelper.GetProjectLandingPageFromEmptyTemplateSystemPrompt(templateContent, appName, svgContent,landingPageDescription, features, projectUuid);
            else
                systemPrompt = OpenAiAssistantHelper.GetProjectLandingPageSystemPrompt(templateContent, appName, svgContent,landingPageDescription, features, projectUuid);

            /*ChatResponse response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
            String result =  response.getChoices().get(0).getMessage().getContent();*/
            request = new ChatRequest(chatgptModel,systemPrompt , prompt , chatgptTemperature, chatgptMaxTokens ,true);
            String result =  chatStream(request, projectPath);
            result = Parser.removeLinesContainingBackticks(result);
            finalResult = result;
            List<Message.Content> assitantcontent = new ArrayList<>();
            assitantcontent.add(new Message.Content("text", result));
            request.getMessages().add(new Message("assistant", assitantcontent));


            /*while(!result.contains("</html>")){

                usercontent = new ArrayList<>();
                usercontent.add(new Message.Content("text","Continue your prior response. IMPORTANT: Immediately begin from where you left off without any interruptions.\n" +
                        "Do not repeat any content, including artifact and action tags"));
                request.getMessages().add(new Message("user", usercontent));

                response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
                result =  response.getChoices().get(0).getMessage().getContent();
                result = Parser.removeLinesContainingBackticks(result);
                finalResult = finalResult+ result;

                assitantcontent = new ArrayList<>();
                assitantcontent.add(new Message.Content("text",result));
                request.getMessages().add(new Message("assistant", assitantcontent));
            }*/
            return  new Pair<>(request,finalResult);
        }
        catch (Exception e) {
            logger.error(e.getMessage());

        }
        return new Pair<>(request,finalResult);
    }


    public Pair<ChatRequest, String> getUpdatedProjectLandingPage(String prompt, ChatRequest request){
        String finalResult = "";
        try {
            List<Message.Content> usercontent = new ArrayList<>();
            usercontent.add(new Message.Content("text",prompt));
            request.getMessages().add(new Message("user", usercontent));

            ChatResponse response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
            String result =  response.getChoices().get(0).getMessage().getContent();
            result = Parser.removeLinesContainingBackticks(result);
            finalResult = result;
            List<Message.Content> assitantcontent = new ArrayList<>();
            assitantcontent.add(new Message.Content("text", result));
            request.getMessages().add(new Message("assistant", assitantcontent));

            /*while(!result.contains("</html>") || !result.contains("export default Layout;")){

                usercontent = new ArrayList<>();
                usercontent.add(new Message.Content("text","Continue your prior response. IMPORTANT: Immediately begin from where you left off without any interruptions.\n" +
                        "Do not repeat any content, including artifact and action tags"));
                request.getMessages().add(new Message("user", usercontent));

                response = restTemplate.postForObject(env.getProperty("easyprufung.ai.endpoint"), request, ChatResponse.class);
                result =  response.getChoices().get(0).getMessage().getContent();
                result = Parser.removeLinesContainingBackticks(result);
                finalResult = finalResult+ result;
                assitantcontent = new ArrayList<>();
                assitantcontent.add(new Message.Content("text",result));
                request.getMessages().add(new Message("assistant", assitantcontent));
            }*/
            return  new Pair<>(request,finalResult);
        }
        catch (Exception e) {
            logger.error(e.getMessage());

        }
        return  new Pair<>(request,finalResult);
    }

    public Pair<ChatRequest, String> getUpdatedProjectLandingPageNew(String prompt, String landingPageContent, String projectPath){
        ChatRequest request = null;
        String finalResult = "";
        String chatgptModel = env.getProperty("easyprufung.openai.modelSecond");
        double chatgptTemperature = Double.parseDouble(env.getProperty("easyprufung.openai.temperature"));
        int chatgptMaxTokens = Integer.parseInt(env.getProperty("easyprufung.openai.maxtokens"));
        try {
            String systemPrompt = OpenAiAssistantHelper.GetProjectUpdatedLandingPageSystemPrompt(landingPageContent);
            request = new ChatRequest(chatgptModel,systemPrompt , prompt , chatgptTemperature, chatgptMaxTokens, true);
            ProjectHelper.saveFileContent("",Paths.get(projectPath,ProjectConstants.landingPageDir,"/build/").toString() , ProjectConstants.indexHtmlFileName);
            String result =  chatStream(request, projectPath);
            result = Parser.removeLinesContainingBackticks(result);
            finalResult = result;
            List<Message.Content> assitantcontent = new ArrayList<>();
            assitantcontent.add(new Message.Content("text", result));
            request.getMessages().add(new Message("assistant", assitantcontent));
            return  new Pair<>(request,finalResult);
        }
        catch (Exception e) {
            logger.error(e.getMessage());

        }
        return new Pair<>(request,finalResult);
    }

    public String chatStream(ChatRequest chatRequest, String projectPath) throws Exception {
        chatRequest.setStream(true);
        StringBuilder finalOutput = new StringBuilder();
        Path path = Paths.get(projectPath, ProjectConstants.landingPageDir,"/build/index.html");
        // Open channel for async, non-blocking writing
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
        );
        AtomicLong position = new AtomicLong(0);

        Flux<String> responseFlux = webClient.post()
                .uri("/chat/completions")
                .bodyValue(chatRequest)
                .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n")))
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()).trim())
                .takeUntil(line -> line.equals("[DONE]"))
                .filter(line -> !line.equals("[DONE]"))
                .map(line -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(line);
                        return jsonNode
                                .path("choices").get(0)
                                .path("delta").path("content")
                                .asText("");
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(delta -> !delta.isEmpty());

        // Write each chunk asynchronously and in order
        responseFlux.concatMap(chunk -> {
            byte[] bytes = chunk.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            long pos = position.getAndAdd(bytes.length);
            // Use DataBufferUtils to write non-blocking
            return DataBufferUtils.write(
                    Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(buffer)), channel, pos
            ).then(Mono.fromRunnable(() -> {
                synchronized (finalOutput) {
                    finalOutput.append(chunk);
                }
            }));
        }).then().block(); // Wait for completion

        channel.close();
        return finalOutput.toString();
    }
}
