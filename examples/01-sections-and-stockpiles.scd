// Pattern Toolbox, example 1
// The ground covered by AC Toolbox Tutorials 1 and 2:
// data sections, generators for parameters, stockpiles, choices.
//
// Evaluate the blocks one at a time.

// ---------------------------------------------------------------- a section
(
PTDataSection(\section1, (
	clock:    "100",        // the basic beat, in milliseconds
	number:   "60",         // sixty notes
	rhythm:   "1",          // each note lasts one beat
	pitch:    "Pseries(c2, 1)", // a rising chromatic line from c2
	velocity: "mf",
	channel:  "1"
)).make;
)

PT(\section1).play;
PT(\section1).plot;              // pitch over time
PT(\section1).plot(\rhythm);
PT(\section1).textScore(20);


// ------------------------------------------------- generators for everything
(
PTDataSection(\section6, (
	clock:    "100",
	number:   "200",
	rhythm:   "Pbrown(0.1, 5, 0.5)",
	pitch:    "Pbrown(c2, c6, 5)",
	velocity: "Pbrown(pp, ff, 5)",
	channel:  "1"
)).make.play;
)


// --------------------------------------------------------- filling ten seconds
(
PTDataSection(\section8, (
	clock:  "100",
	number: "PT.untilTime(10)",
	rhythm: "Pwhite(1.0, 2)",
	pitch:  "Pwhite(c3, c5)",
	velocity: "Pwhite(30, 90)"
)).make;
)

PT(\section8).length;            // however many notes fitted
PT(\section8).duration;


// ------------------------------------------------------------- rests are negative
(
PTDataSection(\section2, (
	clock:  "150",
	number: "50",
	rhythm: "Prand([1, -1], inf)",
	pitch:  "c4 d4 e4 f4"
)).make.play;
)

PT(\section2).length;            // events, including rests
PT(\section2).numNotes;          // sounding notes only


// ----------------------------------------------------------------- stockpiles
(
PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");
PTStockpile.specify(\sillyScale, "60 61 63 65 66 68 70 72");
PTStockpile.generate(\pitch1, "Pshuf((60..71), 1)", "12");   // a twelve tone row
)

PT(\cmajor).postValues;
PT(\pitch1).postValues;

// used by name in another object
(
PTDataSection(\section5, (
	clock:  "150",
	number: "100",
	rhythm: "Prand([1, 2, 3], inf)",
	pitch:  "Prand(cmajor, inf)"
)).make.play;
)

// transposed on the way out, without changing the stockpile
(
PTDataSection(\section3, (
	clock:  "150",
	number: "50",
	rhythm: "Prand([1, 2, 3], inf)",
	pitch:  "Prand(cmajor, inf) + Prand([-12, 0, 12], inf)"
)).make.play;
)


// ------------------------------------------------------------------- chords
(
PTDataSection(\section4, (
	clock:  "150",
	number: "50",
	rhythm: "Prand([1, -1], inf)",
	pitch:  "Prand([[c4, e4, g4], [c4, d4, e4, f4], [d4, g4, b4]], inf)"
)).make.play;
)


// ----------------------------------------------------------------- variants
(
PTDataSection(\noise1, (
	clock:  "150",
	number: "64",
	rhythm: "Pbrown(0.5, 2.0, 0.2)",
	pitch:  "Pbrown(c2, c5, 4)",
	velocity: "Pbrown(30, 90, 5)"
)).make;
)

PT(\noise1).play;
PT(\noise1).variant.play;        // same rules, called noise1a
PT(\noise1a).play;
PT(\noise1).reproduce.play;      // the original result, exactly


// ------------------------------------------------------------ the environment
PT.post;
PT.save("~/Desktop/example1.ptx");
PT.stopAll;
