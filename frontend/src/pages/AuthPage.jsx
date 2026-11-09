import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import { useUser } from "../App";
import Alert from "../components/Alert";

export default function AuthPage({ mode }) {
  const isLogin = mode === "login";
  const { login } = useUser();
  const [form, setForm] = useState({ name: "", email: "", password: "", phone: "" });
  const [error, setError] = useState(null);
  const [fields, setFields] = useState({});
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setFields({});
    try {
      const body = isLogin
        ? { email: form.email, password: form.password }
        : { ...form, phone: form.phone || null };
      login(await api(isLogin ? "/auth/login" : "/auth/register", { method: "POST", body }));
    } catch (err) {
      setError(err.message);
      setFields(err.fields);
    } finally {
      setBusy(false);
    }
  }

  const field = (key, label, type = "text", props = {}) => (
    <div>
      <label className="label" htmlFor={key}>{label}</label>
      <input id={key} type={type} className="input" value={form[key]} onChange={set(key)} {...props} />
      {fields[key] && <p className="mt-1 text-xs text-red-600">{label} {fields[key]}</p>}
    </div>
  );

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <form onSubmit={submit} className="card w-full max-w-sm space-y-4">
        <div>
          <h1 className="text-2xl font-bold">🏦 Smart Banking</h1>
          <p className="text-sm text-slate-500">{isLogin ? "Log in to your account" : "Create an account"}</p>
        </div>
        <Alert>{error}</Alert>
        {!isLogin && field("name", "Full name", "text", { required: true })}
        {field("email", "Email", "email", { required: true, autoComplete: "email" })}
        {field("password", "Password", "password", {
          required: true,
          autoComplete: isLogin ? "current-password" : "new-password",
        })}
        {!isLogin && field("phone", "Phone (optional)", "tel")}
        <button className="btn w-full" disabled={busy}>{busy ? "Please wait…" : isLogin ? "Log in" : "Sign up"}</button>
        <p className="text-center text-sm text-slate-500">
          {isLogin ? "New here? " : "Already have an account? "}
          <Link className="font-medium text-indigo-600" to={isLogin ? "/register" : "/login"}>
            {isLogin ? "Create an account" : "Log in"}
          </Link>
        </p>
      </form>
    </div>
  );
}
