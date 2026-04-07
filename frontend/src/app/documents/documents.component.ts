import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RagService, DocumentMetadata } from '../services/rag.service';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.scss',
})
export class DocumentsComponent implements OnInit {
  documents: DocumentMetadata[] = [];
  loading = false;
  uploadStatus = '';
  uploading = false;

  constructor(private ragService: RagService) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.loading = true;
    this.ragService.listDocuments().subscribe({
      next: (docs) => { this.documents = docs; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.uploading = true;
    this.uploadStatus = `Indexing "${file.name}"…`;

    this.ragService.uploadDocument(file).subscribe({
      next: () => {
        this.uploadStatus = `✅ "${file.name}" indexed successfully`;
        this.uploading = false;
        this.loadDocuments();
        input.value = '';
      },
      error: () => {
        this.uploadStatus = `❌ Failed to index "${file.name}"`;
        this.uploading = false;
        input.value = '';
      },
    });
  }
}
