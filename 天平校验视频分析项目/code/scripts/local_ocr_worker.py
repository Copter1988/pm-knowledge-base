#!/usr/bin/env python
"""Local OCR bridge used by Java. RapidOCR runs fully on the local machine."""

import contextlib
import json
import sys
import warnings

import cv2
import numpy as np

warnings.filterwarnings("ignore")


def main():
    request = json.load(sys.stdin)
    images = request.get("images") or []
    max_width = int(request.get("maxWidth") or 480)

    with contextlib.redirect_stdout(sys.stderr):
        from rapidocr_onnxruntime import RapidOCR
        engine = RapidOCR()

    frames = []
    for item in images:
        path = item.get("path")
        second = item.get("second")
        image = None
        if path:
            # cv2.imread does not handle non-ASCII Windows paths reliably.
            with open(path, "rb") as stream:
                image = cv2.imdecode(np.frombuffer(stream.read(), dtype=np.uint8),
                                     cv2.IMREAD_COLOR)
        texts = []
        if image is not None:
            height, width = image.shape[:2]
            if width > max_width:
                scale = max_width / float(width)
                image = cv2.resize(image, (max_width, int(height * scale)),
                                   interpolation=cv2.INTER_AREA)
            with contextlib.redirect_stdout(sys.stderr):
                result, _ = engine(image)
            for box, text, score in result or []:
                value = str(text).strip()
                if value:
                    texts.append({
                        "text": value,
                        "score": round(float(score), 5),
                        "box": [[round(float(x), 1), round(float(y), 1)] for x, y in box],
                    })
        frames.append({"path": path, "second": second, "texts": texts})

    json.dump({"ok": True, "frames": frames}, sys.stdout, ensure_ascii=False)
    sys.stdout.write("\n")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        json.dump({"ok": False, "error": str(exc)}, sys.stdout, ensure_ascii=False)
        sys.stdout.write("\n")
        raise
