package com.smartpruefung.backend.activity;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import static com.smartpruefung.backend.Admin.Console.Service.AdminManagementService.row;

@Service
public class ActivityService {
 public static final List<String> TYPES=List.of("EXAM_STARTED","EXAM_SUBMITTED","PRICING_VIEWED","STRIPE_LINK_CLICKED");
 private final JdbcTemplate db;
 public ActivityService(JdbcTemplate db) { this.db=db; }
 // Exam calls join the existing business transaction: rolled-back work emits no event.
 public void exam(String type,String user,UUID session) {
  insert(type+":"+session,user,type,session.toString(),null,"exam",false);
 }
 public void browser(String eventId,String user,String type,String plan,String source) {
  if (!List.of("PRICING_VIEWED","STRIPE_LINK_CLICKED").contains(type))
   throw new IllegalArgumentException("Unsupported browser event");
  if ("STRIPE_LINK_CLICKED".equals(type) && !List.of("b1","b1_unlimited").contains(plan == null ? "" : plan))
   throw new IllegalArgumentException("Unknown payment plan");
  insert("browser:"+user+":"+eventId,user,type,null,plan,source,true);
 }
 private void insert(String id,String user,String type,String session,String plan,String source,boolean deduplicate) {
  try {
   db.update("insert into user_activity (id,user_uuid,event_type,occurred_at,session_id,plan,source) values (?,?,?,?,?,?,?)",
    id,user,type,LocalDateTime.ofInstant(Instant.now(),ZoneOffset.UTC),session,plan,source);
  } catch (DuplicateKeyException e) { if (!deduplicate) throw e; }
 }
 private Filter filter(String from,String to,String type,String user) {
  Instant start=parse(from,Instant.now().minus(Duration.ofDays(30)));
  Instant end=parse(to,Instant.now());
  if (!start.isBefore(end) || Duration.between(start,end).toDays()>366)
   throw new IllegalArgumentException("Choose a date range of up to 366 days; to is exclusive");
  String sql=" where a.occurred_at >= ? and a.occurred_at < ?";
  List<Object> args=new ArrayList<>(List.of(LocalDateTime.ofInstant(start,ZoneOffset.UTC),LocalDateTime.ofInstant(end,ZoneOffset.UTC)));
  if (type!=null && !type.isBlank()) {
   if(!TYPES.contains(type)) throw new IllegalArgumentException("Unknown action");
   sql+=" and a.event_type = ?"; args.add(type);
  }
  if(user!=null && !user.isBlank()) { sql+=" and a.user_uuid = ?"; args.add(user); }
  return new Filter(sql,args);
 }
 private Instant parse(String input,Instant fallback) {
  try {return input==null || input.isBlank()?fallback:Instant.parse(input);}
  catch (java.time.format.DateTimeParseException e) {throw new IllegalArgumentException("Dates must use ISO UTC timestamps");}
 }
 private static class Filter {
  String sql; List<Object> args;
  Filter(String sql,List<Object> args){this.sql=sql;this.args=args;}
  Object[] values(){return args.toArray();}
 }
 public Object summary(String from,String to,String type,String user) {
  Filter f=filter(from,to,type,user);
  var counts=db.queryForList("select a.event_type as action, count(*) as total, count(distinct a.user_uuid) as users from user_activity a"+f.sql+" group by a.event_type",f.values());
  var trend=db.queryForList("select cast(a.occurred_at as date) as day, a.event_type as action, count(*) as total from user_activity a"+f.sql+" group by cast(a.occurred_at as date), a.event_type order by day",f.values());
  Long total=db.queryForObject("select count(*) from user_activity a"+f.sql,Long.class,f.values());
  Long users=db.queryForObject("select count(distinct a.user_uuid) from user_activity a"+f.sql,Long.class,f.values());
  return row("total",total,"users",users,"counts",counts,"trend",trend);
 }
 public Object list(String from,String to,String type,String user,int page,int size,boolean grouped) {
  if(page<0 || page>100000 || size<1 || size>100) throw new IllegalArgumentException("Invalid pagination");
  Filter f=filter(from,to,type,user);
  String join=" from user_activity a left join users u on a.user_uuid=u.uuid";
  String group=grouped?" group by a.user_uuid, u.email, a.event_type":"";
  String fields=grouped?"a.user_uuid as `userUuid`,u.email,a.event_type as action,count(*) as total,max(a.occurred_at) as `lastActivity`":
   "a.id,a.user_uuid as `userUuid`,u.email,a.event_type as action,a.occurred_at as `occurredAt`,a.session_id as `sessionId`,a.plan,a.source";
  String order=grouped?" order by total desc,a.user_uuid,a.event_type":" order by a.occurred_at desc,a.id desc";
  Long total=grouped?db.queryForObject("select count(*) from (select a.user_uuid,a.event_type from user_activity a"+f.sql+" group by a.user_uuid,a.event_type) counts",Long.class,f.values()):
   db.queryForObject("select count(*) from user_activity a"+f.sql,Long.class,f.values());
  List<Object> args=new ArrayList<>(f.args);args.add(size);args.add(page*size);
  return row("content",db.query("select "+fields+join+f.sql+group+order+" limit ? offset ?", (rs,n) -> {
   Map<String,Object> item=new LinkedHashMap<>();
   for(int i=1;i<=rs.getMetaData().getColumnCount();i++) {
    Object value=rs.getObject(i);
    if(value instanceof Timestamp) value=((Timestamp)value).toLocalDateTime().toInstant(ZoneOffset.UTC).toString();
    String key=rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT);
    switch(key) {case "useruuid": key="userUuid"; break; case "occurredat": key="occurredAt"; break;
     case "sessionid": key="sessionId"; break; case "lastactivity": key="lastActivity"; break; default: break;}
    item.put(key,value);
   }
   return item;
  },args.toArray()),
   "totalElements",total,"totalPages",(total+size-1)/size,"number",page,"size",size);
 }
}
