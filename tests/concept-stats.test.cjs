const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const html = fs.readFileSync(path.join(__dirname, '../src/main/resources/static/index.html'), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
const graphScript = fs.readFileSync(path.join(__dirname, '../src/main/resources/static/concept-graph.js'), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));
const stats = (total = 3, correct = 2, accuracy = 66.7, state = 'PRACTICING', eligible = true, prerequisites = []) => [
  { conceptId: 1, conceptName: 'Processes', totalAttempts: total, correctAnswers: correct, accuracyPercent: accuracy, state, eligible, prerequisites }
];

function setup(statsResponses, questionResponses = [], answerResponses = []) {
  class Element {
    children = [];
    listeners = {};
    textContent = '';
    hidden = false;
    attributes = {};
    setAttribute(name, value) { this.attributes[name] = String(value); }
    removeAttribute(name) { delete this.attributes[name]; }
    focus() { this.focused = true; }
    append(...children) { this.children.push(...children); }
    replaceChildren(...children) { this.children = children; }
    addEventListener(event, callback) { this.listeners[event] = callback; }
    querySelectorAll() { return []; }
  }
  const elements = new Map();
  const get = selector => {
    if (!elements.has(selector)) elements.set(selector, new Element());
    return elements.get(selector);
  };
  const calls = [];
  const answerBodies = [];
  let selectedOption = 'A';
  const window = { location: { hash: '' }, listeners: {},
    addEventListener(event, callback) { this.listeners[event] = callback; }, scrollTo() {} };
  const context = vm.createContext({
    window,
    crypto: require("node:crypto").webcrypto,
    document: { querySelector: get, createElement: () => new Element(),
      createElementNS: (namespace, tag) => Object.assign(new Element(), { tagName: tag }) },
    FormData: class { get() { return selectedOption; } },
    fetch: async (url, options) => {
      calls.push(url);
      if (url === '/api/questions/eligible') {
        const response = questionResponses.shift();
        if (response instanceof Error) throw response;
        if (response === null) return { ok: true, json: async () => [] };
        return { ok: true, json: async () => Array.isArray(response) ? response : [response ||
          { id: 1, prompt: 'Question', options: { A: 'Yes', B: 'No' } }] };
      }
      if (url.endsWith('/answer')) {
        answerBodies.push(JSON.parse(options.body));
        const result = answerResponses.shift();
        if (result instanceof Error) throw result;
        if (result?.ok === false) return result;
        return { ok: true, json: async () => result || { correct: true, correctOptionId: 'A', explanation: 'Explanation' } };
      }
      const result = statsResponses.shift();
      if (result instanceof Error) throw result;
      return { ok: true, json: async () => result };
    }
  });
  vm.runInContext(fs.readFileSync(path.join(__dirname, "../src/main/resources/static/practice-session.js"), "utf8"), context);
  vm.runInContext(graphScript, context);
  vm.runInContext(script, context);
  return { get, calls, answerBodies, select(id) { selectedOption = id; get('#answer-form').listeners.change(); },
    navigate(id) { window.location.hash = `#${id}`; window.listeners.hashchange(); } };
}

test('loads counts and rounded accuracy, then refreshes after an answer', async () => {
  const { get, calls } = setup([stats(), stats(4, 3, 75)]);
  await flush();
  assert.equal(get('#stats-table').hidden, false);
  assert.equal(get('#overview-attempts').textContent, '3');
  assert.equal(get('#overview-accuracy').textContent, '66.7%');
  assert.equal(get('#overview-mastered').textContent, '0 / 1');
  assert.deepEqual(get('#stats-rows').children[0].children.map(c => c.textContent), ['Processes', 3, 2, '66.7%', 'Practicing', 'Available', 'None']);
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await flush();
  assert.deepEqual(get('#stats-rows').children[0].children.map(c => c.textContent), ['Processes', 4, 3, '75.0%', 'Practicing', 'Available', 'None']);
  assert.equal(calls.filter(url => url === '/api/stats/concepts').length, 2);
  assert.equal(get('#overview-attempts').textContent, '4');
  assert.equal(get('#overview-accuracy').textContent, '75.0%');
  assert.equal(get('#next').hidden, false);
});

test('displays state from the API and refreshes it after an answer', async () => {
  const { get } = setup([stats(4, 4, 100), stats(5, 5, 100, 'MASTERED')]);
  const unstarted = setup([stats(0, 0, 0, 'NOT_STARTED')]);
  await flush();
  assert.equal(unstarted.get('#stats-rows').children[0].children[4].textContent, 'Not started');
  assert.equal(get('#stats-rows').children[0].children[4].textContent, 'Practicing');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await flush();
  assert.equal(get('#stats-rows').children[0].children[4].textContent, 'Mastered');
});

test('handles empty concepts and zero attempts', async () => {
  const empty = setup([[]]);
  const zero = setup([stats(0, 0, 0)]);
  await flush();
  assert.equal(empty.get('#stats-table').hidden, true);
  assert.equal(empty.get('#stats-status').textContent, 'No concepts available yet.');
  assert.equal(zero.get('#stats-rows').children[0].children[3].textContent, '0.0%');
});

test('statistics failure permits practice and retry recovers', async () => {
  const { get } = setup([new Error('Offline'), stats()]);
  await flush();
  assert.equal(get('#stats-retry').hidden, false);
  assert.equal(get('#prompt').textContent, 'Question');
  await get('#stats-retry').listeners.click();
  assert.equal(get('#stats-table').hidden, false);
  assert.equal(get('#stats-retry').hidden, true);
});

test('a failed refresh does not undo a saved answer', async () => {
  const { get } = setup([stats(), new Error('Offline')]);
  await flush();
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await flush();
  assert.equal(get('#next').hidden, false);
  assert.equal(get('#submit').disabled, true);
  assert.equal(get('#feedback').children[0].textContent, 'Correct!');
  assert.equal(get('#stats-retry').hidden, false);
  assert.equal(get('#stats-table').hidden, true);
});

test('requests a new recommendation after answering and retries failures', async () => {
  const { get, calls } = setup([stats(), stats()], [undefined, new Error('Offline'),
    { id: 2, prompt: 'Recommended question', options: { A: 'Yes' }, concept: { name: 'Threads' } }]);
  await flush();
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await get('#next').listeners.click();
  assert.equal(get('#next').textContent, 'Retry next question');
  assert.equal(get('#next').disabled, false);
  await get('#next').listeners.click();
  assert.equal(get('#prompt').textContent, 'Recommended question');
  assert.equal(get('#status').textContent, 'Practice: Threads');
  assert.equal(get('#next').hidden, true);
  assert.equal(calls.filter(url => url === '/api/questions/eligible').length, 3);
});

test('no available questions disables answers and permits checking again', async () => {
  const { get } = setup([stats()], [null]);
  await flush();
  assert.match(get('#status').textContent, /No eligible questions/);
  assert.equal(get('#submit').disabled, true);
  assert.equal(get('#next').textContent, 'Check for questions');
  await get('#next').listeners.click();
  assert.equal(get('#prompt').textContent, 'Question');
});

test('refreshes concept availability after an answer', async () => {
  const { get } = setup([stats(0, 0, 0, 'NOT_STARTED', false), stats(0, 0, 0, 'NOT_STARTED', true)]);
  await flush();
  assert.equal(get('#stats-rows').children[0].children[5].textContent, 'Locked');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await flush();
  assert.equal(get('#stats-rows').children[0].children[5].textContent, 'Available');
});

test('explains only unmet prerequisites and updates after mastery', async () => {
  const prerequisite = { conceptId: 2, conceptName: 'Processes', totalAttempts: 4, accuracyPercent: 100, state: 'PRACTICING' };
  const mastered = { conceptId: 3, conceptName: 'Threads', totalAttempts: 5, accuracyPercent: 100, state: 'MASTERED' };
  const { get } = setup([
    stats(0, 0, 0, 'NOT_STARTED', false, [prerequisite, mastered]),
    stats(0, 0, 0, 'NOT_STARTED', true, [{ ...prerequisite, totalAttempts: 5, state: 'MASTERED' }, mastered])
  ]);
  await flush();
  assert.equal(get('#stats-rows').children[0].children[6].textContent, 'Needs mastery: Processes (4 attempts, 100.0%)');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await flush();
  assert.equal(get('#stats-rows').children[0].children[6].textContent, 'Mastered: Processes, Threads');
});

const graphConcept = (id, name, parents = [], state = 'NOT_STARTED', eligible = true) => ({
  ...stats(0, 0, 0, state, eligible)[0], conceptId: id, conceptName: name,
  prerequisites: parents.map(parent => ({ conceptId: parent, conceptName: `Concept ${parent}`,
    totalAttempts: 0, accuracyPercent: 0, state: 'NOT_STARTED' }))
});
const graphNodes = get => get('#concept-graph').children[0].children.filter(child => child.attributes.class === 'graph-node');

test('draws prerequisite arrows, labels, and layers and refreshes availability', async () => {
  const root = graphConcept(1, 'Processes', [], 'PRACTICING');
  const child = graphConcept(2, 'Threads', [1], 'NOT_STARTED', false);
  const { get } = setup([[child, root], [{ ...root, state: 'MASTERED' }, { ...child, eligible: true }]]);
  await flush();
  const svg = get('#concept-graph').children[0];
  assert.equal(svg.attributes.role, 'img');
  assert.equal(svg.children.filter(child => child.attributes.class === 'graph-edge').length, 1);
  const nodes = graphNodes(get);
  assert.equal(nodes.length, 2);
  assert.ok(Number(nodes[0].children[0].attributes.y) < Number(nodes[1].children[0].attributes.y));
  assert.equal(nodes[1].children.at(-1).textContent, 'Locked');
  assert.equal(nodes[1].children[0].attributes['stroke-dasharray'], '6 4');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await flush();
  assert.equal(graphNodes(get)[1].children.at(-1).textContent, 'Available');
});

test('graph handles empty, cyclic, and missing-prerequisite data without hiding statistics', async () => {
  const empty = setup([[]]);
  const cyclic = setup([[graphConcept(1, 'A', [2]), graphConcept(2, 'B', [1])]]);
  const missing = setup([[graphConcept(1, 'A', [99])]]);
  await flush();
  assert.equal(empty.get('#graph-status').textContent, 'No concepts available yet.');
  assert.match(cyclic.get('#graph-status').textContent, /cycle/);
  assert.equal(cyclic.get('#concept-graph').children.length, 0);
  assert.equal(cyclic.get('#stats-table').hidden, false);
  assert.match(missing.get('#graph-status').textContent, /missing/);
});

test('graph places a concept below all its prerequisites and handles disconnected roots', async () => {
  const { get } = setup([[graphConcept(1, 'A'), graphConcept(2, 'B', [1]),
    graphConcept(3, 'C', [1, 2]), graphConcept(4, 'D')]]);
  await flush();
  const nodes = graphNodes(get);
  const y = index => Number(nodes[index].children[0].attributes.y);
  assert.ok(y(0) < y(1) && y(1) < y(2));
  assert.equal(y(0), y(3));
});

test('statistics failure clears the graph and retry restores it', async () => {
  const { get } = setup([stats(), new Error('Offline'), stats()]);
  await flush();
  assert.equal(graphNodes(get).length, 1);
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await flush();
  assert.equal(get('#concept-graph').children.length, 0);
  assert.match(get('#graph-status').textContent, /Could not load/);
  await get('#stats-retry').listeners.click();
  assert.equal(graphNodes(get).length, 1);
});

test('study views switch without replacing the current question or submitting an answer', async () => {
  const { get, calls, navigate, select } = setup([stats()]);
  await flush();
  select('A');
  navigate('concept-map');
  assert.equal(get('#practice').hidden, true);
  assert.equal(get('#concept-map').hidden, false);
  assert.equal(get('[data-view="concept-map"]').attributes['aria-current'], 'page');
  assert.equal(get('#concept-list').children.length, 1);
  navigate('concept-stats');
  assert.equal(get('#concept-map').hidden, true);
  assert.equal(get('#concept-stats').hidden, false);
  navigate('practice');
  assert.equal(get('#prompt').textContent, 'Question');
  assert.equal(get('#submit').disabled, false);
  assert.equal(calls.filter(url => url.endsWith('/answer')).length, 0);
  assert.equal(calls.filter(url => url === '/api/questions/eligible').length, 1);
});

test('incorrect answer reveals the correct choice and explanation, then resets for the next question', async () => {
  const { get, select } = setup([stats(), stats()], [], [
    { correct: false, correctOptionId: 'B', explanation: 'The explanation for B.' }
  ]);
  await flush();
  select('A');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await flush();
  const first = get('#options').children.find(label => label.children[0].value === 'A');
  const second = get('#options').children.find(label => label.children[0].value === 'B');
  assert.equal(first.attributes['data-outcome'], 'incorrect');
  assert.equal(second.attributes['data-outcome'], 'correct');
  assert.equal(first.children[0].disabled, true);
  assert.equal(second.children[3].textContent, '✓ Correct');
  assert.equal(get('#feedback').attributes['data-result'], 'incorrect');
  assert.equal(get('#feedback').children[1].textContent, 'The explanation for B.');
  assert.equal(get('#feedback').children[2].textContent, 'Correct answer: B. No');
  assert.equal(get('#submit').hidden, true);
  assert.equal(get('#next').hidden, false);
  assert.equal(get('#feedback').focused, true);
  await get('#next').listeners.click();
  assert.equal(get('#submit').hidden, false);
  assert.equal(get('#submit').disabled, true);
  assert.equal(get('#next').hidden, true);
  assert.equal(get('#feedback').children.length, 0);
  assert.equal(get('#options').children[0].attributes['data-outcome'], undefined);
});

test('a rejected answer can be retried without disabling options or creating duplicate submissions', async () => {
  const { get, calls, select } = setup([stats(), stats()], [], [
    { ok: false, status: 400 }, { correct: true, correctOptionId: 'A', explanation: 'Correct explanation.' }
  ]);
  await flush();
  select('A');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  assert.equal(get('#submit').disabled, false);
  assert.equal(get('#submit').hidden, false);
  assert.match(get('#feedback').textContent, /400/);
  assert.notEqual(get('#options').children[0].children[0].disabled, true);
  assert.equal(get('#next').hidden, true);
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  await flush();
  assert.equal(get('#feedback').attributes['data-result'], 'correct');
  assert.equal(get('#options').children.find(label => label.children[0].value === 'A').attributes['data-outcome'], 'correct');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  assert.equal(calls.filter(url => url.endsWith('/answer')).length, 2);
});


test('lost answer responses reuse the original submission and choice on retry', async () => {
  const { get, select, answerBodies } = setup([stats(), stats()], [], [new Error('Connection lost')]);
  await flush();
  select('A');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  assert.match(get('#answer-hint').textContent, /unknown/);
  assert.equal(get('#options').children[0].children[0].disabled, true);
  select('B');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  assert.equal(answerBodies.length, 2);
  assert.deepEqual(answerBodies[0], answerBodies[1]);
  assert.equal(answerBodies[1].optionId, 'A');
  await get('#next').listeners.click();
  select('A');
  await get('#answer-form').listeners.submit({ preventDefault() {} });
  assert.notEqual(answerBodies[2].submissionId, answerBodies[0].submissionId);
});


test('practice advances through the eligible pool without duplicates and retains stable answer IDs', async () => {
  const eligible = [1, 2, 3].map(id => ({ id, prompt: `Question ${id}`, options: { '10': 'Yes', '20': 'No', Z: 'Maybe' } }));
  const { get, select, answerBodies } = setup([stats(), stats(), stats(), stats()],
    [eligible, [...eligible].reverse(), eligible]);
  await flush();
  const prompts = [];
  for (let i = 0; i < 3; i++) {
    prompts.push(get('#prompt').textContent);
    assert.equal(get('#options').children.length, 3);
    assert.deepEqual(new Set(get('#options').children.map(label => label.children[0].value)), new Set(['10', '20', 'Z']));
    select('20');
    await get('#answer-form').listeners.submit({ preventDefault() {} });
    if (i < 2) await get('#next').listeners.click();
  }
  assert.equal(new Set(prompts).size, 3);
  assert.ok(answerBodies.every(body => body.optionId === '20'));
});
