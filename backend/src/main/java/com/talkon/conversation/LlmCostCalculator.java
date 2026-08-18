// LLMの利用トークンを概算円額へ変換します。料金改定や為替変更をコード変更なしで反映するための計算Serviceです。

package com.talkon.conversation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmCostCalculator {
  private final double inputUsdPerMillion;
  private final double outputUsdPerMillion;
  private final double yenPerUsd;

  public LlmCostCalculator(
      @Value("${app.llm.input-usd-per-million:0.40}") double inputUsdPerMillion,
      @Value("${app.llm.output-usd-per-million:1.60}") double outputUsdPerMillion,
      @Value("${app.llm.yen-per-usd:160}") double yenPerUsd) {
    this.inputUsdPerMillion = inputUsdPerMillion;
    this.outputUsdPerMillion = outputUsdPerMillion;
    this.yenPerUsd = yenPerUsd;
  }

  public long estimateMicros(int inputTokens, int outputTokens) {
    double usd =
        Math.max(0, inputTokens) * inputUsdPerMillion / 1_000_000
            + Math.max(0, outputTokens) * outputUsdPerMillion / 1_000_000;
    return Math.round(usd * yenPerUsd * 1_000_000);
  }
}
