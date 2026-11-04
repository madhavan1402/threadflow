import { describe, expect, it } from 'vitest';
import { validateLogin } from './auth';

describe('login form validation', () => {
  it('requires email and password', () => {
    expect(validateLogin('', '')).toEqual({
      email: 'Email is required',
      password: 'Password is required',
    });
  });

  it('rejects malformed email', () => {
    expect(validateLogin('not-an-email', 'password')).toEqual({
      email: 'Enter a valid email address',
    });
  });

  it('accepts valid login fields', () => {
    expect(validateLogin('merch@threadflow.local', 'correct-password')).toEqual({});
  });
});
