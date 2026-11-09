import { createContext, useContext, useEffect, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { api, auth } from "./api";
import Layout from "./components/Layout";
import AuthPage from "./pages/AuthPage";
import Dashboard from "./pages/Dashboard";
import AccountPage from "./pages/AccountPage";
import TransferPage from "./pages/TransferPage";
import AdminPage from "./pages/AdminPage";

const UserContext = createContext(null);
export const useUser = () => useContext(UserContext);

export default function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(auth.token));

  useEffect(() => {
    if (!auth.token) return;
    api("/me").then(setUser).catch(() => (auth.token = null)).finally(() => setLoading(false));
  }, []);

  const login = ({ token, user }) => {
    auth.token = token;
    setUser(user);
  };
  const logout = () => {
    auth.token = null;
    setUser(null);
  };

  if (loading) return <p className="p-8 text-slate-500">Loading…</p>;

  return (
    <UserContext.Provider value={{ user, login, logout }}>
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/" /> : <AuthPage mode="login" />} />
        <Route path="/register" element={user ? <Navigate to="/" /> : <AuthPage mode="register" />} />
        <Route element={user ? <Layout /> : <Navigate to="/login" />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/accounts/:number" element={<AccountPage />} />
          <Route path="/transfer" element={<TransferPage />} />
          <Route path="/admin" element={user?.role === "ADMIN" ? <AdminPage /> : <Navigate to="/" />} />
        </Route>
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </UserContext.Provider>
  );
}
