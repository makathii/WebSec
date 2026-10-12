import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from '../config';
import { Page, Post, Profile } from '../models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  get(username: string) {
    return this.http.get<Profile>(`${API_URL}/api/users/${encodeURIComponent(username)}`);
  }

  posts(username: string, page: number) {
    return this.http.get<Page<Post>>(`${API_URL}/api/users/${encodeURIComponent(username)}/posts`, {
      params: new HttpParams().set('page', page),
    });
  }
}
