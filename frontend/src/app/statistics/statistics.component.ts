import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RagService, StatsResponse, RecentRequest } from '../services/rag.service';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.scss',
})
export class StatisticsComponent implements OnInit {
  stats: StatsResponse | null = null;
  loading = false;
  error: string | null = null;

  // Filter state
  availableVectorStoreTypes: string[] = [];
  selectedVectorStoreTypes: Set<string> = new Set();
  availableModels: string[] = [];
  selectedModels: Set<string> = new Set();
  recentLimit = 50;
  readonly recentLimitOptions = [10, 25, 50, 100, 200];

  constructor(private ragService: RagService) {}

  ngOnInit(): void {
    this.ragService.getVectorStoreTypes().subscribe({
      next: (types) => {
        this.availableVectorStoreTypes = types;
        types.forEach(t => this.selectedVectorStoreTypes.add(t));
        this.loadStats();
      },
      error: () => {
        this.availableVectorStoreTypes = ['PGVECTOR', 'SIMPLE'];
        this.availableVectorStoreTypes.forEach(t => this.selectedVectorStoreTypes.add(t));
        this.loadStats();
      },
    });
    this.ragService.getModels().subscribe({
      next: (models) => {
        this.availableModels = models;
        // No pre-selection → "all models" by default (empty set = no filter)
      },
      error: () => { this.availableModels = []; },
    });
  }

  loadStats(): void {
    this.loading = true;
    this.error = null;
    const vsTypes = this.selectedVectorStoreTypes.size === this.availableVectorStoreTypes.length
      ? [] : Array.from(this.selectedVectorStoreTypes);
    const models = this.selectedModels.size === 0 ? [] : Array.from(this.selectedModels);
    this.ragService.getStats(vsTypes, models, this.recentLimit).subscribe({
      next: (s) => { this.stats = s; this.loading = false; },
      error: () => { this.error = 'Could not load statistics.'; this.loading = false; },
    });
  }

  toggleVectorStoreType(type: string): void {
    if (this.selectedVectorStoreTypes.has(type)) {
      if (this.selectedVectorStoreTypes.size > 1) {
        this.selectedVectorStoreTypes.delete(type);
      }
    } else {
      this.selectedVectorStoreTypes.add(type);
    }
    this.loadStats();
  }

  toggleModel(model: string): void {
    if (this.selectedModels.has(model)) {
      this.selectedModels.delete(model);
    } else {
      this.selectedModels.add(model);
    }
    this.loadStats();
  }

  selectAllModels(): void {
    this.selectedModels.clear();
    this.loadStats();
  }

  onRecentLimitChange(): void {
    this.loadStats();
  }

  isVsSelected(type: string): boolean {
    return this.selectedVectorStoreTypes.has(type);
  }

  isModelSelected(model: string): boolean {
    return this.selectedModels.has(model);
  }

  allModelsSelected(): boolean {
    return this.selectedModels.size === 0;
  }

  formatMs(v: number): string {
    return v >= 1000 ? (v / 1000).toFixed(2) + ' s' : v.toFixed(0) + ' ms';
  }

  formatMb(v: number): string {
    return v.toFixed(0) + ' MB';
  }

  formatCpu(v: number): string {
    return v < 0 ? 'N/A' : v.toFixed(1) + ' %';
  }

  cpuRowClass(v: number): string {
    if (v < 0) return '';
    if (v > 80) return 'high';
    if (v > 50) return 'medium';
    return '';
  }
}
