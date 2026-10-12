import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PasswordChecklist } from '../../components/password-checklist/password-checklist';
import { AuthService } from '../../services/auth.service';
import { EMAIL, USERNAME, errorMessage, passwordRules, passwordsMatch } from '../../validation';

function strongPassword(group: AbstractControl): ValidationErrors | null {
  const { password, username, email } = group.value;
  return passwordRules(password, username, email).every((rule) => rule.ok) ? null : { weakPassword: true };
}

@Component({
  selector: 'app-signup',
  imports: [ReactiveFormsModule, RouterLink, PasswordChecklist],
  templateUrl: './signup.html',
})
export class Signup {
  private auth = inject(AuthService);
  private router = inject(Router);

  protected error = signal('');
  protected sending = signal(false);
  protected showPassword = signal(false);

  protected form = new FormGroup(
    {
      email: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(254), Validators.pattern(EMAIL)],
      }),
      username: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.pattern(USERNAME)],
      }),
      password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      confirm: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    },
    { validators: [strongPassword, passwordsMatch] },
  );

  // usernames are lowercase only, so we help a bit while typing
  lowercaseUsername() {
    const control = this.form.controls.username;
    control.setValue(control.value.toLowerCase().trim());
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, username, password } = this.form.getRawValue();
    this.sending.set(true);
    this.error.set('');
    this.auth.signup(email.trim().toLowerCase(), username, password).subscribe({
      next: () => {
        this.auth.login(username, password).subscribe({
          next: () => this.router.navigate(['/']),
          error: () => this.router.navigate(['/login']),
        });
      },
      error: (err) => {
        this.sending.set(false);
        this.error.set(errorMessage(err, 'Could not sign up'));
      },
    });
  }
}
