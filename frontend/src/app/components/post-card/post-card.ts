import { Component, inject, input, linkedSignal, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Post } from '../../models';
import { AuthService } from '../../services/auth.service';
import { PostService } from '../../services/post.service';
import { TimeAgoPipe } from '../../time-ago.pipe';

@Component({
  selector: 'app-post-card',
  imports: [RouterLink, TimeAgoPipe],
  templateUrl: './post-card.html',
})
export class PostCard {
  data = input.required<Post>();

  protected post = linkedSignal(() => this.data());
  protected liking = signal(false);
  protected auth = inject(AuthService);
  protected posts = inject(PostService);
  private router = inject(Router);

  toggleLike() {
    if (!this.auth.user()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (this.liking()) {
      return;
    }
    const post = this.post();
    this.liking.set(true);
    const request = post.likedByMe ? this.posts.unlike(post.id) : this.posts.like(post.id);
    request.subscribe({
      next: () => {
        this.post.set({
          ...post,
          likedByMe: !post.likedByMe,
          likeCount: post.likeCount + (post.likedByMe ? -1 : 1),
        });
        this.liking.set(false);
      },
      error: () => this.liking.set(false),
    });
  }
}
