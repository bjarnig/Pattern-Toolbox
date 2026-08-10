// Pattern Toolbox, example 3
// The ground covered by AC Toolbox Tutorials 3 and 4: shapes and masks.
//
// A shape is one line, a mask is two. Neither carries a scale. They are contours,
// and they mean nothing until they are converted into a range.

// ------------------------------------------------------------------- shapes

// specified: evenly spaced points, in any range you like
PTShape.specify(\arch, "0 40 80 100 80 40 0");
PT(\arch).plot;

// only the contour matters, not the numbers: these two are the same shape
PTShape.specify(\arch2, "20 36 52 60 52 36 20");
PT(\arch).convert(7, 60, 72) == PT(\arch2).convert(7, 60, 72);

// generated
PTShape.generate(\wobble, "Pbrown(0, 100, 12)", "40");
PT(\wobble).plot;

// drawn: drag left to right, then press make
PTShape.draw(\hand);

// converting: n values following the contour, mapped into a range.
// Integer bounds give integer results, so this gives note numbers.
PT(\arch).convert(20, c3, c5);

// PT.fromNumber asks the section how many notes it is making, so the shape
// stretches to fit rather than repeating.
(
PTDataSection(\shapeSection, (
	clock:  "100",
	number: "120",
	rhythm: "1",
	pitch:  "arch.convert(PT.fromNumber, c3, c5)"
)).make.play;
)

// a shape can drive any parameter
(
PTDataSection(\shapeRhythm, (
	clock:  "100",
	number: "80",
	rhythm: "arch.convert(PT.fromNumber, 0.5, 4.0)",
	pitch:  "Pwhite(c3, c5)",
	velocity: "wobble.convert(PT.fromNumber, 30, 110)"
)).make.play;
)


// -------------------------------------------------------------------- masks

// A tendency mask: two lines describing a field that changes over time.
// Events happen between them.
PTMask.specify(\mask1, "100 90 70 100 60", "0 20 40 30 55");
PT(\mask1).plot;

// drawn: choose top or bottom in the popup, then drag
PTMask.draw(\mask2);

// converting picks a value between the boundaries at each point in time.
// Both lines are scaled together, so the field keeps its shape.
(
PTDataSection(\maskSection, (
	clock:  "100",
	number: "200",
	rhythm: "1",
	pitch:  "mask1.convert(PT.fromNumber, 48, 72)"
)).make.play;
)

// By default the choice inside the field is uniform. Give it a beta generator
// with small shape parameters and the values hug the two boundaries instead, so
// the mask is heard as two lines rather than as a filled band.
(
PTDataSection(\maskEdges, (
	clock:  "100",
	number: "200",
	rhythm: "1",
	pitch:  "mask1.convert(PT.fromNumber, 48, 72, PTbeta(0.0, 100, 0.1, 0.1))"
)).make.play;
)
PT(\maskEdges).plot;             // compare the piano rolls of these two

// quantised to quarter tones: float bounds plus a rounding unit
PT(\mask1).convert(20, 48, 72.0, nil, 0.5);

// how open the field is at each point, useful for relating parameters
(
PTDataSection(\maskSpread, (
	clock:  "80",
	number: "150",
	rhythm: "1",
	pitch:  "mask1.convert(PT.fromNumber, 48, 84)",
	velocity: "mask1.spread(150, 30, 120)"
)).make.play;
)


// ------------------------------------------------- reading from a stockpile

// Limit a curve to a collection: the lowest point of the curve reads the first
// element, the highest the last. The contour survives, the pitch content is
// constrained.
PTStockpile.specify(\cmajor, "c3 d3 e3 f3 g3 a3 b3 c4 d4 e4 f4 g4 a4 b4 c5");

PT.readFrom(\cmajor, \arch, 15);

(
PTDataSection(\readSection, (
	clock:  "120",
	number: "100",
	rhythm: "1",
	pitch:  "PT.readFrom(cmajor, arch, PT.fromNumber)"
)).make.play;
)

// the same with a mask: the shape is followed, but only scale tones sound
(
PTDataSection(\readMask, (
	clock:  "120",
	number: "150",
	rhythm: "1",
	pitch:  "PT.readFrom(cmajor, mask1, PT.fromNumber)"
)).make.play;
)

PT.browse;
PT.stopAll;
