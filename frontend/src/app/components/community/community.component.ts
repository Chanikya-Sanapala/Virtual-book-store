import { Component, OnInit } from '@angular/core';
import { CommunityService } from '../../services/community.service';
import { AuthService } from '../../services/auth.service';
import { CommunityPost } from '../../models/interfaces';
import { NotificationService } from '../../services/notification.service';

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

  constructor(
    private communityService: CommunityService, 
    public authService: AuthService,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts() {
    this.communityService.getPosts().subscribe(data => {
      this.posts = data.sort((a, b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime());
    });
  }

  onSubmit() {
    this.communityService.createPost(this.newPost).subscribe({
      next: () => {
        this.loadPosts();
        this.newPost = { title: '', content: '' };
        this.showForm = false;
        this.notificationService.show('Post shared with the community!');
      },
      error: (err) => {
        this.notificationService.show('Failed to share post.', 'error');
      }
    });
  }
}
