import { Component, computed, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { from, switchMap } from 'rxjs';
import { solveCaptcha } from '../../captcha';
import { CaptchaSolution } from '../../models';
import { AuthService } from '../../services/auth.service';
import { errorMessage, safeReturnUrl } from '../../validation';

const REASONS: Record<string, string> = {
  expired: 'Your session expired, please log in again.',
  'password-changed': 'Your password was changed, please log in with the new password.',
};

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  // from the query string, bound by withComponentInputBinding
  returnUrl = input<string>();
  reason = input<string>();

  private auth = inject(AuthService);
  private router = inject(Router);

  protected info = computed(() => REASONS[this.reason() ?? ''] ?? '');
  protected error = signal('');
  protected sending = signal(false);
  protected checking = signal(false);
  protected showPassword = signal(false);

  protected form = new FormGroup({
    login: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(254)] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(256)] }),
  });

  submit() {
    if (this.form.invalid) {
      return;
    }
    const { login, password } = this.form.getRawValue();
    this.sending.set(true);
    this.error.set('');
    this.tryLogin(login.trim(), password);
  }

  private tryLogin(login: string, password: string, captcha?: CaptchaSolution) {
    this.auth.login(login, password, captcha).subscribe({
      next: () => this.router.navigateByUrl(safeReturnUrl(this.returnUrl())),
      error: (err) => {
        if (err.status === 428 && !captcha) {
          this.solveCaptchaAndRetry(login, password);
          return;
        }
        this.sending.set(false);
        this.form.controls.password.reset();
        this.error.set(err.status === 400 ? 'Invalid login or password' : errorMessage(err));
      },
    });
  }

  private solveCaptchaAndRetry(login: string, password: string) {
    this.checking.set(true);
    this.auth
      .captchaChallenge()
      .pipe(switchMap((challenge) => from(solveCaptcha(challenge))))
      .subscribe({
        next: (solution) => {
          this.checking.set(false);
          this.tryLogin(login, password, solution);
        },
        error: (err) => {
          this.checking.set(false);
          this.sending.set(false);
          this.error.set(errorMessage(err, 'Could not check that you are not a bot, please try again'));
        },
      });
  }
}
