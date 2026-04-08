import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RagService, StatsResponse, MetricStats, RecentRequest } from '../services/rag.service';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.scss',
})
export class StatisticsComponent implements OnInit {
  stats: StatsResponse | null = null;
  loading = false;
  switching = false;
  error: string | null = null;

  constructor(private ragService: RagService) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    this.error = null;
    this.ragService.getStats().subscribe({
      next: (s) => { this.stats = s; this.loading = false; },
      error: () => { this.error = 'Could not load statistics.'; this.loading = false; },
    });
  }

  switchVectorStore(type: string): void {
    this.switching = true;
    this.ragService.setActiveVectorStore(type).subscribe({
      next: () => { this.switching = false; this.loadStats(); },
      error: () => { this.switching = false; },
    });
  }

  otherStore(): string {
    return this.stats?.activeVectorStore === 'PGVECTOR' ? 'SIMPLE' : 'PGVECTOR';
  }

  otherStoreLabel(): string {
    return this.stats?.activeVectorStore === 'PGVECTOR' ? 'In-Memory (Simple)' : 'PostgreSQL (PgVector)';
  }

  activeLabel(): string {
    return this.stats?.activeVectorStore === 'PGVECTOR' ? 'PostgreSQL (PgVector)' : 'In-Memory (Simple)';
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
