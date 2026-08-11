/**
 * 音声入力の基本操作と利用制限表示を検証します。録音導線が会話画面から失われる回帰を防ぐテストです。
 */

import { render, screen } from "@testing-library/react";
import { VoiceRecorder } from "./VoiceRecorder";

test("録音ボタンと最大録音時間を表示する", () => {
  render(
    <VoiceRecorder
      conversationId={1}
      disabled={false}
      onConversationUpdated={vi.fn()}
      onError={vi.fn()}
    />,
  );

  expect(
    screen.getByRole("button", { name: "🎙️ 音声で話す" }),
  ).toBeInTheDocument();
  expect(screen.getByText(/最大60秒/)).toBeInTheDocument();
});
