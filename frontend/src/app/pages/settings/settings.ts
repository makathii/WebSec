import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PasswordChecklist } from '../../components/password-checklist/password-checklist';
import { AuthService } from '../../services/auth.service';
import { errorMessage, passwordRules, passwordsMatch } from '../../validation';

@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, PasswordChecklist],
  templateUrl: './settings.html',
})
export class Settings {
  protected auth = inject(AuthService);
  private router = inject(Router);

  protected passwordError = signal('');
  protected deleteError = signal('');
  protected sending = signal(false);
  protected showPassword = signal(false);

  protected passwordForm = new FormGroup(
    {
      current: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      confirm: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    },
    { validators: [(group: AbstractControl) => this.strongPassword(group), passwordsMatch] },
  );

  protected deleteForm = new FormGroup({
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  private strongPassword(group: AbstractControl): ValidationErrors | null {
    const ok = passwordRules(group.value.password, this.auth.user()?.username).every((rule) => rule.ok);
    return ok ? null : { weakPassword: true };
  }

  changePassword() {
    if (this.passwordForm.invalid) {
      return;
    }
    const { current, password } = this.passwordForm.getRawValue();
    this.sending.set(true);
    this.passwordError.set('');
    this.auth.changePassword(current, password).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { reason: 'password-changed' } }),
      error: (err) => {
        this.sending.set(false);
        this.passwordError.set(errorMessage(err, 'Could not change the password'));
      },
    });
  }

  deleteAccount() {
    const { password, username } = this.deleteForm.getRawValue();
    if (username !== this.auth.user()?.username) {
      this.deleteError.set('Please type your username to confirm.');
      return;
    }
    this.sending.set(true);
    this.deleteError.set('');
    this.auth.deleteAccount(password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.sending.set(false);
        this.deleteError.set(errorMessage(err, 'Could not delete the account'));
      },
    });
  }
}
