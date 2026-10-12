import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from '../config';
import { Comment, Page, Post } from '../models';

@Injectable({ providedIn: 'root' })
export class PostService {
  private http = inject(HttpClient);

  feed(page: number) {
    return this.http.get<Page<Post>>(`${API_URL}/api/posts`, { params: new HttpParams().set('page', page) });
  }

  get(id: number) {
    return this.http.get<Post>(`${API_URL}/api/posts/${id}`);
  }

  create(image: File, caption: string) {
    const form = new FormData();
    form.append('image', image);
    form.append('caption', caption);
    return this.http.post<Post>(`${API_URL}/api/posts`, form);
  }

  delete(id: number) {
    return this.http.delete<void>(`${API_URL}/api/posts/${id}`);
  }

  like(id: number) {
    return this.http.put<void>(`${API_URL}/api/posts/${id}/like`, null);
  }

  unlike(id: number) {
    return this.http.delete<void>(`${API_URL}/api/posts/${id}/like`);
  }

  comments(id: number, page: number) {
    return this.http.get<Page<Comment>>(`${API_URL}/api/posts/${id}/comments`, {
      params: new HttpParams().set('page', page),
    });
  }

  addComment(id: number, text: string) {
    return this.http.post<Comment>(`${API_URL}/api/posts/${id}/comments`, { text });
  }

  deleteComment(postId: number, commentId: number) {
    return this.http.delete<void>(`${API_URL}/api/posts/${postId}/comments/${commentId}`);
  }

  imageSrc(post: Post) {
    return API_URL + post.imageUrl;
  }
}
