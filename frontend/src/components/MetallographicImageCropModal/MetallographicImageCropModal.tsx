import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Modal, Select, Button, Space, Alert } from 'antd';
import { readImageDpi } from '@/utils/imageDpi';
import {
  MET_CANVAS_WIDTH_CM,
  MET_CANVAS_HEIGHT_CM,
  MET_CROP_WIDTH_CM,
  MET_CROP_HEIGHT_CM,
  MET_SCALE_BAR_OPTIONS,
  type MetScaleBarUm,
  type NormalizedCanvasResult,
  clampCropPosition,
  loadImageFromFile,
  buildNormalizedCanvas,
  computeCropRectOnCanvas,
  renderMetallographicImage,
  drawScaleBarOnCanvas,
  type CropRect,
} from '@/utils/metallographicImageEditor';

interface MetallographicImageCropModalProps {
  open: boolean;
  file: File | null;
  onConfirm: (blob: Blob) => void;
  onCancel: () => void;
}

const PREVIEW_MAX_W = 720;
const PREVIEW_MAX_H = 520;

const MetallographicImageCropModal: React.FC<MetallographicImageCropModalProps> = ({
  open,
  file,
  onConfirm,
  onCancel,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [normalized, setNormalized] = useState<NormalizedCanvasResult | null>(null);
  const [cropRect, setCropRect] = useState<CropRect | null>(null);
  const [scaleUm, setScaleUm] = useState<MetScaleBarUm>(50);
  const [dpi, setDpi] = useState(96);
  const [cropScaledDown, setCropScaledDown] = useState(false);
  const [previewScale, setPreviewScale] = useState(1);
  const [dragging, setDragging] = useState(false);
  const dragStart = useRef<{ mx: number; my: number; cx: number; cy: number } | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !file) {
      setNormalized(null);
      setCropRect(null);
      setCropScaledDown(false);
      setLoadError(null);
      return;
    }
    let cancelled = false;
    setLoadError(null);

    Promise.all([loadImageFromFile(file), readImageDpi(file)])
      .then(([img, imageDpi]) => {
        if (cancelled) return;
        const norm = buildNormalizedCanvas(img, imageDpi);
        const cropResult = computeCropRectOnCanvas(norm.width, norm.height, imageDpi);
        setDpi(imageDpi);
        setNormalized(norm);
        setCropRect(cropResult.rect);
        setCropScaledDown(cropResult.scaledDown);
        const scale = Math.min(PREVIEW_MAX_W / norm.width, PREVIEW_MAX_H / norm.height, 1);
        setPreviewScale(scale);
      })
      .catch(() => {
        if (!cancelled) {
          setLoadError('图片加载失败，请重试');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [open, file]);

  const drawPreview = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas || !normalized || !cropRect) return;

    const dispW = normalized.width * previewScale;
    const dispH = normalized.height * previewScale;
    canvas.width = dispW;
    canvas.height = dispH;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.drawImage(normalized.canvas, 0, 0, dispW, dispH);

    const sx = cropRect.x * previewScale;
    const sy = cropRect.y * previewScale;
    const sw = cropRect.width * previewScale;
    const sh = cropRect.height * previewScale;

    ctx.fillStyle = 'rgba(0, 0, 0, 0.55)';
    ctx.fillRect(0, 0, dispW, sy);
    ctx.fillRect(0, sy + sh, dispW, dispH - sy - sh);
    ctx.fillRect(0, sy, sx, sh);
    ctx.fillRect(sx + sw, sy, dispW - sx - sw, sh);

    ctx.strokeStyle = '#000000';
    ctx.lineWidth = 2;
    ctx.strokeRect(sx, sy, sw, sh);

    const cornerLen = Math.min(16, sw * 0.08);
    ctx.lineWidth = 3;
    const corners: [number, number, number, number][] = [
      [sx, sy, cornerLen, cornerLen],
      [sx + sw, sy, -cornerLen, cornerLen],
      [sx, sy + sh, cornerLen, -cornerLen],
      [sx + sw, sy + sh, -cornerLen, -cornerLen],
    ];
    corners.forEach(([cx, cy, dx, dy]) => {
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(cx + dx, cy);
      ctx.moveTo(cx, cy);
      ctx.lineTo(cx, cy + dy);
      ctx.stroke();
    });

    ctx.save();
    ctx.beginPath();
    ctx.rect(sx, sy, sw, sh);
    ctx.clip();
    ctx.translate(sx, sy);
    drawScaleBarOnCanvas(ctx, sw, sh, scaleUm);
    ctx.restore();
  }, [normalized, cropRect, previewScale, scaleUm]);

  useEffect(() => {
    drawPreview();
  }, [drawPreview]);

  const clientToCanvasCoords = (clientX: number, clientY: number) => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const x = ((clientX - rect.left) / rect.width) * canvas.width;
    const y = ((clientY - rect.top) / rect.height) * canvas.height;
    return { x: x / previewScale, y: y / previewScale };
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!cropRect || !normalized) return;
    const { x, y } = clientToCanvasCoords(e.clientX, e.clientY);
    if (
      x >= cropRect.x &&
      x <= cropRect.x + cropRect.width &&
      y >= cropRect.y &&
      y <= cropRect.y + cropRect.height
    ) {
      setDragging(true);
      dragStart.current = { mx: x, my: y, cx: cropRect.x, cy: cropRect.y };
      e.currentTarget.setPointerCapture(e.pointerId);
    }
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!dragging || !dragStart.current || !cropRect || !normalized) return;
    const { x, y } = clientToCanvasCoords(e.clientX, e.clientY);
    const dx = x - dragStart.current.mx;
    const dy = y - dragStart.current.my;
    const pos = clampCropPosition(
      normalized.width,
      normalized.height,
      cropRect.width,
      cropRect.height,
      dragStart.current.cx + dx,
      dragStart.current.cy + dy,
    );
    setCropRect({ ...cropRect, x: pos.x, y: pos.y });
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLCanvasElement>) => {
    setDragging(false);
    dragStart.current = null;
    e.currentTarget.releasePointerCapture(e.pointerId);
  };

  const handleConfirm = async () => {
    if (!normalized || !cropRect) return;
    setConfirming(true);
    try {
      const blob = await renderMetallographicImage(normalized.canvas, cropRect, scaleUm);
      onConfirm(blob);
    } finally {
      setConfirming(false);
    }
  };

  return (
    <Modal
      title="金相标尺裁剪"
      open={open}
      onCancel={onCancel}
      width={800}
      footer={
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" loading={confirming} onClick={handleConfirm} disabled={!normalized}>
            确认裁剪
          </Button>
        </Space>
      }
      destroyOnClose
    >
      <div style={{ marginBottom: 12, color: '#666', fontSize: 13 }}>
        工作画布 {MET_CANVAS_WIDTH_CM}×{MET_CANVAS_HEIGHT_CM} cm · 裁剪 {MET_CROP_WIDTH_CM}×
        {MET_CROP_HEIGHT_CM} cm · DPI {dpi}
        <br />
        拖动框选区域，标尺线段固定 1cm（仅标签随选项变化）
      </div>

      {cropScaledDown && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="原图分辨率不足，裁剪框已缩至当前画布可容纳的最大尺寸"
        />
      )}

      {loadError && (
        <Alert type="error" showIcon style={{ marginBottom: 12 }} message={loadError} />
      )}

      <div style={{ display: 'flex', justifyContent: 'center', overflow: 'auto', marginBottom: 16 }}>
        <canvas
          ref={canvasRef}
          style={{
            maxWidth: '100%',
            cursor: dragging ? 'grabbing' : 'grab',
            touchAction: 'none',
            border: '1px solid #d9d9d9',
          }}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerLeave={handlePointerUp}
        />
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <span>标尺：</span>
        <Select
          value={scaleUm}
          onChange={setScaleUm}
          style={{ minWidth: 120 }}
          options={MET_SCALE_BAR_OPTIONS.map((v) => ({ label: `${v} μm`, value: v }))}
        />
      </div>
    </Modal>
  );
};

export default MetallographicImageCropModal;
