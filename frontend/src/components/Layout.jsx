import { NavLink, Outlet } from "react-router-dom";
import { useUser } from "../App";

const link = ({ isActive }) =>
  `rounded-md px-3 py-1.5 text-sm font-medium ${isActive ? "bg-indigo-50 text-indigo-700" : "text-slate-600 hover:text-slate-900"}`;

export default function Layout() {
  const { user, logout } = useUser();
  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-2 px-4 py-3">
          <span className="mr-4 text-lg font-bold text-indigo-700">🏦 Smart Banking</span>
          <NavLink to="/" end className={link}>Accounts</NavLink>
          <NavLink to="/transfer" className={link}>Transfer</NavLink>
          {user.role === "ADMIN" && <NavLink to="/admin" className={link}>Admin</NavLink>}
          <div className="ml-auto flex items-center gap-3 text-sm">
            <span className="text-slate-500">{user.name}</span>
            <button onClick={logout} className="btn-ghost py-1">Log out</button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
