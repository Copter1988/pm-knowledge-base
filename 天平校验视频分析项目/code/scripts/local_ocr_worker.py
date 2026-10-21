#!/usr/bin/env python
"""Local OCR bridge used by Java. RapidOCR runs fully on the local machine."""

import contextlib
import json
import re
import sys
import warnings
from collections import Counter, defaultdict
from decimal import Decimal

import cv2
import numpy as np

warnings.filterwarnings("ignore")

NUMBER_RE = re.compile(r"[+-]?\d[\d,]*(?:\.\d+)?")
CHECKER_CODE_RE = re.compile(r"\bJY([0-9OQDISBZL]{3,})\b")
LEVEL_PATTERN = ("L1", "L1", "L1", "L2", "L2", "L2", "L3", "L3", "L3")
OCR_CHAR_MAP = str.maketrans({
    "O": "0", "Q": "0", "D": "0",
    "I": "1", "L": "1",
    "Z": "2",
    "S": "5",
    "B": "8",
})


def read_image(path):
    # cv2.imread does not handle non-ASCII Windows paths reliably.
    with open(path, "rb") as stream:
        return cv2.imdecode(np.frombuffer(stream.read(), dtype=np.uint8),
                            cv2.IMREAD_COLOR)


def normalize_checker_code(value):
    if not value:
        return None
    digits = str(value).upper().replace(" ", "")
    if digits.startswith("JY"):
        digits = digits[2:]
    digits = (digits.replace("O", "0").replace("Q", "0").replace("D", "0")
              .replace("I", "1").replace("L", "1").replace("Z", "2")
              .replace("S", "5").replace("B", "8"))
    return "JY" + digits


def detect_checker_card(card_frames):
    best_code = None
    best_score = 0.0
    for frame in card_frames:
        for item in frame.get("texts") or []:
            text = str(item.get("text") or "").upper().replace(" ", "")
            match = CHECKER_CODE_RE.search(text)
            if not match:
                continue
            code = normalize_checker_code(match.group(0))
            score = float(item.get("score") or 0)
            if score > best_score:
                best_code = code
                best_score = score
    return best_code


def crop_roi(image, roi):
    height, width = image.shape[:2]
    x = int(width * float(roi.get("x", 0.12)))
    y = int(height * float(roi.get("y", 0.62)))
    w = int(width * float(roi.get("width", 0.78)))
    h = int(height * float(roi.get("height", 0.17)))
    return image[max(0, y):min(height, y + h),
                 max(0, x):min(width, x + w)]


def prepare_for_recognition(image):
    height, width = image.shape[:2]
    if height == 0 or width == 0:
        return image
    image = cv2.resize(image, (width * 3, height * 3),
                       interpolation=cv2.INTER_CUBIC)
    return cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)


def parse_number(text):
    compact = text.upper().replace(" ", "")
    compact = compact.replace("\uff0c", ".").replace(",", ".").translate(OCR_CHAR_MAP)
    match = NUMBER_RE.search(compact)
    if not match:
        return None
    token = match.group(0)
    negative = token.startswith("-")
    unsigned = token.lstrip("+-")
    has_dot = "." in unsigned
    digits = "".join(ch for ch in unsigned if ch.isdigit())
    if not digits or not any(ch != "0" for ch in digits) or negative:
        return None
    return {
        "text": text,
        "digits": digits,
        "hasDot": has_dot,
        "dotDigits": len(unsigned.split(".", 1)[1]) if has_dot else None,
        "numeric": Decimal(digits),
    }


def display_candidates(number, nominal, preferred_unit=None):
    digits = number["digits"]
    candidates = []
    if number["hasDot"]:
        shown_values = [Decimal(digits) / (Decimal(10) ** number["dotDigits"])]
        decimal_counts = [number["dotDigits"]]
    else:
        shown_values = []
        decimal_counts = []
        for decimal_count in range(0, min(6, max(0, len(digits) - 1)) + 1):
            shown_values.append(Decimal(digits) / (Decimal(10) ** decimal_count))
            decimal_counts.append(decimal_count)

    units = [preferred_unit] if preferred_unit else ["g", "mg"]
    for shown, decimal_count in zip(shown_values, decimal_counts):
        if shown <= 0:
            continue
        for unit in units:
            grams = shown / Decimal(1000) if unit == "mg" else shown
            relative = abs(grams - nominal) / nominal
            if relative <= Decimal("0.02"):
                candidates.append({
                    "grams": grams,
                    "shown": shown,
                    "unit": unit,
                    "decimalDigits": decimal_count,
                    "relative": relative,
                })
    if not candidates:
        return None
    return min(candidates, key=lambda item: (
        item["relative"],
        item["decimalDigits"],
        0 if item["unit"] == "g" else 1,
    ))


def closest_candidate(number, levels, preferred_unit=None):
    best = None
    for level in levels:
        nominal = Decimal(str(level["nominal"]))
        if nominal <= 0:
            continue
        candidate = display_candidates(number, nominal, preferred_unit)
        if candidate is None:
            continue
        item = dict(candidate)
        item["level"] = level["level"]
        if best is None or item["relative"] < best["relative"]:
            best = item
    return best


def same_plateau(left, right):
    maximum = max(abs(left), abs(right))
    if maximum == 0:
        return left == right
    return abs(left - right) / maximum <= Decimal("0.0005")


def stable_segments(observations):
    segments = []
    current = None
    gaps = []
    for previous, item in zip(observations, observations[1:]):
        gap = item["second"] - previous["second"]
        if gap > 0:
            gaps.append(gap)
    sample_step = min(gaps) if gaps else 1
    maximum_gap = max(3, sample_step + 1)

    for obs in sorted(observations, key=lambda item: item["second"]):
        same = (current is not None
                and obs["second"] - current["lastSecond"] <= maximum_gap
                and same_plateau(current["anchorNumeric"], obs["number"]["numeric"]))
        if same:
            current["items"].append(obs)
            current["lastSecond"] = obs["second"]
        else:
            if current is not None:
                segments.append(current)
            current = {
                "items": [obs],
                "lastSecond": obs["second"],
                "anchorNumeric": obs["number"]["numeric"],
            }
    if current is not None:
        segments.append(current)

    result = []
    for segment in segments:
        items = segment["items"]
        duration = items[-1]["second"] - items[0]["second"]
        if len(items) < 3 or duration < 2:
            continue
        values = sorted(item["number"]["numeric"] for item in items)
        median = values[len(values) // 2]
        representative = max(
            items,
            key=lambda item: (
                -abs(item["number"]["numeric"] - median),
                item["score"],
            ))
        average_score = sum(item["score"] for item in items) / len(items)
        result.append({
            "items": items,
            "number": representative["number"],
            "second": items[len(items) // 2]["second"],
            "duration": duration,
            "samples": len(items),
            "confidence": average_score,
            "quality": average_score + min(duration, 20) * 0.015,
        })
    return result


def assign_levels(segments, levels):
    level_by_name = {str(item["level"]): item for item in levels}
    pattern = [level for level in LEVEL_PATTERN if level in level_by_name]
    if len(pattern) < len(LEVEL_PATTERN):
        return []

    count = len(segments)
    negative = -1e100
    dp = [[negative] * 10 for _ in range(count + 1)]
    choice = [[None] * 10 for _ in range(count + 1)]
    dp[0][0] = 0.0

    for index, segment in enumerate(segments):
        for slot in range(10):
            score = dp[index][slot]
            if score == negative:
                continue
            if score > dp[index + 1][slot]:
                dp[index + 1][slot] = score
                choice[index + 1][slot] = ("skip", slot)
            if slot >= 9:
                continue
            level = pattern[slot]
            nominal = Decimal(str(level_by_name[level]["nominal"]))
            candidate = display_candidates(segment["number"], nominal)
            if candidate is None:
                continue
            candidate_score = (
                score
                + segment["quality"]
                + 1.0
                - float(candidate["relative"]) * 100.0
            )
            if candidate_score >= dp[index + 1][slot + 1]:
                dp[index + 1][slot + 1] = candidate_score
                choice[index + 1][slot + 1] = ("take", slot)

    best_slot = max(range(10), key=lambda slot: (dp[count][slot], slot))
    assigned = {}
    index = count
    slot = best_slot
    while index > 0:
        selected = choice[index][slot]
        if selected is None:
            break
        action, previous_slot = selected
        if action == "take":
            assigned[previous_slot] = segments[index - 1]
        slot = previous_slot
        index -= 1
    return [(assigned.get(slot), pattern[slot]) for slot in range(best_slot)]


def unit_from_text(text):
    compact = str(text or "").upper().replace(" ", "")
    match = NUMBER_RE.search(compact)
    suffix = compact[match.end():] if match else compact
    if "MG" in suffix or suffix.startswith("M"):
        return "mg"
    if "G" in suffix:
        return "g"
    return None


def detect_unit(image, screen_roi, engine):
    screen = crop_roi(image, screen_roi)
    screen = cv2.resize(
        screen, None, fx=3, fy=3, interpolation=cv2.INTER_CUBIC)
    with contextlib.redirect_stdout(sys.stderr):
        result, _ = engine(screen)
    units = []
    for _, text, score in result or []:
        compact = str(text).strip().lower().replace(" ", "")
        if compact in ("g", "mg"):
            units.append((float(score), compact))
    return max(units)[1] if units else None


def format_fixed(digits, decimal_count):
    if decimal_count <= 0:
        return digits
    digits = digits.zfill(decimal_count + 1)
    return digits[:-decimal_count] + "." + digits[-decimal_count:]


def find_stable_readings(reading_images, roi, screen_roi, levels, engine):
    if not levels:
        return [], []
    observations = []
    for item in reading_images:
        path = item.get("path")
        second = item.get("second")
        image = read_image(path) if path else None
        if image is None:
            continue
        display = prepare_for_recognition(crop_roi(image, roi))
        with contextlib.redirect_stdout(sys.stderr):
            result, _ = engine(display, use_det=False, use_cls=False, use_rec=True)
        if not result:
            continue
        text, score = result[0]
        if float(score) < 0.55:
            continue
        number = parse_number(str(text))
        if number is None or closest_candidate(number, levels) is None:
            continue
        observations.append({
            "second": int(second),
            "text": str(text),
            "score": float(score),
            "number": number,
        })

    segments = stable_segments(observations)
    assigned = assign_levels(segments, levels)

    units_by_level = {}
    selected = []
    for slot, (segment, level) in enumerate(assigned):
        if segment is None:
            continue
        unit = unit_from_text(segment["number"]["text"])
        if unit is None:
            unit = units_by_level.get(level)
        if unit is None:
            frame = segment["items"][len(segment["items"]) // 2]
            image = read_image(next(
                item["path"] for item in reading_images
                if int(item.get("second", -1)) == frame["second"]))
            unit = detect_unit(image, screen_roi, engine) if image is not None else None
        if unit is None:
            unit = "g"
        units_by_level[level] = unit
        selected.append((slot, segment, level, unit))

    decimals = defaultdict(list)
    level_by_name = {str(item["level"]): item for item in levels}
    for _, segment, level, unit in selected:
        nominal = Decimal(str(level_by_name[level]["nominal"]))
        candidate = display_candidates(segment["number"], nominal, unit)
        if candidate is not None:
            decimals[(level, unit)].append(candidate["decimalDigits"])
    decimal_mode = {}
    for key, values in decimals.items():
        counts = Counter(values)
        decimal_mode[key] = max(counts, key=lambda value: (counts[value], value))

    readings = []
    for slot, segment, level, unit in selected:
        nominal = Decimal(str(level_by_name[level]["nominal"]))
        decimal_count = decimal_mode.get(
            (level, unit),
            display_candidates(
                segment["number"], nominal, unit)["decimalDigits"])
        raw = format_fixed(segment["number"]["digits"], decimal_count)
        shown = Decimal(raw)
        grams = shown / Decimal(1000) if unit == "mg" else shown
        readings.append({
            "seq": slot + 1,
            "level": level,
            "raw": raw,
            "unit": unit,
            "grams": format(grams.normalize(), "f"),
            "second": segment["second"],
            "confidence": round(segment["confidence"], 4),
            "stableSamples": segment["samples"],
            "stableSeconds": segment["duration"],
        })
    return readings, observations


def main():
    request = json.load(sys.stdin)
    card_images = request.get("images") or []
    reading_images = request.get("readingImages") or []
    levels = request.get("levels") or []
    roi = request.get("readingRoi") or {}
    screen_roi = request.get("screenRoi") or {}
    expected_checker = normalize_checker_code(request.get("expectedCheckerCardCode"))
    max_width = int(request.get("maxWidth") or 480)

    with contextlib.redirect_stdout(sys.stderr):
        from rapidocr_onnxruntime import RapidOCR
        engine = RapidOCR()

    card_frames = []
    for item in card_images:
        path = item.get("path")
        second = item.get("second")
        image = read_image(path) if path else None
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
        card_frames.append({"path": path, "second": second, "texts": texts})
        current_checker = detect_checker_card(card_frames)
        if expected_checker and current_checker and current_checker != expected_checker:
            # 身份已经足以判定不一致，不再扫描其余卡片或整段示值。
            break

    checker_code = detect_checker_card(card_frames)
    early_checker_mismatch = bool(
        expected_checker and checker_code and expected_checker != checker_code)
    if early_checker_mismatch:
        readings, observations = [], []
    else:
        readings, observations = find_stable_readings(
            reading_images, roi, screen_roi, levels, engine)
    observation_dump = [
        {
            "second": item["second"],
            "text": item["text"],
            "score": round(item["score"], 4),
        }
        for item in observations
    ]
    payload = json.dumps({
        "ok": True,
        "frames": card_frames,
        "readings": readings,
        "observations": observation_dump,
        "checkerCardCode": checker_code,
        "earlyCheckerMismatch": early_checker_mismatch,
    }, ensure_ascii=False)
    sys.stdout.write(payload)
    sys.stdout.write("\n")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        json.dump({"ok": False, "error": str(exc)}, sys.stdout, ensure_ascii=False)
        sys.stdout.write("\n")
        raise
