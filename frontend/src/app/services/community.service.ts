import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommunityPost } from '../models/interfaces';
import { AuthService } from './auth.service';

const COMMUNITY_API = 'http://localhost:8080/api/community/';

@Injectable({
  providedIn: 'root'
})
export class CommunityService {
  constructor(private http: HttpClient, private authService: AuthService) { }

  private getHeaders() {
    const token = this.authService.currentUserValue.token;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  getPosts(): Observable<CommunityPost[]> {
    return this.http.get<CommunityPost[]>(COMMUNITY_API);
  }

  createPost(post: CommunityPost): Observable<CommunityPost> {
    return this.http.post<CommunityPost>(COMMUNITY_API, post, { headers: this.getHeaders() });
  }
}
