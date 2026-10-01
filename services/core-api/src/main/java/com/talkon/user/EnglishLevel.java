// ユーザーが選択できる英会話難易度を定義します。会話生成へ安全にレベルを渡すための列挙型です。

package com.talkon.user;

/** EnglishLevelに関する責務をまとめる列挙型です。 関連する処理やデータの役割を一箇所へ集約し、呼び出し側との境界を明確にするために必要です。 */
public enum EnglishLevel {
  BEGINNER,
  INTERMEDIATE,
  ADVANCED
}
