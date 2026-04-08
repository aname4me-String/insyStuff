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

  getModels(): Observable<string[]> {
    return this.http.get<{ models: string[] }>(`${this.base}/models`).pipe(
      map(res => res.models)
    );
  }
}
