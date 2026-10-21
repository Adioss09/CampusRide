"""
CampusRide Simulated Passenger Grouping Module

Implements basic ride-pooling compatibility grouping for synthetic students:
- Matches 2 or more simulated students who:
  1. Share the exact same origin university gate.
  2. Share the exact same destination transit hub.
  3. Are willing to share a ride (`willing_to_share == True`).
  4. Have compatible departure times (same 10-minute slot).
- Students who cannot be matched remain as individual/unmatched passengers.
- Guarantees each student belongs to at most one group.
- Reports clear group IDs, member student IDs, and summary statistics.

NOTE: This is a simulation preparing for future ride pooling. It does not
implement actual ride booking, payment processing, or live driver dispatch.
"""

from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
import logging
import pandas as pd

logger = logging.getLogger(__name__)


@dataclass
class PassengerGroup:
    """Represents a formed ride-pooling group of compatible students."""
    group_id: str
    origin_gate: str
    destination_hub: str
    departure_slot: str
    member_student_ids: List[str]
    group_size: int


@dataclass
class GroupingResult:
    """Summary and details of simulated passenger grouping."""
    groups: List[PassengerGroup]
    unmatched_student_ids: List[str]
    total_students: int
    eligible_students: int
    ineligible_students: int
    matched_students: int
    unmatched_students: int
    groups_formed: int

    def to_dataframe(self) -> pd.DataFrame:
        """Flattens grouping assignments into a student-level DataFrame."""
        records: List[Dict[str, Any]] = []

        # Matched students
        for grp in self.groups:
            for s_id in grp.member_student_ids:
                records.append({
                    "student_id": s_id,
                    "group_id": grp.group_id,
                    "origin_gate": grp.origin_gate,
                    "destination_hub": grp.destination_hub,
                    "departure_slot": grp.departure_slot,
                    "group_size": grp.group_size,
                    "pooling_status": "MATCHED",
                })

        # Unmatched students
        for s_id in self.unmatched_student_ids:
            records.append({
                "student_id": s_id,
                "group_id": None,
                "origin_gate": None,
                "destination_hub": None,
                "departure_slot": None,
                "group_size": 1,
                "pooling_status": "UNMATCHED",
            })

        df = pd.DataFrame(records)
        return df.sort_values(by="student_id").reset_index(drop=True)

    def summary(self) -> str:
        """Returns a formatted textual summary of grouping metrics."""
        lines = [
            "=== Simulated Passenger Grouping Summary ===",
            f"Total Students:        {self.total_students}",
            f"Eligible to Share:     {self.eligible_students} ({(self.eligible_students / self.total_students * 100):.1f}%)" if self.total_students > 0 else "0",
            f"Unwilling to Share:    {self.ineligible_students}",
            f"Matched Students:      {self.matched_students}",
            f"Unmatched Students:    {self.unmatched_students}",
            f"Groups Formed (>= 2):  {self.groups_formed}",
        ]
        return "\n".join(lines)


def _partition_into_groups(members: List[str], max_group_size: int = 4) -> Tuple[List[List[str]], List[str]]:
    """
    Partitions a bucket of compatible student IDs into groups of size [2, max_group_size].
    Leaves remaining solitary students unmatched.
    """
    n = len(members)
    if n < 2:
        return [], list(members)

    if n <= max_group_size:
        return [list(members)], []

    # Partition into chunks of at most max_group_size
    chunks: List[List[str]] = []
    i = 0
    while i < n:
        chunk = members[i : i + max_group_size]
        chunks.append(chunk)
        i += max_group_size

    # If the last chunk has only 1 student, rebalance from previous chunk if possible
    unmatched: List[str] = []
    if len(chunks[-1]) == 1:
        last_single = chunks.pop()
        if chunks and len(chunks[-1]) > 2:
            # Borrow one member from the penultimate chunk: e.g. 4 + 1 -> 3 + 2
            borrowed = chunks[-1].pop()
            chunks.append([borrowed, last_single[0]])
        else:
            # Cannot borrow without reducing previous chunk below 2
            unmatched.extend(last_single)

    return chunks, unmatched


def group_passengers(
    df: pd.DataFrame,
    max_group_size: int = 4,
    origin_col: str = "origin_zone",
    destination_col: str = "destination_zone",
) -> GroupingResult:
    """
    Groups simulated students by matching:
    1. Same origin university gate
    2. Same destination transit hub
    3. Same 10-minute departure slot
    4. Willingness to share (`willing_to_share == True`)

    Args:
        df: DataFrame containing student simulation records.
        max_group_size: Maximum passenger capacity per pooled group (default 4).
        origin_col: Column name for trip origin.
        destination_col: Column name for trip destination.

    Returns:
        GroupingResult with formed groups, unmatched students, and summary statistics.
    """
    if df.empty:
        return GroupingResult(
            groups=[],
            unmatched_student_ids=[],
            total_students=0,
            eligible_students=0,
            ineligible_students=0,
            matched_students=0,
            unmatched_students=0,
            groups_formed=0,
        )

    # Ensure required columns exist
    req_cols = ["student_id", "departure_slot", "willing_to_share", origin_col, destination_col]
    for col in req_cols:
        if col not in df.columns:
            raise ValueError(f"Required column '{col}' missing from student DataFrame.")

    total_students = len(df)
    assigned_students: set = set()
    groups: List[PassengerGroup] = []
    unmatched_ids: List[str] = []

    # 1. Identify ineligible students (unwilling to share)
    ineligible_df = df[~df["willing_to_share"].astype(bool)]
    unmatched_ids.extend(ineligible_df["student_id"].tolist())
    assigned_students.update(ineligible_df["student_id"].tolist())

    # 2. Filter eligible students
    eligible_df = df[df["willing_to_share"].astype(bool)]
    eligible_count = len(eligible_df)
    ineligible_count = len(ineligible_df)

    # 3. Group by (origin, destination, departure_slot)
    grouped_buckets = eligible_df.groupby([origin_col, destination_col, "departure_slot"])

    group_counter = 1
    for (origin, destination, slot), bucket in grouped_buckets:
        student_ids = bucket["student_id"].tolist()
        formed_chunks, left_over = _partition_into_groups(student_ids, max_group_size=max_group_size)

        for chunk in formed_chunks:
            grp_id = f"GRP_{group_counter:04d}"
            group_counter += 1
            groups.append(
                PassengerGroup(
                    group_id=grp_id,
                    origin_gate=str(origin),
                    destination_hub=str(destination),
                    departure_slot=str(slot),
                    member_student_ids=chunk,
                    group_size=len(chunk),
                )
            )
            assigned_students.update(chunk)

        unmatched_ids.extend(left_over)
        assigned_students.update(left_over)

    matched_count = sum(g.group_size for g in groups)
    unmatched_count = len(unmatched_ids)

    return GroupingResult(
        groups=groups,
        unmatched_student_ids=unmatched_ids,
        total_students=total_students,
        eligible_students=eligible_count,
        ineligible_students=ineligible_count,
        matched_students=matched_count,
        unmatched_students=unmatched_count,
        groups_formed=len(groups),
    )


if __name__ == "__main__":
    from src.synthetic_simulation import run_simulation
    df, _, _ = run_simulation(num_students=150, seed=42)
    result = group_passengers(df)
    print(result.summary())
    if result.groups:
        print(f"\nSample Group (1 of {len(result.groups)}):")
        sample = result.groups[0]
        print(f"  ID: {sample.group_id}")
        print(f"  Route: {sample.origin_gate} -> {sample.destination_hub}")
        print(f"  Slot: {sample.departure_slot}")
        print(f"  Members: {sample.member_student_ids}")
