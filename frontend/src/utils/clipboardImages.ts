import type { ClipboardEvent } from 'react';

function extensionForImageMime(mime: string): string {
  if (mime === 'image/jpeg' || mime === 'image/jpg') return 'jpg';
  if (mime === 'image/gif') return 'gif';
  if (mime === 'image/webp') return 'webp';
  if (mime === 'image/png') return 'png';
  return 'png';
}

function withDefaultImageName(f: File, index: number): File {
  const base = Date.now();
  if (f.name && f.name.trim()) {
    return f;
  }
  const ext = extensionForImageMime(f.type || 'image/png');
  return new File([f], `paste-${base}-${index}.${ext}`, { type: f.type || 'image/png' });
}

/**
 * Extract image files from a paste / clipboard DataTransfer object.
 * Returns empty array if there are no images (caller should not preventDefault).
 */
export function getImageFilesFromDataTransfer(data: DataTransfer | null): File[] {
  if (!data) {
    return [];
  }
  const out: File[] = [];
  const seen = new Set<string>();

  const pushFile = (f: File | null | undefined) => {
    if (!f || !f.type.startsWith('image/')) return;
    const normalized = withDefaultImageName(f, out.length);
    const key = `${normalized.name}-${normalized.size}-${normalized.type}`;
    if (seen.has(key)) return;
    seen.add(key);
    out.push(normalized);
  };

  if (data.files && data.files.length > 0) {
    for (let i = 0; i < data.files.length; i++) {
      pushFile(data.files[i]);
    }
  }

  const items = data.items;
  if (items) {
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      if (item.kind !== 'file') continue;
      if (!item.type.startsWith('image/')) continue;
      const f = item.getAsFile();
      pushFile(f);
    }
  }

  return out;
}

/** Async alias for callers that expect a Promise (same data as sync helper). */
export function getImageFilesFromClipboardData(data: DataTransfer | null): Promise<File[]> {
  return Promise.resolve(getImageFilesFromDataTransfer(data));
}

export function getImageFilesFromClipboardEvent(e: ClipboardEvent<Element>): File[] {
  return getImageFilesFromDataTransfer(e.clipboardData);
}
