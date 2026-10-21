"""
CampusRide Outflow Estimation Module

Calculates estimated student outflow in 10-minute intervals from validated timetable data.
Handles exact boundary intervals (e.g., 16:00 -> 16:00–16:10).
Preserves the distinction between estimated student dismissal outflow and actual ride demand.
Documents the absence of verified campus pickup-zone spatial mappings without fabricating GPS coordinates.
"""

from datetime import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any, Set
import logging
import pandas as pd

from src.config import (
    CLEANED_TIMETABLE_PATH,
    DEMAND_WINDOW_MINUTES,
    OUTFLOW_PATH,
    OUTFLOW_COLUMNS,
    DAYS_OF_WEEK,
)

logger = logging.getLogger(__name__)


def time_to_minutes(t_str: str) -> int:
    """Convert 'HH:MM' string to total minutes from midnight."""
    h, m = map(int, t_str.strip().split(":"))
    return h * 60 + m


def minutes_to_time_str(mins: int) -> str:
    """Convert total minutes to 'HH:MM' string (capping at 24:00)."""
    if mins == 1440:
        return "24:00"
    h = (mins // 60) % 24
    m = mins % 60
    return f"{h:02d}:{m:02d}"


def get_slot_bounds(end_time_str: str, window_minutes: int = DEMAND_WINDOW_MINUTES) -> Tuple[str, str]:
    """
    Computes (slot_start, slot_end) containing the class dismissal time.
    Exact boundary requirement:
      A class ending at 16:00 contributes to 16:00–16:10.
      A class ending at 16:05 contributes to 16:00–16:10.
      A class ending at 16:10 contributes to 16:10–16:20.
    """
    total_mins = time_to_minutes(end_time_str)
    slot_start_mins = (total_mins // window_minutes) * window_minutes
    slot_end_mins = slot_start_mins + window_minutes
    return minutes_to_time_str(slot_start_mins), minutes_to_time_str(slot_end_mins)


class OutflowEstimator:
    """Aggregates timetable dismissals into 10-minute outflow demand windows."""

    def __init__(
        self,
        window_minutes: int = DEMAND_WINDOW_MINUTES,
        output_csv_path: Optional[Path] = None,
    ):
        self.window_minutes = window_minutes
        self.output_csv_path = output_csv_path or OUTFLOW_PATH
        self.output_csv_path.parent.mkdir(parents=True, exist_ok=True)

    def calculate_outflow(
        self,
        timetable_df: pd.DataFrame,
        save_csv: bool = True,
        include_empty_slots: bool = False,
    ) -> pd.DataFrame:
        """
        Aggregate distinct, validated timetable records by day and class end-time.
        Excludes DUPLICATE records to avoid inflation.
        Flags ambiguous section overlaps.
        """
        if timetable_df.empty:
            logger.warning("Empty timetable dataframe provided to OutflowEstimator.")
            empty_df = pd.DataFrame(columns=OUTFLOW_COLUMNS)
            if save_csv:
                empty_df.to_csv(self.output_csv_path, index=False)
            return empty_df

        # Filter out records that are invalid or duplicates
        valid_df = timetable_df[timetable_df["validation_status"] == "VALID"].copy()
        review_df = timetable_df[timetable_df["validation_status"] == "NEEDS_REVIEW"].copy()

        # Check for overlaps / conflicts among valid records (same day, same section, overlapping times)
        overlaps = self._detect_conflicts(valid_df)

        slot_buckets: Dict[Tuple[str, str, str], Dict[str, Any]] = {}

        for _, row in valid_df.iterrows():
            day = row["day"]
            end_t = row["end_time"]
            if not day or not end_t:
                continue

            slot_start, slot_end = get_slot_bounds(end_t, self.window_minutes)
            key = (day, slot_start, slot_end)

            if key not in slot_buckets:
                slot_buckets[key] = {
                    "estimated_outflow": 0,
                    "classes": [],
                    "has_overlaps": False,
                }

            students = int(row.get("students", 60) or 60)
            slot_buckets[key]["estimated_outflow"] += students
            course_name = f"{row.get('course', 'UNKNOWN')} ({row.get('section', 'SEC')})"
            slot_buckets[key]["classes"].append(course_name)

            if (day, row.get("course"), row.get("section")) in overlaps:
                slot_buckets[key]["has_overlaps"] = True

        # Build output records
        rows: List[Dict[str, Any]] = []
        for (day, slot_start, slot_end), data in slot_buckets.items():
            classes_list = data["classes"]
            has_overlap = data["has_overlaps"]

            quality_flag = "CONFIRMED"
            notes = f"Estimated dismissal from {len(classes_list)} class(es): {', '.join(classes_list[:5])}"
            if len(classes_list) > 5:
                notes += f" and {len(classes_list) - 5} more"

            if has_overlap:
                quality_flag = "HAS_UNRESOLVED_OVERLAPS"
                notes += " | Warning: Section schedule overlap detected in this window."

            rows.append({
                "day": day,
                "slot_start": slot_start,
                "slot_end": slot_end,
                "estimated_outflow": data["estimated_outflow"],
                "distinct_classes": len(classes_list),
                "quality_flag": quality_flag,
                "notes": notes,
            })

        outflow_df = pd.DataFrame(rows, columns=OUTFLOW_COLUMNS)

        # Sort chronologically by day of week order and slot start
        if not outflow_df.empty:
            day_order = {day: i for i, day in enumerate(DAYS_OF_WEEK)}
            outflow_df["day_rank"] = outflow_df["day"].map(lambda d: day_order.get(d, 99))
            outflow_df = outflow_df.sort_values(by=["day_rank", "slot_start"]).drop(columns=["day_rank"])

        if save_csv:
            outflow_df.to_csv(self.output_csv_path, index=False)
            logger.info(f"Saved {len(outflow_df)} outflow windows to {self.output_csv_path}")

        return outflow_df

    def _detect_conflicts(self, df: pd.DataFrame) -> Set[Tuple[str, Optional[str], Optional[str]]]:
        """Detect sections with overlapping class times on the same day."""
        conflicts = set()
        for day in df["day"].dropna().unique():
            day_df = df[df["day"] == day]
            for (sec, course), group in day_df.groupby(["section", "course"]):
                if sec and len(group) > 1:
                    # Check intervals
                    sorted_slots = group.sort_values(by="start_time")
                    times = [
                        (time_to_minutes(r["start_time"]), time_to_minutes(r["end_time"]))
                        for _, r in sorted_slots.iterrows()
                        if r["start_time"] and r["end_time"]
                    ]
                    for i in range(len(times) - 1):
                        if times[i][1] > times[i + 1][0]:  # overlap
                            conflicts.add((day, course, sec))
        return conflicts
