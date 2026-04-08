import { Component, ViewChild, ElementRef, AfterViewChecked, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RagService, ConversationMessage, Conversation, SourceReference } from '../services/rag.service';

interface DisplayMessage {
  role: 'user' | 'assistant';
  text: string;
  sources?: SourceReference[];
  loading?: boolean;
  model?: string;
}

const DEFAULT_CHAT_TITLE = 'New Chat';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent implements AfterViewChecked, OnInit {
  @ViewChild('messagesEnd') private messagesEnd!: ElementRef;

  conversations: Conversation[] = [];
  currentConversationId: number | null = null;
  messages: DisplayMessage[] = [];
  question = '';
  sending = false;
  availableModels: string[] = [];
  selectedModel = '';
  loadingChats = false;
  loadingMessages = false;

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
    this.loadConversations();
  }

  ngAfterViewChecked(): void {
    this.messagesEnd?.nativeElement.scrollIntoView({ behavior: 'smooth' });
  }

  loadConversations(): void {
    this.loadingChats = true;
    this.ragService.listConversations().subscribe({
      next: (convs) => {
        this.conversations = convs;
        this.loadingChats = false;
        if (convs.length > 0 && this.currentConversationId === null) {
          this.selectConversation(convs[0].id);
        }
      },
      error: () => { this.loadingChats = false; },
    });
  }

  selectConversation(id: number): void {
    this.currentConversationId = id;
    this.messages = [];
    this.loadingMessages = true;
    this.ragService.getConversationMessages(id).subscribe({
      next: (msgs) => {
        this.messages = msgs.map(m => ({
          role: m.role,
          text: m.content,
          sources: m.sources,
          model: m.model ?? undefined,
        }));
        this.loadingMessages = false;
      },
      error: () => { this.loadingMessages = false; },
    });
  }

  newConversation(): void {
    this.ragService.createConversation().subscribe({
      next: (conv) => {
        this.conversations = [conv, ...this.conversations];
        this.currentConversationId = conv.id;
        this.messages = [{ role: 'assistant', text: 'Hello! Upload a PDF and ask me anything about it.' }];
      },
      error: () => {},
    });
  }

  deleteConversation(conv: Conversation, event: MouseEvent): void {
    event.stopPropagation();
    this.ragService.deleteConversation(conv.id).subscribe({
      next: () => {
        this.conversations = this.conversations.filter(c => c.id !== conv.id);
        if (this.currentConversationId === conv.id) {
          this.currentConversationId = null;
          this.messages = [];
          if (this.conversations.length > 0) {
            this.selectConversation(this.conversations[0].id);
          }
        }
      },
      error: () => {},
    });
  }

  send(): void {
    const q = this.question.trim();
    if (!q || this.sending) return;

    if (this.currentConversationId === null) {
      this.ragService.createConversation(q.substring(0, 50)).subscribe({
        next: (conv) => {
          this.conversations = [conv, ...this.conversations];
          this.currentConversationId = conv.id;
          this.doSend(q);
        },
        error: () => {},
      });
      return;
    }

    this.doSend(q);
  }

  private doSend(q: string): void {
    this.question = '';
    this.sending = true;
    this.messages.push({ role: 'user', text: q });
    this.messages.push({ role: 'assistant', text: '', loading: true });

    this.ragService.sendConversationMessage(this.currentConversationId!, q, this.selectedModel).subscribe({
      next: (res: ConversationMessage) => {
        const last = this.messages[this.messages.length - 1];
        last.text = res.content;
        last.sources = res.sources;
        last.loading = false;
        last.model = res.model ?? undefined;
        this.sending = false;
        // Update conversation title in sidebar
        const conv = this.conversations.find(c => c.id === this.currentConversationId);
        if (conv && conv.title === DEFAULT_CHAT_TITLE) {
          this.loadConversations();
        }
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
