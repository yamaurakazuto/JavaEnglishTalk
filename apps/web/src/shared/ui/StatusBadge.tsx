import { Conversation } from "../api";

export function StatusBadge({ status }: { status: Conversation["status"] }) {
  return (
    <span className={`status-badge ${status.toLowerCase()}`}>
      {status === "ACTIVE" ? "進行中" : "終了"}
    </span>
  );
}
