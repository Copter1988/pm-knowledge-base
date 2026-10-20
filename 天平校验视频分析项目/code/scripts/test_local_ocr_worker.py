import unittest
from decimal import Decimal

import local_ocr_worker as worker


LEVELS = [
    {"level": "L1", "nominal": "100"},
    {"level": "L2", "nominal": "20"},
    {"level": "L3", "nominal": "2"},
]


class LocalOcrWorkerTest(unittest.TestCase):
    def test_restores_missing_decimal_from_nominal(self):
        number = worker.parse_number("1000004")
        candidate = worker.display_candidates(
            number, Decimal("100"), preferred_unit="g")

        self.assertEqual("100.0004", format(candidate["shown"], "f"))
        self.assertEqual("g", candidate["unit"])
        self.assertEqual(Decimal("100.0004"), candidate["grams"])

    def test_milligram_display_is_converted_to_grams(self):
        number = worker.parse_number("20000.1m")
        candidate = worker.display_candidates(
            number, Decimal("20"), preferred_unit="mg")

        self.assertEqual("20000.1", format(candidate["shown"], "f"))
        self.assertEqual("mg", candidate["unit"])
        self.assertEqual(Decimal("20.0001"), candidate["grams"])

    def test_zero_and_negative_values_are_ignored(self):
        self.assertIsNone(worker.parse_number("0.0000g"))
        self.assertIsNone(worker.parse_number("-0.0002g"))

    def test_three_stable_platforms_per_level_are_assigned_in_order(self):
        observations = []
        plan = [
            ("1000004", 10), ("1000004", 20), ("1000004", 30),
            ("200002", 45), ("200002", 55), ("200002", 65),
            ("20001", 80), ("20001", 90), ("20001", 100),
        ]
        for text, start in plan:
            number = worker.parse_number(text)
            for second in range(start, start + 5):
                observations.append({
                    "second": second,
                    "text": text,
                    "score": 0.98,
                    "number": number,
                })

        segments = worker.stable_segments(observations)
        assigned = worker.assign_levels(segments, LEVELS)

        self.assertEqual(9, len(assigned))
        self.assertEqual(
            ["L1", "L1", "L1", "L2", "L2", "L2", "L3", "L3", "L3"],
            [level for _, level in assigned])


if __name__ == "__main__":
    unittest.main()
