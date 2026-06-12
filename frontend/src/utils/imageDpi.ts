import exifr from 'exifr';

export const DEFAULT_IMAGE_DPI = 96;

/** 厘米转像素（基于 DPI） */
export function cmToPixels(cm: number, dpi: number): number {
  return (cm / 2.54) * dpi;
}

/**
 * 从图片文件读取 DPI；优先 EXIF XResolution，缺省 96。
 */
export async function readImageDpi(file: File): Promise<number> {
  try {
    const exif = await exifr.parse(file, { pick: ['XResolution', 'YResolution', 'ResolutionUnit'] });
    if (!exif) {
      return DEFAULT_IMAGE_DPI;
    }

    const xRes = toPositiveNumber(exif.XResolution);
    const yRes = toPositiveNumber(exif.YResolution);
    const resolution = xRes ?? yRes;
    if (resolution == null || resolution <= 0) {
      return DEFAULT_IMAGE_DPI;
    }

    const unit = typeof exif.ResolutionUnit === 'number' ? exif.ResolutionUnit : Number(exif.ResolutionUnit);
    // 2 = inches, 3 = centimeters
    if (unit === 3) {
      return resolution * 2.54;
    }
    return resolution;
  } catch {
    return DEFAULT_IMAGE_DPI;
  }
}

function toPositiveNumber(value: unknown): number | null {
  const n = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(n) || n <= 0) {
    return null;
  }
  return n;
}
