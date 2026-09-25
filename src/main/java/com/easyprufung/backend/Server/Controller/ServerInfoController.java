package com.easyprufung.backend.Server.Controller;

import com.easyprufung.backend.EndPoints;
import com.easyprufung.backend.Project.Service.ProjectService;
import com.easyprufung.backend.Server.Controller.DTO.DnsDTO;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.User;
import com.easyprufung.backend.Utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.TXTRecord;

import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@RepositoryRestController
@RequestMapping
@RequiredArgsConstructor
public class ServerInfoController {
    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @GetMapping(path = EndPoints.Public.SERVERINFO)
    public ResponseEntity<?> getServerInfo() {
        try {
            return ResponseEntity.ok(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.DOMAIN_CHECK)
    public ResponseEntity<?> domainCheck(@RequestBody DnsDTO dnsDTO, @RequestHeader("Authorization") String authorizationHeader) {
        try {
            boolean checked = false;
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(dnsDTO.getProjectUuid()))
                    .findFirst();
            if(project.isPresent()){
                String targetIp = "141.95.86.144";
                InetAddress[] addresses = InetAddress.getAllByName(dnsDTO.getDomain());
                boolean found = false;

                for (InetAddress addr : addresses) {
                    if (addr.getHostAddress().equals(targetIp)) {
                        found = true;
                        break;
                    }
                }
                if(found){
                    String dnsName = "_verifytoken." + dnsDTO.getDomain();
                    Lookup lookup = new Lookup(dnsName, Type.TXT);
                    Record[] records = lookup.run();
                    if (records == null) {
                        checked = false;
                    }
                    else {
                        for (Record record : records) {
                            TXTRecord txt = (TXTRecord) record;
                            List<String> strings = txt.getStrings();
                            for (String value : strings) {
                                if (value.equals(project.get().getDnsVerificationToken())) {
                                    checked=  true;
                                }
                            }
                        }
                    }
                }
            }
            return ResponseEntity.ok(checked);
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }
    }
}