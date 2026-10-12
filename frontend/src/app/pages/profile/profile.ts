import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Post, Profile as ProfileData } from '../../models';
import { AuthService } from '../../services/auth.service';
import { PostService } from '../../services/post.service';
import { UserService } from '../../services/user.service';
import { USERNAME } from '../../validation';

@Component({
  selector: 'app-profile',
  imports: [RouterLink, DatePipe],
  templateUrl: './profile.html',
})
export class Profile {
  username = input.required<string>();

  private userService = inject(UserService);
  private auth = inject(AuthService);
  protected postService = inject(PostService);

  protected profile = signal<ProfileData | null>(null);
  protected posts = signal<Post[]>([]);
  protected hasNext = signal(false);
  protected notFound = signal(false);
  protected isMe = computed(() => this.auth.user()?.username === this.username());
  private page = 0;

  constructor() {
    // runs again when navigating from one profile to another
    effect(() => this.open(this.username()));
  }

  private open(username: string) {
    this.profile.set(null);
    this.posts.set([]);
    this.notFound.set(false);
    this.page = 0;

    if (!USERNAME.test(username)) {
      this.notFound.set(true);
      return;
    }
    this.userService.get(username).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.loadPosts();
      },
      error: () => this.notFound.set(true),
    });
  }

  loadPosts() {
    this.userService.posts(this.username(), this.page).subscribe((result) => {
      this.posts.update((posts) => [...posts, ...result.items]);
      this.hasNext.set(result.hasNext);
      this.page++;
    });
  }
}
