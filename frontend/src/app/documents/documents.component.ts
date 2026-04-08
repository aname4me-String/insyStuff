import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RagService, DocumentMetadata } from '../services/rag.service';

interface UploadStatus {
  name: string;
  status: 'pending' | 'uploading' | 'success' | 'error';
}

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.scss',
})
export class DocumentsComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  documents: DocumentMetadata[] = [];
  loading = false;
  uploadStatuses: UploadStatus[] = [];
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
    const files = Array.from(input.files ?? []);
    if (!files.length) return;
    this.uploadStatuses = files.map(f => ({ name: f.name, status: 'pending' }));
    this.uploading = true;
    this.uploadNext(files, 0);
  }

  private uploadNext(files: File[], index: number): void {
    if (index >= files.length) {
      this.uploading = false;
      if (this.fileInput) {
        this.fileInput.nativeElement.value = '';
      }
      this.loadDocuments();
      return;
    }

    const file = files[index];
    this.uploadStatuses[index] = { name: file.name, status: 'uploading' };

    this.ragService.uploadDocument(file).subscribe({
      next: () => {
        this.uploadStatuses[index] = { name: file.name, status: 'success' };
        this.uploadNext(files, index + 1);
      },
      error: () => {
        this.uploadStatuses[index] = { name: file.name, status: 'error' };
        this.uploadNext(files, index + 1);
      },
    });
  }
}
