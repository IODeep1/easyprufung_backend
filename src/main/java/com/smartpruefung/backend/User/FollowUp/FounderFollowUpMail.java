package com.smartpruefung.backend.User.FollowUp;

import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Service.EmailLocalization;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.mail.internet.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

/** Synchronous transport: called only by the background job so success can be persisted. */
@Service
public class FounderFollowUpMail {
 private final JavaMailSender smtp;
 private final String from;
 private final String replyTo;
 public FounderFollowUpMail(JavaMailSender smtp,
  @Value("${smartpruefung.email.founder.from-address:contact@smartpruefung.com}") String from,
  @Value("${smartpruefung.email.founder.reply-to:contact@smartpruefung.com}") String replyTo){
  this.smtp=smtp;this.from=from;this.replyTo=replyTo;
 }
 public void send(User user) throws Exception {
  String language=EmailLocalization.normalizeLanguage(user.getPreferredLanguage());
  String base="founder-followup/"+language;
  String body=read(base+".txt");
  Properties subjects=new Properties();
  try(Reader reader=new InputStreamReader(resource("founder-followup/subjects.properties"),StandardCharsets.UTF_8)) {
   subjects.load(reader);
  }
  String subject=subjects.getProperty(language,subjects.getProperty("en"));
  String name=user.getFirstname()==null?"":user.getFirstname().replaceAll("[\\r\\n\\t]+"," ").trim();
  if(name.isBlank()) name="en".equals(language)?"there":"";
  // Empty-name greetings omit the separating space in every translation.
  body=body.replace(" [NAME]",name.isBlank()?"":" "+name).replace("[NAME]",name);
  MimeMessage message=smtp.createMimeMessage();
  MimeMessageHelper helper=new MimeMessageHelper(message,false,"UTF-8");
  helper.setFrom(new InternetAddress(from,"Aziz","UTF-8"));
  helper.setReplyTo(new InternetAddress(replyTo,"Aziz","UTF-8"));
  helper.setTo(user.getEmail());helper.setSubject(subject);
  helper.setText(body,false); // text/plain only: no HTML, multipart layout or tracking pixel
  smtp.send(message);
 }
 private InputStream resource(String name) throws IOException {
  InputStream in=getClass().getClassLoader().getResourceAsStream(name);
  if(in==null) throw new FileNotFoundException("Missing founder email translation");
  return in;
 }
 private String read(String name) throws IOException {
  try(InputStream in=resource(name)){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}
 }
}
