import { Component, ElementRef, OnDestroy, effect, input, output, viewChild } from '@angular/core';

// Cloudflare Turnstile, see https://developers.cloudflare.com/turnstile/get-started/client-side-rendering/
// The script has to come from Cloudflare directly (no self-hosting), the CSP in nginx.conf.template allows it.
const SCRIPT_URL = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';

interface TurnstileApi {
  render(
    container: HTMLElement,
    options: {
      sitekey: string;
      action?: string;
      theme?: 'light' | 'dark' | 'auto';
      size?: 'normal' | 'flexible' | 'compact';
      callback: (token: string) => void;
      'expired-callback': () => void;
      'error-callback': () => void;
    },
  ): string;
  reset(widgetId: string): void;
  remove(widgetId: string): void;
}

declare global {
  interface Window {
    turnstile?: TurnstileApi;
  }
}

let loading: Promise<TurnstileApi> | null = null;

function loadTurnstile(): Promise<TurnstileApi> {
  if (!loading) {
    loading = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = SCRIPT_URL;
      script.async = true;
      script.onload = () => (window.turnstile ? resolve(window.turnstile) : reject(new Error('Turnstile missing')));
      script.onerror = () => {
        loading = null;
        script.remove();
        reject(new Error('Turnstile could not be loaded'));
      };
      document.head.appendChild(script);
    });
  }
  return loading;
}

@Component({
  selector: 'app-turnstile',
  template: '<div #container></div>',
})
export class Turnstile implements OnDestroy {
  siteKey = input.required<string>();
  action = input<string>();

  // a fresh token, or null when the token expired or the check failed
  token = output<string | null>();
  failed = output<void>();

  private container = viewChild.required<ElementRef<HTMLElement>>('container');
  private api: TurnstileApi | null = null;
  private widgetId: string | null = null;
  private destroyed = false;

  constructor() {
    effect(() => {
      const siteKey = this.siteKey();
      const action = this.action();
      const element = this.container().nativeElement;
      loadTurnstile().then(
        (api) => {
          if (this.destroyed) {
            return;
          }
          this.api = api;
          this.widgetId = api.render(element, {
            sitekey: siteKey,
            action,
            theme: 'light',
            size: 'flexible',
            callback: (token) => this.token.emit(token),
            'expired-callback': () => this.token.emit(null),
            'error-callback': () => {
              this.token.emit(null);
              this.failed.emit();
            },
          });
        },
        () => this.failed.emit(),
      );
    });
  }

  // tokens are single use, get a new one after each submit
  reset() {
    if (this.api && this.widgetId) {
      this.token.emit(null);
      this.api.reset(this.widgetId);
    }
  }

  ngOnDestroy() {
    this.destroyed = true;
    if (this.api && this.widgetId) {
      this.api.remove(this.widgetId);
    }
  }
}
