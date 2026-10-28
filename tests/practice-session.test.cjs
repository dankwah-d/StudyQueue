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
  const secondCycle = Array.from({ length: 4 }, () => session.next(eligible).id);
  assert.equal(new Set(secondCycle).size, 4);
  assert.notEqual(secondCycle[0], 1);
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
  assert.equal(session.next(pool(9)).id, 9);
});

test('answer shuffling preserves stable IDs and variable choice counts', () => {
  for (const options of [{ Z: 'Only answer' }, { '10': 'Correct', '20': 'Wrong', other: 'Also wrong' }]) {
    const entries = shuffled(Object.entries(options), () => 0);
    assert.equal(entries.length, Object.keys(options).length);
    for (const [id, text] of entries) assert.equal(options[id], text);
  }
});
