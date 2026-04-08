import { Component, ViewChild, ElementRef, AfterViewChecked, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RagService, ChatResponse, SourceReference } from '../services/rag.service';

interface Message {
  role: 'user' | 'assistant';
  text: string;
  sources?: SourceReference[];
  loading?: boolean;
  model?: string;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent implements AfterViewChecked, OnInit {
  @ViewChild('messagesEnd') private messagesEnd!: ElementRef;

  messages: Message[] = [
    { role: 'assistant', text: 'Hello! Upload a PDF and ask me anything about it.' },
  ];
  question = '';
  sending = false;
  availableModels: string[] = [];
  selectedModel = '';

  constructor(private ragService: RagService) {}

  ngOnInit(): void {
    this.ragService.getModels().subscribe({
      next: (models) => {
        this.availableModels = models;
        if (models.length > 0) {
          this.selectedModel = models[0];
        }
      },
      error: () => {
        this.availableModels = [];
      },
    });
  }

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

    this.ragService.chat(q, this.selectedModel || undefined).subscribe({
      next: (res: ChatResponse) => {
        const last = this.messages[this.messages.length - 1];
        last.text = res.answer;
        last.sources = res.sources;
        last.loading = false;
        last.model = this.selectedModel || undefined;
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
