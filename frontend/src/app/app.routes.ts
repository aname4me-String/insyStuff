import { Routes } from '@angular/router';
import { ChatComponent } from './chat/chat.component';
import { DocumentsComponent } from './documents/documents.component';

export const routes: Routes = [
  { path: '', redirectTo: 'chat', pathMatch: 'full' },
  { path: 'chat', component: ChatComponent },
  { path: 'documents', component: DocumentsComponent },
];
