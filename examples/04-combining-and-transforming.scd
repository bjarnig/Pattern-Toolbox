// Pattern Toolbox, example 4
// AC Toolbox Tutorials 5, 7, 23 and 25: combining sections, transforming them,
// filtering them, and grouping them.

PTStockpile.specify(\cmajor, "c3 e3 g3 c4 e4 g4 c5");

(
PTDataSection(\bass, (
	clock: "200", number: "16", rhythm: "2",
	pitch: "Prand(cmajor, inf) - 24", velocity: "70"
)).make;

PTDataSection(\lead, (
	clock: "200", number: "32", rhythm: "1",
	pitch: "Prand(cmajor, inf)", velocity: "Pwhite(60, 110)"
)).make;
)


// ------------------------------------------------------------- combinations

// one after another
PTSequence(\pair, (sections: "bass lead")).make.play;

// a number between two names is a delay in seconds
PTSequence(\spaced, (sections: "bass 2 lead")).make.play;

// at the same time
PTParallel(\duo, (sections: "bass lead")).make.play;

// a number before a name is that voice's offset from the start
PTParallel(\stagger, (sections: "bass 0.4 lead")).make.play;
PT(\stagger).plot;                 // the voices overlap in the piano roll

// or give explicit start times
PTTimed(\entries, (sections: "bass lead", times: "0 1.5")).make.play;

// a combination is a section like any other, so they nest
PTParallel(\canon, (sections: "pair 1 pair 2 pair")).make.play;


// ------------------------------------------------------------- transformers

// A transformed section is its own object. Its rules say what was done, so it
// can be remade, varied and saved like anything else. The source is untouched.

PTDerived(\leadUp, (source: "lead", transform: "PT.transpose(12)")).make.play;

// transforms apply left to right, so the order matters
PTDerived(\clipped, (source: "lead", transform: "[PT.transpose(24), PT.limit(c3, c6)]")).make;
PTDerived(\clipped2, (source: "lead", transform: "[PT.limit(c3, c6), PT.transpose(24)]")).make;

// the amount can be a generator, giving a different interval per note
PTDerived(\wander, (source: "lead", transform: "PT.transpose(Pwhite(-5, 5))")).make.play;

// time
PTDerived(\slow, (source: "lead", transform: "PT.stretch(2)")).make.play;

// fold keeps the contour where limit flattens it against the ceiling
PTDerived(\folded, (source: "lead", transform: "PT.fold(c4, c5)")).make.play;

// velocity, and amp follows automatically
PTDerived(\loud, (source: "lead", transform: "PT.louder(30)")).make.play;

// order and extent
PTDerived(\back, (source: "lead", transform: "PT.reverse")).make.play;
PTDerived(\head, (source: "lead", transform: "PT.keep(8)")).make.play;
PTDerived(\middle, (source: "lead", transform: "PT.slice(8, 24)")).make.play;

// several sources are joined in sequence first, then transformed
PTDerived(\bothUp, (source: "bass lead", transform: "PT.transpose(7)")).make.play;


// ------------------------------------------------------------------ filters

// keep what the test accepts and close the gaps: the section gets shorter
PTDerived(\highOnly, (source: "lead", transform: "PT.filter({ |e| e.midinote > 60 })")).make;

// or silence them instead, keeping the timing intact
PTDerived(\highGone, (source: "lead", transform: "PT.mute({ |e| e.midinote > 60 })")).make;

PT(\highOnly).duration;
PT(\highGone).duration;            // the same as lead

// drop immediate repetitions
PTDerived(\noRepeats, (source: "lead", transform: "PT.dedupe")).make;

// transform only the events that pass a test, leaving the rest in place
(
PTDerived(\accented, (
	source: "lead",
	transform: "PT.transformIf({ |e| e.velocity > 90 }, PT.transpose(12))"
)).make.play;
)


// -------------------------------------------------------------- communities

// an explicit group: names, not music
PTCommunity(\voices, (members: "bass lead")).make;
PT(\voices).names;
PT(\voices).postValues;

// or a family of variants of one section, all sharing its rules
PTCommunity(\family, (source: "lead", number: "5")).make;
PT(\family).names;                 // lead1 ... lead5, each with its own seed

// fold the group into something you can hear
PT(\family).asSequence(\familyChain, 0.5).play;
PT(\family).asParallel(\familyCloud, 0.25).play;

// this is the AC Toolbox idiom of hearing many variants in a row: make the
// family again and the whole chain is new material on the same rules
PT(\family).make;
PT(\family).asSequence(\familyChain, 0.5).play;

PT.browse;
PT.stopAll;
