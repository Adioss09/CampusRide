---
name: Campus Mobility System
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#3d4947'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#6d7a77'
  outline-variant: '#bcc9c6'
  surface-tint: '#006a61'
  primary: '#00685f'
  on-primary: '#ffffff'
  primary-container: '#008378'
  on-primary-container: '#f4fffc'
  inverse-primary: '#6bd8cb'
  secondary: '#006a63'
  on-secondary: '#ffffff'
  secondary-container: '#99efe5'
  on-secondary-container: '#006f67'
  tertiary: '#515c71'
  on-tertiary: '#ffffff'
  tertiary-container: '#6a758a'
  on-tertiary-container: '#fefcff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#89f5e7'
  primary-fixed-dim: '#6bd8cb'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#005049'
  secondary-fixed: '#9cf2e8'
  secondary-fixed-dim: '#80d5cb'
  on-secondary-fixed: '#00201d'
  on-secondary-fixed-variant: '#00504a'
  tertiary-fixed: '#d8e3fb'
  tertiary-fixed-dim: '#bcc7de'
  on-tertiary-fixed: '#111c2d'
  on-tertiary-fixed-variant: '#3c475a'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1rem
  margin-lg: 1.5rem
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.25rem
  space-xl: 1.5rem
  space-2xl: 2rem
---

## Brand & Style

This design system establishes a focused, high-utility native Android interface tailored for real-time university transit networks, micro-mobility monitoring, and student ride-sharing logistics. The visual aesthetic fuses Material Design 3 guidelines with operational clarity: high-contrast typography, strict spatial hierarchies, and unmistakable semantic signaling.

The personality balances civic dependability with academic agility. Rather than treating transit as purely transactional, the UI communicates spatial awareness, punctuality, and route intelligence under fluctuating campus pressures (class transitions, game days, inclement weather). Visual elements feature controlled surfaces, low-glare white panels against an off-white ambient canvas, and tactile feedback patterns tuned for one-handed thumb interaction during transit.

## Colors

The palette leverages an off-white ambient backdrop with crisp layered surfaces to maintain high outdoors legibility while avoiding eye strain. Demand levels are strictly codified using standard mobility semantics to give operators and riders instant situational comprehension.

### Core Canvas & Typography
- **Background (Canvas)**: `#F8FAFC`
- **Surface (Cards, Bottom Sheets, Top Bar)**: `#FFFFFF`
- **Surface Variant (Subtle Containers, Inactive Chips)**: `#F1F5F9`
- **Surface Border / Divider**: `#E2E8F0`
- **Text Primary (Headlines, Key Metrics)**: `#0F172A`
- **Text Secondary (Subtitles, Timestamps, Labels)**: `#1E293B`
- **Text Muted (Placeholders, Disabled States)**: `#64748B`

### Brand Accents
- **Primary Accent**: `#0D9488` (Teal 600) for standard CTAs, active route lines, and selected states.
- **Primary Pressed / Dark Accent**: `#0F766E` (Teal 700) for active/pressed button states and primary header accents.
- **Primary Container**: `#CCFBF1` (Teal 100) paired with `#115E59` (Teal 800) for active selection surfaces.

### Semantic Demand System
- **Low Demand**: `#0D9488` (Fill: `#F0FDFA`, Border: `#99F6E4`) — Standard campus conditions, minimal wait times.
- **Medium Demand**: `#F59E0B` (Fill: `#FFFBEB`, Border: `#FDE68A`) — Peak class transit intervals, moderate fleet usage.
- **High Demand**: `#EA580C` (Fill: `#FFF7ED`, Border: `#FED7AA`) — Limited seat capacity, major route delays.
- **Critical Demand**: `#DC2626` (Fill: `#FEF2F2`, Border: `#FECACA`) — Route capacity exceeded, emergency weather routing, shuttle saturation.

## Typography

Typography prioritizes rapid scannability on small screens. Using Inter, letter spacing is slightly condensed for numbers and route codes while retaining generous optical balance for body metadata and arrival estimates.

- **Numerics & Route Tags**: Rendered with `title-lg` or `label-md` using tabular lining numbers where applicable for ETA countdowns and seat availability counters.
- **Hierarchy Mapping**:
  - `display-lg`: Hero statistics and status overview counters.
  - `headline-lg`: Top app bar titles and bottom sheet stage headers.
  - `title-md`: Transit stop headings and driver card names.
  - `body-md`: Primary narrative labels, route details, and operational notices.
  - `label-sm`: Micro-caps demand tags, live indicator badges, and vehicle capacity indicators.

## Layout & Spacing

The layout model adopts a strict 4-column fluid mobile grid spanning a default 360dp to 412dp viewport width, with fixed 16dp (`1rem`) lateral margins and 16dp gutters. 

- **Vertical Grid**: Arranged on a 4dp baseline rhythm. All vertical spacings between list items, chips, and cards conform strictly to multiples of 4dp (typically 8dp, 12dp, 16dp, or 24dp).
- **Safe Area Insets**:
  - Top status bar: 24dp to 48dp dynamic edge-to-edge inset with clear scrim or translucent backdrop.
  - Bottom navigation bar: 80dp baseline height with safe area inset padding (`windowInsets.navigationBars`).
- **Responsive Adaptations**:
  - **Compact (<600dp)**: Single-column scroll view; persistent map underlay with draggable bottom sheet (collapsed: 120dp, half-expanded: 50vh, full: 90vh).
  - **Medium (600dp - 839dp / Tablets & Foldables)**: Split-pane layout; 380dp persistent left rail panel with full-bleed interactive map canvas on the right.

## Elevation & Depth

Visual hierarchy uses Material 3 surface tinting and structural border containment rather than heavy drop shadows, preventing visual noise in bright daylight conditions.

- **Level 0 (Base Canvas - `#F8FAFC`)**: Map underlays and background workspace. Zero shadow, flat foundation.
- **Level 1 (Card & Content Surface - `#FFFFFF`)**:
  - Border: 1px solid `#E2E8F0`
  - Shadow: `0px 1px 3px rgba(15, 23, 42, 0.05), 0px 1px 2px rgba(15, 23, 42, 0.03)`
  - Use case: Standard transit cards, route schedule items, vehicle capacity tiles.
- **Level 2 (Interactive Floating Elements & Persistent Top Bars - `#FFFFFF`)**:
  - Border: 1px solid `#CBD5E1`
  - Shadow: `0px 4px 6px -1px rgba(15, 23, 42, 0.07), 0px 2px 4px -2px rgba(15, 23, 42, 0.05)`
  - Use case: Filter chip groups over maps, floating action buttons (FAB), location re-center buttons.
- **Level 3 (Modal Bottom Sheets & Dialogs - `#FFFFFF`)**:
  - Border: Top-only 1px solid `#E2E8F0` (on sheets) or full 1px border (on dialogs).
  - Shadow: `0px -8px 24px -4px rgba(15, 23, 42, 0.10), 0px -2px 6px -1px rgba(15, 23, 42, 0.04)`
  - Scrim: `rgba(15, 23, 42, 0.45)` for modal backdrops.

## Shapes

The design system implements a native Android M3 corner geometry calibrated between 8dp and 28dp, establishing a tactile, approachable profile:

- **Small Containers (Tags, Badges, Micro-chips)**: 8dp corner radius (`rounded-sm`).
- **Standard Surfaces (Cards, List Groups, Text Input Boxes)**: 12dp to 16dp corner radius (`rounded-md` to `rounded-lg`).
- **Major Surfaces (Bottom Sheets, Dialogs, Banner Announcements)**: Top corners 24dp to 28dp (`rounded-xl`).
- **Interactive Controls (Action Buttons, Floating Chips, Search Bars)**: Fully rounded pill shape (`9999px` / `rounded-full`).

## Components

### Buttons
- **Primary CTA**: Height 48dp, fully pill-shaped (`rounded-full`), background `#0D9488`, content `#FFFFFF`, typography `label-lg`. Pressed state transitions to `#0F766E`.
- **Tonal Button**: Height 48dp, pill-shaped, background `#CCFBF1`, content `#0F766E`, font `label-lg`.
- **Outlined / Secondary**: Height 48dp, pill-shaped, border 1.5px solid `#E2E8F0`, background transparent, text `#0F172A`.

### Chips
- **Filter Chips**: Height 32dp, pill-shaped. Unselected: background `#FFFFFF`, border 1px solid `#E2E8F0`, text `#1E293B`. Selected: background `#CCFBF1`, border 1px solid `#0D9488`, text `#0F766E` with leading checkmark icon.
- **Demand Indicator Chips**: Height 24dp, radius 8dp, horizontal padding 8dp. Uses semantic palette:
  - *Low*: Background `#F0FDFA`, text `#0D9488`, border 1px solid `#99F6E4`.
  - *Medium*: Background `#FFFBEB`, text `#D97706`, border 1px solid `#FDE68A`.
  - *High*: Background `#FFF7ED`, text `#EA580C`, border 1px solid `#FED7AA`.
  - *Critical*: Background `#FEF2F2`, text `#DC2626`, border 1px solid `#FECACA`.

### Cards & Surface Lists
- **Ride Option & Vehicle Card**: Background `#FFFFFF`, radius 16dp, border 1px solid `#E2E8F0`, padding 16dp. Displays route title (`title-md`), live ETA indicator (`label-lg`), demand badge, and capacity icon row.
- **Divided List Groups**: Enclosed in a 16dp rounded card with internal divider lines (`#F1F5F9`, 1px) inset by 16dp on the start margin.

### Bottom Sheet
- **Component Geometry**: Top-left and top-right radii 28dp, background `#FFFFFF`, border-top 1px solid `#E2E8F0`.
- **Drag Handle**: Centered at top, width 32dp, height 4dp, radius 2dp, background `#CBD5E1`, vertical margin 12dp.
- **Scroll Behavior**: Anchored peek height at 140dp showing immediate arrival and active ride stage; expands smoothly to full sheet detailing pickup waypoints and rider matches.

### Text Inputs & Search Fields
- **Search Bar (M3 Docked)**: Height 52dp, pill-shaped, background `#FFFFFF`, border 1px solid `#CBD5E1`, elevation Level 2. Leading icon `#0D9488`, placeholder text `#64748B`.
- **Form Fields**: Height 56dp, radius 12dp, background `#F8FAFC`, border 1.5px solid `#E2E8F0`. Focused state border turns `#0D9488` with 2px stroke width.

### Navigation Bar
- **M3 Bottom Navigation**: Height 80dp, background `#FFFFFF`, border-top 1px solid `#E2E8F0`.
- **Active Indicator**: Horizontal pill (64dp × 32dp) in `#CCFBF1` behind active icon (`#0F766E`).
- **Labels**: 12dp medium weight typography; inactive items rendered in `#64748B`.