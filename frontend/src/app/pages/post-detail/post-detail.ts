import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PostCard } from '../../components/post-card/post-card';
import { Comment, Post } from '../../models';
import { AuthService } from '../../services/auth.service';
import { PostService } from '../../services/post.service';
import { TimeAgoPipe } from '../../time-ago.pipe';
import { TEXT, errorMessage } from '../../validation';

@Component({
  selector: 'app-post-detail',
  imports: [PostCard, RouterLink, ReactiveFormsModule, TimeAgoPipe],
  templateUrl: './post-detail.html',
})
export class PostDetail implements OnInit {
  id = input.required<string>();

  protected auth = inject(AuthService);
  private postService = inject(PostService);
  private router = inject(Router);

  protected post = signal<Post | null>(null);
  protected comments = signal<Comment[]>([]);
  protected hasMoreComments = signal(false);
  protected notFound = signal(false);
  protected error = signal('');
  protected sending = signal(false);
  private commentPage = 0;

  protected commentForm = new FormGroup({
    text: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(500), Validators.pattern(TEXT)],
    }),
  });
  protected commentText = this.commentForm.controls.text;

  ngOnInit() {
    if (!/^\d{1,18}$/.test(this.id())) {
      this.notFound.set(true);
      return;
    }
    this.postService.get(this.postId).subscribe({
      next: (post) => {
        this.post.set(post);
        this.loadComments();
      },
      error: () => this.notFound.set(true),
    });
  }

  private get postId() {
    return Number(this.id());
  }

  loadComments() {
    this.postService.comments(this.postId, this.commentPage).subscribe((result) => {
      this.comments.update((comments) => [...comments, ...result.items]);
      this.hasMoreComments.set(result.hasNext);
      this.commentPage++;
    });
  }

  addComment() {
    const text = this.commentText.value.trim();
    if (this.commentText.invalid || !text) {
      return;
    }
    this.sending.set(true);
    this.postService.addComment(this.postId, text).subscribe({
      next: (comment) => {
        this.comments.update((comments) => [...comments, comment]);
        this.changeCommentCount(1);
        this.commentText.reset();
        this.error.set('');
        this.sending.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err, 'Could not add the comment'));
        this.sending.set(false);
      },
    });
  }

  deleteComment(comment: Comment) {
    if (!confirm('Delete this comment?')) {
      return;
    }
    this.postService.deleteComment(this.postId, comment.id).subscribe({
      next: () => {
        this.comments.update((comments) => comments.filter((c) => c.id !== comment.id));
        this.changeCommentCount(-1);
      },
      error: (err) => this.error.set(errorMessage(err, 'Could not delete the comment')),
    });
  }

  deletePost() {
    if (!confirm('Delete this post? This cannot be undone.')) {
      return;
    }
    this.postService.delete(this.postId).subscribe({
      next: () => this.router.navigate(['/u', this.auth.user()?.username]),
      error: (err) => this.error.set(errorMessage(err, 'Could not delete the post')),
    });
  }

  isMine(username: string) {
    return this.auth.user()?.username === username;
  }

  private changeCommentCount(delta: number) {
    this.post.update((post) => (post ? { ...post, commentCount: post.commentCount + delta } : post));
  }
}
