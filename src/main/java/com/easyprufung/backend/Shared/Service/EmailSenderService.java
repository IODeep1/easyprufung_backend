package com.easyprufung.backend.Shared.Service;

import com.easyprufung.backend.Shared.Models.Email;
import freemarker.template.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailSenderService {
    private static final Logger logger = LoggerFactory.getLogger(EmailSenderService.class);

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    Configuration fmConfiguration;

    @Async
    public void sendEmail(Email mail, String template) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage,true);
            try {
                // Set the "From" field with a display name
                message.setFrom(InternetAddress.parse("EasyPrüfung <contact@easyprufung.com>")[0]);
            } catch (Exception e) {
                message.setFrom(mail.getFrom());
               
            }
            message.setTo(mail.getTo());
            message.setText(geContentFromTemplate(mail.getModel(), template),true);
            message.setSubject(mail.getSubject());
            mailSender.send(message.getMimeMessage());
        } catch (MessagingException e) {
            logger.error(e.getMessage());
           
        }
    }


    public String geContentFromTemplate(Map<String, Object> model, String template) {
        StringBuffer content = new StringBuffer();

        try {
            content.append(FreeMarkerTemplateUtils.processTemplateIntoString(fmConfiguration.getTemplate(template), model));
        } catch (Exception e) {
            logger.error(e.getMessage());
           
        }
        return content.toString();
    }
}