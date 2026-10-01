// LLMが返す教材向けフィードバックを定義します。無意味な修正を除外し、保存前に内容を検証するDTOです。

package com.talkon.feedback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** FeedbackDataに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public record FeedbackData(
    @NotBlank String summary,
    @NotNull @Size(min = 1, max = 5) List<@NotBlank String> strengths,
    @NotNull @Size(max = 5) List<@Valid Correction> corrections,
    @NotNull @Size(max = 5) List<@NotBlank String> vocabularyTips,
    @NotBlank String overallComment) {

  public FeedbackData {
    corrections =
        corrections == null
            ? null
            : corrections.stream()
                .filter(item -> !item.original().trim().equalsIgnoreCase(item.corrected().trim()))
                .toList();
  }

  /** Correctionに関する責務をまとめるデータ構造です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
  public record Correction(
      @NotBlank String original,
      @NotBlank String corrected,
      @NotBlank String reasonJa,
      @NotBlank String alternative,
      @NotNull FeedbackCategory category) {}
}
