/**
 * 予期しないReact例外を操作可能な案内へ置き換えます。白画面のまま復帰できなくなることを防ぐ境界です。
 */

import { Component, ErrorInfo, ReactNode } from "react";

type ErrorBoundaryState = {
  failed: boolean;
};

export class ErrorBoundary extends Component<
  { children: ReactNode },
  ErrorBoundaryState
> {
  state: ErrorBoundaryState = { failed: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { failed: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Unexpected React error", error, info);
  }

  render() {
    if (this.state.failed) {
      return (
        <main className="fatal-error" role="alert">
          <h1>画面を表示できませんでした</h1>
          <p>ページを再読み込みするか、ダッシュボードへ戻ってください。</p>
          <a href="/">ダッシュボードへ戻る</a>
        </main>
      );
    }
    return this.props.children;
  }
}
