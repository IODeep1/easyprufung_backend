package com.smartpruefung.backend.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.*;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.User.Service.EmailLocalization;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.web.client.RestTemplate;

@org.springframework.test.context.TestPropertySource(locations="classpath:integration-fixtures.properties")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:registration-email-tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "smartpruefung.google.client-secret=unit-test-secret", "smartpruefung.email.workers=1", "smartpruefung.email.queue-capacity=1"
})
@AutoConfigureMockMvc
@ActiveProfiles("admin-test")
class RegistrationEmailIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsersRepository users;
    @Autowired com.smartpruefung.backend.User.FollowUp.FounderFollowUpRepository followUps;
    @Autowired @Qualifier("emailExecutor") ThreadPoolTaskExecutor executor;
    // Keep the real @Async mail service/proxy; block only the external SMTP transport.
    @MockBean JavaMailSender smtp;
    @MockBean RestTemplate google;
    CountDownLatch release;

    @BeforeEach
    void prepare() {
        release = new CountDownLatch(1);
        when(smtp.createMimeMessage()).thenAnswer(invocation ->
                new MimeMessage(Session.getInstance(new Properties())));
    }

    @AfterEach
    void drainWorker() throws Exception {
        release.countDown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while ((executor.getActiveCount() != 0 || !executor.getThreadPoolExecutor().getQueue().isEmpty())
                && System.nanoTime() < deadline) Thread.sleep(10);
        assertEquals(0, executor.getActiveCount());
        assertTrue(executor.getThreadPoolExecutor().getQueue().isEmpty());
    }

    Map<String,Object> signup(String email) {
        return new HashMap<>(Map.of("email",email,"password","Registration-Test-493!",
                "firstname","New","lastname","Learner","preferredLanguage","de"));
    }

    JsonNode register(String path, Object input) throws Exception {
        MvcResult response = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(input))).andExpect(status().isOk()).andReturn();
        JsonNode user = json.readTree(response.getResponse().getContentAsString());
        assertFalse(user.path("token").asText().isBlank());
        assertFalse(user.path("subscription").isNull());
        var job=followUps.findByUserUuid(user.path("uuid").asText()).orElseThrow();
        assertEquals("PENDING",job.getStatus());
        assertEquals(users.findByEmail(user.path("email").asText()).getCreatedDate().toInstant().plus(Duration.ofDays(2)),job.getDueAt());
        return user;
    }

    CountDownLatch slowSmtp(String email, AtomicReference<MimeMessage> sent) {
        CountDownLatch entered = new CountDownLatch(1);
        doAnswer(invocation -> {
            User stored = users.findByEmail(email);
            assertNotNull(stored, "Only saved accounts can receive a welcome email");
            assertNotNull(stored.getSubscription(), "The free subscription must be saved first");
            assertTrue(Thread.currentThread().getName().startsWith("email-delivery-"));
            sent.set(invocation.getArgument(0));
            entered.countDown();
            if (!release.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Test SMTP was not released");
            return null;
        }).when(smtp).send(any(MimeMessage.class));
        return entered;
    }

    @Test
    void passwordRegistrationReturnsWhileSmtpIsStillBlocked() throws Exception {
        String email = UUID.randomUUID()+"@example.com";
        AtomicReference<MimeMessage> sent = new AtomicReference<>();
        CountDownLatch entered = slowSmtp(email,sent);
        JsonNode account = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> register("/public/user/create",signup(email)));
        assertEquals(email,account.path("email").asText());
        assertTrue(entered.await(3,TimeUnit.SECONDS));
        assertEquals(1,release.getCount(), "Response completed without releasing SMTP");
        assertEquals(new EmailLocalization().resolve(EmailLocalization.Kind.WELCOME,"de").getSubject(),sent.get().getSubject());
        verify(smtp,times(1)).send(any(MimeMessage.class));
    }

    @Test
    void googleRegistrationIsNonBlockingAndReturningGoogleUserGetsNoSecondWelcome() throws Exception {
        String email = UUID.randomUUID()+"@example.com";
        when(google.exchange(anyString(),eq(org.springframework.http.HttpMethod.GET),any(org.springframework.http.HttpEntity.class),eq(String.class))).thenReturn(
                org.springframework.http.ResponseEntity.ok(json.writeValueAsString(Map.of("email",email,"email_verified",true,"given_name","New","family_name","Google"))));
        when(google.postForObject(eq("https://oauth2.googleapis.com/token"),any(org.springframework.http.HttpEntity.class),eq(String.class)))
                .thenReturn("{\"access_token\":\"fake-google-test-token\"}");
        AtomicReference<MimeMessage> sent = new AtomicReference<>();
        CountDownLatch entered = slowSmtp(email,sent);
        var input = Map.of("code","test-code","codeVerifier","a".repeat(43),"redirectUri","http://localhost:3006/google/callback","preferredLanguage","fr");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> register("/public/user/google/login",input));
        assertTrue(entered.await(3,TimeUnit.SECONDS));
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> register("/public/user/google/login",input));
        assertEquals(1,release.getCount());
        assertTrue(executor.getThreadPoolExecutor().getQueue().isEmpty());
        assertEquals(new EmailLocalization().resolve(EmailLocalization.Kind.WELCOME,"fr").getSubject(),sent.get().getSubject());
        verify(smtp,times(1)).send(any(MimeMessage.class));
    }

    @Test
    void smtpFailureDoesNotInvalidateNewAccountOrLogin() throws Exception {
        CountDownLatch attempted = new CountDownLatch(1);
        doAnswer(invocation -> { attempted.countDown(); throw new MailSendException("Simulated SMTP outage"); })
                .when(smtp).send(any(MimeMessage.class));
        String email=UUID.randomUUID()+"@example.com";
        register("/public/user/create",signup(email));
        assertTrue(attempted.await(3,TimeUnit.SECONDS));
        register("/public/user/login",Map.of("email",email,"password","Registration-Test-493!"));
    }

    @Test
    void fullMailQueueNeverRunsSmtpOnRegistrationThread() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        doAnswer(invocation -> {
            assertTrue(Thread.currentThread().getName().startsWith("email-delivery-"));
            entered.countDown(); release.await(15,TimeUnit.SECONDS); return null;
        }).when(smtp).send(any(MimeMessage.class));
        register("/public/user/create",signup(UUID.randomUUID()+"@example.com"));
        assertTrue(entered.await(3,TimeUnit.SECONDS));
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> register("/public/user/create",signup(UUID.randomUUID()+"@example.com")));
        assertEquals(1,executor.getThreadPoolExecutor().getQueue().size());
        String third=UUID.randomUUID()+"@example.com";
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> register("/public/user/create",signup(third)));
        assertNotNull(users.findByEmail(third).getSubscription());
        assertEquals(1,executor.getThreadPoolExecutor().getQueue().size());
        assertEquals(1,release.getCount());
        verify(smtp,times(1)).send(any(MimeMessage.class));
    }

    @Test
    void failedRegistrationDoesNotSendWelcome() throws Exception {
        var input=signup(UUID.randomUUID()+"@example.com");input.put("password","weak");
        mvc.perform(post("/public/user/create").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(input))).andExpect(status().isBadRequest());
        verify(smtp,never()).send(any(MimeMessage.class));
        assertTrue(executor.getThreadPoolExecutor().getQueue().isEmpty());
    }
}
