const { test } = require('node:test');
const assert = require('node:assert/strict');
const { PracticeSession, shuffled } = require('../src/main/resources/static/practice-session.js');
const pool = (...ids) => ids.map(id => ({ id, options: { A: 'Yes', B: 'No' } }));

test('shuffle preserves values and input and can change their order', () => {
  const input = [1, 2, 3, 4];
  assert.deepEqual(shuffled(input, () => 0), [2, 3, 4, 1]);
  assert.deepEqual(input, [1, 2, 3, 4]);
  assert.deepEqual(shuffled([], () => 0), []);
});

test('session consumes a shuffled stable queue without duplicates before exhaustion', () => {
  const session = new PracticeSession(() => 0);
  const eligible = pool(1, 2, 3, 4);
  assert.equal(session.next(eligible).id, 2);
  // Refreshing server order and data must not reshuffle pending questions.
  assert.equal(session.next([...eligible].reverse()).id, 3);
  assert.equal(session.next(eligible).id, 4);
  assert.equal(session.next(eligible).id, 1);
  assert.equal(session.next(eligible), null);
  assert.equal(session.total, 4);
});

test('newly unlocked questions append without repeating seen questions', () => {
  const session = new PracticeSession(() => 0.99);
  assert.equal(session.next(pool(1, 2)).id, 1);
  assert.equal(session.next(pool(1, 2, 3)).id, 2);
  assert.equal(session.next(pool(1, 2, 3)).id, 3);
});

test('relocked questions are removed and can return after unlocking', () => {
  const session = new PracticeSession(() => 0.99);
  assert.equal(session.next(pool(1, 2, 3)).id, 1);
  assert.equal(session.next(pool(1, 3)).id, 3);
  assert.equal(session.next(pool(1, 2, 3)).id, 2);
  assert.equal(session.next([]), null);
  assert.equal(session.next(pool(9)).id, 9);
  assert.equal(session.next(pool(9)), null);
});

test('answer shuffling preserves stable IDs and variable choice counts', () => {
  for (const options of [{ Z: 'Only answer' }, { '10': 'Correct', '20': 'Wrong', other: 'Also wrong' }]) {
    const entries = shuffled(Object.entries(options), () => 0);
    assert.equal(entries.length, Object.keys(options).length);
    for (const [id, text] of entries) assert.equal(options[id], text);
  }
});


test('explicit order is deterministic and a fresh session can reshuffle', () => {
  const ordered = new PracticeSession(() => 0, 'ordered');
  assert.deepEqual([ordered.next(pool(3, 1, 2)).id, ordered.next(pool(2, 3, 1)).id,
    ordered.next(pool(1, 2, 3)).id], [1, 2, 3]);
  const first = new PracticeSession(() => 0);
  const second = new PracticeSession(() => 0.99);
  assert.notEqual(first.next(pool(1, 2, 3)).id, second.next(pool(1, 2, 3)).id);
});


test('recommended order adapts unseen questions and reconciles unlocks without repeats', () => {
  const session = new PracticeSession(() => 0, 'recommended');
  assert.equal(session.next(pool(3, 1, 2)).id, 3);
  // Ranking changes after an answer; newly unlocked 4 can take priority.
  assert.equal(session.next(pool(4, 2, 3, 1)).id, 4);
  // Question 2 relocks; seen 3 and 4 remain excluded.
  assert.equal(session.next(pool(3, 1, 4)).id, 1);
  assert.equal(session.next(pool(3, 1, 4)), null);
  assert.equal(session.total, 3);
  assert.equal(session.next(pool(2, 3, 1, 4)).id, 2);
  assert.equal(session.next(pool(2, 3, 1, 4)), null);
});


test('topological sessions preserve server order and insert newly unlocked unseen questions', () => {
  const session = new PracticeSession(() => 0, 'topological');
  assert.equal(session.next(pool(9, 3, 1)).id, 9);
  assert.equal(session.next(pool(9, 7, 3, 1)).id, 7);
  assert.equal(session.next(pool(9, 7, 1)).id, 1);
  assert.equal(session.next(pool(9, 7, 1)), null);
  assert.equal(session.next(pool(9, 7, 3, 1)).id, 3);
  assert.equal(session.next(pool(9, 7, 3, 1)), null);
});
