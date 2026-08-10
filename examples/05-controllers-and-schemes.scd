// Pattern Toolbox, example 5
// AC Toolbox Tutorial 13: a level higher.
//
// A generator written into a section's rules knows nothing about what happened in
// the previous section. Every make builds it afresh, so no generator can say
// anything about the relation between one section and the next. A controller can:
// it keeps its state outside any section, and remembers what it has done.

// ------------------------------------------------------------- the basic idea

PTController(\howMany, (source: "2 10 20")).make;

PTDataSection(\bit, (clock: "150", number: "howMany", pitch: "Pwhite(c3, c5)")).specify;

PT(\bit).make.length;              // 2
PT(\bit).make.length;              // 10
PT(\bit).make.length;              // 20
PT(\bit).make.length;              // 2 again: the list cycles

PT(\howMany).history;              // everything it has handed out
PT(\howMany).reset;                // back to the beginning
PT(\howMany).plot;


// -------------------------------------------------- a value per note, or one

// Used bare in a slot, a controller supplies a value every time that slot is
// calculated: once per note.
(
PTController(\steps, (source: "Pbrown(c3, c5, 3)")).make;
PTDataSection(\walking, (number: "40", pitch: "steps")).make.play;
)

// PT.takeOne pulls exactly one value and hands that single value to a generator,
// so it holds for the whole section. This is the AC Toolbox take-one.
(
PTController(\low, (source: "48 50 52 54 56")).make;
PTController(\high, (source: "72 74 76 78 80")).make;

PTDataSection(\band, (
	clock:  "120",
	number: "30",
	pitch:  "PTbeta(PT.takeOne(low), PT.takeOne(high), 0.3, 0.3)"
)).specify;
)

// each make sits inside a different band, and the bands walk outwards
PT(\band).make.play;
PT(\band).make.play;


// ------------------------------------- change on a level above the section

// Tutorial 13's central example: five variants of one section, each tending to
// be faster and louder than the one before. No generator inside the section can
// do this, because the trend lives between the sections, not within them.
(
PTController(\rhythmCeiling, (source: "Pseries(3.0, -0.375, 5)")).make;   // 3.0 down to 1.5
PTController(\velocityFloor, (source: "Pseries(40, 7.5, 5)")).make;       // 40 up to 70

PTDataSection(\gesture, (
	clock:    "100",
	number:   "24",
	rhythm:   "PTbeta(1.0, PT.takeOne(rhythmCeiling), 0.2, 0.2)",
	pitch:    "Pbrown(c2, c6, 6)",
	velocity: "PTbeta(PT.takeOne(velocityFloor), 90, 0.2, 0.2)"
)).specify;

PTCommunity(\gestures, (source: "gesture", number: "5")).make;
)

// each variant is faster and louder than the last
PT(\gestures).sections.collect { |s| s.extract(\rhythm).mean.round(0.001) };
PT(\gestures).sections.collect { |s| s.extract(\velocity).mean.round(0.1) };

PT(\gestures).asSequence(\whole, 0.5).play;
PT(\whole).plot;


// ------------------------------------------- relating two parameters exactly

// Sometimes two parameters must be derived from the SAME value. Two controllers
// synchronised to a third solve it: whichever asks first makes the master
// produce, and the other sees that very same value rather than a new one.
(
PTController(\choose, (source: "Prand([1, 2, 3, 10], inf)")).make;
PTController(\forRhythm, (syncTo: "choose")).make;
PTController(\forVelocity, (syncTo: "choose")).make;

PTStockpile(\table, (source: "1 40 2 55 3 70 10 110")).make;   // rhythm -> velocity

PTDataSection(\linked, (
	clock:    "120",
	number:   "40",
	rhythm:   "forRhythm",
	pitch:    "Pwhite(c3, c5)",
	velocity: "PT.lookup(forVelocity, table)"
)).make.play;
)

// short notes are quiet and long ones are loud, note for note
PT(\linked).asEvents.keep(8).collect { |e| [e.dur.round(0.001), e.velocity] };


// ------------------------------------------------------------------ schemes

// A scheme is a script: remake these objects, in this order. Convenience,
// because a section that reads a generated shape needs the shape remade first.
// Design, because the order is itself a decision.
(
PTShape(\curve, (source: "Pbrown(0, 100, 20)", number: "16")).make;
PTDataSection(\theme, (
	clock: "110", number: "48",
	pitch: "curve.convert(PT.fromNumber, c3, c5)",
	velocity: "curve.convert(PT.fromNumber, 40, 110)"
)).make;

PTScheme(\round, (members: "curve theme")).make;
)

PT(\round).apply;                  // a new curve, and a theme that follows it
PT(\theme).play;
PT(\theme).plot;

// Without a reset a controller carries on, which is usually what you want.
// Name the controllers in the reset slot when a pass should start over.
(
PTScheme(\fromTheTop, (
	members: "gestures whole",
	reset:   "rhythmCeiling velocityFloor"
)).make;
)

PT(\fromTheTop).apply;             // the same trend again, on new material
PT(\whole).play;

// or run several passes and keep an eye on what each one produced
PT(\fromTheTop).applyTimes(3, { |made, i|
	"pass %: % notes".format(i + 1, PT(\whole).numNotes).postln;
});

PT.browse;
PT.stopAll;
