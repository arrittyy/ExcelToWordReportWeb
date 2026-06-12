import { cmToPixels } from './imageDpi';

/** 工作画布物理尺寸（cm） */
export const MET_CANVAS_WIDTH_CM = 10.8;
export const MET_CANVAS_HEIGHT_CM = 7.87;

/** 裁剪输出物理尺寸（cm），对应 Word 附图 */
export const MET_CROP_WIDTH_CM = 7.2;
export const MET_CROP_HEIGHT_CM = 5.25;
export const MET_CROP_ASPECT = MET_CROP_WIDTH_CM / MET_CROP_HEIGHT_CM;

/** 标尺线段固定物理长度（cm） */
export const MET_SCALE_BAR_LENGTH_CM = 1.0;

export const MET_OUTPUT_WIDTH = 2160;
export const MET_OUTPUT_HEIGHT = 1575;

export const MET_SCALE_BAR_OPTIONS = [10, 20, 25, 50, 100, 200] as const;
export type MetScaleBarUm = (typeof MET_SCALE_BAR_OPTIONS)[number];

export interface CropRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface NormalizedCanvasResult {
  canvas: HTMLCanvasElement;
  width: number;
  height: number;
  dpi: number;
}

export interface CropRectResult {
  rect: CropRect;
  /** 裁剪框是否因画布不足而被同比缩小 */
  scaledDown: boolean;
  /** 未缩小前的目标裁剪宽（px） */
  targetWidth: number;
  /** 未缩小前的目标裁剪高（px） */
  targetHeight: number;
}

export function loadImageFromFile(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve(img);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('无法加载图片'));
    };
    img.src = url;
  });
}

/**
 * 将原图等比 contain 绘制到 10.8×7.87 cm 归一化画布。
 */
export function buildNormalizedCanvas(source: HTMLImageElement, dpi: number): NormalizedCanvasResult {
  const width = Math.round(cmToPixels(MET_CANVAS_WIDTH_CM, dpi));
  const height = Math.round(cmToPixels(MET_CANVAS_HEIGHT_CM, dpi));

  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('无法创建画布');
  }

  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, width, height);

  const scale = Math.min(width / source.naturalWidth, height / source.naturalHeight);
  const drawW = source.naturalWidth * scale;
  const drawH = source.naturalHeight * scale;
  const drawX = (width - drawW) / 2;
  const drawY = (height - drawH) / 2;

  ctx.drawImage(source, drawX, drawY, drawW, drawH);

  return { canvas, width, height, dpi };
}

/**
 * 在归一化画布上计算 7.20×5.25 cm 固定裁剪框（居中初始位置）。
 */
export function computeCropRectOnCanvas(canvasW: number, canvasH: number, dpi: number): CropRectResult {
  let targetWidth = cmToPixels(MET_CROP_WIDTH_CM, dpi);
  let targetHeight = cmToPixels(MET_CROP_HEIGHT_CM, dpi);
  let width = targetWidth;
  let height = targetHeight;
  let scaledDown = false;

  if (width > canvasW || height > canvasH) {
    const scale = Math.min(canvasW / width, canvasH / height);
    width *= scale;
    height *= scale;
    scaledDown = true;
  }

  return {
    rect: {
      x: (canvasW - width) / 2,
      y: (canvasH - height) / 2,
      width,
      height,
    },
    scaledDown,
    targetWidth,
    targetHeight,
  };
}

/** 将裁剪框左上角限制在画布范围内 */
export function clampCropPosition(
  canvasW: number,
  canvasH: number,
  cropW: number,
  cropH: number,
  x: number,
  y: number,
): { x: number; y: number } {
  const maxX = Math.max(0, canvasW - cropW);
  const maxY = Math.max(0, canvasH - cropH);
  return {
    x: Math.min(Math.max(0, x), maxX),
    y: Math.min(Math.max(0, y), maxY),
  };
}

/** 在宽为 MET_CROP_WIDTH_CM 的输出/裁剪预览上，标尺线段恒为 1cm */
export function scaleBarWidthPixels(cropOutputWidthPx: number): number {
  return cropOutputWidthPx * (MET_SCALE_BAR_LENGTH_CM / MET_CROP_WIDTH_CM);
}

export function drawScaleBarOnCanvas(
  ctx: CanvasRenderingContext2D,
  canvasW: number,
  canvasH: number,
  scaleUm: MetScaleBarUm,
): void {
  const barWidth = scaleBarWidthPixels(canvasW);
  const marginX = canvasW * 0.02;
  const marginY = canvasH * 0.02;
  const padding = Math.max(6, canvasW * 0.008);
  const fontSize = Math.max(14, canvasW * 0.022);
  const lineY = canvasH - marginY - padding * 2;
  const boxLeft = canvasW - marginX - barWidth - padding * 2;
  const boxTop = lineY - fontSize - padding * 2.5;
  const boxW = barWidth + padding * 2;
  const boxH = fontSize + padding * 3;

  ctx.save();
  ctx.fillStyle = '#ffffff';
  ctx.strokeStyle = '#000000';
  ctx.lineWidth = Math.max(1, canvasW * 0.0015);
  ctx.fillRect(boxLeft, boxTop, boxW, boxH);
  ctx.strokeRect(boxLeft, boxTop, boxW, boxH);

  const text = `${scaleUm}μm`;
  ctx.fillStyle = '#000000';
  ctx.font = `${fontSize}px Arial, sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(text, boxLeft + boxW / 2, boxTop + padding + fontSize / 2);

  const lineLeft = boxLeft + padding;
  const lineRight = lineLeft + barWidth;
  const tickH = Math.max(4, fontSize * 0.35);
  ctx.beginPath();
  ctx.moveTo(lineLeft, lineY);
  ctx.lineTo(lineRight, lineY);
  ctx.moveTo(lineLeft, lineY - tickH);
  ctx.lineTo(lineLeft, lineY + tickH);
  ctx.moveTo(lineRight, lineY - tickH);
  ctx.lineTo(lineRight, lineY + tickH);
  ctx.stroke();
  ctx.restore();
}

export function renderMetallographicImage(
  normalizedCanvas: HTMLCanvasElement,
  cropRect: CropRect,
  scaleUm: MetScaleBarUm,
  outputWidth = MET_OUTPUT_WIDTH,
  outputHeight = MET_OUTPUT_HEIGHT,
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement('canvas');
    canvas.width = outputWidth;
    canvas.height = outputHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      reject(new Error('无法创建画布'));
      return;
    }

    ctx.drawImage(
      normalizedCanvas,
      cropRect.x,
      cropRect.y,
      cropRect.width,
      cropRect.height,
      0,
      0,
      outputWidth,
      outputHeight,
    );
    drawScaleBarOnCanvas(ctx, outputWidth, outputHeight, scaleUm);

    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob);
        } else {
          reject(new Error('图片导出失败'));
        }
      },
      'image/jpeg',
      0.92,
    );
  });
}
