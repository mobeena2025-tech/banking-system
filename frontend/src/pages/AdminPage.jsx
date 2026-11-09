import { useEffect, useState } from "react";
import { api, money } from "../api";
import Alert from "../components/Alert";

export default function AdminPage() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState(null);

  const load = () => api(`/admin/accounts?page=${page}&size=20`).then(setData).catch((e) => setError(e.message));
  useEffect(() => { load(); }, [page]);

  async function setStatus(number, status) {
    setError(null);
    try {
      await api(`/admin/accounts/${number}/status`, { method: "PATCH", body: { status } });
      load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">All accounts</h1>
      <Alert>{error}</Alert>
      <div className="card overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="text-left text-slate-500">
            <tr>
              <th className="px-5 py-2 font-medium">Account</th>
              <th className="px-2 py-2 font-medium">Owner</th>
              <th className="px-2 py-2 font-medium">Status</th>
              <th className="px-2 py-2 text-right font-medium">Balance</th>
              <th className="px-5 py-2" />
            </tr>
          </thead>
          <tbody>
            {data?.content.map((a) => (
              <tr key={a.accountNumber} className="border-t border-slate-100">
                <td className="px-5 py-2 font-mono">{a.accountNumber}<div className="font-sans text-xs text-slate-400">{a.type}</div></td>
                <td className="px-2 py-2">{a.ownerName}<div className="text-xs text-slate-400">{a.ownerEmail}</div></td>
                <td className="px-2 py-2">{a.status}</td>
                <td className="px-2 py-2 text-right">{money(a.balance)}</td>
                <td className="px-5 py-2 text-right">
                  {a.status === "ACTIVE" && <button className="btn-ghost py-1 text-xs" onClick={() => setStatus(a.accountNumber, "FROZEN")}>Freeze</button>}
                  {a.status === "FROZEN" && <button className="btn-ghost py-1 text-xs" onClick={() => setStatus(a.accountNumber, "ACTIVE")}>Unfreeze</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {data && data.totalPages > 1 && (
          <div className="flex justify-between border-t border-slate-100 px-5 py-3 text-sm">
            <button className="btn-ghost py-1" disabled={data.first} onClick={() => setPage(page - 1)}>Previous</button>
            <span className="text-slate-500">Page {page + 1} of {data.totalPages}</span>
            <button className="btn-ghost py-1" disabled={data.last} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        )}
      </div>
    </div>
  );
}
