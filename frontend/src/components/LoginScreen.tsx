import { useState, type FormEvent } from 'react';
import { Eye, EyeOff, LockKeyhole } from 'lucide-react';
import { AuthApiError, getDefaultWorkspaceHash, login, validateLogin } from '../auth/auth';
import { useAuth } from '../auth/AuthProvider';

export function LoginScreen() {
  const { setUser } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const validation = validateLogin(email, password);
    setErrors(validation);
    setServerError('');
    if (Object.keys(validation).length) return;

    setLoading(true);
    try {
      const user = await login(email.trim(), password);
      setUser(user);
      window.location.hash = getDefaultWorkspaceHash(user.roles);
    } catch (error) {
      if (error instanceof AuthApiError) {
        if (error.status === 401) setServerError(error.message || 'Email or password is incorrect.');
        else setServerError(error.message || 'ThreadFlow could not reach the server.');
      } else {
        setServerError('ThreadFlow could not reach the server. Try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-brand-panel" aria-label="ThreadFlow production workspace">
        <div className="login-brand">
          <div className="brand-mark"><span /></div>
          <strong>ThreadFlow</strong>
        </div>
        <div className="login-production-note">
          <strong>Every order, from yarn to dispatch.</strong>
          <p>Production intelligence for knitwear exports.</p>
          <ul className="login-highlights" aria-label="ThreadFlow capabilities">
            <li>Order tracking</li>
            <li>Production progress</li>
            <li>Delay intelligence</li>
          </ul>
        </div>
        <a
          className="login-photo-credit"
          href="https://commons.wikimedia.org/wiki/File:Garment_factory_in_Bangladesh_Women_working.jpg"
          target="_blank"
          rel="noreferrer"
        >
          Garment factory photograph by Tareq Salahuddin, CC BY 2.0
        </a>
      </section>
      <section className="login-form-panel">
        <form className="login-form" onSubmit={submit} noValidate>
          <div className="login-heading">
            <div className="login-lock" aria-hidden="true"><LockKeyhole size={18} /></div>
            <h1>Sign in to ThreadFlow</h1>
            <p>Use your ThreadFlow workspace account.</p>
          </div>

          {serverError && <div className="login-alert" role="alert">{serverError}</div>}

          <label className="login-field">
            <span>Email address</span>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? 'login-email-error' : undefined}
              disabled={loading}
            />
            {errors.email && <small id="login-email-error">{errors.email}</small>}
          </label>

          <label className="login-field">
            <span>Password</span>
            <div className="password-control">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? 'login-password-error' : undefined}
                disabled={loading}
              />
              <button
                type="button"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                onClick={() => setShowPassword((visible) => !visible)}
              >
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </div>
            {errors.password && <small id="login-password-error">{errors.password}</small>}
          </label>

          <button className="login-submit" type="submit" disabled={loading}>
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
      </section>
    </main>
  );
}
