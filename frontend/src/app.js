'use strict';

const API_BASE = '/api';

// ── Pane switching ──────────────────────────────────────────────────────────

function showPane(name) {
  document.querySelectorAll('.pane').forEach(p => p.classList.add('hidden'));
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('pane-' + name).classList.remove('hidden');
  document.getElementById('btn-' + name).classList.add('active');
  if (name === 'docs') loadDocuments();
}

// ── Chat ────────────────────────────────────────────────────────────────────

async function sendMessage(event) {
  event.preventDefault();

  const input = document.getElementById('question-input');
  const question = input.value.trim();
  if (!question) return;

  input.value = '';
  resizeTextarea(input);
  setSendDisabled(true);

  displayMessage('user', question);

  const typingId = displayTyping();

  try {
    const response = await fetch(`${API_BASE}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question }),
    });

    removeTyping(typingId);

    if (!response.ok) {
      const err = await response.text();
      displayMessage('assistant', `⚠️ Error: ${err}`);
      return;
    }

    const data = await response.json();
    displayMessage('assistant', data.answer, data.sources ?? []);
  } catch (e) {
    removeTyping(typingId);
    displayMessage('assistant', `⚠️ Could not reach the backend. Is it running?\n${e.message}`);
  } finally {
    setSendDisabled(false);
    document.getElementById('question-input').focus();
  }
}

function handleKey(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    document.getElementById('chat-form').dispatchEvent(new Event('submit', { cancelable: true }));
  }
  resizeTextarea(event.target);
}

function resizeTextarea(el) {
  el.style.height = 'auto';
  el.style.height = Math.min(el.scrollHeight, 160) + 'px';
}

// ── Message rendering ───────────────────────────────────────────────────────

function displayMessage(role, text, sources = []) {
  const container = document.getElementById('messages');

  const msg = document.createElement('div');
  msg.className = `message ${role}`;

  const bubble = document.createElement('div');
  bubble.className = 'bubble';
  bubble.textContent = text;
  msg.appendChild(bubble);

  if (sources.length > 0) {
    msg.appendChild(renderSources(sources));
  }

  container.appendChild(msg);
  container.scrollTop = container.scrollHeight;
  return msg;
}

function displayTyping() {
  const id = 'typing-' + Date.now();
  const container = document.getElementById('messages');

  const msg = document.createElement('div');
  msg.className = 'message assistant';
  msg.id = id;

  const bubble = document.createElement('div');
  bubble.className = 'bubble typing';
  bubble.innerHTML = '<div class="dot"></div><div class="dot"></div><div class="dot"></div>';
  msg.appendChild(bubble);

  container.appendChild(msg);
  container.scrollTop = container.scrollHeight;
  return id;
}

function removeTyping(id) {
  const el = document.getElementById(id);
  if (el) el.remove();
}

function renderSources(sources) {
  const div = document.createElement('div');
  div.className = 'sources';

  const seen = new Set();
  sources.forEach(src => {
    const key = `${src.fileName}:${src.pageNumber}`;
    if (seen.has(key)) return;
    seen.add(key);

    const badge = document.createElement('span');
    badge.className = 'source-badge';
    const page = src.pageNumber != null ? ` · p.${src.pageNumber}` : '';
    badge.textContent = `📄 ${src.fileName}${page}`;
    div.appendChild(badge);
  });

  return div;
}

// ── Documents ───────────────────────────────────────────────────────────────

async function loadDocuments() {
  const list = document.getElementById('docs-list');
  list.innerHTML = '<p class="empty-hint">Loading…</p>';

  try {
    const response = await fetch(`${API_BASE}/documents`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const docs = await response.json();
    if (docs.length === 0) {
      list.innerHTML = '<p class="empty-hint">No documents indexed yet.</p>';
      return;
    }

    list.innerHTML = '';
    docs.forEach(doc => {
      const card = document.createElement('div');
      card.className = 'doc-card';

      const name = document.createElement('div');
      name.className = 'doc-name';
      name.textContent = doc.fileName ?? '—';

      const meta = document.createElement('div');
      meta.className = 'doc-meta';
      const parts = [];
      if (doc.totalPages)      parts.push(`${doc.totalPages} pages`);
      if (doc.pdfAuthor)       parts.push(`Author: ${doc.pdfAuthor}`);
      if (doc.pdfTitle)        parts.push(`Title: ${doc.pdfTitle}`);
      if (doc.creationTs)      parts.push(`Indexed: ${new Date(doc.creationTs).toLocaleString()}`);
      meta.textContent = parts.join('  ·  ');

      card.appendChild(name);
      card.appendChild(meta);
      list.appendChild(card);
    });
  } catch (e) {
    list.innerHTML = `<p class="empty-hint">⚠️ Failed to load documents: ${e.message}</p>`;
  }
}

// ── Upload ──────────────────────────────────────────────────────────────────

async function uploadDocument(event) {
  const file = event.target.files[0];
  if (!file) return;

  const status = document.getElementById('upload-status');
  status.className = 'upload-status';
  status.textContent = `Uploading "${file.name}"…`;

  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await fetch(`${API_BASE}/index`, {
      method: 'POST',
      body: formData,
    });

    const text = await response.text();
    if (!response.ok) {
      status.className = 'upload-status error';
      status.textContent = `Error: ${text}`;
    } else {
      status.className = 'upload-status success';
      status.textContent = `✓ ${text}`;
      setTimeout(() => { status.textContent = ''; status.className = 'upload-status'; }, 4000);
    }
  } catch (e) {
    status.className = 'upload-status error';
    status.textContent = `Upload failed: ${e.message}`;
  } finally {
    // Reset input so the same file can be re-uploaded
    event.target.value = '';
  }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

function setSendDisabled(disabled) {
  document.getElementById('send-btn').disabled = disabled;
  document.getElementById('question-input').disabled = disabled;
}
