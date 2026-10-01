package com.talkon.speechservice.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final InternalKeyInterceptor internalKey;

  public WebConfig(InternalKeyInterceptor internalKey) {
    this.internalKey = internalKey;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(internalKey).addPathPatterns("/internal/**");
  }
}
