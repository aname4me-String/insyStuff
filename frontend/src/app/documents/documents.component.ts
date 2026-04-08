import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RagService, DocumentMetadata } from '../services/rag.service';

interface UploadStatus {
  name: string;
  status: 'pending' | 'uploading' | 'success' | 'error';
}

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.scss',
})
export class DocumentsComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  documents: DocumentMetadata[] = [];
  loading = false;
  uploadStatuses: UploadStatus[] = [];
  uploading = false;

  renamingId: number | null = null;
  renameValue = '';

  vectorStoreTypes: string[] = [];
  selectedVectorStore = '';

  constructor(private ragService: RagService) {}

  ngOnInit(): void {
    this.ragService.getVectorStoreTypes().subscribe({
      next: (types) => {
        this.vectorStoreTypes = types;
      },
      error: () => {
        this.vectorStoreTypes = ['PGVECTOR', 'SIMPLE'];
      },
    });
    this.ragService.getActiveVectorStore().subscribe({
      next: (res) => {
        this.selectedVectorStore = res.activeVectorStore;
        this.loadDocuments();
      },
      error: () => {
        this.loadDocuments();
      },
    });
  }

  loadDocuments(): void {
    this.loading = true;
    this.ragService.listDocuments(this.selectedVectorStore).subscribe({
      next: (docs) => { this.documents = docs; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  onVectorStoreChange(): void {
    this.loadDocuments();
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

    this.ragService.uploadDocument(file, this.selectedVectorStore).subscribe({
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

  deleteDocument(doc: DocumentMetadata): void {
    if (!confirm(`Delete "${doc.fileName}"? This will remove all indexed data for this document.`)) return;
    this.ragService.deleteDocument(doc.id).subscribe({
      next: () => { this.documents = this.documents.filter(d => d.id !== doc.id); },
      error: () => { alert('Failed to delete document.'); },
    });
  }

  startRename(doc: DocumentMetadata): void {
    this.renamingId = doc.id;
    this.renameValue = doc.fileName;
  }

  cancelRename(): void {
    this.renamingId = null;
    this.renameValue = '';
  }

  confirmRename(doc: DocumentMetadata): void {
    const newName = this.renameValue.trim();
    if (!newName || newName === doc.fileName) {
      this.cancelRename();
      return;
    }
    this.ragService.renameDocument(doc.id, newName).subscribe({
      next: (updated) => {
        const idx = this.documents.findIndex(d => d.id === doc.id);
        if (idx !== -1) this.documents[idx] = updated;
        this.cancelRename();
      },
      error: () => {
        alert('Failed to rename document.');
        this.cancelRename();
      },
    });
  }
}
