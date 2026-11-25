package com.smartpruefung.backend.User.FollowUp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.Admin.Console.Model.AdminUserState;
import com.smartpruefung.backend.Admin.Console.Repository.AdminUserStateRepository;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.mail.Session;
import javax.mail.internet.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@org.springframework.test.context.TestPropertySource(locations="classpath:integration-fixtures.properties")
@SpringBootTest(properties={
 "smartpruefung.email.founder.max-attempts=5",
 "spring.datasource.url=jdbc:h2:mem:founder-follow-up-tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@ActiveProfiles("admin-test")
class FounderFollowUpIntegrationTest {
 static final Instant CREATED=Instant.parse("2026-11-01T12:30:00Z");
 static final Instant DUE=CREATED.plus(Duration.ofDays(2));
 @Autowired FounderFollowUpService service;
 @Autowired FounderFollowUpRepository jobs;
 @Autowired UsersRepository users;
 @Autowired AdminUserStateRepository states;
 @MockBean JavaMailSender smtp;
 @MockBean(name="founderFollowUpClock") Clock clock;
 List<MimeMessage> sent;
 @BeforeEach void setup() throws Exception {
  jobs.deleteAll();sent=new CopyOnWriteArrayList<>();when(clock.instant()).thenReturn(CREATED);
  when(smtp.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
  doAnswer(i -> {MimeMessage m=i.getArgument(0);m.saveChanges();sent.add(m);return null;}).when(smtp).send(any(MimeMessage.class));
 }
 User user(String language) {
  User user=new User();user.setUuid(UUID.randomUUID().toString());user.setEmail(UUID.randomUUID()+"@example.com");
  user.setFirstname("Samira");user.setLastname("Learner");user.setPreferredLanguage(language);
  user.setCreatedDate(Timestamp.from(CREATED));return users.saveAndFlush(user);
 }
 FounderFollowUp enqueue(User user){service.schedule(user);return jobs.findByUserUuid(user.getUuid()).orElseThrow();}
 FounderFollowUp latest(FounderFollowUp job){return jobs.findById(job.getId()).orElseThrow();}

 @Test void waitsFull48HoursAndSendsOnlyOnceAcrossRepeatedPolls(){
  User user=user("en");FounderFollowUp job=enqueue(user);service.schedule(user);
  assertEquals(1,jobs.count());assertEquals(DUE,job.getDueAt());
  var scheduler=new FounderFollowUpScheduler(jobs,service,clock);
  when(clock.instant()).thenReturn(DUE.minusSeconds(1));scheduler.run();assertEquals(0,sent.size());
  when(clock.instant()).thenReturn(DUE);scheduler.run();assertEquals(1,sent.size());
  assertEquals("SENT",latest(job).getStatus());assertEquals(DUE,latest(job).getSentAt());
  scheduler.run();service.deliver(job.getId());assertEquals(1,sent.size());
 }

 @Test void allSixLanguagesArePlainTextWithFounderSenderAndPersonalName() throws Exception {
  Map<String,String> subjects=Map.of("en","How is your B1 preparation going?","de","Wie läuft deine B1-Vorbereitung?",
   "fr","Comment se passe ta préparation au B1 ?","ar","كيف تسير استعداداتك لامتحان B1؟",
   "uk","Як проходить твоя підготовка до B1?","tr","B1 hazırlığın nasıl gidiyor?");
  for(String language:List.of("en","de","fr","ar","uk","tr")){
   User user=user(language);FounderFollowUp job=enqueue(user);when(clock.instant()).thenReturn(DUE);
   service.deliver(job.getId());MimeMessage message=sent.get(sent.size()-1);
   assertEquals(subjects.get(language),message.getSubject());
   assertTrue(message.isMimeType("text/plain"));assertFalse(message.isMimeType("multipart/*"));
   String body=(String)message.getContent();assertTrue(body.contains("Samira"));assertTrue(body.contains("SmartPrüfung"));
   assertFalse(body.contains("[NAME]"));assertFalse(body.contains("<html"));
   assertEquals("Aziz",((InternetAddress)message.getFrom()[0]).getPersonal());
   assertEquals("contact@smartpruefung.com",((InternetAddress)message.getReplyTo()[0]).getAddress());
   assertEquals(user.getEmail(),((InternetAddress)message.getAllRecipients()[0]).getAddress());
  }
  assertEquals(6,sent.size());
 }

 @Test void resolvesLanguageAtDeliveryAndNormalizesLanguageCase() throws Exception {
  User user=user("en");FounderFollowUp job=enqueue(user);
  user.setPreferredLanguage("DE");user.setFirstname("Mina");user.setEmail(UUID.randomUUID()+"@example.com");users.saveAndFlush(user);
  when(clock.instant()).thenReturn(DUE);service.deliver(job.getId());
  assertEquals("Wie läuft deine B1-Vorbereitung?",sent.get(0).getSubject());
  assertTrue(((String)sent.get(0).getContent()).startsWith("Hallo Mina,"));
  assertEquals(user.getEmail(),((InternetAddress)sent.get(0).getAllRecipients()[0]).getAddress());
 }

 @Test void unknownLanguageAndMissingNameUseEnglishFallback() throws Exception {
  User user=user("xx");user.setFirstname(null);users.saveAndFlush(user);FounderFollowUp job=enqueue(user);
  when(clock.instant()).thenReturn(DUE);service.deliver(job.getId());
  assertEquals("How is your B1 preparation going?",sent.get(0).getSubject());
  assertTrue(((String)sent.get(0).getContent()).startsWith("Hi there,"));
 }

 @Test void databaseQueueCanBeReadByANewSchedulerAfterDowntime(){
  User user=user("en");FounderFollowUp job=enqueue(user);
  when(clock.instant()).thenReturn(DUE.plus(Duration.ofDays(3)));
  new FounderFollowUpScheduler(jobs,service,clock).run();
  assertEquals(1,sent.size());assertEquals("SENT",latest(job).getStatus());
 }

 @Test void smtpFailureRetriesLaterAndStopsAfterFiveAttempts() throws Exception {
  FounderFollowUp job=enqueue(user("en"));
  doThrow(new MailSendException("Simulated failure")).when(smtp).send(any(MimeMessage.class));
  when(clock.instant()).thenReturn(DUE);service.deliver(job.getId());
  assertEquals("PENDING",latest(job).getStatus());assertNull(latest(job).getSentAt());
  assertEquals(DUE.plusSeconds(3600),latest(job).getNextAttemptAt());
  service.deliver(job.getId());verify(smtp,times(1)).send(any(MimeMessage.class));
  for(int i=1;i<5;i++){when(clock.instant()).thenReturn(DUE.plusSeconds(i*3600));service.deliver(job.getId());}
  assertEquals("FAILED",latest(job).getStatus());assertEquals(5,latest(job).getAttempts());
  when(clock.instant()).thenReturn(DUE.plus(Duration.ofDays(5)));service.deliver(job.getId());
  verify(smtp,times(5)).send(any(MimeMessage.class));
 }

 @Test void transientFailureThenSuccessDoesNotResend() throws Exception {
  FounderFollowUp job=enqueue(user("en"));
  doThrow(new MailSendException("Temporary outage")).doAnswer(i->{MimeMessage m=i.getArgument(0);m.saveChanges();sent.add(m);return null;})
   .when(smtp).send(any(MimeMessage.class));
  when(clock.instant()).thenReturn(DUE);service.deliver(job.getId());
  when(clock.instant()).thenReturn(DUE.plusSeconds(3600));service.deliver(job.getId());service.deliver(job.getId());
  assertEquals("SENT",latest(job).getStatus());assertEquals(2,latest(job).getAttempts());assertNull(latest(job).getLastError());
  assertEquals(1,sent.size());verify(smtp,times(2)).send(any(MimeMessage.class));
 }

 @Test void skipsDeletedAndDisabledUsersAndDoesNotBackfillExistingAccounts(){
  User deleted=user("en");FounderFollowUp a=enqueue(deleted);users.delete(deleted);users.flush();
  User disabled=user("de");FounderFollowUp b=enqueue(disabled);
  AdminUserState state=new AdminUserState();state.setId(disabled.getId());state.setEnabled(false);states.saveAndFlush(state);
  user("fr"); // Existing user without an enrolled job: must not receive an email.
  when(clock.instant()).thenReturn(DUE.plus(Duration.ofDays(5)));new FounderFollowUpScheduler(jobs,service,clock).run();
  assertEquals("CANCELLED",latest(a).getStatus());assertEquals("CANCELLED",latest(b).getStatus());assertTrue(sent.isEmpty());
 }

 @Test void concurrentWorkersDoNotBothSendTheSameJob() throws Exception {
  FounderFollowUp job=enqueue(user("en"));when(clock.instant()).thenReturn(DUE);
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  doAnswer(i->{entered.countDown();if(!release.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("SMTP test timeout");sent.add(i.getArgument(0));return null;})
   .when(smtp).send(any(MimeMessage.class));
  ExecutorService workers=Executors.newFixedThreadPool(2);
  try {
   Future<?> first=workers.submit(()->service.deliver(job.getId()));assertTrue(entered.await(3,TimeUnit.SECONDS));
   Future<?> second=workers.submit(()->service.deliver(job.getId()));release.countDown();
   first.get(5,TimeUnit.SECONDS);second.get(5,TimeUnit.SECONDS);
   assertEquals(1,sent.size());assertEquals("SENT",latest(job).getStatus());
  }finally{release.countDown();workers.shutdownNow();}
 }
}
