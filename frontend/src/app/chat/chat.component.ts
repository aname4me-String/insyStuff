import { Component, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RagService, ChatResponse, SourceReference } from '../services/rag.service';

interface Message {
  role: 'user' | 'assistant';
  text: string;
  sources?: SourceReference[];
  loading?: boolean;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent implements AfterViewChecked {
  @ViewChild('messagesEnd') private messagesEnd!: ElementRef;

  messages: Message[] = [
    { role: 'assistant', text: 'Hello! Upload a PDF and ask me anything about it.' },
  ];
  question = '';
  sending = false;

  constructor(private ragService: RagService) {}

  ngAfterViewChecked(): void {
    this.messagesEnd?.nativeElement.scrollIntoView({ behavior: 'smooth' });
  }

  send(): void {
    const q = this.question.trim();
    if (!q || this.sending) return;

    this.question = '';
    this.sending = true;
    this.messages.push({ role: 'user', text: q });
    this.messages.push({ role: 'assistant', text: '', loading: true });

    this.ragService.chat(q).subscribe({
      next: (res: ChatResponse) => {
        const last = this.messages[this.messages.length - 1];
        last.text = res.answer;
        last.sources = res.sources;
        last.loading = false;
        this.sending = false;
      },
      error: () => {
        const last = this.messages[this.messages.length - 1];
        last.text = '⚠️ Could not reach the backend. Is it running?';
        last.loading = false;
        this.sending = false;
      },
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }
}
