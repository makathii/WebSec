import { AbstractControl, ValidationErrors } from '@angular/forms';

// Same rules as backend/.../validation/Patterns.java and PasswordPolicy.java.
// Only for nicer forms, the backend checks everything again.
export const USERNAME = /^(?!\.)[a-z0-9_.]{3,20}(?<!\.)$/;
export const EMAIL = /^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,185}\.[A-Za-z]{2,24}$/;
export const TEXT = /^[\p{L}\p{M}\p{N}\p{P}\p{Zs}\p{So}\p{Sc}\p{Sk}+=‍]*$/u;

export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

export interface PasswordRule {
  text: string;
  ok: boolean;
}

export function passwordRules(password: string, username = '', email = ''): PasswordRule[] {
  const lower = password.toLowerCase();
  const classes = [/[a-z]/, /[A-Z]/, /[0-9]/, /[^a-zA-Z0-9]/].filter((r) => r.test(password)).length;
  const localPart = email.toLowerCase().split('@')[0];
  const containsName =
    (username.length >= 3 && lower.includes(username.toLowerCase())) ||
    (localPart.length >= 3 && lower.includes(localPart));

  return [
    { text: '12 to 128 characters', ok: password.length >= 12 && password.length <= 128 },
    { text: 'At least 3 of: lowercase, uppercase, digit, symbol', ok: classes >= 3 },
    { text: 'No character more than 3 times in a row', ok: !/(.)\1{3}/.test(password) },
    { text: 'Does not contain your username or email', ok: !containsName },
    { text: 'Only letters, digits, spaces and standard symbols', ok: /^[\x20-\x7E]*$/.test(password) },
  ];
}

// 0 = too weak ... 4 = strong. Common passwords are only detected by the backend.
export function passwordStrength(password: string, username = '', email = ''): number {
  if (!passwordRules(password, username, email).every((rule) => rule.ok)) {
    return password.length > 0 ? 1 : 0;
  }
  const classes = [/[a-z]/, /[A-Z]/, /[0-9]/, /[^a-zA-Z0-9]/].filter((r) => r.test(password)).length;
  let score = 2;
  if (password.length >= 16) score++;
  if (classes === 4 || password.length >= 20) score++;
  return Math.min(score, 4);
}

export function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirm')?.value;
  return password === confirm ? null : { mismatch: true };
}

// only allow paths inside this app, so ?returnUrl can't be used as an open redirect
export function safeReturnUrl(url: string | undefined): string {
  return url && /^\/(?![/\\])/.test(url) ? url : '/';
}

export function errorMessage(err: unknown, fallback = 'Something went wrong'): string {
  const body = (err as { error?: { error?: unknown } })?.error;
  return typeof body?.error === 'string' ? body.error : fallback;
}
