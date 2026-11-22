package com.smartpruefung.backend.activity;

import java.net.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Only explicitly trusted proxy peers can supply country headers. No DNS lookups. */
@Component
public class ActivityClientAddress {
 private final List<Network> proxies=new ArrayList<>();
 public ActivityClientAddress(@Value("${ACTIVITY_TRUSTED_PROXIES:127.0.0.1/32,::1/128}") String trusted) {
  for(String item:trusted.split(",")) if(!item.isBlank()) proxies.add(new Network(item.trim()));
 }
 public boolean trusted(InetAddress address) {return address!=null && proxies.stream().anyMatch(n->n.contains(address));}
 public boolean trusted(HttpServletRequest request) {return trusted(parse(request.getRemoteAddr()));}
 public static InetAddress parse(String input) {
  if(input==null || input.length()>64) return null;
  String value=input.trim();
  try {
   if(value.contains(":")) {
    if(!value.matches("[0-9a-fA-F:.]+")) return null;
    return InetAddress.getByName(value); // Numeric IPv6 only; hostnames cannot reach this call.
   }
   String[] parts=value.split("\\.",-1);
   if(parts.length!=4) return null;
   byte[] bytes=new byte[4];
   for(int i=0;i<4;i++) {
    if(!parts[i].matches("[0-9]{1,3}") || (parts[i].length()>1 && parts[i].startsWith("0"))) return null;
    int n=Integer.parseInt(parts[i]);if(n>255) return null;bytes[i]=(byte)n;
   }
   return InetAddress.getByAddress(bytes);
  } catch(IllegalArgumentException | UnknownHostException ignored) {return null;}
 }
 private static final class Network {
  final byte[] address;final int bits;
  Network(String cidr) {
   String[] parts=cidr.split("/",-1);InetAddress ip=parse(parts[0]);
   if(ip==null || parts.length>2) throw new IllegalArgumentException("Invalid ACTIVITY_TRUSTED_PROXIES entry");
   address=ip.getAddress();bits=parts.length==2?Integer.parseInt(parts[1]):address.length*8;
   if(bits<0 || bits>address.length*8) throw new IllegalArgumentException("Invalid trusted proxy prefix");
  }
  boolean contains(InetAddress ip) {
   byte[] other=ip.getAddress();if(other.length!=address.length) return false;
   for(int i=0;i<bits;i++) if((other[i/8] & (1<<(7-i%8)))!=(address[i/8] & (1<<(7-i%8)))) return false;
   return true;
  }
 }
}
