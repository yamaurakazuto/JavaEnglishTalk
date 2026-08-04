package com.kazuto.talkon.feedback;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
 public record FeedbackData(@NotBlank String summary,@NotNull @Size(min=1,max=5) List<@NotBlank String> strengths,
 @NotNull @Size(min=1,max=5) List<@Valid Improvement> improvements,@NotNull List<@Valid Correction> corrections,@NotBlank String overallComment){
 public record Improvement(@NotBlank String original,@NotBlank String reason,@NotBlank String suggestion){}
 public record Correction(@NotBlank String original,@NotBlank String corrected,@NotBlank String explanation){}
}
