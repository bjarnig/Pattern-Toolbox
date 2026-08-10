// Pattern Toolbox, example 6
// AC Toolbox Tutorials 8, 9 and 10.
//
// Two things here, and they pull in opposite directions.
//
// A note structure treats a note as one indivisible thing, so you can write down
// music you already know. A data section, by contrast, calculates rhythm and
// pitch apart from each other and is excellent at material and useless at
// melodies.
//
// A density section inverts the priority the other way: instead of "make this
// many notes", it says "here is an amount of time, fill it this thickly".


// -------------------------------------------------------- note structures

// The AC Toolbox ships a note structure of the Wilhelmus for this chapter. Any
// melody will do. Rhythm comes first, then pitch; r is a rest and . is a delay.
PTNoteStructure(\theme, (
	source: "PT.melody(\"2 c4 1 d4 1 e4 2 g4 1 f4 1 e4 2 d4 1 r 2 c4\")"
)).make;

PT(\theme).postValues;
PT(\theme).size;                   // how many notes
PT(\theme).beats;                  // how long, in clock units

// Played as written. The clock turns units into time.
PTNoteSection(\played, (clock: "300", notes: "theme")).make.play;
PT(\played).plot;

// Notes can be nested. In parallel they sound together, which is how a chord
// with its own inner rhythm gets written down.
(
PTNoteStructure(\chords, (source: "PT.seq(
	PT.par(PT.note(4, c3), PT.seq(PT.note(2, e4), PT.note(2, g4))),
	PT.par(PT.note(4, f3), PT.seq(PT.note(1, a4), PT.note(1, g4), PT.note(2, f4)))
)")).make;
)

PTNoteSection(\harmony, (clock: "300", notes: "chords")).make.play;
PT(\harmony).plot;                 // the low notes hold under the moving ones

// The AC Toolbox spellings work too, if they are more familiar:
// PT.aNote, PT.aRest, PT.aDelay, PT.inSequence, PT.inParallel


// --------------------------------------- a melody as a supply of material

// Through a generator the structure is flattened and becomes something to choose
// from. This is how the AC Toolbox turns a known tune into raw material.
PTNoteSection(\scattered, (clock: "200", number: "60", notes: "Prand(theme, inf)")).make.play;

// No note repeats until all have been used once
PTNoteSection(\permuted, (clock: "200", number: "60", notes: "Pxrand(theme, inf)")).make.play;

// Read in the order a shape describes, rather than at random
(
PTShape(\arch, (source: "0 30 70 100 60 20 0")).make;
PTNoteSection(\shaped, (
	clock: "200", number: "80",
	notes: "PT.readFrom(theme, arch, PT.fromNumber)"
)).make.play;
)

// Interpolate between two structures: step by step the second becomes more
// likely, so one melody turns into the other. Rainer Boesch's idea.
(
PTNoteStructure(\other, (source: "PT.melody(\"1 c5 1 b4 1 gs4 1 fs4 1 e4\")")).make;
PTNoteSection(\morphing, (
	clock: "180", number: "160",
	notes: "PT.interpolate(theme, other, 160)"
)).make.play;
)
PT(\morphing).plot;

// or interpolate along a shape instead of a straight line
(
PTNoteSection(\morphing2, (
	clock: "180", number: "160",
	notes: "PT.interpolate(theme, other, 160, arch)"
)).make.play;
)

// A section can be note material too. Its rhythms arrive in seconds, so a clock
// of 1000 reproduces the original timing.
(
PTDataSection(\raw, (clock: "1000", number: "20", rhythm: "Pwhite(0.1, 0.4)",
	pitch: "Pwhite(c3, c5)")).make;
PTNoteSection(\reordered, (clock: "1000", number: "40",
	notes: "Pxrand(PT.asList(raw), inf)")).make.play;
)


// ------------------------------------------------- density: filling time

// Attack points as a percentage of the total time. Zero is the beginning, 100
// the end. A list decides the number of notes by its own length.
(
PTDensity(\even, (
	time:     "10",
	attacks:  "0 10 20 30 40 50 60 70 80 90",
	duration: "200",
	pitch:    "Pwhite(c3, c5)"
)).make.play;
)

// With a generator, the number slot decides how many attacks are drawn, and they
// are sorted. The density curve of the section is the density curve of the
// generator: here the notes crowd both ends of the ten seconds and thin out in
// the middle.
(
PTDensity(\edges, (
	time: "10", number: "120",
	attacks: "PTbeta(0.0, 100, 0.2, 0.2)",
	duration: "150", pitch: "Pwhite(c2, c6)"
)).make.play;
)
PT(\edges).plot;

// The Xenakis approach instead: let the function decide the distance to the next
// attack rather than the absolute position. PT.attacks adds up intervals and
// then maps the result across the available time.
(
PTDensity(\intervals, (
	time: "12", number: "80",
	attacks: "PT.attacks(PTbeta(0.0, 0.1, 1, 1))",
	duration: "120", pitch: "Pwhite(c3, c6)"
)).make.play;
)

// Or give the density itself a shape, read as notes per second.
(
PTShape(\swell, (source: "1 4 20 8 2 14 30 6 1")).make;
PTDensityCurve(\swelling, (
	time: "14", curve: "swell",
	min: "1", max: "24", unit: "1",
	duration: "120", pitch: "Pwhite(c3, c6)"
)).make.play;
)
PT(\swelling).plot;                // thick where the shape is high

// A mask makes the density a tendency rather than a fixed contour: at each step
// the count is chosen somewhere between the two lines.
(
PTMask(\loose, (top: "30 24 18 12", bottom: "2 6 10 14")).make;
PTDensityCurve(\loosening, (
	time: "12", curve: "loose",
	min: "1", max: "30", unit: "1",
	duration: "150", pitch: "Pwhite(c2, c5)"
)).make.play;
)

PT.browse;
PT.stopAll;
