import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PostCard } from '../../components/post-card/post-card';
import { Post } from '../../models';
import { AuthService } from '../../services/auth.service';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-feed',
  imports: [PostCard, RouterLink],
  templateUrl: './feed.html',
})
export class Feed implements OnInit {
  private postService = inject(PostService);
  protected auth = inject(AuthService);

  protected posts = signal<Post[]>([]);
  protected hasNext = signal(false);
  protected loading = signal(true);
  protected error = signal('');
  private page = 0;

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.postService.feed(this.page).subscribe({
      next: (result) => {
        this.posts.update((posts) => [...posts, ...result.items]);
        this.hasNext.set(result.hasNext);
        this.page++;
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load the feed. Please try again later.');
        this.loading.set(false);
      },
    });
  }
}
