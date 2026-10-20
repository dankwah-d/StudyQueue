# Concept graph

The practice page draws an SVG graph from `GET /api/stats/concepts`, using the same
response that populates the statistics table. No additional requests, dependencies,
or migrations are required. It refreshes after saved answers and statistics retries.

Arrows point from prerequisites to their dependent concepts. Concepts appear in
layers below all their prerequisites, with names ordered within each layer. Root
and disconnected concepts are included. Each node shows its learning state and
Available or Locked; locked nodes also have a dashed border. Color is supplemental.

The SVG includes an accessible title and a description listing concept states,
availability, and prerequisite names. Full names remain in SVG titles and the
statistics table when a node label is shortened. The graph scrolls horizontally on
narrow screens rather than shrinking its labels below readable size.

The view is read-only. If relationships contain a cycle or refer to missing
concepts, it displays an explanation instead of drawing a misleading graph. This
detects invalid graph data for presentation; it does not enforce acyclic database
relationships. The statistics table remains available. Failed requests clear the
graph and use the existing Retry statistics action.

Verification: `node --test tests/concept-stats.test.cjs` covers arrows, labels,
layers, disconnected roots, multiple prerequisites, state/availability refresh,
empty data, cycles, missing prerequisites, and failure/retry. Browser visual and
screen-reader checks remain outstanding because no browser connection is available.
