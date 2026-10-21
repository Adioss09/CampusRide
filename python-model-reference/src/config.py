"""
CampusRide Configuration and Constants

Defines path configurations, schema definitions, defaults, and parameters.
"""

from pathlib import Path
import os
import shutil

# Project Root Directory
BASE_DIR = Path(__file__).resolve().parent.parent

# Data Directories
DATA_DIR = BASE_DIR / "data"
RAW_TIMETABLE_DIR = DATA_DIR / "raw" / "timetables"
INTERIM_OCR_DIR = DATA_DIR / "interim" / "ocr_output"
PROCESSED_DIR = DATA_DIR / "processed"
REPORTS_DIR = BASE_DIR / "reports"

# Processed File Paths
CLEANED_TIMETABLE_PATH = PROCESSED_DIR / "timetable_cleaned.csv"
OUTFLOW_PATH = PROCESSED_DIR / "outflow.csv"
VALIDATION_REPORT_PATH = REPORTS_DIR / "timetable_validation_report.md"
OCR_SUMMARY_REPORT_PATH = REPORTS_DIR / "ocr_extraction_summary.md"
SYNTHETIC_STUDENTS_PATH = PROCESSED_DIR / "synthetic_students.csv"
SYNTHETIC_SIMULATION_SUMMARY_PATH = REPORTS_DIR / "synthetic_simulation_summary.md"

# Outflow & Student Estimation Configuration
# NOTE: 60 students per section is an initial configurable estimate, not a verified ground truth.
DEFAULT_STUDENTS_PER_SECTION = int(os.environ.get("CAMPUSRIDE_STUDENTS_PER_SECTION", 60))

# Demand Window in Minutes (Phase 1 specifies 10-minute demand intervals)
DEMAND_WINDOW_MINUTES = 10

# Expected CSV Schemas
TIMETABLE_CLEANED_COLUMNS = [
    "course",
    "section",
    "day",
    "start_time",
    "end_time",
    "location",
    "students",
    "source_image",
    "validation_status",
]

OUTFLOW_COLUMNS = [
    "day",
    "slot_start",
    "slot_end",
    "estimated_outflow",
    "distinct_classes",
    "quality_flag",
    "notes",
]

# Standard Day Normalization Mapping
DAYS_OF_WEEK = [
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday",
]

# University Gates (7 Campus Locations for entry/exit/pickup)
# Preserves the configured Zone 7 latitude 9.109668 exactly as specified.
UNIVERSITY_GATES = [
    "Zone 1 - Main Gate",
    "Zone 2 - North Gate",
    "Zone 3 - Engineering Gate",
    "Zone 4 - Management Gate",
    "Zone 5 - Library Plaza",
    "Zone 6 - South Gate",
    "Zone 7 - Sports Complex",
]
CONFIGURED_PICKUP_ZONES = UNIVERSITY_GATES
CONFIGURED_DROP_OFF_ZONES = UNIVERSITY_GATES
CAMPUS_ZONES = UNIVERSITY_GATES

UNIVERSITY_GATE_COORDINATES = {
    "Zone 1 - Main Gate": {"latitude": 9.112000, "longitude": 74.800000},
    "Zone 2 - North Gate": {"latitude": 9.115000, "longitude": 74.802000},
    "Zone 3 - Engineering Gate": {"latitude": 9.113500, "longitude": 74.804000},
    "Zone 4 - Management Gate": {"latitude": 9.111000, "longitude": 74.806000},
    "Zone 5 - Library Plaza": {"latitude": 9.110000, "longitude": 74.803000},
    "Zone 6 - South Gate": {"latitude": 9.108000, "longitude": 74.801000},
    "Zone 7 - Sports Complex": {"latitude": 9.109668, "longitude": 74.805000},
}
ZONE_COORDINATES = UNIVERSITY_GATE_COORDINATES

# Transit Hubs (4 External Destinations)
TRANSIT_HUBS = [
    "Transit Hub 1",
    "Transit Hub 2",
    "Transit Hub 3",
    "Transit Hub 4",
]

TRANSIT_HUB_COORDINATES = {
    "Transit Hub 1": {"latitude": 19.127970, "longitude": 72.830969},
    "Transit Hub 2": {"latitude": 19.113311, "longitude": 72.854992},
    "Transit Hub 3": {"latitude": 19.100122, "longitude": 72.843751},
    "Transit Hub 4": {"latitude": 19.119651, "longitude": 72.846287},
}

# Synthetic Student Schema
SYNTHETIC_STUDENT_COLUMNS = [
    "student_id",
    "departure_slot",
    "origin_zone",        # University gate (Zone 1 - Zone 7)
    "destination_zone",   # Transit hub (Transit Hub 1 - 4)
    "willing_to_share",
    "is_synthetic",
]



DAY_NAME_MAP = {
    "mon": "Monday",
    "monday": "Monday",
    "tue": "Tuesday",
    "tues": "Tuesday",
    "tuesday": "Tuesday",
    "wed": "Wednesday",
    "wednesday": "Wednesday",
    "thu": "Thursday",
    "thur": "Thursday",
    "thurs": "Thursday",
    "thursday": "Thursday",
    "fri": "Friday",
    "friday": "Friday",
    "sat": "Saturday",
    "saturday": "Saturday",
    "sun": "Sunday",
    "sunday": "Sunday",
}

# Tesseract executable detection
def get_tesseract_cmd() -> str:
    """Find the path to the Tesseract binary across common environments."""
    env_path = os.environ.get("TESSERACT_CMD")
    if env_path and os.path.exists(env_path):
        return env_path
    
    which_cmd = shutil.which("tesseract")
    if which_cmd:
        return which_cmd

    common_paths = [
        "/opt/homebrew/bin/tesseract",
        "/usr/local/bin/tesseract",
        "/usr/bin/tesseract",
    ]
    for p in common_paths:
        if os.path.exists(p):
            return p

    return "tesseract"
