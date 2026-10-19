export interface User {
  username: string;
  createdAt: string;
}

export interface Profile {
  username: string;
  createdAt: string;
  postCount: number;
}

export interface Session {
  accessToken: string;
  expiresIn: number;
  user: User;
}

export interface CaptchaConfig {
  siteKey: string;
}

export interface Post {
  id: number;
  username: string;
  imageUrl: string;
  caption: string | null;
  likeCount: number;
  commentCount: number;
  likedByMe: boolean;
  createdAt: string;
}

export interface Comment {
  id: number;
  username: string;
  text: string;
  createdAt: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  hasNext: boolean;
}
