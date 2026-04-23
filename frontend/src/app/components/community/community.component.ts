import { Component, OnInit } from '@angular/core';
import { CommunityService } from '../../services/community.service';
import { AuthService } from '../../services/auth.service';
import { CommunityPost } from '../../models/interfaces';

@Component({
  selector: 'app-community',
  templateUrl: './community.component.html',
  styleUrls: ['./community.component.css'],
  standalone: false
})
export class CommunityComponent implements OnInit {
  posts: CommunityPost[] = [];
  newPost: CommunityPost = { title: '', content: '' };
  showForm = false;

  constructor(private communityService: CommunityService, public authService: AuthService) { }

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts() {
    this.communityService.getPosts().subscribe(data => {
      this.posts = data.sort((a, b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime());
    });
  }

  onSubmit() {
    this.communityService.createPost(this.newPost).subscribe(() => {
      this.loadPosts();
      this.newPost = { title: '', content: '' };
      this.showForm = false;
    });
  }
}
