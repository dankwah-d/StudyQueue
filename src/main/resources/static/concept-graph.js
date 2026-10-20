function renderConceptGraph(container, concepts) {
  container.replaceChildren();
  if (concepts.length === 0) return "No concepts available yet.";

  const ordered = [...concepts].sort((a, b) => a.conceptName.localeCompare(b.conceptName));
  const nodes = new Map(ordered.map(concept => [concept.conceptId,
    { concept, parents: (concept.prerequisites || []).map(item => item.conceptId), children: [], depth: 0 }]));
  for (const node of nodes.values()) {
    for (const parent of node.parents) {
      if (!nodes.has(parent)) return "Graph unavailable: a prerequisite concept is missing. See the statistics table for details.";
      nodes.get(parent).children.push(node);
    }
    node.remaining = node.parents.length;
  }
  const queue = [...nodes.values()].filter(node => node.remaining === 0);
  for (let index = 0; index < queue.length; index++) {
    const node = queue[index];
    for (const child of node.children) {
      child.depth = Math.max(child.depth, node.depth + 1);
      if (--child.remaining === 0) queue.push(child);
    }
  }
  if (queue.length !== nodes.size) return "Graph unavailable: prerequisite relationships contain a cycle. See the statistics table for details.";

  const layers = [];
  for (const node of nodes.values()) (layers[node.depth] ||= []).push(node);
  const width = Math.max(520, ...layers.map(layer => layer.length * 244 + 32));
  const height = layers.length * 148;
  const svgElement = (tag, attributes = {}, text) => {
    const element = document.createElementNS("http://www.w3.org/2000/svg", tag);
    for (const [key, value] of Object.entries(attributes)) element.setAttribute(key, value);
    if (text !== undefined) element.textContent = text;
    return element;
  };
  const states = { NOT_STARTED: "Not started", PRACTICING: "Practicing", MASTERED: "Mastered" };
  const svg = svgElement("svg", { viewBox: `0 0 ${width} ${height}`, width,
    role: "img", "aria-labelledby": "concept-graph-title concept-graph-description" });
  svg.append(svgElement("title", { id: "concept-graph-title" }, "Concept prerequisites and progress"));
  svg.append(svgElement("desc", { id: "concept-graph-description" },
    ordered.map(concept => `${concept.conceptName}: ${states[concept.state] || "Unknown"}, ${concept.eligible ? "available" : "locked"}. Prerequisites: ${(concept.prerequisites || []).map(item => item.conceptName).join(", ") || "none"}.`).join(" ")));
  const definitions = svgElement("defs");
  const marker = svgElement("marker", { id: "prerequisite-arrow", viewBox: "0 0 10 10",
    refX: 10, refY: 5, markerWidth: 7, markerHeight: 7, orient: "auto-start-reverse" });
  marker.append(svgElement("path", { d: "M 0 0 L 10 5 L 0 10 z", fill: "#8b9ab1" }));
  definitions.append(marker);
  svg.append(definitions);

  for (const layer of layers) {
    layer.forEach((node, index) => {
      node.x = (width - layer.length * 244) / 2 + index * 244 + 12;
      node.y = node.depth * 148 + 16;
    });
  }
  for (const node of nodes.values()) {
    for (const parentId of node.parents) {
      const parent = nodes.get(parentId);
      const startX = parent.x + 110;
      const startY = parent.y + 100;
      const endX = node.x + 110;
      const endY = node.y;
      svg.append(svgElement("path", { class: "graph-edge",
        d: `M ${startX} ${startY} C ${startX} ${startY + 24}, ${endX} ${endY - 24}, ${endX} ${endY}`,
        fill: "none", stroke: "#8b9ab1", "stroke-width": 1.6, "marker-end": "url(#prerequisite-arrow)" }));
    }
  }
  const fills = { NOT_STARTED: "#fffefa", PRACTICING: "#eaf3df", MASTERED: "#fcf0ca" };
  for (const node of nodes.values()) {
    const concept = node.concept;
    const group = svgElement("g", { class: "graph-node", "data-concept-id": concept.conceptId });
    group.append(svgElement("rect", { x: node.x, y: node.y, width: 220, height: 100, rx: 12,
      fill: fills[concept.state] || "#fffefa", stroke: concept.eligible ? "#438263" : "#a5b39b",
      "stroke-width": 1.5, "stroke-dasharray": concept.eligible ? "none" : "6 4" }));
    const text = (offset, value, weight = "normal") => svgElement("text", {
      x: node.x + 110, y: node.y + offset, "text-anchor": "middle",
      "font-size": 14, "font-weight": weight, fill: "#172033"
    }, value);
    group.append(svgElement("title", {}, concept.conceptName));
    group.append(text(28, concept.conceptName.length > 24 ? `${concept.conceptName.slice(0, 23)}…` : concept.conceptName, "bold"));
    group.append(text(53, states[concept.state] || "Unknown"));
    group.append(text(78, concept.eligible ? "Available" : "Locked"));
    svg.append(group);
  }
  container.append(svg);
  return "";
}
