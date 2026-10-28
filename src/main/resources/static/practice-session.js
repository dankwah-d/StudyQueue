function shuffled(values, random = Math.random) {
  const result = [...values];
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}

// One page lifetime is a session. Refreshing eligibility never reorders pending IDs.
class PracticeSession {
  constructor(random = Math.random) {
    this.random = random;
    this.pending = [];
    this.seen = new Set();
    this.lastId = null;
  }

  next(eligible) {
    const byId = new Map(eligible.map(question => [question.id, question]));
    this.pending = this.pending.filter(id => byId.has(id) && !this.seen.has(id));
    const queued = new Set(this.pending);
    const additions = [...byId.keys()].filter(id => !this.seen.has(id) && !queued.has(id));
    this.pending.push(...shuffled(additions, this.random));
    if (!this.pending.length && byId.size) {
      this.seen.clear();
      this.pending = shuffled(byId.keys(), this.random);
      // Avoid an immediate repeat at a cycle boundary when alternatives exist.
      if (this.pending.length > 1 && this.pending[0] === this.lastId) {
        [this.pending[0], this.pending[1]] = [this.pending[1], this.pending[0]];
      }
    }
    if (!this.pending.length) return null;
    const id = this.pending.shift();
    this.seen.add(id);
    this.lastId = id;
    return byId.get(id);
  }
}

if (typeof module !== "undefined") module.exports = { PracticeSession, shuffled };
