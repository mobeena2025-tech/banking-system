import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, money } from "../api";
import Alert from "../components/Alert";

const badge = {
  ACTIVE: "bg-green-100 text-green-700",
  FROZEN: "bg-amber-100 text-amber-700",
  CLOSED: "bg-slate-200 text-slate-600",
};

export default function Dashboard() {
  const [accounts, setAccounts] = useState(null);
  const [type, setType] = useState("SAVINGS");
  const [error, setError] = useState(null);

  const load = () => api("/accounts").then(setAccounts).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  async function open() {
    setError(null);
    try {
      await api("/accounts", { method: "POST", body: { type } });
      load();
    } catch (e) {
      setError(e.message);
    }
  }

  const total = (accounts || []).filter((a) => a.status !== "CLOSED").reduce((s, a) => s + Number(a.balance), 0);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm text-slate-500">Total balance</p>
          <p className="text-3xl font-bold">{money(total)}</p>
        </div>
        <div className="flex gap-2">
          <select className="input w-auto" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="SAVINGS">Savings</option>
            <option value="CHECKING">Checking</option>
          </select>
          <button className="btn" onClick={open}>Open account</button>
        </div>
      </div>
      <Alert>{error}</Alert>

      {accounts === null ? (
        <p className="text-slate-500">Loading…</p>
      ) : accounts.length === 0 ? (
        <div className="card text-center text-slate-500">No accounts yet. Open one to get started.</div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {accounts.map((a) => (
            <Link key={a.accountNumber} to={`/accounts/${a.accountNumber}`} className="card block hover:border-indigo-300">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-slate-500">{a.type}</span>
                <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${badge[a.status]}`}>{a.status}</span>
              </div>
              <p className="mt-2 text-2xl font-semibold">{money(a.balance)}</p>
              <p className="mt-1 font-mono text-sm text-slate-500">{a.accountNumber}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
