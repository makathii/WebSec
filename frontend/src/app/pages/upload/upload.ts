import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PostService } from '../../services/post.service';
import { MAX_IMAGE_BYTES, TEXT, errorMessage } from '../../validation';

@Component({
  selector: 'app-upload',
  imports: [ReactiveFormsModule],
  templateUrl: './upload.html',
})
export class Upload implements OnDestroy {
  private postService = inject(PostService);
  private router = inject(Router);

  protected file = signal<File | null>(null);
  protected preview = signal<string | null>(null);
  protected error = signal('');
  protected sending = signal(false);

  protected form = new FormGroup({
    caption: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(300), Validators.pattern(TEXT)],
    }),
  });
  protected caption = this.form.controls.caption;

  selectFile(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.error.set('');
    this.setFile(null);
    if (!file) {
      return;
    }
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.error.set('Only JPEG and PNG images are allowed.');
    } else if (file.size > MAX_IMAGE_BYTES) {
      this.error.set('The image must be smaller than 5 MB.');
    } else {
      this.setFile(file);
    }
  }

  submit() {
    const file = this.file();
    if (!file || this.caption.invalid) {
      return;
    }
    this.sending.set(true);
    this.error.set('');
    this.postService.create(file, this.caption.value.trim()).subscribe({
      next: (post) => this.router.navigate(['/p', post.id]),
      error: (err) => {
        this.sending.set(false);
        this.error.set(errorMessage(err, 'Upload failed'));
      },
    });
  }

  ngOnDestroy() {
    this.setFile(null);
  }

  private setFile(file: File | null) {
    const old = this.preview();
    if (old) {
      URL.revokeObjectURL(old);
    }
    this.file.set(file);
    this.preview.set(file ? URL.createObjectURL(file) : null);
  }
}
