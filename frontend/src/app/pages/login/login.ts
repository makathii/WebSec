import { Component, computed, inject, input, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Turnstile } from '../../components/turnstile/turnstile';
import { AuthService } from '../../services/auth.service';
import { errorMessage, safeReturnUrl } from '../../validation';

const REASONS: Record<string, string> = {
  expired: 'Your session expired, please log in again.',
  'password-changed': 'Your password was changed, please log in with the new password.',
};

const CAPTCHA_FAILED = 'Could not check that you are not a bot, please reload the page and try again.';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Turnstile],
  templateUrl: './login.html',
})
export class Login {
  // from the query string, bound by withComponentInputBinding
  returnUrl = input<string>();
  reason = input<string>();

  private auth = inject(AuthService);
  private router = inject(Router);
  private turnstile = viewChild(Turnstile);

  protected info = computed(() => REASONS[this.reason() ?? ''] ?? '');
  protected error = signal('');
  protected sending = signal(false);
  protected showPassword = signal(false);

  // set once the backend asks for a captcha (428), the widget is shown from then on
  protected captchaSiteKey = signal<string | null>(null);
  protected captchaToken = signal<string | null>(null);
  // after a 428 the login is sent again as soon as the captcha is solved
  private retryAfterCaptcha = false;

  protected form = new FormGroup({
    login: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(254)] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(256)] }),
  });

  protected canSubmit = computed(() => !this.sending() && (!this.captchaSiteKey() || !!this.captchaToken()));

  submit() {
    if (this.form.invalid || !this.canSubmit()) {
      return;
    }
    const { login, password } = this.form.getRawValue();
    const captcha = this.captchaToken() ?? undefined;
    // a token works only once, the next attempt needs a new one
    this.turnstile()?.reset();
    this.sending.set(true);
    this.error.set('');

    this.auth.login(login.trim(), password, captcha).subscribe({
      next: () => this.router.navigateByUrl(safeReturnUrl(this.returnUrl())),
      error: (err) => {
        this.sending.set(false);
        if (err.status === 428) {
          // retry by itself only the first time, a rejected token needs a click on "Log in" again
          this.requireCaptcha(!captcha);
          return;
        }
        this.form.controls.password.reset();
        this.error.set(err.status === 400 ? 'Invalid login or password' : errorMessage(err));
      },
    });
  }

  protected onCaptchaToken(token: string | null) {
    this.captchaToken.set(token);
    if (token && this.retryAfterCaptcha) {
      this.retryAfterCaptcha = false;
      this.submit();
    }
  }

  protected onCaptchaFailed() {
    this.retryAfterCaptcha = false;
    this.error.set(CAPTCHA_FAILED);
  }

  private requireCaptcha(retry: boolean) {
    this.retryAfterCaptcha = retry;
    this.error.set(retry ? 'Please confirm that you are not a bot.' : 'The bot check failed, please try again.');
    if (this.captchaSiteKey()) {
      return;
    }
    this.auth.captchaConfig().subscribe({
      next: (config) => this.captchaSiteKey.set(config.siteKey),
      error: (err) => {
        this.retryAfterCaptcha = false;
        this.error.set(errorMessage(err, CAPTCHA_FAILED));
      },
    });
  }
}
