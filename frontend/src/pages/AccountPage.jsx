import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, money } from "../api";
import Alert from "../components/Alert";

export default function AccountPage() {
  const { number } = useParams();
  const [account, setAccount] = useState(null);
  const [statement, setStatement] = useState(null);
  const [page, setPage] = useState(0);
  const [amount, setAmount] = useState("");
  const [msg, setMsg] = useState({});
  const [busy, setBusy] = useState(false);

  const load = () => {
    api(`/accounts/${number}`).then(setAccount).catch((e) => setMsg({ error: e.message }));
    api(`/accounts/${number}/transactions?page=${page}&size=10`).then(setStatement).catch(() => {});
  };
  useEffect(load, [number, page]);

  async function move(kind) {
    setBusy(true);
    setMsg({});
    try {
      const t = await api(`/accounts/${number}/${kind}`, { method: "POST", body: { amount } });
      setMsg({ ok: `${kind === "deposit" ? "Deposited" : "Withdrew"} ${money(t.amount)}. Ref ${t.reference}` });
      setAmount("");
      setPage(0);
      load();
    } catch (e) {
      setMsg({ error: e.fields?.amount ? `Amount ${e.fields.amount}` : e.message });
    } finally {
      setBusy(false);
    }
  }

  async function close() {
    if (!confirm("Close this account? This can't be undone.")) return;
    try {
      setAccount(await api(`/accounts/${number}/close`, { method: "POST" }));
    } catch (e) {
      setMsg({ error: e.message });
    }
  }

  if (!account) return <div className="space-y-4"><Link to="/" className="text-sm text-indigo-600">← Accounts</Link><Alert>{msg.error}</Alert></div>;
  const active = account.status === "ACTIVE";

  return (
    <div className="space-y-6">
      <Link to="/" className="text-sm text-indigo-600">← Accounts</Link>
      <div className="card flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-sm text-slate-500">{account.type} · <span className="font-mono">{account.accountNumber}</span> · {account.status}</p>
          <p className="text-3xl font-bold">{money(account.balance)}</p>
        </div>
        {account.status !== "CLOSED" && <button className="btn-ghost text-sm" onClick={close}>Close account</button>}
      </div>

      {active && (
        <div className="card space-y-3">
          <label className="label" htmlFor="amount">Amount</label>
          <div className="flex flex-wrap gap-2">
            <input id="amount" className="input max-w-xs" inputMode="decimal" placeholder="0.00"
              value={amount} onChange={(e) => setAmount(e.target.value)} />
            <button className="btn" disabled={busy || !amount} onClick={() => move("deposit")}>Deposit</button>
            <button className="btn-ghost" disabled={busy || !amount} onClick={() => move("withdraw")}>Withdraw</button>
          </div>
        </div>
      )}
      <Alert>{msg.error}</Alert>
      <Alert kind="ok">{msg.ok}</Alert>

      <div className="card overflow-x-auto p-0">
        <h2 className="px-5 pt-4 font-semibold">Transactions</h2>
        <table className="mt-2 w-full text-sm">
          <thead className="text-left text-slate-500">
            <tr>
              <th className="px-5 py-2 font-medium">Date</th>
              <th className="px-2 py-2 font-medium">Details</th>
              <th className="px-2 py-2 text-right font-medium">Amount</th>
              <th className="px-5 py-2 text-right font-medium">Balance</th>
            </tr>
          </thead>
          <tbody>
            {statement?.content.map((t) => (
              <tr key={t.reference} className="border-t border-slate-100">
                <td className="whitespace-nowrap px-5 py-2 text-slate-500">{new Date(t.createdAt).toLocaleString()}</td>
                <td className="px-2 py-2">
                  {t.type === "TRANSFER" ? (t.direction === "DEBIT" ? `To ${t.counterparty}` : `From ${t.counterparty}`) : t.type.toLowerCase()}
                  {t.description && <span className="text-slate-400"> · {t.description}</span>}
                </td>
                <td className={`px-2 py-2 text-right font-medium ${t.direction === "CREDIT" ? "text-green-600" : ""}`}>
                  {t.direction === "CREDIT" ? "+" : "−"}{money(t.amount)}
                </td>
                <td className="px-5 py-2 text-right">{money(t.balanceAfter)}</td>
              </tr>
            ))}
            {statement?.content.length === 0 && (
              <tr><td colSpan="4" className="px-5 py-6 text-center text-slate-500">No transactions yet</td></tr>
            )}
          </tbody>
        </table>
        {statement && statement.totalPages > 1 && (
          <div className="flex justify-between border-t border-slate-100 px-5 py-3 text-sm">
            <button className="btn-ghost py-1" disabled={statement.first} onClick={() => setPage(page - 1)}>Newer</button>
            <span className="text-slate-500">Page {page + 1} of {statement.totalPages}</span>
            <button className="btn-ghost py-1" disabled={statement.last} onClick={() => setPage(page + 1)}>Older</button>
          </div>
        )}
      </div>
    </div>
  );
}
