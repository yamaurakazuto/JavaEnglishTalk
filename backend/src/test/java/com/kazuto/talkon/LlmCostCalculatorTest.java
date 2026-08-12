// Token単価と為替から概算円額を計算できることを検証します。料金表示の桁や単位の回帰を防ぐテストです。

package com.kazuto.talkon;

import static org.assertj.core.api.Assertions.assertThat;

import com.kazuto.talkon.conversation.LlmCostCalculator;
import org.junit.jupiter.api.Test;

class LlmCostCalculatorTest {
  @Test
  void estimatesConversationCostAsMicroYen() {
    var calculator = new LlmCostCalculator(0.40, 1.60, 160);

    assertThat(calculator.estimateMicros(1_200, 80)).isEqualTo(97_280);
  }
}
