function shuffled(values, random = Math.random) {
  const result = [...values];
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}

// Refreshing eligibility never reorders pending IDs or silently starts another session.
class PracticeSession {
  constructor(random = Math.random, order = "random") {
    this.random = random;
    this.order = order;
    this.pending = [];
    this.seen = new Set();
    this.lastId = null;
    this.total = 0;
  }

  next(eligible) {
    const byId = new Map(eligible.map(question => [question.id, question]));
    this.pending = this.pending.filter(id => byId.has(id) && !this.seen.has(id));
    const queued = new Set(this.pending);
    const additions = [...byId.keys()].filter(id => !this.seen.has(id) && !queued.has(id));
    this.pending.push(...(this.order === "ordered"
      ? additions.sort((a, b) => a - b) : shuffled(additions, this.random)));
    this.total = this.seen.size + this.pending.length;
    if (!this.pending.length) return null;
    const id = this.pending.shift();
    this.seen.add(id);
    this.lastId = id;
    return byId.get(id);
  }
}

if (typeof module !== "undefined") module.exports = { PracticeSession, shuffled };
