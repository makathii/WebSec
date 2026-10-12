import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <div class="column narrow center">
      <h1>Page not found</h1>
      <p class="muted">The link may be broken or the page was removed.</p>
      <a routerLink="/" class="button">Back to the feed</a>
    </div>
  `,
})
export class NotFound {}
