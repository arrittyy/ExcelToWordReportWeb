const MAX_UPLOAD_BYTES_DEFAULT = 10 * 1024 * 1024;

type CompressOptions = {
  /**
   * 单文件最大字节数（不含 multipart overhead）
   * 需要与后端校验（ImagesController）保持一致。
   */
  maxBytes?: number;
  /** 初始最大边（像素） */
  initialMaxDimension?: number;
  /** 最小质量（0~1） */
  minQuality?: number;
  /** 质量步长（0~1） */
  qualityStep?: number;
  /** 当质量压不下去时，最大边每次缩小比例 */
  dimensionShrinkFactor?: number;
  /** 二次缩放时的最小最大边（像素） */
  minMaxDimension?: number;
};

const blobToFile = (blob: Blob, fileName: string, type: string): File => {
  return new File([blob], fileName, { type });
};

const getJpegFileName = (originalName: string): string => {
  const hasExt = originalName.includes('.');
  if (!hasExt) return `${originalName}.jpg`;
  return originalName.replace(/\.[^.]+$/, '.jpg');
};

const toJpegBlob = (
  canvas: HTMLCanvasElement,
  quality: number
): Promise<Blob> => {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (b) => {
        if (!b) {
          reject(new Error('图片压缩失败：toBlob 返回空'));
          return;
        }
        resolve(b);
      },
      'image/jpeg',
      quality
    );
  });
};

const loadImage = (fileOrUrl: File | string): Promise<HTMLImageElement> => {
  return new Promise((resolve, reject) => {
    const img = new Image();
    let objectUrl: string | null = null;
    img.onload = () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      resolve(img);
    };
    img.onerror = () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      reject(new Error('图片解析失败'));
    };
    if (typeof fileOrUrl === 'string') {
      img.src = fileOrUrl;
      return;
    }
    objectUrl = URL.createObjectURL(fileOrUrl);
    img.src = objectUrl;
  });
};

/**
 * 将图片压缩并尽量转换为 JPEG，以满足后端 10MB 限制。
 * - 如果原图本身就是 JPEG 且已在限制内，会直接返回原文件。
 * - 非 JPEG 的位图会绘制到 canvas，再转成 JPEG。
 */
export const compressImageForUpload = async (
  file: File,
  options: CompressOptions = {}
): Promise<File> => {
  const maxBytes = options.maxBytes ?? MAX_UPLOAD_BYTES_DEFAULT;
  const initialMaxDimension = options.initialMaxDimension ?? 4096;
  // 质量/分辨率适当更激进，提升“保底压到 <=10MB”的成功率
  const minQuality = options.minQuality ?? 0.35;
  const qualityStep = options.qualityStep ?? 0.05;
  const dimensionShrinkFactor = options.dimensionShrinkFactor ?? 0.75;
  const minMaxDimension = options.minMaxDimension ?? 640;

  // 已经合规且为 JPEG：直接复用，减少不必要耗时
  if (file.size <= maxBytes && file.type === 'image/jpeg') {
    return file;
  }

  // SVG 无法稳定 canvas 绘制（也可能导致 Word 解析失败）
  if (file.type === 'image/svg+xml') {
    throw new Error('不支持 SVG，请先转换为 JPG 或 PNG 后再上传');
  }

  const originalMaxDim = Math.max(file.size, 0);
  if (originalMaxDim < 1) {
    // 理论上不会走到这里
    return file;
  }

  const img = await loadImage(file);

  const originalWidth = img.naturalWidth || img.width;
  const originalHeight = img.naturalHeight || img.height;
  const originalLongSide = Math.max(originalWidth, originalHeight);

  // 计算文件名
  const fileName = getJpegFileName(file.name || 'image');

  let currentMaxDimension = initialMaxDimension;

  while (currentMaxDimension >= minMaxDimension) {
    const scale = Math.min(1, currentMaxDimension / originalLongSide);
    const targetWidth = Math.max(1, Math.round(originalWidth * scale));
    const targetHeight = Math.max(1, Math.round(originalHeight * scale));

    const canvas = document.createElement('canvas');
    canvas.width = targetWidth;
    canvas.height = targetHeight;

    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('图片压缩失败：canvas 初始化失败');
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = 'high';

    // JPEG 不支持透明：铺白底
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

    // 从高质量开始逐步降低
    for (let q = 0.92; q >= minQuality; q = Math.max(minQuality, q - qualityStep)) {
      const blob = await toJpegBlob(canvas, q);
      if (blob.size <= maxBytes) {
        return blobToFile(blob, fileName, 'image/jpeg');
      }
      // 当 q 递减到 minQuality 后跳出：避免死循环
      if (q === minQuality) break;
    }

    // 缩小最大边后再试
    currentMaxDimension = Math.floor(currentMaxDimension * dimensionShrinkFactor);
  }

  throw new Error('图片过大：压缩后仍超过 10MB，请换用更小尺寸的图片');
};

