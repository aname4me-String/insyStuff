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

export type WsStreamEvent =
  | { type: 'token'; content: string }
  | { type: 'done'; messageId: number; sources: SourceReference[] }
  | { type: 'error'; message: string };

export interface MetricStats {
  avg: number;
  median: number;
  min: number;
  max: number;
}

export interface RecentRequest {
  id: number;
  timestamp: string;
  question: string;
  model: string;
  vectorStoreType: string;
  vectorSearchMs: number;
  totalResponseMs: number;
  tokenCount: number;
  sourceCount: number;
  ramUsedMb: number;
  cpuLoadPercent: number;
}

export interface ModelBreakdownEntry {
  requestCount: number;
  avgResponseMs: number;
  avgVectorSearchMs: number;
  avgTokenCount: number;
  avgSourceCount: number;
}

export interface StatsResponse {
  totalRequests: number;
  activeVectorStore: string;
  vectorSearchMs: MetricStats;
  totalResponseMs: MetricStats;
  tokenCount: MetricStats;
  sourceCount: MetricStats;
  ramUsedMb: MetricStats;
  cpuLoadPercent: MetricStats;
  recentRequests: RecentRequest[];
  modelBreakdown: Record<string, ModelBreakdownEntry>;
  vectorStoreBreakdown: Record<string, ModelBreakdownEntry>;
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

  uploadDocument(file: File, vectorStoreType?: string): Observable<string> {
    const form = new FormData();
    form.append('file', file);
    const params = vectorStoreType ? `?vectorStoreType=${encodeURIComponent(vectorStoreType)}` : '';
    return this.http.post(`${this.base}/index${params}`, form, { responseType: 'text' });
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

  streamConversationMessage(chatId: number, question: string, model?: string, vectorStoreType?: string): Observable<WsStreamEvent> {
    return new Observable(observer => {
      const proto = location.protocol === 'https:' ? 'wss' : 'ws';
      const ws = new WebSocket(`${proto}://${location.host}/ws/chat`);

      ws.onopen = () => {
        ws.send(JSON.stringify({ chatId, question, model: model ?? '', vectorStoreType: vectorStoreType ?? '' }));
      };

      ws.onmessage = (event: MessageEvent) => {
        const data: WsStreamEvent = JSON.parse(event.data as string);
        observer.next(data);
        if (data.type === 'done' || data.type === 'error') {
          ws.close();
          observer.complete();
        }
      };

      ws.onerror = () => { observer.error(new Error('WebSocket connection failed – check network or server availability')); };
      ws.onclose = () => { observer.complete(); };

      return () => { if (ws.readyState < 2) ws.close(); };
    });
  }

  getStats(vectorStoreTypes?: string[], models?: string[], recentLimit?: number): Observable<StatsResponse> {
    const params: Record<string, string> = {};
    if (vectorStoreTypes && vectorStoreTypes.length > 0) {
      params['vectorStoreTypes'] = vectorStoreTypes.join(',');
    }
    if (models && models.length > 0) {
      params['models'] = models.join(',');
    }
    if (recentLimit !== undefined) {
      params['recentLimit'] = String(recentLimit);
    }
    return this.http.get<StatsResponse>(`${this.base}/stats`, { params });
  }

  getVectorStoreTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/settings/vectorstore/types`);
  }

  getActiveVectorStore(): Observable<{ activeVectorStore: string }> {
    return this.http.get<{ activeVectorStore: string }>(`${this.base}/settings/vectorstore`);
  }

  setActiveVectorStore(type: string): Observable<{ activeVectorStore: string }> {
    return this.http.put<{ activeVectorStore: string }>(`${this.base}/settings/vectorstore`, { activeVectorStore: type });
  }
}
