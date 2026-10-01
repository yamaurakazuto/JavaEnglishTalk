import { Link } from "react-router-dom";
import { User } from "../api";

type ShellProps = {
  children: React.ReactNode;
  user: User;
  onLogout: () => void;
};

export function Shell({ children, user, onLogout }: ShellProps) {
  return (
    <>
      <header>
        <Link className="brand" to="/">
          TalkOn
        </Link>
        <nav>
          <Link to="/history">履歴</Link>
          <span>{user.displayName}</span>
          <button className="link" onClick={onLogout}>
            ログアウト
          </button>
        </nav>
      </header>
      <main>{children}</main>
    </>
  );
}
