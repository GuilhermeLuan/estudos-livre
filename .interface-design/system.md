# Estuda Livre Interface System

## Direction and feel

Estuda Livre should feel like a calm, self-hosted study workspace: focused, trustworthy, and slightly tactile. The visual language borrows from notebooks, binders, bookmarks, and warm paper without becoming decorative or nostalgic. `prototype.html` is the primary visual reference for new product screens.

The product signature is the bookmark silhouette. Repeat it deliberately in the brand mark, active navigation, and progress-related moments so the interface remains recognizable without relying on illustration.

## Color tokens

| Token | Value | Use |
| --- | --- | --- |
| `paper` | `#f3f0e7` | Main canvas |
| `raised` | `#faf8f2` | Cards and raised controls |
| `inset` | `#ebe7dc` | Recessed and secondary surfaces |
| `ink` | `#202b26` | Primary text |
| `ink-soft` | `#4f5d56` | Supporting text |
| `ink-faint` | `#77827c` | Metadata and placeholders |
| `binder` | `#285c48` | Primary actions and healthy states |
| `binder-soft` | `#dce8e0` | Selected and healthy backgrounds |
| `marker` | `#d5a932` | Progress and attention accents |
| `marker-soft` | `#f4e8bd` | Highlight backgrounds |
| `late` | `#ad5b43` | Errors and unavailable states |
| `late-soft` | `#f3ddd6` | Error backgrounds |

Do not introduce gradients or external font/CDN dependencies. Skeleton loading may use a restrained animated tonal band because it communicates state rather than decoration.

## Depth, geometry, and spacing

- Use a 4px spacing base. Prefer increments of 8px for layout and 4px for compact internal alignment.
- Use subtle shadows only: a light shadow for controls and a medium, diffuse shadow for focal cards. Do not mix shadows with strong borders on the same surface.
- Controls use an 8px radius, ordinary cards 14px, and focal panels 18px.
- The navigation rail shares the paper canvas and is separated by a quiet border; it should not look like a floating card.
- Minimum interactive height is 44px.

## Hierarchy and typography

- Display and editorial headings: `Iowan Old Style`, `Palatino Linotype`, `Book Antiqua`, `Georgia`, serif.
- UI and body text: `Avenir Next`, `Gill Sans`, `Trebuchet MS`, sans-serif.
- Suggested scale: 10px captions, 11px metadata, 14px body, 16px supporting emphasis, 28px section heading, and 34–52px display heading.
- Establish hierarchy with size, spacing, and color before adding weight. Reserve bold text for actions, labels, and the most important status.
- The standard focal sequence is page headline, focal status card, then detailed rows or secondary actions.

## Layout and responsive behavior

- Desktop navigation rail: 224px. Main content: centered, maximum width 920px.
- At 760px and below, replace the rail with a compact top brand header and allow content to use the full viewport width.
- At 420px and below, stack row content and actions when horizontal space would reduce readability or touch targets.
- Preserve generous whitespace around the main headline and focal card; dense data belongs inside structured rows, not across the whole page.

## Reusable component patterns

### Brand mark

- Bookmark silhouette: approximately 26 × 32px.
- Pair with a compact wordmark; do not enlarge it into a decorative hero graphic.

### Navigation item

- Minimum height 44px, padding 10px 12px, radius 8px.
- Active state uses `binder-soft`, stronger `ink`, and a bookmark/accent cue.
- Hover and focus must remain visible without moving surrounding content.

### Focal status card

- Radius 18px with the medium shadow.
- Header padding 24px on desktop and 20px on mobile.
- Status icon container: 48px square, radius 12px.
- Use one concise status sentence followed by actionable detail.

### Health row

- Minimum height 76px, padding 16px 24px; use 16px 20px on mobile.
- Align service identity, supporting detail, and status pill on desktop; stack only when needed.
- Status pills keep at least 36px of visual height and combine color with text or iconography.

### Primary button

- Minimum height 44px, padding 10px 16px, radius 8px.
- Use `binder` for the primary action and reserve `late` for destructive or recovery contexts.

### Current cycle stage

- Present the current stage immediately above the cycle details as a focal strip, using a 4-column desktop layout: bookmark index, subject copy, progress numbers, and the primary action.
- Use a 42 × 52px `marker-soft` bookmark for the stage position and a 4px progress track spanning the full strip.
- Keep the subject name editorial and prominent; target, studied time, and remaining time use compact tabular values so the next action stays dominant.
- At 760px, move progress numbers below the copy; at 420px, stack numbers and the action at full width.

### Study session desk

- Use a compact focal panel with `binder-soft` tint for an active timer and `marker-soft` tint for a paused timer. The state color communicates status; do not add decorative accent colors.
- The timer is the visual anchor: tabular numerals, strong `binder` color while active, and enough whitespace to be readable at a glance.
- Desktop layout uses a status mark, session identity, timer, and one transition action. Below 420px, preserve the identity pair and place the transition action on its own full-width row.
- The backend remains the time source. Local interpolation may animate an active timer, but every transition and reload must resynchronize with persisted state.

### Decision dialog

- Use native `<dialog>` with a `paper-raised` panel, 18px radius, medium shadow, dimmed/blurred backdrop, and a 200ms entrance animation that honors reduced-motion preferences.
- Header uses a 44–48px bookmark/status mark beside an editorial 27px title; body fields sit on the inset paper surface and actions remain in a separated footer.
- Put the reversible action first and the consequential primary action last. On narrow mobile widths, stack footer actions at full width without changing their semantic order.

## Interaction and state rules

- Loading uses a quiet skeleton/shimmer and honors `prefers-reduced-motion`.
- Healthy state uses `binder`; degraded or failed state uses `late` and exposes an immediate retry action.
- Keyboard focus must be clearly visible. Hover is supplementary and never the only affordance.
- Server-derived state must be real and refreshable; do not simulate health or completion state in the UI.
- Validate every product screen at desktop and mobile widths, including loading, success, failure, and empty states.
