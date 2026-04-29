import { Component, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { AiService } from '../../services/ai.service';

interface Message {
  text: string;
  sender: 'user' | 'ai';
  timestamp: Date;
}

@Component({
  selector: 'app-ai-chatbot',
  templateUrl: './ai-chatbot.component.html',
  styleUrls: ['./ai-chatbot.component.css'],
  standalone: false
})
export class AiChatbotComponent implements AfterViewChecked {
  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;

  isOpen = false;
  userInput = '';
  messages: Message[] = [
    {
      text: "Hi! I'm Leafy, your AI book assistant. How can I help you find your next great read today?",
      sender: 'ai',
      timestamp: new Date()
    }
  ];
  isLoading = false;

  constructor(private aiService: AiService) {}

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
  }

  sendMessage() {
    if (!this.userInput.trim() || this.isLoading) return;

    const userMsg = this.userInput;
    this.messages.push({
      text: userMsg,
      sender: 'user',
      timestamp: new Date()
    });

    this.userInput = '';
    this.isLoading = true;

    this.aiService.sendMessage(userMsg).subscribe({
      next: (res) => {
        this.messages.push({
          text: res.response,
          sender: 'ai',
          timestamp: new Date()
        });
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Chat error:', err);
        this.messages.push({
          text: "I'm sorry, I'm having trouble connecting to my brain right now. Please try again later.",
          sender: 'ai',
          timestamp: new Date()
        });
        this.isLoading = false;
      }
    });
  }

  private scrollToBottom(): void {
    try {
      this.myScrollContainer.nativeElement.scrollTop = this.myScrollContainer.nativeElement.scrollHeight;
    } catch(err) { }
  }
}
