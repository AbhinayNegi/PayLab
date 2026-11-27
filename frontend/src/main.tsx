import { useEffect, useRef, useState, type FormEvent } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { api, ApiError, type Profile, type Session } from './api';
import './styles.css';

const sessionKey = 'paylab.session';
const passwordHint = 'Use 8–128 characters, with an uppercase letter, a lowercase letter, and a digit.';
type BrowserSession = { token: string; expiresAt: number };

function readSession(): BrowserSession | null {
  try {
    const stored: BrowserSession | null = JSON.parse(sessionStorage.getItem(sessionKey) ?? 'null');
    if (stored && typeof stored.token === 'string' && Number.isFinite(stored.expiresAt)
        && stored.expiresAt > Date.now()) return stored;
  } catch {
    // Malformed session data is treated as signed out.
  }
  sessionStorage.removeItem(sessionKey);
  return null;
}

function Brand() {
  return <div className="brand"><span className="brand-mark" aria-hidden="true">P</span>PayLab</div>;
}

function AuthForm({ registration, onLogin }: { registration: boolean; onLogin: (session: Session) => void }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [fields, setFields] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const sending = useRef(false);
  const form = useRef<HTMLFormElement>(null);

  function showFields(next: Record<string, string>) {
    setFields(next);
    const input = Object.keys(next)[0];
    if (input) requestAnimationFrame(() => form.current?.querySelector<HTMLInputElement>('[name="' + input + '"]')?.focus());
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (sending.current) return;
    const values = new FormData(event.currentTarget);
    const fullName = String(values.get('fullName') ?? '').trim();
    const email = String(values.get('email') ?? '').trim();
    const password = String(values.get('password') ?? '');
    const confirmPassword = String(values.get('confirmPassword') ?? '');
    const next: Record<string, string> = {};
    if (registration && (!fullName || fullName.length > 120)) next.fullName = 'Enter a full name up to 120 characters.';
    const emailInput = document.createElement('input');
    emailInput.type = 'email';
    emailInput.value = email;
    if (!email || email.length > 254 || /\s/.test(email) || emailInput.validity.typeMismatch) {
      next.email = 'Enter a valid email address.';
    }
    if (registration && (password.length < 8 || password.length > 128 ||
        !/[A-Z]/.test(password) || !/[a-z]/.test(password) || !/[0-9]/.test(password))) next.password = passwordHint;
    if (!registration && (!password.trim() || password.length > 128)) next.password = 'Enter your password.';
    if (registration && (!confirmPassword || password !== confirmPassword)) next.confirmPassword = 'Passwords must match.';
    setError('');
    showFields(next);
    if (Object.keys(next).length) return;

    sending.current = true;
    setBusy(true);
    try {
      if (registration) {
        await api<Profile>('/auth/register', { body: { fullName, email, password, confirmPassword } });
        navigate('/login', { replace: true, state: { registered: true, email } });
      } else {
        const session = await api<Session>('/auth/login', { body: { email, password } });
        onLogin(session);
      }
    } catch (failure) {
      if (failure instanceof ApiError) {
        setError(failure.message);
        showFields(failure.fields);
      } else {
        setError('Could not reach PayLab. Try again.');
      }
    } finally {
      sending.current = false;
      setBusy(false);
    }
  }

  function field(name: string, label: string, type: string, autocomplete: string, maxLength: number, hint?: string) {
    return <div className="field">
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} type={type} autoComplete={autocomplete} required maxLength={maxLength}
        defaultValue={name === 'email' ? location.state?.email : undefined}
        aria-invalid={Boolean(fields[name])}
        aria-describedby={[hint && name + '-hint', fields[name] && name + '-error'].filter(Boolean).join(' ') || undefined} />
      {hint && <small id={name + '-hint'}>{hint}</small>}
      {fields[name] && <small className="error-text" id={name + '-error'}>{fields[name]}</small>}
    </div>;
  }

  return <main className="auth-layout">
    <aside className="auth-aside">
      <Brand />
      <div className="aside-copy">
        <span className="eyebrow">Merchant workspace</span>
        <p>Payment<br />operations.</p>
        <div className="aside-caption">Manage your PayLab merchant account.</div>
      </div>
      <span className="aside-footer">PayLab · Training environment</span>
    </aside>
    <section className="auth-main" aria-labelledby="auth-title">
      <div className="auth-content">
        <h1 id="auth-title">{registration ? 'Create an account' : 'Sign in to PayLab'}</h1>
        <p className="muted">{registration ? 'Set up your merchant profile.' : 'Access your merchant dashboard.'}</p>
        {!registration && location.state?.registered && <p className="notice" role="status">Account created. Sign in to continue.</p>}
        {error && <p className="error" role="alert">{error}</p>}
        <form ref={form} onSubmit={submit} noValidate>
          {registration && field('fullName', 'Full name', 'text', 'name', 120)}
          {field('email', 'Email address', 'email', 'email', 254)}
          {field('password', 'Password', 'password', registration ? 'new-password' : 'current-password', 128,
            registration ? passwordHint : undefined)}
          {registration && field('confirmPassword', 'Confirm password', 'password', 'new-password', 128)}
          <button className="primary" disabled={busy}>{busy ? 'Please wait…' : registration ? 'Register' : 'Login'}</button>
        </form>
        <p className="auth-switch">{registration ? 'Already registered? ' : 'New to PayLab? '}
          <Link to={registration ? '/login' : '/register'}>{registration ? 'Log in' : 'Create an account'}</Link>
        </p>
      </div>
    </section>
  </main>;
}

function Dashboard({ profile, logout }: { profile: Profile; logout: () => void }) {
  const initials = profile.fullName.trim().split(/\s+/).slice(0, 2).map(part => Array.from(part)[0]).join('').toUpperCase();
  return <div className="dashboard">
    <aside className="sidebar">
      <Brand />
      <nav aria-label="Dashboard sections">
        <a href="#overview">Overview</a><a href="#wallet">Wallet</a><a href="#transactions">Transactions</a>
      </nav>
    </aside>
    <div className="workspace">
      <header className="topbar">
        <span className="workspace-label">Merchant workspace</span>
        <div className="topbar-actions"><span className="avatar" aria-hidden="true">{initials}</span>
          <span className="account-name">{profile.fullName}</span><button className="secondary" onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="dashboard-content">
        <section id="overview" aria-labelledby="overview-title">
          <h1 id="overview-title">Dashboard overview</h1>
          <p className="muted">Welcome, {profile.fullName}.</p>
          <article className="card" aria-labelledby="profile-title">
            <h2 id="profile-title">Merchant profile</h2>
            <dl data-testid="merchant-profile"><div><dt>Full name</dt><dd>{profile.fullName}</dd></div>
              <div><dt>Email address</dt><dd>{profile.email}</dd></div>
              <div><dt>Role</dt><dd className="role-name">{profile.role.toLowerCase()}</dd></div></dl>
          </article>
        </section>
        <div className="card-grid">
          <section className="card" id="wallet" aria-labelledby="wallet-title">
            <h2 id="wallet-title">Wallet</h2><p className="muted">No wallet activity to display.</p>
          </section>
          <section className="card" id="transactions" aria-labelledby="transactions-title">
            <h2 id="transactions-title">Transactions</h2><p className="muted">No transactions to display.</p>
          </section>
        </div>
      </main>
    </div>
  </div>;
}

function ProtectedDashboard({ token, logout }: { token: string; logout: () => void }) {
  const [profile, setProfile] = useState<Profile>();
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    api<Profile>('/users/me', { token, signal: controller.signal }).then(setProfile).catch(failure => {
      if (controller.signal.aborted) return;
      if (failure instanceof ApiError && failure.status === 401) logout();
      else setError(true);
    });
    return () => controller.abort();
  }, [token, logout, retry]);

  if (profile) return <Dashboard profile={profile} logout={logout} />;
  return <main className="session-status">
    {error ? <><p role="alert">Could not load your merchant profile.</p>
      <button className="secondary" onClick={() => { setError(false); setRetry(retry + 1); }}>Retry</button>{' '}
      <button className="secondary" onClick={logout}>Log out</button></>
      : <p role="status">Loading your merchant profile…</p>}
  </main>;
}

function App() {
  const [session, setSession] = useState(readSession);
  const token = session?.token;
  const [logout] = useState(() => () => {
    sessionStorage.removeItem(sessionKey);
    setSession(null);
  });

  useEffect(() => {
    if (!session) return;
    const checkExpiration = () => {
      if (Date.now() >= session.expiresAt) logout();
    };
    const timer = window.setTimeout(checkExpiration, Math.max(0, session.expiresAt - Date.now()));
    window.addEventListener('focus', checkExpiration);
    return () => {
      window.clearTimeout(timer);
      window.removeEventListener('focus', checkExpiration);
    };
  }, [session, logout]);

  function login(result: Session) {
    const next = { token: result.accessToken, expiresAt: Date.now() + result.expiresIn * 1000 };
    sessionStorage.setItem(sessionKey, JSON.stringify(next));
    setSession(next);
  }

  return <Routes>
    <Route path="/login" element={token ? <Navigate to="/dashboard" replace /> :
      <AuthForm key="login" registration={false} onLogin={login} />} />
    <Route path="/register" element={token ? <Navigate to="/dashboard" replace /> :
      <AuthForm key="register" registration onLogin={login} />} />
    <Route path="/dashboard" element={token ? <ProtectedDashboard token={token} logout={logout} /> :
      <Navigate to="/login" replace />} />
    <Route path="*" element={<Navigate to={token ? '/dashboard' : '/login'} replace />} />
  </Routes>;
}

createRoot(document.getElementById('root')!).render(<BrowserRouter><App /></BrowserRouter>);
