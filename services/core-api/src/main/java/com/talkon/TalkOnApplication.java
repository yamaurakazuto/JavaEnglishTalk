// TalkOnバックエンドを起動するエントリーポイントです。起動責務を一箇所に集約するために独立させています。

package com.talkon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/** TalkOnApplicationに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class TalkOnApplication {
  /** TalkOnバックエンドを起動します。 Spring Bootの初期化処理を開始するアプリケーションの入口として必要です。 */
  public static void main(String[] args) {
    SpringApplication.run(TalkOnApplication.class, args);
  }
}
