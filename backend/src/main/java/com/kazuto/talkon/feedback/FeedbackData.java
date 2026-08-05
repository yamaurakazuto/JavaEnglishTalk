// LLMが返す構造化フィードバックと検証制約を定義します。不正なAI出力を保存前に拒否するためのDTOです。

package com.kazuto.talkon.feedback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FeedbackData(
    @NotBlank String summary,
    @NotNull @Size(min = 1, max = 5) List<@NotBlank String> strengths,
    @NotNull @Size(min = 1, max = 5) List<@Valid Improvement> improvements,
    @NotNull List<@Valid Correction> corrections,
    @NotBlank String overallComment) {
  public record Improvement(
      @NotBlank String original, @NotBlank String reason, @NotBlank String suggestion) {}

  public record Correction(
      @NotBlank String original, @NotBlank String corrected, @NotBlank String explanation) {}
}
