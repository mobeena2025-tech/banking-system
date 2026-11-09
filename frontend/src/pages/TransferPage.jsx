import { useEffect, useState } from "react";
import { api, money } from "../api";
import Alert from "../components/Alert";

const newKey = () => (crypto.randomUUID ? crypto.randomUUID() : String(Date.now()));

export default function TransferPage() {
  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState({ fromAccount: "", toAccount: "", amount: "", description: "" });
  // One key per transfer attempt: a double click or retry can't send the money twice.
  const [key, setKey] = useState(newKey);
  const [msg, setMsg] = useState({});
  const [fields, setFields] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api("/accounts").then((all) => {
      const active = all.filter((a) => a.status === "ACTIVE");
      setAccounts(active);
      if (active[0]) setForm((f) => ({ ...f, fromAccount: active[0].accountNumber }));
    });
  }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setMsg({});
    setFields({});
    try {
      const t = await api("/transfers", { method: "POST", body: { ...form, idempotencyKey: key } });
      setMsg({ ok: `Sent ${money(t.amount)} to ${t.toAccount}. Reference ${t.reference}` });
      setForm({ ...form, toAccount: "", amount: "", description: "" });
      setKey(newKey());
      setAccounts(await api("/accounts").then((all) => all.filter((a) => a.status === "ACTIVE")));
    } catch (err) {
      setMsg({ error: err.message });
      setFields(err.fields);
    } finally {
      setBusy(false);
    }
  }

  if (accounts.length === 0) {
    return <div className="card text-slate-500">You need an active account to send money.</div>;
  }

  return (
    <form onSubmit={submit} className="card mx-auto max-w-lg space-y-4">
      <h1 className="text-xl font-semibold">Send money</h1>
      <div>
        <label className="label" htmlFor="from">From</label>
        <select id="from" className="input" value={form.fromAccount} onChange={set("fromAccount")}>
          {accounts.map((a) => (
            <option key={a.accountNumber} value={a.accountNumber}>
              {a.type} · {a.accountNumber} · {money(a.balance)}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label className="label" htmlFor="to">To account number</label>
        <input id="to" className="input font-mono" placeholder="12 digits" required value={form.toAccount} onChange={set("toAccount")} />
        {fields.toAccount && <p className="mt-1 text-xs text-red-600">Account {fields.toAccount}</p>}
      </div>
      <div>
        <label className="label" htmlFor="amt">Amount</label>
        <input id="amt" className="input" inputMode="decimal" placeholder="0.00" required value={form.amount} onChange={set("amount")} />
        {fields.amount && <p className="mt-1 text-xs text-red-600">Amount {fields.amount}</p>}
      </div>
      <div>
        <label className="label" htmlFor="desc">Note (optional)</label>
        <input id="desc" className="input" maxLength={255} value={form.description} onChange={set("description")} />
      </div>
      <Alert>{msg.error}</Alert>
      <Alert kind="ok">{msg.ok}</Alert>
      <button className="btn w-full" disabled={busy}>{busy ? "Sending…" : "Send"}</button>
    </form>
  );
}
