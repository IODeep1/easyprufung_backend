package com.smartpruefung.backend.partner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Dedicated AES-256-GCM key, supplied by the deployment secret store; never reuse the legacy auth key. */
@Component public class PartnerCrypto {
 private final Environment env;
 public PartnerCrypto(Environment env){this.env=env;}
 private SecretKeySpec key(){try {byte[] k=Base64.getDecoder().decode(env.getRequiredProperty("smartpruefung.partner.encryption-key"));if(k.length!=32)throw new IllegalArgumentException();return new SecretKeySpec(k,"AES");}catch(Exception e){throw new IllegalStateException("Partner payout encryption is not configured.");}}
 public String encrypt(String value){try{byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key(),new GCMParameterSpec(128,iv));byte[] encrypted=c.doFinal(value.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(iv)+"."+Base64.getEncoder().encodeToString(encrypted);}catch(Exception e){throw new IllegalStateException("Unable to protect payout details.");}}
 public String decrypt(String value){try{String[] parts=value.split("\\.");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));return new String(c.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("Unable to read payout details.");}}
}
