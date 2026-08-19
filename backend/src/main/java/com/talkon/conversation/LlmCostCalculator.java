// LLMの利用トークンを概算円額へ変換します。料金改定や為替変更をコード変更なしで反映するための計算Serviceです。

package com.talkon.conversation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** LlmCostCalculatorに関する責務をまとめるクラスです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
@Component
public class LlmCostCalculator {
  private final double inputUsdPerMillion;
  private final double outputUsdPerMillion;
  private final double yenPerUsd;

  /** LlmCostCalculatorを利用可能な状態で生成します。 必要な依存関係や初期値を生成時にそろえ、不完全な状態を防ぐために必要です。 */
  public LlmCostCalculator(
      @Value("${app.llm.input-usd-per-million:0.40}") double inputUsdPerMillion,
      @Value("${app.llm.output-usd-per-million:1.60}") double outputUsdPerMillion,
      @Value("${app.llm.yen-per-usd:160}") double yenPerUsd) {
    this.inputUsdPerMillion = inputUsdPerMillion;
    this.outputUsdPerMillion = outputUsdPerMillion;
    this.yenPerUsd = yenPerUsd;
  }

  /** estimate microsに関する処理を実行します。 このクラスの責務を一箇所へ保ち、呼び出し側の処理を単純にするために必要です。 */
  public long estimateMicros(int inputTokens, int outputTokens) {
    double usd =
        Math.max(0, inputTokens) * inputUsdPerMillion / 1_000_000
            + Math.max(0, outputTokens) * outputUsdPerMillion / 1_000_000;
    return Math.round(usd * yenPerUsd * 1_000_000);
  }
}
