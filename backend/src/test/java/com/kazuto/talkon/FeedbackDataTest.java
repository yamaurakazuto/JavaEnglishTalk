// Feedbackの無意味な修正除外を検証します。AI出力が教材品質を下げないことを守る単体テストです。

package com.kazuto.talkon;

import static org.assertj.core.api.Assertions.assertThat;

import com.kazuto.talkon.feedback.FeedbackCategory;
import com.kazuto.talkon.feedback.FeedbackData;
import java.util.List;
import org.junit.jupiter.api.Test;

class FeedbackDataTest {
  @Test
  void removesCorrectionWhenOriginalAndCorrectedAreEqual() {
    var feedback =
        new FeedbackData(
            "summary",
            List.of("strength"),
            List.of(
                new FeedbackData.Correction(
                    "I am tired.", "I am tired.", "説明", "I feel tired.", FeedbackCategory.GRAMMAR)),
            List.of(),
            "comment");

    assertThat(feedback.corrections()).isEmpty();
  }
}
