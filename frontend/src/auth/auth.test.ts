import { describe, expect, it } from 'vitest';
import { getDefaultWorkspaceHash, isWorkspaceAllowed, validateLogin } from './auth';

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

describe('role workspaces', () => {
  it('redirects merchandisers to orders', () => {
    expect(getDefaultWorkspaceHash(['MERCHANDISER'])).toBe('#orders');
  });

  it('redirects supervisors to production', () => {
    expect(getDefaultWorkspaceHash(['SUPERVISOR'])).toBe('#production');
  });

  it('blocks supervisor access to orders', () => {
    expect(isWorkspaceAllowed('#orders', ['SUPERVISOR'])).toBe(false);
  });

  it('blocks merchandiser access to production', () => {
    expect(isWorkspaceAllowed('#production', ['MERCHANDISER'])).toBe(false);
  });

  it('allows overview only as a shared workspace', () => {
    expect(isWorkspaceAllowed('#overview', ['SUPERVISOR'])).toBe(true);
    expect(isWorkspaceAllowed('#overview', ['MERCHANDISER'])).toBe(true);
  });
});
