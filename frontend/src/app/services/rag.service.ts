import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface SourceReference {
  fileName: string;
  pageNumber: number | null;
}

export interface ChatResponse {
  answer: string;
  sources: SourceReference[];
}

export interface DocumentMetadata {
  id: number;
  fileName: string;
  pdfTitle: string | null;
  pdfAuthor: string | null;
  totalPages: number | null;
  creationTs: string | null;
}

export interface Conversation {
  id: number;
  title: string;
  createdAt: string;
}

export interface ConversationMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  model: string | null;
  sources: SourceReference[];
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class RagService {
  private readonly base = '/api';

  constructor(private http: HttpClient) {}

  chat(question: string, model?: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.base}/chat`, { question, model });
  }

  listDocuments(): Observable<DocumentMetadata[]> {
    return this.http.get<DocumentMetadata[]>(`${this.base}/documents`);
  }

  uploadDocument(file: File): Observable<string> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post(`${this.base}/index`, form, { responseType: 'text' });
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/documents/${id}`);
  }

  renameDocument(id: number, fileName: string): Observable<DocumentMetadata> {
    return this.http.patch<DocumentMetadata>(`${this.base}/documents/${id}`, { fileName });
  }

  getModels(): Observable<string[]> {
    return this.http.get<{ models: string[] }>(`${this.base}/models`).pipe(
      map(res => res.models)
    );
  }

  listConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(`${this.base}/chats`);
  }

  createConversation(title?: string): Observable<Conversation> {
    return this.http.post<Conversation>(`${this.base}/chats`, title ? { title } : {});
  }

  deleteConversation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/chats/${id}`);
  }

  getConversationMessages(chatId: number): Observable<ConversationMessage[]> {
    return this.http.get<ConversationMessage[]>(`${this.base}/chats/${chatId}/messages`);
  }

  sendConversationMessage(chatId: number, question: string, model?: string): Observable<ConversationMessage> {
    return this.http.post<ConversationMessage>(`${this.base}/chats/${chatId}/messages`, { question, model });
  }
}
