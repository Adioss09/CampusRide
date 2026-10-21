"""
CampusRide Mamdani Fuzzy Inference Engine

Intelligent Systems foundation for ranking campus pickup-zone priority.
Evaluates:
- Estimated student outflow (Low, Medium, High)
- Time until dismissal (Imminent, Approaching, Far)
- Driver availability (Low, Medium, High) [OPTIONAL - never fabricated]

Features:
- Pure Python & NumPy implementation of Mamdani min-max inference
- Centroid defuzzification
- Full explainability & rule firing tracing
- Explicit handling of missing driver inputs (switches to 2-input rule base)
- Clear governance disclaimer that this ranks pickup urgency, not predicts demand
"""

from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple, Any
import logging
import numpy as np

logger = logging.getLogger(__name__)


def trimf(x: np.ndarray, params: Tuple[float, float, float]) -> np.ndarray:
    """Triangular membership function: [a, b, c]."""
    a, b, c = params
    x = np.asarray(x, dtype=float)
    y = np.zeros_like(x)
    if a != b:
        idx1 = (a < x) & (x <= b)
        y[idx1] = (x[idx1] - a) / (b - a)
    if b != c:
        idx2 = (b < x) & (x < c)
        y[idx2] = (c - x[idx2]) / (c - b)
    y[x == b] = 1.0
    return np.clip(y, 0.0, 1.0)


def trapmf(x: np.ndarray, params: Tuple[float, float, float, float]) -> np.ndarray:
    """Trapezoidal membership function: [a, b, c, d]."""
    a, b, c, d = params
    x = np.asarray(x, dtype=float)
    y = np.zeros_like(x)
    if a != b:
        idx1 = (a < x) & (x < b)
        y[idx1] = (x[idx1] - a) / (b - a)
    idx2 = (b <= x) & (x <= c)
    y[idx2] = 1.0
    if c != d:
        idx3 = (c < x) & (x < d)
        y[idx3] = (d - x[idx3]) / (d - c)
    return np.clip(y, 0.0, 1.0)


@dataclass
class FiredRule:
    rule_id: int
    rule_text: str
    antecedent_membership: float
    consequent_term: str


@dataclass
class FuzzyInferenceResult:
    crisp_score: float  # [0.0, 1.0]
    priority_label: str  # 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    driver_input_status: str  # 'SUPPLIED' or 'MISSING_OMITTED'
    fired_rules: List[FiredRule] = field(default_factory=list)
    explanation: str = ""
    disclaimer: str = (
        "Zone priority score ranks relative pickup-zone dispatch urgency "
        "based on supplied dismissal and supply parameters. It does not predict "
        "individual passenger intent or actual ride bookings."
    )


class MamdaniFuzzyEngine:
    """
    Mamdani Fuzzy Inference System for Pickup Zone Priority Ranking.
    """

    def __init__(self, resolution: int = 200):
        # Output universe for Zone Priority: 0.0 to 1.0
        self.resolution = resolution
        self.priority_universe = np.linspace(0.0, 1.0, resolution)

        # Output Fuzzy Sets: LOW, MEDIUM, HIGH, CRITICAL
        self.priority_terms = {
            "LOW": trapmf(self.priority_universe, (0.0, 0.0, 0.15, 0.35)),
            "MEDIUM": trimf(self.priority_universe, (0.25, 0.50, 0.75)),
            "HIGH": trimf(self.priority_universe, (0.60, 0.78, 0.92)),
            "CRITICAL": trapmf(self.priority_universe, (0.80, 0.90, 1.0, 1.0)),
        }

        # Outflow Universe: 0 to 300 students
        self.outflow_params = {
            "LOW": (0.0, 0.0, 30.0, 80.0),       # trapmf
            "MEDIUM": (50.0, 110.0, 170.0),       # trimf
            "HIGH": (130.0, 200.0, 300.0, 300.0), # trapmf
        }

        # Time Until Dismissal Universe: 0 to 60 minutes
        self.time_params = {
            "IMMINENT": (0.0, 0.0, 4.0, 10.0),     # trapmf
            "APPROACHING": (6.0, 14.0, 24.0),       # trimf
            "FAR": (18.0, 32.0, 60.0, 60.0),        # trapmf
        }

        # Driver Availability Universe: 0 to 30 drivers
        self.driver_params = {
            "LOW": (0.0, 0.0, 2.0, 6.0),           # trapmf
            "MEDIUM": (4.0, 10.0, 18.0),           # trimf
            "HIGH": (14.0, 22.0, 30.0, 30.0),      # trapmf
        }

    def evaluate(
        self,
        estimated_outflow: float,
        time_until_dismissal: float,
        driver_availability: Optional[float] = None,
    ) -> FuzzyInferenceResult:
        """
        Evaluate Mamdani FIS:
        - estimated_outflow: float (students expected to exit)
        - time_until_dismissal: float (minutes until class dismisses)
        - driver_availability: Optional[float] (active drivers nearby; None if unobserved)
        """
        # Input validation
        if estimated_outflow is None:
            raise ValueError("estimated_outflow is a required input and cannot be None.")
        if time_until_dismissal is None:
            raise ValueError("time_until_dismissal is a required input and cannot be None.")

        try:
            outflow = float(estimated_outflow)
        except (ValueError, TypeError) as e:
            raise ValueError(f"estimated_outflow must be numeric, got {estimated_outflow!r}") from e

        try:
            dismissal_mins = float(time_until_dismissal)
        except (ValueError, TypeError) as e:
            raise ValueError(f"time_until_dismissal must be numeric, got {time_until_dismissal!r}") from e

        if outflow < 0.0:
            raise ValueError(f"estimated_outflow must be non-negative, got {outflow}")
        if dismissal_mins < 0.0:
            raise ValueError(f"time_until_dismissal must be non-negative, got {dismissal_mins}")

        # 1. Fuzzify Outflow
        mu_outflow = {
            "LOW": float(trapmf(np.array([outflow]), self.outflow_params["LOW"])[0]),
            "MEDIUM": float(trimf(np.array([outflow]), self.outflow_params["MEDIUM"])[0]),
            "HIGH": float(trapmf(np.array([outflow]), self.outflow_params["HIGH"])[0]),
        }

        # 2. Fuzzify Time
        mu_time = {
            "IMMINENT": float(trapmf(np.array([dismissal_mins]), self.time_params["IMMINENT"])[0]),
            "APPROACHING": float(trimf(np.array([dismissal_mins]), self.time_params["APPROACHING"])[0]),
            "FAR": float(trapmf(np.array([dismissal_mins]), self.time_params["FAR"])[0]),
        }

        # 3. Handle Driver Availability
        has_drivers = driver_availability is not None
        mu_driver = {}
        if has_drivers:
            try:
                drivers = float(driver_availability)
            except (ValueError, TypeError) as e:
                raise ValueError(f"driver_availability must be numeric or None, got {driver_availability!r}") from e
            if drivers < 0.0:
                raise ValueError(f"driver_availability must be non-negative, got {drivers}")

            mu_driver = {
                "LOW": float(trapmf(np.array([drivers]), self.driver_params["LOW"])[0]),
                "MEDIUM": float(trimf(np.array([drivers]), self.driver_params["MEDIUM"])[0]),
                "HIGH": float(trapmf(np.array([drivers]), self.driver_params["HIGH"])[0]),
            }

        # 4. Evaluate Rules
        if has_drivers:
            rules, active_rules = self._evaluate_3_input_rules(mu_outflow, mu_time, mu_driver)
            driver_status = "SUPPLIED"
        else:
            rules, active_rules = self._evaluate_2_input_rules(mu_outflow, mu_time)
            driver_status = "MISSING_OMITTED"

        # 5. Aggregate Implicated Output Fuzzy Sets (Mamdani Max-Min)
        aggregated = np.zeros_like(self.priority_universe)
        for term, strength in rules.items():
            if strength > 0.0:
                # Implication: min(strength, term_membership)
                implication = np.minimum(strength, self.priority_terms[term])
                # Aggregation: max(aggregated, implication)
                aggregated = np.maximum(aggregated, implication)

        # 6. Centroid Defuzzification
        crisp_score = self._defuzzify_centroid(aggregated)

        # 7. Map to categorical priority label
        priority_label = self._score_to_label(crisp_score)

        # 8. Generate explanation
        explanation = self._build_explanation(
            outflow=outflow,
            dismissal_mins=dismissal_mins,
            driver_avail=driver_availability,
            crisp_score=crisp_score,
            priority_label=priority_label,
            active_rules=active_rules,
            driver_status=driver_status,
        )

        return FuzzyInferenceResult(
            crisp_score=round(crisp_score, 4),
            priority_label=priority_label,
            driver_input_status=driver_status,
            fired_rules=active_rules,
            explanation=explanation,
        )

    def _evaluate_2_input_rules(
        self,
        mu_outflow: Dict[str, float],
        mu_time: Dict[str, float],
    ) -> Tuple[Dict[str, float], List[FiredRule]]:
        """
        2-input Mamdani rule base when driver availability is missing.
        Rules:
        R1: IF Outflow is HIGH and Time is IMMINENT THEN Priority is CRITICAL
        R2: IF Outflow is HIGH and Time is APPROACHING THEN Priority is HIGH
        R3: IF Outflow is HIGH and Time is FAR THEN Priority is MEDIUM
        R4: IF Outflow is MEDIUM and Time is IMMINENT THEN Priority is HIGH
        R5: IF Outflow is MEDIUM and Time is APPROACHING THEN Priority is MEDIUM
        R6: IF Outflow is MEDIUM and Time is FAR THEN Priority is LOW
        R7: IF Outflow is LOW and Time is IMMINENT THEN Priority is MEDIUM
        R8: IF Outflow is LOW and Time is APPROACHING THEN Priority is LOW
        R9: IF Outflow is LOW and Time is FAR THEN Priority is LOW
        """
        rule_defs = [
            (1, "HIGH", "IMMINENT", "CRITICAL"),
            (2, "HIGH", "APPROACHING", "HIGH"),
            (3, "HIGH", "FAR", "MEDIUM"),
            (4, "MEDIUM", "IMMINENT", "HIGH"),
            (5, "MEDIUM", "APPROACHING", "MEDIUM"),
            (6, "MEDIUM", "FAR", "LOW"),
            (7, "LOW", "IMMINENT", "MEDIUM"),
            (8, "LOW", "APPROACHING", "LOW"),
            (9, "LOW", "FAR", "LOW"),
        ]

        term_strengths = {"LOW": 0.0, "MEDIUM": 0.0, "HIGH": 0.0, "CRITICAL": 0.0}
        active_rules: List[FiredRule] = []

        for r_id, o_term, t_term, cons_term in rule_defs:
            firing = min(mu_outflow[o_term], mu_time[t_term])
            if firing > 0.0:
                term_strengths[cons_term] = max(term_strengths[cons_term], firing)
                active_rules.append(
                    FiredRule(
                        rule_id=r_id,
                        rule_text=f"IF Outflow is {o_term} AND Time is {t_term} THEN Priority is {cons_term}",
                        antecedent_membership=round(firing, 4),
                        consequent_term=cons_term,
                    )
                )

        return term_strengths, active_rules

    def _evaluate_3_input_rules(
        self,
        mu_outflow: Dict[str, float],
        mu_time: Dict[str, float],
        mu_driver: Dict[str, float],
    ) -> Tuple[Dict[str, float], List[FiredRule]]:
        """
        3-input Mamdani rule base when driver availability is supplied.
        Takes supply deficit / surplus into account.
        """
        rule_defs = [
            # High Outflow + Imminent Dismissal
            (101, "HIGH", "IMMINENT", "LOW", "CRITICAL"),
            (102, "HIGH", "IMMINENT", "MEDIUM", "CRITICAL"),
            (103, "HIGH", "IMMINENT", "HIGH", "HIGH"),

            # High Outflow + Approaching Dismissal
            (104, "HIGH", "APPROACHING", "LOW", "CRITICAL"),
            (105, "HIGH", "APPROACHING", "MEDIUM", "HIGH"),
            (106, "HIGH", "APPROACHING", "HIGH", "MEDIUM"),

            # High Outflow + Far Dismissal
            (107, "HIGH", "FAR", "LOW", "HIGH"),
            (108, "HIGH", "FAR", "MEDIUM", "MEDIUM"),
            (109, "HIGH", "FAR", "HIGH", "LOW"),

            # Medium Outflow + Imminent Dismissal
            (110, "MEDIUM", "IMMINENT", "LOW", "CRITICAL"),
            (111, "MEDIUM", "IMMINENT", "MEDIUM", "HIGH"),
            (112, "MEDIUM", "IMMINENT", "HIGH", "MEDIUM"),

            # Medium Outflow + Approaching Dismissal
            (113, "MEDIUM", "APPROACHING", "LOW", "HIGH"),
            (114, "MEDIUM", "APPROACHING", "MEDIUM", "MEDIUM"),
            (115, "MEDIUM", "APPROACHING", "HIGH", "LOW"),

            # Medium Outflow + Far Dismissal
            (116, "MEDIUM", "FAR", "LOW", "MEDIUM"),
            (117, "MEDIUM", "FAR", "MEDIUM", "LOW"),
            (118, "MEDIUM", "FAR", "HIGH", "LOW"),

            # Low Outflow + Imminent Dismissal
            (119, "LOW", "IMMINENT", "LOW", "HIGH"),
            (120, "LOW", "IMMINENT", "MEDIUM", "MEDIUM"),
            (121, "LOW", "IMMINENT", "HIGH", "LOW"),

            # Low Outflow + Approaching Dismissal
            (122, "LOW", "APPROACHING", "LOW", "MEDIUM"),
            (123, "LOW", "APPROACHING", "MEDIUM", "LOW"),
            (124, "LOW", "APPROACHING", "HIGH", "LOW"),

            # Low Outflow + Far Dismissal
            (125, "LOW", "FAR", "LOW", "LOW"),
            (126, "LOW", "FAR", "MEDIUM", "LOW"),
            (127, "LOW", "FAR", "HIGH", "LOW"),
        ]

        term_strengths = {"LOW": 0.0, "MEDIUM": 0.0, "HIGH": 0.0, "CRITICAL": 0.0}
        active_rules: List[FiredRule] = []

        for r_id, o_term, t_term, d_term, cons_term in rule_defs:
            firing = min(mu_outflow[o_term], mu_time[t_term], mu_driver[d_term])
            if firing > 0.0:
                term_strengths[cons_term] = max(term_strengths[cons_term], firing)
                active_rules.append(
                    FiredRule(
                        rule_id=r_id,
                        rule_text=(
                            f"IF Outflow is {o_term} AND Time is {t_term} "
                            f"AND DriverAvail is {d_term} THEN Priority is {cons_term}"
                        ),
                        antecedent_membership=round(firing, 4),
                        consequent_term=cons_term,
                    )
                )

        return term_strengths, active_rules

    def _defuzzify_centroid(self, aggregated_mf: np.ndarray) -> float:
        """Centroid (Center of Gravity) defuzzification."""
        sum_mf = np.sum(aggregated_mf)
        if sum_mf == 0.0:
            return 0.5  # Neutral fallback when no rules fire
        return float(np.sum(self.priority_universe * aggregated_mf) / sum_mf)

    def _score_to_label(self, score: float) -> str:
        if score >= 0.80:
            return "CRITICAL"
        elif score >= 0.60:
            return "HIGH"
        elif score >= 0.35:
            return "MEDIUM"
        else:
            return "LOW"

    def _build_explanation(
        self,
        outflow: float,
        dismissal_mins: float,
        driver_avail: Optional[float],
        crisp_score: float,
        priority_label: str,
        active_rules: List[FiredRule],
        driver_status: str,
    ) -> str:
        driver_str = (
            f"{driver_avail:.1f} drivers (supplied)"
            if driver_avail is not None
            else "unobserved/omitted (2-input inference mode activated)"
        )
        lines = [
            f"Pickup Zone Priority Assessment: {priority_label} (Score: {crisp_score:.2f})",
            f"- Estimated Outflow: {outflow:.0f} students",
            f"- Time to Dismissal: {dismissal_mins:.1f} mins",
            f"- Driver Supply: {driver_str}",
            f"- Active Fired Rules ({len(active_rules)}):",
        ]
        for r in sorted(active_rules, key=lambda x: x.antecedent_membership, reverse=True):
            lines.append(f"  * [Rule {r.rule_id} | strength={r.antecedent_membership:.2f}] {r.rule_text}")

        if driver_status == "MISSING_OMITTED":
            lines.append(
                "- Note: Driver supply was not provided, so inference was performed "
                "purely using dismissal volume and temporal urgency."
            )

        return "\n".join(lines)
