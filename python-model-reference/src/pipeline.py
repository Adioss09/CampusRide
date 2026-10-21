"""
CampusRide Phase 1 End-to-End Pipeline Runner

Coordinates:
1. Image discovery in data/raw/timetables/
2. Robust OCR preprocessing & extraction
3. Timetable parsing (grid/cell reconstruction)
4. Data cleaning and validation (schema enforcement)
5. Outflow 10-minute window aggregation
6. Mamdani Fuzzy Inference ranking foundation
"""

from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Any
import logging
import sys
from pathlib import Path

# Ensure project root is on sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import pandas as pd

from src.config import (
    CLEANED_TIMETABLE_PATH,
    DATA_DIR,
    INTERIM_OCR_DIR,
    OUTFLOW_PATH,
    RAW_TIMETABLE_DIR,
    REPORTS_DIR,
    VALIDATION_REPORT_PATH,
    OCR_SUMMARY_REPORT_PATH,
    DEFAULT_STUDENTS_PER_SECTION,
)
from src.ocr import TimetableOCREngine, OCRImageDiagnostics
from src.timetable_parser import TimetableParser, RawTimetableRecord
from src.clean import TimetableCleaner
from src.outflow import OutflowEstimator
from src.fuzzy_engine import MamdaniFuzzyEngine

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger("CampusRide.Pipeline")


class CampusRidePipeline:
    def __init__(
        self,
        raw_dir: Optional[Path] = None,
        interim_dir: Optional[Path] = None,
        processed_dir: Optional[Path] = None,
        reports_dir: Optional[Path] = None,
        students_per_section: int = DEFAULT_STUDENTS_PER_SECTION,
    ):
        self.raw_dir = raw_dir or RAW_TIMETABLE_DIR
        self.interim_dir = interim_dir or INTERIM_OCR_DIR
        self.processed_dir = processed_dir or (DATA_DIR / "processed")
        self.reports_dir = reports_dir or REPORTS_DIR

        self.ocr_engine = TimetableOCREngine(interim_dir=self.interim_dir)
        self.parser = TimetableParser()
        self.cleaner = TimetableCleaner(
            students_per_section=students_per_section,
            output_csv_path=self.processed_dir / "timetable_cleaned.csv",
            report_path=self.reports_dir / "timetable_validation_report.md",
        )
        self.outflow_estimator = OutflowEstimator(
            output_csv_path=self.processed_dir / "outflow.csv"
        )
        self.fuzzy_engine = MamdaniFuzzyEngine()

    def run(self) -> Dict[str, Any]:
        """Execute full pipeline."""
        logger.info("=== Starting CampusRide Phase 1 Pipeline ===")

        # 1. Discover timetable images
        image_extensions = {".png", ".jpg", ".jpeg", ".tiff", ".bmp", ".webp"}
        image_files = [
            f for f in self.raw_dir.iterdir()
            if f.is_file() and f.suffix.lower() in image_extensions
        ] if self.raw_dir.exists() else []

        logger.info(f"Discovered {len(image_files)} image(s) in {self.raw_dir}")

        all_raw_records: List[RawTimetableRecord] = []
        diagnostics_list: List[OCRImageDiagnostics] = []

        if not image_files:
            logger.warning(
                f"No timetable images found in {self.raw_dir}. "
                "Initializing empty structured outputs with required schema headers. "
                "Real-image validation remains pending."
            )
            # Generate empty CSVs with headers
            cleaned_df, metrics = self.cleaner.clean_and_validate([], save_csv=True, save_report=True)
            outflow_df = self.outflow_estimator.calculate_outflow(cleaned_df, save_csv=True)
            self._save_ocr_summary([], self.reports_dir / "ocr_extraction_summary.md")

            # Run synthetic demonstration of the fuzzy engine
            sample_fuzzy_results = self._run_sample_fuzzy_evaluations()

            return {
                "images_processed": 0,
                "raw_records": 0,
                "cleaned_records": len(cleaned_df),
                "outflow_windows": len(outflow_df),
                "status": "COMPLETED_EMPTY_INPUTS_PENDING_REAL_IMAGES",
                "fuzzy_demonstration": sample_fuzzy_results,
            }

        # 2. Process each image through OCR and Parser
        for img_path in sorted(image_files):
            cells, diag = self.ocr_engine.process_image(img_path)
            diagnostics_list.append(diag)
            if cells:
                records = self.parser.parse_cells(cells, source_image=img_path.name)
                all_raw_records.extend(records)
                logger.info(f"Parsed {len(records)} records from {img_path.name}")
            else:
                logger.warning(f"No usable cells extracted from {img_path.name}")

        # 3. Clean and validate
        cleaned_df, metrics = self.cleaner.clean_and_validate(
            all_raw_records, save_csv=True, save_report=True
        )

        # 4. Outflow demand window estimation
        outflow_df = self.outflow_estimator.calculate_outflow(
            cleaned_df, save_csv=True
        )

        # 5. Save OCR summary report
        self._save_ocr_summary(diagnostics_list, OCR_SUMMARY_REPORT_PATH)

        # 6. Sample Fuzzy inference on top outflow windows
        fuzzy_results = []
        for _, row in outflow_df.head(5).iterrows():
            outflow_val = float(row["estimated_outflow"])
            res = self.fuzzy_engine.evaluate(
                estimated_outflow=outflow_val,
                time_until_dismissal=10.0,
                driver_availability=None,
            )
            fuzzy_results.append({
                "day": row["day"],
                "slot": f"{row['slot_start']}-{row['slot_end']}",
                "outflow": outflow_val,
                "score": res.crisp_score,
                "label": res.priority_label,
            })

        logger.info("=== CampusRide Phase 1 Pipeline Run Complete ===")
        return {
            "images_processed": len(image_files),
            "raw_records": len(all_raw_records),
            "cleaned_records": len(cleaned_df),
            "outflow_windows": len(outflow_df),
            "status": "SUCCESS",
            "fuzzy_samples": fuzzy_results,
        }

    def _save_ocr_summary(
        self, diagnostics: List[OCRImageDiagnostics], out_path: Path
    ):
        """Write OCR extraction summary markdown report."""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        lines = [
            "# CampusRide OCR Extraction Summary Report",
            f"**Generated:** {timestamp}",
            f"**Total Source Images:** {len(diagnostics)}",
            "",
            "---",
            "",
            "## Image Diagnostic Breakdown",
            "",
        ]

        if not diagnostics:
            lines.append("*No timetable images present in data/raw/timetables/ to process.*")
        else:
            lines.append("| Image | Dimensions | Sharpness (Blur Var) | Skew Angle | Layout | Cells | Confidence | Status |")
            lines.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |")
            for d in diagnostics:
                lines.append(
                    f"| `{d.image_name}` | {d.original_width}x{d.original_height} | "
                    f"{d.blur_score} | {d.estimated_skew_angle}° | `{d.detected_layout}` | "
                    f"{d.cell_count} | {d.avg_confidence}% | `{d.status}` |"
                )

        lines.extend([
            "",
            "---",
            "## Storage Locations",
            f"- **Raw OCR Text & Coordinates:** `data/interim/ocr_output/`",
            f"- **Cleaned Timetable CSV:** `data/processed/timetable_cleaned.csv`",
            f"- **10-Minute Outflow CSV:** `data/processed/outflow.csv`",
            f"- **Validation Warnings:** `reports/timetable_validation_report.md`",
        ])

        with open(out_path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines))
        logger.info(f"Saved OCR summary report to {out_path}")

    def _run_sample_fuzzy_evaluations(self) -> List[Dict[str, Any]]:
        """Run standard benchmark scenarios through Mamdani FIS."""
        scenarios = [
            {"desc": "High outflow, imminent dismissal, low driver supply", "outflow": 240, "mins": 3.0, "drivers": 2},
            {"desc": "High outflow, imminent dismissal, unobserved drivers", "outflow": 240, "mins": 3.0, "drivers": None},
            {"desc": "Medium outflow, approaching dismissal, medium drivers", "outflow": 120, "mins": 15.0, "drivers": 10},
            {"desc": "Low outflow, far dismissal, high drivers", "outflow": 40, "mins": 45.0, "drivers": 25},
        ]
        results = []
        for s in scenarios:
            res = self.fuzzy_engine.evaluate(
                estimated_outflow=s["outflow"],
                time_until_dismissal=s["mins"],
                driver_availability=s["drivers"],
            )
            results.append({
                "scenario": s["desc"],
                "score": res.crisp_score,
                "label": res.priority_label,
                "driver_status": res.driver_input_status,
                "fired_rules_count": len(res.fired_rules),
            })
        return results


def main():
    pipeline = CampusRidePipeline()
    summary = pipeline.run()
    print("\n--- Pipeline Execution Summary ---")
    for k, v in summary.items():
        print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
