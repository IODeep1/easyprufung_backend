package com.easyprufung.backend.Contact.Service;

import com.easyprufung.backend.Contact.Contact;
import com.easyprufung.backend.Contact.DTO.ContactDTO;
import com.easyprufung.backend.Contact.Repository.ContactsRepository;
import com.easyprufung.backend.Shared.Models.Email;
import com.easyprufung.backend.Shared.Service.EmailSenderService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContactService {


    @Autowired
    ContactsRepository contactsRepository;
    @Autowired
    EmailSenderService emailSenderService;


    //Methods
    public Contact createContact(ContactDTO contactDTO) {
        try {
            ModelMapper modelMapper = new ModelMapper();
            Date date = new Date();
            contactDTO.setCreatedDate(new Timestamp(date.getTime()));
            contactDTO.setUpdatedDate(new Timestamp(date.getTime()));
            SendEmail(contactDTO);
            Contact contact = modelMapper.map(contactDTO, Contact.class);
            return contactsRepository.save(contact);
        }
        catch (Exception e){
            return null;
        }
    }


    void SendEmail(ContactDTO contactDTO){
        try {
            String template = "";
            Email emailToSend = new Email();
            emailToSend.setFrom("contact@easyprufung.com");
            emailToSend.setTo("contact@easyprufung.com");
            Map<String, Object> model = new HashMap<>();
            model.put("firstName", contactDTO.getFirstname());
            model.put("lastName",  contactDTO.getLastname());
            model.put("email",  contactDTO.getEmail());
            model.put("message",  contactDTO.getMessage());
            if(contactDTO.getType().equals("contact-form")){
                emailToSend.setSubject("Contact subject: "+contactDTO.getSubject());
                model.put("subject",  contactDTO.getSubject());
                template = "contact-form-template.flth";
            }
            else {
                emailToSend.setSubject("Pricing contact budget: "+contactDTO.getBudget());
                model.put("budget",  contactDTO.getBudget());
                template = "pricing-contact-form-template.flth";
            }
            emailToSend.setModel(model);
            emailSenderService.sendEmail(emailToSend, template);
        }
        catch (Exception e){
        }
    }
}
