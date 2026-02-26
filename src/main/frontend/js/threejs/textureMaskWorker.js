function colorMatch(r1, g1, b1, r2, g2, b2, tolerance = 8) {//tolerance due to area having some noise in and around them
  return (
    Math.abs(r1 - r2) <= tolerance &&
    Math.abs(g1 - g2) <= tolerance &&
    Math.abs(b1 - b2) <= tolerance
  );
}

self.onmessage = function(e) {
  try {
    const { mainData, maskData, maskColorRgb, width, height, opacity = 0.5 } = e.data;

    const src = mainData instanceof ArrayBuffer ? new Uint8ClampedArray(mainData) : new Uint8ClampedArray(mainData.buffer || mainData);
    const mask = maskData instanceof ArrayBuffer ? new Uint8ClampedArray(maskData) : new Uint8ClampedArray(maskData.buffer || maskData);

    const totalPixels = width * height;
    const out = new Uint8ClampedArray(totalPixels * 4);
    const alphaThreshold = 10;

    for (let i = 0, p = 0; i < totalPixels; i++, p += 4) {
      const maskR = mask[p];
      const maskG = mask[p + 1];
      const maskB = mask[p + 2];
      const maskA = mask[p + 3];

      if (maskA > alphaThreshold && colorMatch(maskR, maskG, maskB, maskColorRgb.r, maskColorRgb.g, maskColorRgb.b)) {
        out[p] = Math.round(src[p] * (1 - opacity) + maskColorRgb.r * opacity);
        out[p + 1] = Math.round(src[p + 1] * (1 - opacity) + maskColorRgb.g * opacity);
        out[p + 2] = Math.round(src[p + 2] * (1 - opacity) + maskColorRgb.b * opacity);
        out[p + 3] = 255;
      } else {
        out[p] = src[p];
        out[p + 1] = src[p + 1];
        out[p + 2] = src[p + 2];
        out[p + 3] = src[p + 3];
      }
    }

    self.postMessage({ mainData: out.buffer }, [out.buffer]);
  } catch (err) {
    self.postMessage({ error: err && err.message ? err.message : String(err) });
  }
};

export {};
