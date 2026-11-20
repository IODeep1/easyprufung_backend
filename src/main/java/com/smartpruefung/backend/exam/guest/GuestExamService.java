package com.smartpruefung.backend.exam.guest;

import com.smartpruefung.backend.exam.domain.*;
import com.smartpruefung.backend.exam.dto.*;
import com.smartpruefung.backend.exam.repository.*;
import com.smartpruefung.backend.exam.service.*;
import com.smartpruefung.backend.exam.mapper.ExamViewMapper;
import java.time.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GuestExamService {
 private final GuestExamRepository guests;
 private final ExamSessionRepository sessions;
 private final ExamGenerationService generation;
 private final ExamQueryService queries;
 private final ExamProgressService progress;
 private final ExamAccessService access;
 private final ExamViewMapper mapper;
 private final JdbcTemplate jdbc;
 private final String code;
 private final int limit;
 public GuestExamService(GuestExamRepository guests,ExamSessionRepository sessions,ExamGenerationService generation,ExamQueryService queries,
  ExamProgressService progress,ExamAccessService access,ExamViewMapper mapper,JdbcTemplate jdbc,
  @Value("${smartpruefung.guest.exam-code:TELC_DEUTSCH_B1_WRITTEN}") String code,
  @Value("${smartpruefung.guest.hourly-limit:20}") int limit){
  this.guests=guests;this.sessions=sessions;this.generation=generation;this.queries=queries;this.progress=progress;this.access=access;this.mapper=mapper;this.jdbc=jdbc;this.code=code;this.limit=Math.max(1,limit);
 }
 public static String hash(String input){
  try {byte[] bytes=MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString();}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 @Transactional
 public Map<String,String> create(String client){
  String clientHash=hash(client);
  if(guests.countByClientHashAndCreatedAtAfter(clientHash,Instant.now().minusSeconds(3600))>=limit) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please try again later.");
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  GuestExam guest=new GuestExam();guest.setId(UUID.randomUUID().toString());guest.setTokenHash(hash(token));guest.setClientHash(clientHash);
  guest.setCreatedAt(Instant.now());guest.setExpiresAt(guest.getCreatedAt().plus(Duration.ofDays(30)));guests.save(guest);
  return Map.of("token",token);
 }
 private GuestExam resolve(String token,boolean allowClaimed){
  if(token==null || !token.matches("[A-Za-z0-9_-]{43}")) throw missing();
  GuestExam guest=guests.lockToken(hash(token)).orElseThrow(GuestExamService::missing);
  if(guest.getExpiresAt().isBefore(Instant.now()) || (!allowClaimed && guest.getClaimedUserUuid()!=null)) throw missing();
  return guest;
 }
 private String owner(GuestExam g){return "guest:"+g.getId();}
 @Transactional
 public ExamSessionView start(String token){
  GuestExam guest=resolve(token,false);
  if(guest.getSessionId()!=null) return queries.getSession(guest.getSessionId());
  StartExamRequest request=new StartExamRequest(owner(guest),ExamProvider.TELC,CefrLevel.B1,code);
  ExamSessionView view=generation.start(request);guest.setSessionId(view.getSessionId());return view;
 }
 @Transactional
 public Object state(String token){
  GuestExam guest=resolve(token,false);
  if(guest.getSessionId()==null) return Map.of("finished",false);
  return Map.of("session",queries.getSession(guest.getSessionId()),"progress",progress.get(guest.getSessionId(),owner(guest)),"finished",guest.getFinishedAt()!=null);
 }
 @Transactional
 public ExamProgressView save(String token,ExamProgressView draft){
  GuestExam guest=resolve(token,false);
  if(guest.getFinishedAt()!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Exam is already saved for registration.");
  if(guest.getSessionId()==null) throw missing();
  return progress.save(guest.getSessionId(),owner(guest),draft);
 }
 @Transactional
 public void finish(String token){
  GuestExam guest=resolve(token,false);if(guest.getSessionId()==null) throw missing();
  if(guest.getFinishedAt()==null) guest.setFinishedAt(Instant.now());
 }
 @Transactional
 public ExamSessionView claim(String token,String user){
  GuestExam guest=resolve(token,true);
  if(guest.getSessionId()==null || guest.getFinishedAt()==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Finish the guest exam before claiming it.");
  if(guest.getClaimedUserUuid()!=null){
   if(!user.equals(guest.getClaimedUserUuid())) throw missing();
   return queries.getSession(guest.getSessionId());
  }
  var account=access.lockUser(user);access.requireClaimAccess(account);
  var session=sessions.lockById(guest.getSessionId()).orElseThrow(GuestExamService::missing);
  if(!owner(guest).equals(session.getUserId())) throw missing();
  session.setUserId(user);guest.setClaimedUserUuid(user);
  jdbc.update("update user_activity set user_uuid=? where user_uuid=? and session_id=?",user,owner(guest),session.getId().toString());
  return mapper.toView(session);
 }
 private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Guest exam unavailable. Use the same browser that started the exam.");}
}
