// AI英文を日本語へ翻訳するアプリケーション境界です。会話応答生成と翻訳の責務を分離します。

package com.talkon.conversation;

/** TranslationServiceに関する責務をまとめるインターフェースです。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public interface TranslationService {
  /** 英文全体を日本語へ翻訳します。 会話処理を特定の翻訳APIへ依存させず、翻訳機能を交換可能にするために必要です。 */
  String translate(String englishText);

  /** 英文内の指定単語を文脈に合う日本語へ翻訳します。 単語ツールチップへ文脈を考慮した意味を表示するために必要です。 */
  String translateWord(String word, String sentence);
}
