"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Image from "next/image";
import type { FormEvent, ReactNode } from "react";
import {
  Activity, ArrowDownLeft, ArrowLeftRight, ArrowUpRight, ArrowRight, Bell, BriefcaseBusiness,
  Check, CheckCircle2, ChevronDown, ChevronRight, CreditCard, Download, FileClock, FileText,
  HandCoins, LayoutDashboard, LogOut, Menu, MoreHorizontal, Plus, RefreshCw, Search, Settings,
  ShieldAlert, ShieldCheck, UserRound, Users, Wallet, X, Banknote, LockKeyhole, Sparkles, BadgeCheck,
} from "lucide-react";
import {
  Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import { apiRequest, clearSession, getProfile, login, registerCustomer, readSession, saveSession, type SessionData, ApiError } from "@/lib/api";
import type {
  Account, AuditLog, Customer, FraudAlert, KycReview, Loan, LoanRepayment,
  NotificationItem, Overview, Page, Profile, Staff, Transaction, UserRole,
} from "@/lib/types";

type ViewName = "Dashboard" | "Customers" | "KYC review" | "Accounts" | "Banking" | "Transactions" | "Loans" | "Reports & analytics" | "Staff management" | "Fraud alerts" | "Audit logs" | "Notifications" | "My profile";

type IconType = typeof LayoutDashboard;
type NavItem = { label: ViewName; icon: IconType };

const navByRole: Record<UserRole, NavItem[]> = {
  ADMIN: [
    { label: "Dashboard", icon: LayoutDashboard }, { label: "Customers", icon: Users },
    { label: "KYC review", icon: ShieldCheck }, { label: "Banking", icon: Banknote }, { label: "Transactions", icon: ArrowLeftRight },
    { label: "Loans", icon: BriefcaseBusiness }, { label: "Reports & analytics", icon: Activity },
    { label: "Staff management", icon: UserRound }, { label: "Fraud alerts", icon: ShieldAlert },
    { label: "Audit logs", icon: FileClock }, { label: "Notifications", icon: Bell },
    { label: "My profile", icon: Settings },
  ],
  MANAGER: [
    { label: "Dashboard", icon: LayoutDashboard }, { label: "Customers", icon: Users },
    { label: "KYC review", icon: ShieldCheck }, { label: "Banking", icon: Banknote },
    { label: "Loans", icon: BriefcaseBusiness }, { label: "Notifications", icon: Bell },
    { label: "My profile", icon: Settings },
  ],
  TELLER: [
    { label: "Dashboard", icon: LayoutDashboard }, { label: "Customers", icon: Users },
    { label: "KYC review", icon: ShieldCheck }, { label: "Banking", icon: Banknote },
    { label: "Notifications", icon: Bell }, { label: "My profile", icon: Settings },
  ],
  LOAN_OFFICER: [
    { label: "Dashboard", icon: LayoutDashboard }, { label: "Customers", icon: Users },
    { label: "Loans", icon: BriefcaseBusiness }, { label: "Notifications", icon: Bell },
    { label: "My profile", icon: Settings },
  ],
  CUSTOMER: [
    { label: "Dashboard", icon: LayoutDashboard }, { label: "Accounts", icon: Wallet },
    { label: "Banking", icon: Banknote }, { label: "Transactions", icon: ArrowLeftRight },
    { label: "Loans", icon: BriefcaseBusiness }, { label: "Notifications", icon: Bell },
    { label: "My profile", icon: Settings },
  ],
};

const rolesLabel: Record<UserRole, string> = {
  ADMIN: "Administrator", MANAGER: "Branch manager", TELLER: "Teller",
  LOAN_OFFICER: "Loan officer", CUSTOMER: "Customer",
};
const money = (value?: number | string | null) => new Intl.NumberFormat("en-BD", {
  style: "currency", currency: "BDT", maximumFractionDigits: 2,
}).format(Number(value ?? 0));
const number = (value?: number | null) => new Intl.NumberFormat("en-BD").format(value ?? 0);
const dateTime = (value?: string | null) => value ? new Date(value).toLocaleString("en-BD", { dateStyle: "medium", timeStyle: "short" }) : "—";
const dateOnly = (value?: string | null) => value ? new Date(value).toLocaleDateString("en-BD", { dateStyle: "medium" }) : "—";
const initials = (name?: string | null) => (name || "CB").split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase() || "").join("");

function getError(error: unknown) {
  if (error instanceof ApiError) return error.message;
  return error instanceof Error ? error.message : "Something went wrong. Please try again.";
}

function StatusPill({ value }: { value: string }) {
  const normalized = value.toLowerCase().replaceAll("_", "-");
  const tone = ["active", "verified", "disbursed", "paid-off", "resolved", "completed"].includes(normalized)
    ? "success" : ["pending", "open", "frozen"].includes(normalized) ? "warning" : ["rejected", "failed", "closed"].includes(normalized) ? "danger" : "neutral";
  return <span className={`status-tag status-${tone}`}>{value.replaceAll("_", " ")}</span>;
}

function Panel({ title, subtitle, action, children, className = "" }: {
  title: string; subtitle?: string; action?: ReactNode; children: ReactNode; className?: string;
}) {
  return <section className={`panel data-panel ${className}`}><div className="data-panel-heading"><div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>{action}</div>{children}</section>;
}

function EmptyState({ title = "No records found", detail = "Records will appear here when data is available." }: { title?: string; detail?: string }) {
  return <div className="empty-block"><div className="empty-icon"><FileText size={21} /></div><strong>{title}</strong><span>{detail}</span></div>;
}

function LoadingBlock() {
  return <div className="loading-block"><span className="loader" /> Loading secure banking data…</div>;
}

function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return <label className="form-field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>;
}

function Brand({ light = false }: { light?: boolean }) {
  return <div className={`brand-lockup ${light ? "brand-lockup-light" : ""}`}>
    <Image
      src="/brand/confused-bank-full-logo.svg"
      alt="Confused Bank — Smarter Banking, Made Simple"
      width={1265}
      height={345}
      priority
      className="brand-lockup-logo"
    />
  </div>;
}

function LandingScreen({ onLogin, onRegister }: { onLogin: () => void; onRegister: () => void }) {
  return <main className="hero-screen hero-screen-light">
    <div className="hero-glow hero-glow-one" aria-hidden="true" />
    <div className="hero-glow hero-glow-two" aria-hidden="true" />

    <header className="hero-nav">
      <Brand />
      <nav className="hero-nav-links" aria-label="Main navigation">
        <a href="#experience">The experience</a>
        <a href="#security">Security</a>
        <a href="#features">Features</a>
        <a href="#features">Pricing</a>
      </nav>
      <div className="hero-nav-actions">
        <button className="hero-nav-login" onClick={onLogin}>Sign in</button>
        <button className="hero-nav-cta" onClick={onRegister}>Open an account <ArrowRight size={16} /></button>
      </div>
    </header>

    <section className="hero-main" id="experience">
      <div className="hero-copy">
        <div className="hero-kicker"><span className="hero-kicker-dot" /> A CLEARER WAY TO BANK</div>
        <h1>Make every<br />money move<br /><em>count.</em></h1>
        <p className="hero-description">A smarter banking workspace for everyday customers and the people who keep banking moving. Clear insights, simple actions and a secure place to manage it all.</p>
        <div className="hero-actions">
          <button className="hero-primary" onClick={onLogin}>Explore your banking <ArrowRight size={18} /></button>
          <button className="hero-secondary" onClick={onRegister}>Create an account</button>
        </div>
        <div className="hero-trust-row" id="security">
          <span><ShieldCheck size={18} /> Secure sign-in</span>
          <i />
          <span><LayersIcon /> Role-based access</span>
          <i />
          <span><Activity size={18} /> Clear activity</span>
        </div>
      </div>

      <div className="hero-visual" aria-label="Illustrative Confused Bank dashboard preview">
        <div className="hero-orbit hero-orbit-a" />
        <div className="hero-orbit hero-orbit-b" />
        <div className="hero-preview-card">
          <div className="preview-topline">
            <span className="preview-brand"><span className="preview-brand-icon"><Image src="/brand/confused-bank-emblem.svg" alt="" aria-hidden="true" width={441} height={488} /></span><strong>Confused Bank</strong></span>
            <span className="preview-live"><i /> Live banking</span>
          </div>
          <div className="preview-dashboard-body">
            <div className="preview-account">
              <div className="preview-balance-label">Your financial space</div>
              <div className="preview-total-label">Sample account balance</div>
              <div className="preview-balance">BDT 1,311,025.00</div>
              <div className="preview-demo-note">Illustrative dashboard preview</div>
            </div>
            <div className="preview-chart" aria-hidden="true"><span /><span /><span /><span /></div>
          </div>
          <div className="preview-quick-actions" aria-label="Example banking actions">
            <div className="preview-action"><ArrowUpRight size={22} /><span>Send<br />Money</span></div>
            <div className="preview-action"><ArrowDownLeft size={22} /><span>Receive<br />Money</span></div>
            <div className="preview-action"><FileText size={21} /><span>Pay Bill</span></div>
            <div className="preview-action"><ArrowLeftRight size={22} /><span>Transfer</span></div>
          </div>
        </div>
        <div className="hero-float-card hero-float-security">
          <span className="float-icon"><ShieldCheck size={21} /></span>
          <span><strong>Security first</strong><small>Your account, protected</small></span>
        </div>
        <div className="hero-float-card hero-float-activity">
          <span className="float-icon float-icon-light"><Activity size={22} /></span>
          <span><strong>Clear insights</strong><small>Track, manage, and grow</small></span>
        </div>
      </div>
    </section>

    <section className="hero-feature-row" id="features" aria-label="Platform highlights">
      <article><span className="hero-feature-icon"><Wallet size={23} /></span><div><strong>Everyday banking</strong><small>Accounts and transactions in one view</small></div></article>
      <article><span className="hero-feature-icon"><Activity size={23} /></span><div><strong>Useful insights</strong><small>Understand activity at a glance</small></div></article>
      <article><span className="hero-feature-icon"><LockKeyhole size={23} /></span><div><strong>Protected access</strong><small>Tools tailored to your role</small></div></article>
      <article><span className="hero-feature-icon"><Users size={23} /></span><div><strong>Built for everyone</strong><small>Simple, reliable and easy to use</small></div></article>
    </section>
    <footer className="hero-footer"><span>© {new Date().getFullYear()} Confused Bank</span><span>Smarter banking, made simple <i /> Secure banking workspace</span></footer>
  </main>;
}

function LayersIcon() {
  return <span className="layers-icon" aria-hidden="true"><span /><span /><span /></span>;
}

function LoginScreen({ onLogin, initialMode, onBack }: {
  onLogin: (email: string, password: string) => Promise<void>;
  initialMode: "login" | "register";
  onBack: () => void;
}) {
  const [mode, setMode] = useState<"login" | "register">(initialMode);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [phone, setPhone] = useState("");
  const [nationalId, setNationalId] = useState("");
  const [address, setAddress] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setError(""); setSuccess(""); setBusy(true);
    try {
      if (mode === "login") {
        await onLogin(email.trim(), password);
      } else {
        const response = await registerCustomer({ fullName: fullName.trim(), email: email.trim(), password, phone: phone.trim() || undefined, nationalId: nationalId.trim() || undefined, address: address.trim() || undefined });
        setSuccess(`Customer account created. Your KYC status is ${response.kycStatus}. You can now sign in.`);
        setMode("login"); setPassword("");
      }
    } catch (e) { setError(getError(e)); }
    finally { setBusy(false); }
  };
  return <main className="auth-screen">
    <div className="auth-background-glow auth-background-glow-one" />
    <div className="auth-background-glow auth-background-glow-two" />
    <header className="auth-topbar"><button className="auth-back-button" onClick={onBack}><ArrowRight size={15} className="auth-back-icon" /> Back to home</button><Brand /></header>
    <section className="auth-layout">
      <div className="auth-story">
        <div className="auth-story-kicker"><span /> YOUR MONEY. YOUR MOMENTUM.</div>
        <h1>{mode === "login" ? <>Welcome back to<br /><em>better banking.</em></> : <>Start with a<br /><em>stronger account.</em></>}</h1>
        <p>{mode === "login" ? "Sign in to your secure workspace and pick up right where you left off." : "Create your customer profile to begin your banking journey with Confused Bank."}</p>
        <div className="auth-benefits"><span><ShieldCheck size={17} /> Secure token-based access</span><span><Activity size={17} /> Clear account activity</span><span><Sparkles size={17} /> A simpler banking experience</span></div>
        <div className="auth-story-note"><span className="auth-note-mark"><Image src="/brand/confused-bank-emblem.svg" alt="" aria-hidden="true" width={441} height={488} /></span><span><strong>Banking, made more human.</strong><small>Thoughtful tools for everyday financial tasks.</small></span></div>
      </div>

      <div className="login-card auth-card">
        <div className="auth-card-header"><div><div className="login-eyebrow">{mode === "login" ? "SECURE ACCESS" : "CUSTOMER REGISTRATION"}</div><h2>{mode === "login" ? "Sign in to your account" : "Create your account"}</h2><p>{mode === "login" ? "Enter your details to access your workspace." : "A few details to set up your customer profile."}</p></div><div className="login-symbol"><LockKeyhole size={22} /></div></div>
        <div className="auth-mode-switch" role="group" aria-label="Authentication mode"><button type="button" className={mode === "login" ? "auth-mode-active" : ""} onClick={() => { setMode("login"); setError(""); setSuccess(""); }}>Sign in</button><button type="button" className={mode === "register" ? "auth-mode-active" : ""} onClick={() => { setMode("register"); setError(""); setSuccess(""); }}>Register</button></div>
        <form onSubmit={submit} className="login-form">
          {mode === "register" && <Field label="Full name"><input value={fullName} onChange={(e) => setFullName(e.target.value)} required maxLength={100} autoComplete="name" placeholder="Your full name" /></Field>}
          <Field label="Email address"><input type="email" autoComplete="username" required placeholder="name@example.com" value={email} onChange={(e) => setEmail(e.target.value)} maxLength={150} /></Field>
          <Field label="Password"><div className="password-input"><input type={showPassword ? "text" : "password"} autoComplete={mode === "login" ? "current-password" : "new-password"} required minLength={mode === "register" ? 8 : undefined} maxLength={72} placeholder={mode === "login" ? "Enter your password" : "Create a password (8+ characters)"} value={password} onChange={(e) => setPassword(e.target.value)} /><button type="button" onClick={() => setShowPassword((v) => !v)}>{showPassword ? "Hide" : "Show"}</button></div></Field>
          {mode === "register" && <div className="register-extra-fields"><Field label="Phone number · optional"><input value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={20} autoComplete="tel" placeholder="Your phone number" /></Field><Field label="National ID · optional"><input value={nationalId} onChange={(e) => setNationalId(e.target.value)} maxLength={30} placeholder="National ID number" /></Field><Field label="Address · optional"><input value={address} onChange={(e) => setAddress(e.target.value)} maxLength={255} autoComplete="street-address" placeholder="Current address" /></Field></div>}
          {error && <div className="form-error" role="alert"><ShieldAlert size={16} />{error}</div>}
          {success && <div className="form-success" role="status"><CheckCircle2 size={16} />{success}</div>}
          <button className="button button-primary login-submit" disabled={busy}>{busy ? "Please wait…" : mode === "login" ? "Sign in securely" : "Create customer account"}<ArrowRight size={17} /></button>
        </form>
        <div className="login-foot"><ShieldCheck size={15} /> Protected with secure token-based authentication</div>
        <p className="auth-card-note">{mode === "login" ? "New to Confused Bank? Use Register to create a customer account." : "Already have an account? Choose Sign in above."}</p>
      </div>
    </section>
    <footer className="auth-footer"><span>© {new Date().getFullYear()} Confused Bank</span><span>Secure access <i /> Academic banking simulation</span></footer>
  </main>;
}

export default function BankApp() {
  const [session, setSession] = useState<SessionData | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [booting, setBooting] = useState(true);
  const [authScreen, setAuthScreen] = useState<"hero" | "login" | "register">("hero");
  const [activeView, setActiveView] = useState<ViewName>("Dashboard");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [overview, setOverview] = useState<Overview | null>(null);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [kycItems, setKycItems] = useState<KycReview[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loans, setLoans] = useState<Loan[]>([]);
  const [staff, setStaff] = useState<Staff[]>([]);
  const [fraudAlerts, setFraudAlerts] = useState<FraudAlert[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [selectedAccount, setSelectedAccount] = useState("");
  const [bankingOperation, setBankingOperation] = useState<"DEPOSIT" | "WITHDRAW" | "TRANSFER">("DEPOSIT");
  const [repaymentHistory, setRepaymentHistory] = useState<LoanRepayment[]>([]);
  const [repaymentLoan, setRepaymentLoan] = useState<Loan | null>(null);
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement>(null);
  const [modal, setModal] = useState<"staff" | "account" | null>(null);

  const role = profile?.role;
  const navItems = role ? navByRole[role] : [];
  const unreadCount = notifications.filter((item) => !item.read).length;

  const signOut = useCallback(() => {
    clearSession(); setSession(null); setProfile(null); setOverview(null);
    setAuthScreen("hero"); setActiveView("Dashboard"); setNotifications([]); setShowNotifications(false); setShowProfileMenu(false);
  }, []);

  useEffect(() => {
    if (!showProfileMenu) return;
    const handlePointerDown = (event: PointerEvent) => {
      if (!profileMenuRef.current?.contains(event.target as Node)) setShowProfileMenu(false);
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setShowProfileMenu(false);
    };
    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [showProfileMenu]);

  useEffect(() => {
    let active = true;
    const restoreSession = async () => {
      // Yield once before restoring browser storage so the effect does not
      // synchronously schedule a React state update during its own execution.
      await Promise.resolve();
      const current = readSession();
      if (!current) {
        if (active) setBooting(false);
        return;
      }
      try {
        const data = await getProfile(current.token);
        if (active) {
          setSession(current);
          setProfile(data);
        }
      } catch {
        clearSession();
        if (active) setSession(null);
      } finally {
        if (active) setBooting(false);
      }
    };
    void restoreSession();
    return () => { active = false; };
  }, []);

  const handleLogin = async (email: string, password: string) => {
    const auth = await login(email, password);
    saveSession(auth);
    const nextSession = readSession();
    if (!nextSession) throw new Error("Could not create a secure session. Please try again.");
    try {
      const me = await getProfile(auth.accessToken);
      setSession(nextSession); setProfile(me); setActiveView("Dashboard"); setNotice(`Welcome, ${me.fullName}.`);
    } catch (e) { clearSession(); throw e; }
  };

  const fetchDashboard = useCallback(async (token: string, currentProfile: Profile) => {
    if (currentProfile.role === "ADMIN") {
      const [stats, tx, kyc, loanPage, adminNotices] = await Promise.allSettled([
        apiRequest<Overview>("/api/v1/admin/reports/overview", token),
        apiRequest<Page<Transaction>>("/api/v1/admin/reports/transactions?page=0&size=6", token),
        apiRequest<Page<KycReview>>("/api/v1/employee/kyc/pending?page=0&size=5", token),
        apiRequest<Page<Loan>>("/api/v1/employee/loans/pending?page=0&size=5", token),
        apiRequest<Page<NotificationItem>>("/api/v1/notifications?page=0&size=5", token),
      ]);
      if (stats.status === "fulfilled") setOverview(stats.value);
      if (tx.status === "fulfilled") setTransactions(tx.value.content || []);
      if (kyc.status === "fulfilled") setKycItems(kyc.value.content || []);
      if (loanPage.status === "fulfilled") setLoans(loanPage.value.content || []);
      if (adminNotices.status === "fulfilled") setNotifications(adminNotices.value.content || []);
      if (stats.status === "rejected") throw stats.reason;
    } else if (currentProfile.role === "CUSTOMER") {
      const [accountList, loanPage, notices] = await Promise.allSettled([
        apiRequest<Account[]>("/api/v1/accounts", token),
        apiRequest<Page<Loan>>("/api/v1/loans?page=0&size=5", token),
        apiRequest<Page<NotificationItem>>("/api/v1/notifications?page=0&size=5", token),
      ]);
      if (accountList.status === "fulfilled") {
        setAccounts(accountList.value);
        const accountNo = selectedAccount || accountList.value[0]?.accountNumber;
        if (accountNo) {
          setSelectedAccount(accountNo);
          try { const recent = await apiRequest<Page<Transaction>>(`/api/v1/accounts/${encodeURIComponent(accountNo)}/transactions?page=0&size=6`, token); setTransactions(recent.content || []); } catch { setTransactions([]); }
        }
      }
      if (loanPage.status === "fulfilled") setLoans(loanPage.value.content || []);
      if (notices.status === "fulfilled") setNotifications(notices.value.content || []);
      if (accountList.status === "rejected") throw accountList.reason;
    } else {
      const tasks: { key: "customers" | "kyc" | "loans" | "notifications"; promise: Promise<unknown> }[] = [
        { key: "customers", promise: apiRequest<Page<Customer>>("/api/v1/employee/customers?page=0&size=6", token) },
        { key: "notifications", promise: apiRequest<Page<NotificationItem>>("/api/v1/notifications?page=0&size=5", token) },
      ];
      if (["ADMIN", "MANAGER", "TELLER"].includes(currentProfile.role)) tasks.push({ key: "kyc", promise: apiRequest<Page<KycReview>>("/api/v1/employee/kyc/pending?page=0&size=5", token) });
      if (["ADMIN", "MANAGER", "LOAN_OFFICER"].includes(currentProfile.role)) tasks.push({ key: "loans", promise: apiRequest<Page<Loan>>("/api/v1/employee/loans/pending?page=0&size=5", token) });
      const results = await Promise.all(tasks.map(async (task) => ({ key: task.key, result: await Promise.allSettled([task.promise]) })));
      for (const entry of results) {
        const result = entry.result[0];
        if (result.status === "rejected") { if (entry.key === "customers") throw result.reason; continue; }
        const page = result.value as Page<Customer> | Page<KycReview> | Page<Loan> | Page<NotificationItem>;
        if (entry.key === "customers") setCustomers((page as Page<Customer>).content || []);
        if (entry.key === "kyc") setKycItems((page as Page<KycReview>).content || []);
        if (entry.key === "loans") setLoans((page as Page<Loan>).content || []);
        if (entry.key === "notifications") setNotifications((page as Page<NotificationItem>).content || []);
      }
    }
  }, [selectedAccount]);

  const loadView = useCallback(async () => {
    if (!session || !profile) return;
    setLoading(true); setError("");
    try {
      const token = session.token;
      switch (activeView) {
        case "Dashboard": await fetchDashboard(token, profile); break;
        case "Customers": {
          const data = await apiRequest<Page<Customer>>("/api/v1/employee/customers?page=0&size=50", token);
          setCustomers(data.content || []); break;
        }
        case "KYC review": {
          const data = await apiRequest<Page<KycReview>>("/api/v1/employee/kyc/pending?page=0&size=50", token);
          setKycItems(data.content || []); break;
        }
        case "Accounts": {
          const data = await apiRequest<Account[]>("/api/v1/accounts", token);
          setAccounts(data || []); if (!selectedAccount && data?.[0]) setSelectedAccount(data[0].accountNumber); break;
        }
        case "Banking": {
          if (profile.role === "CUSTOMER") {
            const data = await apiRequest<Account[]>("/api/v1/accounts", token);
            setAccounts(data || []); if (!selectedAccount && data?.[0]) setSelectedAccount(data[0].accountNumber);
          }
          break;
        }
        case "Transactions": {
          if (profile.role === "ADMIN") {
            const data = await apiRequest<Page<Transaction>>("/api/v1/admin/reports/transactions?page=0&size=50", token);
            setTransactions(data.content || []);
          } else if (profile.role === "CUSTOMER") {
            const list = await apiRequest<Account[]>("/api/v1/accounts", token);
            setAccounts(list);
            const accountNo = selectedAccount || list[0]?.accountNumber;
            if (accountNo) {
              setSelectedAccount(accountNo);
              const data = await apiRequest<Page<Transaction>>(`/api/v1/accounts/${encodeURIComponent(accountNo)}/transactions?page=0&size=50`, token);
              setTransactions(data.content || []);
            } else setTransactions([]);
          }
          break;
        }
        case "Loans": {
          if (profile.role === "CUSTOMER") {
            const [data, accountList] = await Promise.all([
              apiRequest<Page<Loan>>("/api/v1/loans?page=0&size=50", token),
              apiRequest<Account[]>("/api/v1/accounts", token),
            ]);
            setLoans(data.content || []); setAccounts(accountList || []);
          } else {
            const data = await apiRequest<Page<Loan>>("/api/v1/employee/loans/pending?page=0&size=50", token);
            setLoans(data.content || []);
          }
          break;
        }
        case "Reports & analytics": {
          const [stats, tx] = await Promise.all([
            apiRequest<Overview>("/api/v1/admin/reports/overview", token),
            apiRequest<Page<Transaction>>("/api/v1/admin/reports/transactions?page=0&size=50", token),
          ]);
          setOverview(stats); setTransactions(tx.content || []); break;
        }
        case "Staff management": {
          const data = await apiRequest<Page<Staff>>("/api/v1/admin/staff?page=0&size=100", token);
          setStaff(data.content || []); break;
        }
        case "Fraud alerts": {
          const data = await apiRequest<Page<FraudAlert>>("/api/v1/admin/fraud-alerts?page=0&size=50", token);
          setFraudAlerts(data.content || []); break;
        }
        case "Audit logs": {
          const data = await apiRequest<Page<AuditLog>>("/api/v1/admin/audit-logs?page=0&size=50", token);
          setAuditLogs(data.content || []); break;
        }
        case "Notifications": {
          const data = await apiRequest<Page<NotificationItem>>("/api/v1/notifications?page=0&size=50", token);
          setNotifications(data.content || []); break;
        }
        case "My profile": break;
      }
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) signOut();
      else setError(getError(e));
    } finally { setLoading(false); }
  }, [session, profile, activeView, fetchDashboard, selectedAccount, signOut]);

  useEffect(() => {
    let active = true;
    const runViewLoad = async () => {
      // Defer the request cycle until after the effect's synchronous phase.
      await Promise.resolve();
      if (active) await loadView();
    };
    void runViewLoad();
    return () => { active = false; };
  }, [loadView, reloadKey]);

  const doAction = async <T,>(path: string, method: string, body?: unknown, success = "Changes saved successfully."): Promise<boolean> => {
    if (!session) return false;
    setError(""); setNotice(""); setLoading(true);
    try {
      await apiRequest<T>(path, session.token, { method, ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
      setNotice(success); setReloadKey((v) => v + 1);
      return true;
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) signOut();
      else setError(getError(e));
      return false;
    } finally { setLoading(false); }
  };

  const selectView = (view: ViewName) => { setActiveView(view); setSidebarOpen(false); setError(""); setNotice(""); setSearch(""); };
  const searchFilter = <T,>(values: T[]): T[] => values.filter((item) => JSON.stringify(item).toLowerCase().includes(search.trim().toLowerCase()));

  if (booting) return <main className="boot-screen"><div className="brand-mark boot-brand-mark"><Image src="/brand/confused-bank-emblem.svg" alt="" aria-hidden="true" width={441} height={488} /></div><span className="loader" /> Securing your workspace…</main>;
  if (!session || !profile) {
    if (authScreen === "hero") return <LandingScreen onLogin={() => setAuthScreen("login")} onRegister={() => setAuthScreen("register")} />;
    return <LoginScreen key={authScreen} onLogin={handleLogin} initialMode={authScreen === "register" ? "register" : "login"} onBack={() => setAuthScreen("hero")} />;
  }

  const renderDashboard = () => {
    const isAdmin = role === "ADMIN";
    const totalBalance = overview?.totalAccountBalance ?? accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0);
    const pendingCount = isAdmin ? overview?.pendingLoans ?? 0 : loans.filter((loan) => loan.status === "PENDING").length;
    const deposits = overview?.depositsLast30Days ?? 0;
    const withdrawals = overview?.withdrawalsLast30Days ?? 0;
    const chartData = [{ label: "Last 30 days", deposits, withdrawals }];
    return <>
      <div className="welcome-row"><div><div className="eyebrow"><span className="live-dot" /> LIVE BANKING WORKSPACE <span className="eyebrow-divider">/</span> {new Date().toLocaleDateString("en-GB", { weekday: "long", day: "2-digit", month: "long", year: "numeric" }).toUpperCase()}</div><h1>Good {new Date().getHours() < 12 ? "morning" : new Date().getHours() < 18 ? "afternoon" : "evening"}, {profile.fullName.split(" ")[0]} <span className="wave">✦</span></h1><p className="page-subtitle">Here&apos;s your latest banking activity and operational overview.</p></div><div className="welcome-actions"><button className="button button-secondary" onClick={() => setReloadKey((v) => v + 1)}><RefreshCw size={15} /> Refresh data</button>{role === "ADMIN" && <button className="button button-primary" onClick={() => selectView("Staff management")}><Plus size={16} /> Add staff</button>}</div></div>
      <section className="stats-grid" aria-label="Banking metrics">
        {isAdmin ? <>
          <StatCard icon={<Wallet size={18} />} label="Total account balance" value={money(totalBalance)} tone="green" caption="Across active bank accounts" />
          <StatCard icon={<Users size={18} />} label="Total customers" value={number(overview?.totalCustomers)} tone="blue" caption="Registered customer profiles" />
          <StatCard icon={<CreditCard size={18} />} label="Active accounts" value={number(overview?.activeAccounts)} tone="violet" caption="Currently active accounts" />
          <StatCard icon={<BriefcaseBusiness size={18} />} label="Pending loan requests" value={number(pendingCount)} tone="amber" caption="Awaiting officer review" />
        </> : role === "CUSTOMER" ? <>
          <StatCard icon={<Wallet size={18} />} label="My total balance" value={money(totalBalance)} tone="green" caption="Combined balance of your accounts" />
          <StatCard icon={<CreditCard size={18} />} label="My accounts" value={number(accounts.length)} tone="blue" caption="Personal bank accounts" />
          <StatCard icon={<BriefcaseBusiness size={18} />} label="My loans" value={number(loans.length)} tone="violet" caption="Loan applications and facilities" />
          <StatCard icon={<Bell size={18} />} label="Notifications" value={number(unreadCount)} tone="amber" caption="Unread account updates" />
        </> : <>
          <StatCard icon={<Users size={18} />} label="Customer directory" value={number(customers.length)} tone="green" caption="Customers in the loaded page" />
          <StatCard icon={<ShieldCheck size={18} />} label="Pending KYC" value={number(kycItems.length)} tone="blue" caption="Awaiting identity review" />
          <StatCard icon={<BriefcaseBusiness size={18} />} label="Pending loans" value={number(loans.length)} tone="violet" caption="Awaiting loan decision" />
          <StatCard icon={<Bell size={18} />} label="Notifications" value={number(unreadCount)} tone="amber" caption="Unread account updates" />
        </>}
      </section>
      <section className="analytics-grid">
        <article className="panel chart-panel"><div className="panel-heading"><div><h2>{isAdmin ? "Banking activity · last 30 days" : "Your banking overview"}</h2><p>{isAdmin ? "Actual deposits and withdrawals recorded by the bank" : "Summary based on your account data"}</p></div><span className="live-data-pill"><i /> LIVE API DATA</span></div>
          {isAdmin ? <><div className="chart-summary"><strong>{money(deposits + withdrawals)}</strong><small>combined transaction volume · last 30 days</small></div><div className="chart-container"><ResponsiveContainer width="100%" height="100%"><BarChart data={chartData} margin={{ top: 16, right: 10, left: 0, bottom: 0 }} barGap={20}><CartesianGrid vertical={false} stroke="#edf0f4" strokeDasharray="4 5" /><XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: "#98a2b3", fontSize: 11 }} /><YAxis axisLine={false} tickLine={false} tick={{ fill: "#98a2b3", fontSize: 10 }} tickFormatter={(v: unknown) => `${Number(v) / 1000000}M`} /><Tooltip formatter={(v: unknown) => money(Number(v))} contentStyle={{ border: "1px solid #edf0f4", borderRadius: 10 }} /><Bar dataKey="deposits" name="Deposits" fill="#1b9a70" radius={[6, 6, 0, 0]} maxBarSize={44} /><Bar dataKey="withdrawals" name="Withdrawals" fill="#b8c7f6" radius={[6, 6, 0, 0]} maxBarSize={44} /></BarChart></ResponsiveContainer></div><div className="chart-footnote"><span><i className="legend-deposit" /> Deposits: <strong>{money(deposits)}</strong></span><span><i className="legend-withdraw" /> Withdrawals: <strong>{money(withdrawals)}</strong></span></div></> : <div className="dashboard-summary-list">{role === "CUSTOMER" ? accounts.slice(0, 4).map((account) => <div className="summary-row" key={account.id}><span className="summary-icon"><Wallet size={16} /></span><span><strong>{account.accountType.replaceAll("_", " ")}</strong><small>{account.accountNumber}</small></span><b>{money(account.balance)}</b></div>) : <><div className="summary-row"><span className="summary-icon"><Users size={16} /></span><span><strong>Customer directory</strong><small>Review customer records and KYC details</small></span><button className="inline-link" onClick={() => selectView("Customers")}>Open</button></div><div className="summary-row"><span className="summary-icon"><ShieldCheck size={16} /></span><span><strong>Identity verification</strong><small>Review pending KYC requests</small></span><button className="inline-link" onClick={() => selectView("KYC review")}>Open</button></div><div className="summary-row"><span className="summary-icon"><BriefcaseBusiness size={16} /></span><span><strong>Loan decisions</strong><small>Review pending loan applications</small></span><button className="inline-link" onClick={() => selectView("Loans")}>Open</button></div></>}</div>}
        </article>
        <article className="panel activity-panel"><div className="panel-heading"><div><h2>Quick actions</h2><p>Shortcuts for your role</p></div><MoreHorizontal size={18} className="subtle-icon" /></div><div className="quick-action-grid">
          {role === "ADMIN" && <><QuickAction icon={<Users size={17} />} title="Customers" detail="View customer directory" onClick={() => selectView("Customers")} /><QuickAction icon={<UserRound size={17} />} title="Manage staff" detail="Create and manage roles" onClick={() => selectView("Staff management")} /><QuickAction icon={<ShieldAlert size={17} />} title="Fraud alerts" detail="Review open alerts" onClick={() => selectView("Fraud alerts")} /><QuickAction icon={<FileClock size={17} />} title="Audit trail" detail="Review system activity" onClick={() => selectView("Audit logs")} /></>}
          {role === "CUSTOMER" && <><QuickAction icon={<Banknote size={17} />} title="Banking" detail="Deposit, withdraw or transfer" onClick={() => selectView("Banking")} /><QuickAction icon={<CreditCard size={17} />} title="My accounts" detail="Balances and account details" onClick={() => selectView("Accounts")} /><QuickAction icon={<HandCoins size={17} />} title="Apply for a loan" detail="Start a loan application" onClick={() => selectView("Loans")} /><QuickAction icon={<ArrowLeftRight size={17} />} title="Transactions" detail="Review account history" onClick={() => selectView("Transactions")} /></>}
          {role !== "ADMIN" && role !== "CUSTOMER" && <><QuickAction icon={<Users size={17} />} title="Customers" detail="Browse customer records" onClick={() => selectView("Customers")} />{["TELLER", "MANAGER"].includes(role || "") && <QuickAction icon={<ShieldCheck size={17} />} title="KYC review" detail="Verify customer identity" onClick={() => selectView("KYC review")} />}<QuickAction icon={<Banknote size={17} />} title="Counter banking" detail="Process counter operations" onClick={() => selectView("Banking")} />{["LOAN_OFFICER", "MANAGER"].includes(role || "") && <QuickAction icon={<BriefcaseBusiness size={17} />} title="Loan review" detail="Review pending applications" onClick={() => selectView("Loans")} />}</>}
        </div></article>
      </section>
      <Panel title="Recent transactions" subtitle={isAdmin ? "Latest entries from the bank-wide transaction ledger" : "Latest transactions available to your account"} action={<button className="button button-secondary button-small" onClick={() => selectView("Transactions")}>View all <ChevronRight size={14} /></button>}>
        <TransactionTable rows={transactions.slice(0, 6)} admin={isAdmin} />
      </Panel>
    </>;
  };

  const renderCustomers = () => <>
    <ModuleHeader title="Customer directory" subtitle="Browse registered customers and review their account identity information." action={<span className="record-count">{number(customers.length)} loaded records</span>} />
    <Panel title="All customers" subtitle="Customer records returned by the employee directory API"><DataTable headers={["CUSTOMER", "CONTACT", "NATIONAL ID", "KYC STATUS", "ACCOUNT STATUS", "JOINED"]}>
      {searchFilter(customers).map((c) => <tr key={c.id}><td><div className="person-cell"><span className="person-avatar">{initials(c.fullName)}</span><span><strong>{c.fullName}</strong><small>Customer #{c.id}</small></span></div></td><td>{c.email}<small className="cell-sub">{c.phone || "No phone provided"}</small></td><td>{c.nationalId || "—"}</td><td><StatusPill value={c.kycStatus} /></td><td><StatusPill value={c.enabled ? "ACTIVE" : "DISABLED"} /></td><td>{dateOnly(c.createdAt)}</td></tr>)}
    </DataTable>{customers.length === 0 && <EmptyState title="No customers found" />}</Panel>
  </>;

  const renderKyc = () => <>
    <ModuleHeader title="KYC verification" subtitle="Review pending customer identity requests. Decisions are recorded by the backend with an audit event." action={<span className="record-count">{kycItems.length} pending</span>} />
    <div className="notice-box"><ShieldCheck size={17} /><span>Only pending KYC applications can be reviewed. Verification decisions notify the customer and are recorded in the audit trail.</span></div>
    <Panel title="Pending review queue" subtitle="Customer identity records awaiting a decision"><DataTable headers={["CUSTOMER", "IDENTITY", "CONTACT", "SUBMITTED", "ACTION"]}>
      {searchFilter(kycItems).map((item) => <tr key={item.customerId}><td><div className="person-cell"><span className="person-avatar">{initials(item.fullName)}</span><span><strong>{item.fullName}</strong><small>Customer #{item.customerId}</small></span></div></td><td>{item.nationalId || "Not supplied"}<small className="cell-sub">{item.address || "No address supplied"}</small></td><td>{item.email}<small className="cell-sub">{item.phone || "No phone"}</small></td><td>{dateTime(item.submittedAt)}</td><td><div className="table-actions"><button className="button button-mini button-primary" onClick={() => { const note = window.prompt("Verification note (optional):", "Identity documents reviewed"); if (note !== null) void doAction(`/api/v1/employee/kyc/${item.customerId}`, "PUT", { status: "VERIFIED", note }, "Customer KYC verified."); }}> <Check size={14} /> Verify</button><button className="button button-mini button-danger" onClick={() => { const note = window.prompt("Reason for rejection:", "Supporting identity documents are insufficient"); if (note !== null) void doAction(`/api/v1/employee/kyc/${item.customerId}`, "PUT", { status: "REJECTED", note }, "Customer KYC rejected."); }}>Reject</button></div></td></tr>)}
    </DataTable>{kycItems.length === 0 && <EmptyState title="KYC queue is clear" detail="There are no pending customer verification requests." />}</Panel>
  </>;

  const renderAccounts = () => <>
    <ModuleHeader title="My accounts" subtitle="Manage your personal Savings, Current and Fixed Deposit accounts." action={<button className="button button-primary" onClick={() => setModal("account")}><Plus size={15} /> Open account</button>} />
    <div className="account-card-grid">{accounts.map((account) => <article className="account-card" key={account.id}><div className="account-card-top"><span className="account-icon"><Wallet size={18} /></span><StatusPill value={account.status} /></div><span className="account-type-label">{account.accountType.replaceAll("_", " ")}</span><strong className="account-balance">{money(account.balance)}</strong><span className="account-number">{account.accountNumber}</span><div className="account-meta"><span>Interest rate <b>{Number(account.annualInterestRate).toFixed(2)}%</b></span><span>Min. balance <b>{money(account.minimumBalance)}</b></span></div><button className="text-link" onClick={() => { setSelectedAccount(account.accountNumber); selectView("Transactions"); }}>View transactions <ChevronRight size={14} /></button></article>)}</div>
    {accounts.length === 0 && <Panel title="No accounts yet"><EmptyState title="No bank accounts" detail={profile?.kycStatus === "VERIFIED" ? "Open your first account to get started." : "KYC verification is required before opening an account."} /></Panel>}
    {modal === "account" && <Modal title="Open a new account" onClose={() => setModal(null)}><form className="form-grid" onSubmit={async (e) => { e.preventDefault(); const fd = new FormData(e.currentTarget); const type = String(fd.get("accountType")); const ok = await doAction<Account>("/api/v1/accounts", "POST", { accountType: type, openingBalance: Number(fd.get("openingBalance")), tenureMonths: type === "FIXED_DEPOSIT" ? Number(fd.get("tenureMonths")) : null }, "Your account has been opened."); if (ok) setModal(null); }}><Field label="Account type"><select name="accountType"><option value="SAVINGS">Savings</option><option value="CURRENT">Current</option><option value="FIXED_DEPOSIT">Fixed Deposit</option></select></Field><Field label="Opening balance (BDT)"><input name="openingBalance" type="number" min="0.01" step="0.01" required placeholder="5000.00" /></Field><Field label="Tenure in months" hint="Required for Fixed Deposit. Choose 1–60 months."><input name="tenureMonths" type="number" min="1" max="60" defaultValue="12" /></Field><div className="form-span form-actions"><button type="button" className="button button-secondary" onClick={() => setModal(null)}>Cancel</button><button className="button button-primary">Create account</button></div></form></Modal>}
  </>;

  const renderBanking = () => {
    const customerMode = role === "CUSTOMER";
    return <>
      <ModuleHeader title={customerMode ? "Banking services" : "Counter operations"} subtitle={customerMode ? "Perform transactions using your own active bank accounts." : "Process customer counter deposits and withdrawals. Customer KYC must be verified."} />
      <div className="operation-layout"><Panel title="New transaction" subtitle={customerMode ? "Deposits, withdrawals and internal transfers" : "Teller counter deposit and withdrawal"}>
        <form className="form-grid" onSubmit={async (e) => { e.preventDefault(); const fd = new FormData(e.currentTarget); const kind = String(fd.get("operation")); const amount = Number(fd.get("amount")); const description = String(fd.get("description") || ""); let path = ""; let body: Record<string, unknown> = { amount, description };
          if (kind === "TRANSFER") { path = "/api/v1/banking/transfer"; body = { ...body, sourceAccountNumber: String(fd.get("accountNumber")), destinationAccountNumber: String(fd.get("destinationAccountNumber")) }; }
          else { path = customerMode ? `/api/v1/banking/${kind.toLowerCase()}` : `/api/v1/employee/counter/${kind.toLowerCase()}`; body = { ...body, accountNumber: String(fd.get("accountNumber")) }; }
          await doAction<Transaction>(path, "POST", body, `${kind.toLowerCase()} request completed successfully.`); }}>
          <Field label="Operation"><select name="operation" value={bankingOperation} onChange={(e) => setBankingOperation(e.target.value as "DEPOSIT" | "WITHDRAW" | "TRANSFER")}><option value="DEPOSIT">Deposit</option><option value="WITHDRAW">Withdrawal</option>{customerMode && <option value="TRANSFER">Transfer</option>}</select></Field>
          <Field label="Source account number">{customerMode && accounts.length ? <select name="accountNumber" required value={selectedAccount || accounts[0]?.accountNumber || ""} onChange={(e) => setSelectedAccount(e.target.value)}>{accounts.map((a) => <option key={a.id} value={a.accountNumber}>{a.accountNumber} · {a.accountType}</option>)}</select> : <input name="accountNumber" required placeholder="Enter customer account number" />}</Field>
          {customerMode && bankingOperation === "TRANSFER" && <Field label="Destination account number"><input name="destinationAccountNumber" required placeholder="Destination account" /></Field>}
          <Field label="Amount (BDT)"><input name="amount" type="number" min="0.01" step="0.01" required placeholder="1000.00" /></Field>
          <Field label="Description"><input name="description" maxLength={255} placeholder="Transaction note (optional)" /></Field>
          <div className="form-span form-actions"><button className="button button-primary"><ArrowLeftRight size={15} /> Submit transaction</button></div>
        </form>
        {!customerMode && <p className="form-note">Counter transactions are performed on verified customer-owned accounts. Manager and Admin roles can also freeze or unfreeze accounts from the account control panel below.</p>}
      </Panel>
      {!customerMode && ["ADMIN", "MANAGER"].includes(role || "") && <Panel title="Account controls" subtitle="Freeze or unfreeze an account under manager authorization"><form className="form-grid" onSubmit={async (e) => { e.preventDefault(); const fd = new FormData(e.currentTarget); const accountNo = String(fd.get("controlAccount")); const action = String(fd.get("controlAction")); await doAction<Account>(`/api/v1/employee/accounts/${encodeURIComponent(accountNo)}/${action}`, "PUT", undefined, `Account ${action === "freeze" ? "frozen" : "unfrozen"}.`); }}><Field label="Account number"><input name="controlAccount" required placeholder="Customer account number" /></Field><Field label="Action"><select name="controlAction"><option value="freeze">Freeze account</option><option value="unfreeze">Unfreeze account</option></select></Field><div className="form-span form-actions"><button className="button button-secondary">Apply account control</button></div></form></Panel>}
      <Panel title="Before you submit" subtitle="Banking controls enforced by the server"><div className="compliance-list"><div><CheckCircle2 size={17} /><span>Amounts must be greater than zero and support up to two decimal places.</span></div><div><CheckCircle2 size={17} /><span>All operations are processed transactionally by the existing banking service.</span></div><div><CheckCircle2 size={17} /><span>High-value and rapid transactions may create fraud review alerts.</span></div></div></Panel>
      </div>
    </>;
  };

  const renderTransactions = () => <>
    <ModuleHeader title="Transaction ledger" subtitle={role === "ADMIN" ? "Bank-wide transaction records, newest first." : "Your transaction history for the selected account."} action={role === "CUSTOMER" ? <select className="toolbar-select" value={selectedAccount} onChange={(e) => { setSelectedAccount(e.target.value); setReloadKey((v) => v + 1); }}><option value="">Select account</option>{accounts.map((a) => <option key={a.id} value={a.accountNumber}>{a.accountNumber}</option>)}</select> : null} />
    <Panel title="Transactions" subtitle={`${transactions.length} transaction records loaded`}><TransactionTable rows={searchFilter(transactions)} admin={role === "ADMIN"} />{transactions.length === 0 && <EmptyState title="No transaction records" detail="Transactions will appear here after banking activity is recorded." />}</Panel>
  </>;

  const renderLoans = () => <>
    <ModuleHeader title={role === "CUSTOMER" ? "My loans" : "Loan applications"} subtitle={role === "CUSTOMER" ? "Track your loan applications, outstanding balance and repayments." : "Review pending customer loan applications and record a decision."} action={role === "CUSTOMER" ? <button className="button button-primary" onClick={() => setNotice("Use the application form below to submit a new loan request.")}><Plus size={15} /> New application</button> : <span className="record-count">{loans.length} awaiting review</span>} />
    {role === "CUSTOMER" && <Panel title="Apply for a loan" subtitle="Eligibility requires verified KYC and an active Savings or Current account"><form className="form-grid" onSubmit={async (e) => { e.preventDefault(); const fd = new FormData(e.currentTarget); await doAction<Loan>("/api/v1/loans", "POST", { loanType: fd.get("loanType"), purpose: fd.get("purpose"), principalAmount: Number(fd.get("principalAmount")), termMonths: Number(fd.get("termMonths")), accountNumber: fd.get("accountNumber") }, "Loan application submitted."); }}><Field label="Loan type"><select name="loanType"><option value="PERSONAL">Personal</option><option value="EDUCATION">Education</option><option value="HOME">Home</option><option value="BUSINESS">Business</option></select></Field><Field label="Disbursement account"><select name="accountNumber" required>{accounts.filter((a) => a.status === "ACTIVE" && a.accountType !== "FIXED_DEPOSIT").map((a) => <option key={a.id} value={a.accountNumber}>{a.accountNumber} · {a.accountType}</option>)}</select></Field><Field label="Principal amount (BDT)"><input name="principalAmount" type="number" min="1000" max="5000000" step="0.01" required placeholder="150000" /></Field><Field label="Term (months)"><input name="termMonths" type="number" min="3" max="84" required defaultValue="24" /></Field><div className="form-span"><Field label="Purpose"><textarea name="purpose" maxLength={500} required rows={2} placeholder="Describe the purpose of this loan" /></Field></div><div className="form-span form-actions"><button className="button button-primary" disabled={!accounts.some((a) => a.status === "ACTIVE" && a.accountType !== "FIXED_DEPOSIT")}>Submit loan application</button></div></form></Panel>}
    <Panel title={role === "CUSTOMER" ? "Loan portfolio" : "Pending applications"} subtitle={role === "CUSTOMER" ? "Your loan records from the banking service" : "Approval disburses the principal to the linked customer account in the same backend transaction."}><DataTable headers={["LOAN", "APPLICANT", "TYPE", "PRINCIPAL", "TERM", "STATUS", "DETAILS"]}>
      {searchFilter(loans).map((loan) => <tr key={loan.id}><td><strong>{loan.loanNumber}</strong><small className="cell-sub">{dateOnly(loan.submittedAt)}</small></td><td>{loan.applicantEmail}</td><td>{loan.loanType}</td><td className="amount-text">{money(loan.principalAmount)}</td><td>{loan.termMonths} months</td><td><StatusPill value={loan.status} /></td><td>{role === "CUSTOMER" ? <div className="table-actions"><LoanRepaymentAction loan={loan} accounts={accounts} token={session!.token} onDone={() => setReloadKey((v) => v + 1)} onError={setError} /><button className="button button-mini button-secondary" onClick={async () => { try { const history = await apiRequest<LoanRepayment[]>(`/api/v1/loans/${loan.id}/repayments`, session!.token); setRepaymentHistory(history); setRepaymentLoan(loan); } catch (e) { setError(getError(e)); } }}>History</button></div> : loan.status === "PENDING" ? <div className="table-actions"><button className="button button-mini button-primary" onClick={() => { const rate = window.prompt("Annual interest rate (%)", "10.50"); if (rate === null) return; const note = window.prompt("Approval note (optional)", "Income documents reviewed"); if (note !== null) void doAction<Loan>(`/api/v1/employee/loans/${loan.id}/decision`, "POST", { approved: true, annualInterestRate: Number(rate), note }, "Loan approved and disbursed."); }}>Approve</button><button className="button button-mini button-danger" onClick={() => { const note = window.prompt("Reason for rejection:", "Required supporting documents are missing"); if (note !== null) void doAction<Loan>(`/api/v1/employee/loans/${loan.id}/decision`, "POST", { approved: false, note }, "Loan application rejected."); }}>Reject</button></div> : <span className="muted-text">Reviewed</span>}</td></tr>)}
    </DataTable>{loans.length === 0 && <EmptyState title={role === "CUSTOMER" ? "No loan records" : "No pending applications"} />}</Panel>
    {repaymentLoan && <Modal title={`Repayment history · ${repaymentLoan.loanNumber}`} onClose={() => setRepaymentLoan(null)}><DataTable headers={["INSTALLMENT", "REFERENCE", "ACCOUNT", "AMOUNT", "PAID AT"]}>{repaymentHistory.map((item) => <tr key={item.reference}><td>{item.installmentNumber}</td><td>{item.reference}</td><td>{item.accountNumber}</td><td className="amount-text">{money(item.amount)}</td><td>{dateTime(item.paidAt)}</td></tr>)}</DataTable>{repaymentHistory.length === 0 && <EmptyState title="No repayments recorded" />}</Modal>}
  </>;

  const renderStaff = () => <>
    <ModuleHeader title="Staff management" subtitle="Create and manage Teller, Loan Officer and Manager accounts." action={<button className="button button-primary" onClick={() => setModal("staff")}><Plus size={15} /> Add staff member</button>} />
    <Panel title="Bank staff" subtitle="Admin-only employee directory and access controls"><DataTable headers={["STAFF MEMBER", "CONTACT", "ROLE", "STATUS", "JOINED", "ACTIONS"]}>
      {searchFilter(staff).map((member) => <tr key={member.id}><td><div className="person-cell"><span className="person-avatar">{initials(member.fullName)}</span><span><strong>{member.fullName}</strong><small>Staff #{member.id}</small></span></div></td><td>{member.email}<small className="cell-sub">{member.phone || "No phone"}</small></td><td><select className="inline-select" value={member.role} onChange={(e) => void doAction<Staff>(`/api/v1/admin/staff/${member.id}/role`, "PATCH", { role: e.target.value }, "Staff role updated.")}><option value="TELLER">Teller</option><option value="LOAN_OFFICER">Loan officer</option><option value="MANAGER">Manager</option></select></td><td><StatusPill value={member.enabled ? "ACTIVE" : "DISABLED"} /></td><td>{dateOnly(member.createdAt)}</td><td><button className={`button button-mini ${member.enabled ? "button-danger" : "button-secondary"}`} onClick={() => void doAction<Staff>(`/api/v1/admin/staff/${member.id}/status`, "PATCH", { enabled: !member.enabled }, `Staff ${member.enabled ? "deactivated" : "activated"}.`)}>{member.enabled ? "Deactivate" : "Activate"}</button></td></tr>)}
    </DataTable>{staff.length === 0 && <EmptyState title="No staff records" detail="Create the first staff account to get started." />}</Panel>
    {modal === "staff" && <Modal title="Create staff account" onClose={() => setModal(null)}><form className="form-grid" onSubmit={async (e) => { e.preventDefault(); const fd = new FormData(e.currentTarget); const ok = await doAction<Staff>("/api/v1/admin/staff", "POST", { fullName: fd.get("fullName"), email: fd.get("email"), password: fd.get("password"), phone: fd.get("phone"), nationalId: fd.get("nationalId"), address: fd.get("address"), role: fd.get("role") }, "Staff account created."); if (ok) setModal(null); }}><Field label="Full name"><input name="fullName" required maxLength={100} /></Field><Field label="Email"><input name="email" type="email" required maxLength={150} /></Field><Field label="Temporary password" hint="8–64 characters"><input name="password" type="password" minLength={8} maxLength={64} required /></Field><Field label="Phone"><input name="phone" maxLength={20} /></Field><Field label="National ID"><input name="nationalId" maxLength={30} /></Field><Field label="Role"><select name="role"><option value="TELLER">Teller</option><option value="LOAN_OFFICER">Loan officer</option><option value="MANAGER">Manager</option></select></Field><div className="form-span"><Field label="Address"><input name="address" maxLength={255} /></Field></div><div className="form-span form-actions"><button type="button" className="button button-secondary" onClick={() => setModal(null)}>Cancel</button><button className="button button-primary">Create staff</button></div></form></Modal>}
  </>;

  const renderFraud = () => <>
    <ModuleHeader title="Fraud monitoring" subtitle="Review open alerts generated by high-value or rapid transaction patterns." action={<span className="record-count">{fraudAlerts.length} open alerts</span>} />
    <div className="notice-box notice-warning"><ShieldAlert size={17} /><span>Alerts are investigation signals only. The backend does not automatically freeze funds or block transactions.</span></div>
    <Panel title="Open fraud alerts" subtitle="Resolve an alert after the appropriate review"><DataTable headers={["ALERT", "ACCOUNT", "AMOUNT", "SEVERITY", "REASON", "CREATED", "ACTION"]}>
      {searchFilter(fraudAlerts).map((alert) => <tr key={alert.id}><td><strong>#{alert.id}</strong><small className="cell-sub">{alert.transactionReference}</small></td><td>{alert.accountNumber}</td><td className="amount-text">{money(alert.amount)}</td><td><StatusPill value={alert.severity} /></td><td className="reason-cell">{alert.reason}</td><td>{dateTime(alert.createdAt)}</td><td><button className="button button-mini button-primary" onClick={() => { if (window.confirm(`Resolve fraud alert #${alert.id}?`)) void doAction<FraudAlert>(`/api/v1/admin/fraud-alerts/${alert.id}/resolve`, "PATCH", undefined, "Fraud alert resolved."); }}>Resolve</button></td></tr>)}
    </DataTable>{fraudAlerts.length === 0 && <EmptyState title="No open fraud alerts" detail="There are currently no alerts awaiting review." />}</Panel>
  </>;

  const renderAudit = () => <>
    <ModuleHeader title="Audit trail" subtitle="Recent operational events recorded by the backend for accountability." action={<span className="record-count">{auditLogs.length} events loaded</span>} />
    <Panel title="Recent audit events" subtitle="Read-only activity log"><DataTable headers={["TIME", "ACTOR", "ACTION", "ENTITY", "ENTITY ID", "DETAILS"]}>
      {searchFilter(auditLogs).map((log) => <tr key={log.id}><td>{dateTime(log.createdAt)}</td><td>{log.actorEmail || "System"}</td><td><strong>{log.action}</strong></td><td>{log.entityType}</td><td>{log.entityId || "—"}</td><td className="reason-cell">{log.details || "—"}</td></tr>)}
    </DataTable>{auditLogs.length === 0 && <EmptyState title="No audit events" />}</Panel>
  </>;

  const renderReports = () => <>
    <ModuleHeader title="Reports & analytics" subtitle="Banking performance summary and transaction ledger for the last 30 days." action={<button className="button button-secondary" onClick={() => window.print()}><Download size={15} /> Print report</button>} />
    <section className="stats-grid report-stats"><StatCard icon={<Users size={18} />} label="Total customers" value={number(overview?.totalCustomers)} tone="blue" caption="All registered customers" /><StatCard icon={<CreditCard size={18} />} label="Active accounts" value={number(overview?.activeAccounts)} tone="green" caption="Accounts with active status" /><StatCard icon={<ArrowDownLeft size={18} />} label="Deposits · 30 days" value={money(overview?.depositsLast30Days)} tone="violet" caption="Recorded deposit volume" /><StatCard icon={<ArrowUpRight size={18} />} label="Withdrawals · 30 days" value={money(overview?.withdrawalsLast30Days)} tone="amber" caption="Recorded withdrawal volume" /></section>
    <Panel title="Monthly interest posting" subtitle="Post interest for the current or previous month. The operation is idempotent by account and period key."><form className="inline-form" onSubmit={async (e) => { e.preventDefault(); const fd = new FormData(e.currentTarget); const period = String(fd.get("period")); await doAction<{ accountsCredited: number }>(`/api/v1/admin/interest/post?period=${encodeURIComponent(period)}`, "POST", undefined, "Interest posting request completed."); }}><Field label="Posting period"><input name="period" type="month" defaultValue={new Date().toISOString().slice(0, 7)} required /></Field><button className="button button-primary">Post monthly interest</button></form></Panel>
    <Panel title="Transaction report" subtitle="Recent bank-wide transaction activity"><TransactionTable rows={searchFilter(transactions)} admin /></Panel>
  </>;

  const renderNotifications = () => <>
    <ModuleHeader title="Notifications" subtitle="In-app updates related to your banking profile and activities." action={<span className="record-count">{unreadCount} unread</span>} />
    <Panel title="Your notifications" subtitle="Mark individual notifications as read"><div className="notification-list">{searchFilter(notifications).map((item) => <article className={`notification-row ${item.read ? "notification-read" : ""}`} key={item.id}><span className="notification-symbol"><Bell size={17} /></span><div className="notification-body"><strong>{item.title}</strong><p>{item.message}</p><small>{dateTime(item.createdAt)}</small></div>{!item.read && <button className="button button-mini button-secondary" onClick={() => void doAction<NotificationItem>(`/api/v1/notifications/${item.id}/read`, "PATCH", undefined, "Notification marked as read.")}>Mark read</button>}<StatusPill value={item.read ? "READ" : "UNREAD"} /></article>)}</div>{notifications.length === 0 && <EmptyState title="You're all caught up" detail="New account updates and activity notices will appear here." />}</Panel>
  </>;

  const renderProfile = () => <>
    <ModuleHeader title="My profile" subtitle="View and update the personal contact details connected to your banking identity." />
    <div className="profile-layout"><Panel title="Account identity" subtitle="Your identity and access role"><div className="profile-summary"><span className="profile-avatar">{initials(profile.fullName)}</span><div><strong>{profile.fullName}</strong><small>{profile.email}</small><StatusPill value={profile.role} /></div></div><div className="profile-info-row"><span>Customer / Staff ID</span><strong>#{profile.userId}</strong></div><div className="profile-info-row"><span>Access role</span><strong>{rolesLabel[profile.role]}</strong></div><div className="profile-info-row"><span>KYC status</span><StatusPill value={profile.kycStatus} /></div></Panel>
      <Panel title="Update contact details" subtitle="The backend validates full name, phone and address lengths"><form className="form-grid" onSubmit={async (e) => { e.preventDefault(); const fd = new FormData(e.currentTarget); const ok = await doAction<Profile>("/api/v1/auth/me", "PUT", { fullName: fd.get("fullName"), phone: fd.get("phone"), address: fd.get("address") }, "Profile updated successfully."); if (ok) { const refreshed = await getProfile(session!.token); setProfile(refreshed); } }}><Field label="Full name"><input name="fullName" required maxLength={100} defaultValue={profile.fullName} /></Field><Field label="Email address"><input value={profile.email} disabled /></Field><Field label="Phone"><input name="phone" maxLength={20} defaultValue={profile.phone || ""} /></Field><div className="form-span"><Field label="Address"><textarea name="address" maxLength={255} rows={3} defaultValue={profile.address || ""} /></Field></div><div className="form-span form-actions"><button className="button button-primary">Save profile</button></div></form></Panel></div>
  </>;

  let content: ReactNode;
  switch (activeView) {
    case "Dashboard": content = renderDashboard(); break;
    case "Customers": content = renderCustomers(); break;
    case "KYC review": content = renderKyc(); break;
    case "Accounts": content = renderAccounts(); break;
    case "Banking": content = renderBanking(); break;
    case "Transactions": content = renderTransactions(); break;
    case "Loans": content = renderLoans(); break;
    case "Reports & analytics": content = renderReports(); break;
    case "Staff management": content = renderStaff(); break;
    case "Fraud alerts": content = renderFraud(); break;
    case "Audit logs": content = renderAudit(); break;
    case "Notifications": content = renderNotifications(); break;
    case "My profile": content = renderProfile(); break;
    default: content = null;
  }

  return <main className="bank-shell">
    {sidebarOpen && <button className="mobile-scrim" aria-label="Close navigation" onClick={() => setSidebarOpen(false)} />}
    <aside className={`sidebar ${sidebarOpen ? "sidebar-open" : ""}`}>
      <div className="brand-row"><Image src="/brand/confused-bank-emblem.svg" alt="Confused Bank emblem" width={441} height={488} priority className="sidebar-brand-emblem" /><div className="brand-copy"><span className="brand-name">Confused<span> Bank</span></span><span className="brand-caption">BANKING PLATFORM</span></div><button className="sidebar-close icon-button" aria-label="Close menu" onClick={() => setSidebarOpen(false)}><X size={18} /></button></div>
      <div className="workspace-switcher"><div className="workspace-icon"><Image src="/brand/confused-bank-emblem.svg" alt="" aria-hidden="true" width={441} height={488} /></div><div className="workspace-copy"><strong>Confused Bank</strong><span>{rolesLabel[profile.role]}</span></div><ChevronDown size={15} className="muted-icon" /></div>
      <div className="nav-section-label">WORKSPACE</div><nav className="main-nav" aria-label="Main navigation">{navItems.map(({ label, icon: Icon }) => <button key={label} className={`nav-link ${activeView === label ? "nav-link-active" : ""}`} onClick={() => selectView(label)}><Icon size={18} strokeWidth={1.9} /><span>{label}</span>{label === "Notifications" && unreadCount > 0 && <span className="nav-count">{unreadCount}</span>}</button>)}</nav>
      <div className="sidebar-bottom"><div className="upgrade-card"><div className="upgrade-icon"><ShieldCheck size={17} /></div><strong>Secure workspace</strong><p>Your session uses temporary browser session storage and bearer-token API access.</p><span className="secure-sidebar-label"><i /> SESSION ACTIVE</span></div><div className="sidebar-user"><div className="avatar avatar-admin">{initials(profile.fullName)}</div><div className="user-copy"><strong>{profile.fullName}</strong><span>{rolesLabel[profile.role]}</span></div></div></div>
    </aside>
    <section className="main-area"><header className="topbar"><div className="topbar-left"><button className="mobile-menu icon-button" aria-label="Open menu" onClick={() => setSidebarOpen(true)}><Menu size={21} /></button><div className="breadcrumb">Workspace <ChevronRight size={14} /> <strong>{activeView}</strong></div></div><div className="topbar-actions"><label className="search-box"><Search size={17} /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search this view…" aria-label="Search current view" /><kbd>⌘ K</kbd></label><div className="notification-wrap"><button className="icon-button notification-button" aria-label="Notifications" onClick={() => { setShowNotifications((v) => !v); if (activeView !== "Notifications") selectView("Notifications"); }}><Bell size={19} />{unreadCount > 0 && <i />}</button>{showNotifications && <div className="notification-popover"><strong>{unreadCount} unread notifications</strong><p>Open the Notifications section to review updates.</p><button className="inline-link" onClick={() => { setShowNotifications(false); selectView("Notifications"); }}>View notifications</button></div>}</div><div className="topbar-divider" /><div className="profile-menu-wrap" ref={profileMenuRef}><button type="button" className="profile-button" aria-haspopup="menu" aria-expanded={showProfileMenu} onClick={() => setShowProfileMenu((open) => !open)}><div className="avatar avatar-small">{initials(profile.fullName)}</div><span><strong>{profile.fullName}</strong><small>{rolesLabel[profile.role]}</small></span><ChevronDown size={15} className={showProfileMenu ? "profile-chevron profile-chevron-open" : "profile-chevron"} /></button>{showProfileMenu && <div className="profile-dropdown" role="menu" aria-label="Account menu"><button type="button" role="menuitem" onClick={() => { setShowProfileMenu(false); selectView("My profile"); }}><UserRound size={16} /><span>Profile</span></button><div className="profile-dropdown-divider" /><button type="button" role="menuitem" className="profile-dropdown-signout" onClick={signOut}><LogOut size={16} /><span>Sign out</span></button></div>}</div></div></header>
      <div className="content-wrap"><div className="content-status-row"><span><span className="live-dot" /> Connected to banking API</span><button className="refresh-link" onClick={() => setReloadKey((v) => v + 1)}><RefreshCw size={13} /> Refresh</button></div>
        {error && <div className="global-alert global-alert-error"><ShieldAlert size={16} /><span>{error}</span><button onClick={() => setError("")} aria-label="Dismiss error"><X size={15} /></button></div>}
        {notice && <div className="global-alert global-alert-success"><CheckCircle2 size={16} /><span>{notice}</span><button onClick={() => setNotice("")} aria-label="Dismiss notice"><X size={15} /></button></div>}
        {loading && activeView !== "Dashboard" && <LoadingBlock />}
        <div key={activeView} className="view-transition">{content}</div>
        <footer className="page-footer"><span>© {new Date().getFullYear()} Confused Bank · Banking Management System</span><span><i className="live-dot footer-status" /> Secure API session <span className="footer-divider">·</span> {profile.role}</span></footer>
      </div>
    </section>
  </main>;
}

function StatCard({ icon, label, value, tone, caption }: { icon: ReactNode; label: string; value: string; tone: string; caption: string }) {
  return <article className="stat-card"><div className="stat-top"><span className={`stat-icon stat-icon-${tone}`}>{icon}</span><span className="stat-period">Live data</span></div><div className="stat-label">{label}</div><div className="stat-value stat-value-live">{value}</div><div className="stat-bottom"><span className="stat-comparison">{caption}</span></div></article>;
}
function QuickAction({ icon, title, detail, onClick }: { icon: ReactNode; title: string; detail: string; onClick: () => void }) {
  return <button className="quick-action" onClick={onClick}><span>{icon}</span><strong>{title}</strong><small>{detail}</small><ChevronRight size={14} className="quick-chevron" /></button>;
}
function ModuleHeader({ title, subtitle, action }: { title: string; subtitle: string; action?: ReactNode }) {
  return <div className="module-header"><div><div className="eyebrow"><span className="live-dot" /> BANKING OPERATIONS</div><h1>{title}</h1><p className="page-subtitle">{subtitle}</p></div>{action}</div>;
}
function DataTable({ headers, children }: { headers: string[]; children: ReactNode }) {
  return <div className="table-scroll"><table className="transaction-table data-table"><thead><tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr></thead><tbody>{children}</tbody></table></div>;
}
function TransactionTable({ rows, admin }: { rows: Transaction[]; admin: boolean }) {
  return <DataTable headers={["REFERENCE", ...(admin ? ["SOURCE", "DESTINATION"] : ["ACCOUNT FLOW"]), "TYPE", "DATE", "AMOUNT"]}>{rows.map((t) => <tr key={t.id}><td><strong className="transaction-id">{t.reference}</strong></td>{admin ? <><td>{t.sourceAccountNumber || "—"}</td><td>{t.destinationAccountNumber || "—"}</td></> : <td>{t.sourceAccountNumber || t.destinationAccountNumber || "—"}</td>}<td><span className={`type-pill type-${t.type.toLowerCase()}`}>{t.type.replaceAll("_", " ")}</span></td><td>{dateTime(t.createdAt)}</td><td className="amount-text">{money(t.amount)}</td></tr>)}</DataTable>;
}
function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}><section className="modal-card" role="dialog" aria-modal="true" aria-label={title}><div className="modal-heading"><div><strong>{title}</strong><small>Confused Bank · Secure operation</small></div><button className="icon-button" onClick={onClose} aria-label="Close"><X size={18} /></button></div>{children}</section></div>;
}
function LoanRepaymentAction({ loan, accounts, token, onDone, onError }: { loan: Loan; accounts: Account[]; token: string; onDone: () => void; onError: (message: string) => void }) {
  const [busy, setBusy] = useState(false);
  const [open, setOpen] = useState(false);
  if (loan.status !== "DISBURSED" || !loan.outstandingAmount || loan.outstandingAmount <= 0) return <span className="muted-text">—</span>;
  return <div className="repayment-action">{!open ? <button className="button button-mini button-secondary" onClick={() => setOpen(true)}>Repay</button> : <form onSubmit={async (e) => { e.preventDefault(); const fd = new FormData(e.currentTarget); setBusy(true); try { await apiRequest<LoanRepayment>(`/api/v1/loans/${loan.id}/repayments`, token, { method: "POST", body: JSON.stringify({ accountNumber: fd.get("accountNumber"), amount: Number(fd.get("amount")) }) }); onDone(); setOpen(false); } catch (error) { onError(getError(error)); } finally { setBusy(false); } }}><select name="accountNumber" required>{accounts.filter((a) => a.status === "ACTIVE").map((a) => <option key={a.id} value={a.accountNumber}>{a.accountNumber}</option>)}</select><input name="amount" type="number" min="0.01" max={loan.outstandingAmount} step="0.01" required placeholder="Installment" defaultValue={loan.monthlyInstallment || loan.outstandingAmount} /><button className="button button-mini button-primary" disabled={busy}>{busy ? "…" : "Pay"}</button><button type="button" className="button button-mini button-secondary" onClick={() => setOpen(false)}>×</button></form>}</div>;
}
