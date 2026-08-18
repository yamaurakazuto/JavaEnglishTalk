// AI英文を日本語へ翻訳するアプリケーション境界です。会話応答生成と翻訳の責務を分離します。

package com.talkon.conversation;

public interface TranslationService {
  String translate(String englishText);

  String translateWord(String word, String sentence);
}
