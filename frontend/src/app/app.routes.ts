import { Routes } from '@angular/router';
import { Feed } from './pages/feed/feed';
import { Login } from './pages/login/login';
import { NotFound } from './pages/not-found/not-found';
import { PostDetail } from './pages/post-detail/post-detail';
import { Profile } from './pages/profile/profile';
import { Settings } from './pages/settings/settings';
import { Signup } from './pages/signup/signup';
import { Upload } from './pages/upload/upload';
import { authGuard } from './services/auth.guard';

export const routes: Routes = [
  { path: '', component: Feed, title: 'WebSecGram' },
  { path: 'p/:id', component: PostDetail, title: 'Post · WebSecGram' },
  { path: 'u/:username', component: Profile, title: 'Profile · WebSecGram' },
  { path: 'login', component: Login, title: 'Log in · WebSecGram' },
  { path: 'signup', component: Signup, title: 'Sign up · WebSecGram' },
  { path: 'upload', component: Upload, canActivate: [authGuard], title: 'New post · WebSecGram' },
  { path: 'settings', component: Settings, canActivate: [authGuard], title: 'Settings · WebSecGram' },
  { path: '**', component: NotFound, title: 'Not found · WebSecGram' },
];
