package com.easyprufung.backend.Mixpanel.Service;

import com.easyprufung.backend.User.User;
import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MixpanelService {
    /*MixpanelAPI mixpanel;
    MessageBuilder messageBuilder;

    MixpanelService(){
         mixpanel = new MixpanelAPI();
         messageBuilder = new MessageBuilder("3e4ffef45755edc953fce110ca570a51");
    }
    public void IdentifyUsers(User user, String ipAddress){
        try {
            String country = "Unknown";
            if(!ipAddress.isEmpty()){

                String geoApiUrl = "https://ipapi.co/" + ipAddress + "/country_name/"; // Replace with your GeoIP API URL
                RestTemplate restTemplate = new RestTemplate();
                country = restTemplate.getForObject(geoApiUrl, String.class);
            }

            JSONObject props = new JSONObject();
            props.put("$name", user.getFirstname() +" "+ user.getLastname());
            props.put("$email", user.getEmail());
            props.put("mp_country_code", country);
            props.put("plan", "no plan");
            JSONObject update = messageBuilder.set(user.getUuid(), props);
            mixpanel.sendMessage(update);
        }
        catch (Exception exc){
        }
    }

    public void UpdateUserPlan(String userUuid, String plan){
        try {
            JSONObject props = new JSONObject();
            props.put("plan", plan);
            JSONObject update = messageBuilder.set(userUuid, props);
            mixpanel.sendMessage(update);
        }
        catch (Exception exc){
        }
    }

    public void TrackEvent(String userId, String eventName, String eventKey, String eventValue){
        try {
            JSONObject props = new JSONObject();
            props.put(eventKey, eventValue);
            JSONObject sentEvent = messageBuilder.event(userId, eventName, props);
            ClientDelivery delivery = new ClientDelivery();
            delivery.addMessage(sentEvent);
            MixpanelAPI mixpanel = new MixpanelAPI();
            mixpanel.deliver(delivery);
        }
        catch (Exception exc){
        }
    }*/
}
