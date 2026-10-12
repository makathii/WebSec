import { Component, computed, input } from '@angular/core';
import { passwordRules, passwordStrength } from '../../validation';

const LABELS = ['', 'Too weak', 'Okay', 'Good', 'Strong'];

@Component({
  selector: 'app-password-checklist',
  templateUrl: './password-checklist.html',
})
export class PasswordChecklist {
  password = input('');
  username = input('');
  email = input('');

  protected rules = computed(() => passwordRules(this.password(), this.username(), this.email()));
  protected strength = computed(() => passwordStrength(this.password(), this.username(), this.email()));
  protected label = computed(() => LABELS[this.strength()]);
}
