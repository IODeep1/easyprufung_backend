package com.smartpruefung.backend.partner;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
@RestControllerAdvice(basePackages="com.smartpruefung.backend.partner")
public class PartnerNoStore implements ResponseBodyAdvice<Object> {
 public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> type){return true;}
 public Object beforeBodyWrite(Object body,MethodParameter p,MediaType type,Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest request,ServerHttpResponse response){response.getHeaders().setCacheControl("no-store");return body;}
}
