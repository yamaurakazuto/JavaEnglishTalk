// TalkOnバックエンドを起動するエントリーポイントです。起動責務を一箇所に集約するために独立させています。

package com.kazuto.talkon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class TalkOnApplication {
  public static void main(String[] args) {
    SpringApplication.run(TalkOnApplication.class, args);
  }
}
