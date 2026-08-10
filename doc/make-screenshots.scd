// Regenerate the screenshots in doc/images.
//
// Uses Image.fromWindow, so Qt renders each window itself: no screen recording
// permission is involved and the result is independent of what else is on screen.
//
// Run the whole file. It builds a small environment, opens one window at a time,
// writes a PNG, and closes it.

(
var dir = PathName(thisProcess.nowExecutingPath).pathOnly +/+ "images";
var shots = [];
var shoot, step;

File.mkdir(dir);

shoot = { |name, window|
	var path = dir +/+ (name ++ ".png");
	var image = Image.fromWindow(window);
	image.write(path);
	image.free;
	shots = shots.add(name);
	"wrote %".format(path).postln;
};

// each window needs a moment to lay out and run its drawFunc before capture
step = { |wait, func| { func.value }.defer(wait) };

PT.removeAll;
PTObjectView.closeAll;

// ------------------------------------------------------------ the environment

PTStockpile.specify(\cmajor, "c3 d3 e3 f3 g3 a3 b3 c4 d4 e4 f4 g4 a4 b4 c5");
PTStockpile.generate(\row, "Pshuf((60..71), 1)", "12");

PTShape.specify(\arch, "0 18 44 72 92 100 96 78 52 26 8 0");
PTMask.specify(\funnel,
	"100 96 88 74 80 66 54 58 44 38 42 30",
	"2 6 10 22 18 30 34 30 36 34 38 28"
);

PTDataSection(\opening, (
	clock:    "150",
	number:   "60",
	rhythm:   "Prand([1, 1, 2, -1], inf)",
	pitch:    "Prand(cmajor, inf)",
	velocity: "Pwhite(50, 105)",
	channel:  "1"
)).make;
PT(\opening).comment_("first sketch, scale tones only");

PTDataSection(\tendency, (
	clock:    "100",
	number:   "220",
	rhythm:   "1",
	pitch:    "funnel.convert(PT.fromNumber, 40, 88, PTbeta(0.0, 100, 0.15, 0.15))",
	velocity: "funnel.spread(220, 40, 115)",
	channel:  "1"
)).make;
PT(\tendency).comment_("mask read at its boundaries");

PT(\opening).variant(\openingA);

// two voices at once, to show how a combination overlaps them
PTDataSection(\pulse, (
	clock: "150", number: "24", rhythm: "2",
	pitch: "Prand(cmajor, inf) - 24", velocity: "72", channel: "2"
)).make;
PTParallel(\duo, (sections: "pulse 0.6 opening")).make;
PT(\duo).comment_("two voices, the second entering late");

// a controller and a scheme, so the browser shows every object type
PTController(\howMany, (source: "Pseries(12, 6, 5)")).make;
PT(\howMany).comment_("more notes with every make");
PTScheme(\round, (members: "funnel tendency", reset: "howMany")).make;
PT(\round).comment_("remake the mask, then what reads it");

// a density section: time first, filled as thickly as a shape says
PTShape(\swell, (source: "1 4 20 8 2 14 30 6 1")).make;
PTDensityCurve(\swelling, (
	time: "14", curve: "swell", min: "1", max: "24", unit: "1",
	duration: "120", pitch: "Pwhite(c2, c6)"
)).make;
PT(\swelling).comment_("density read from a shape, notes per second");

// a live object: rules only, no frozen result
PTBind(\live, (clock: "150", rhythm: "Prand([1, 1, 2, -1], inf)",
	pitch: "Prand(cmajor, inf)", velocity: "Pwhite(50, 110)")).make;
PT(\live).comment_("plays through a Pdef; edit and make while it sounds");

// -------------------------------------------------------------- the captures

step.(0.4, { var w = PTBrowser.new.window;      step.(0.6, { shoot.("browser", w); w.close }) });
step.(1.6, { var w = PT(\opening).edit.window;  step.(0.6, { shoot.("object-dialog", w); w.close }) });
step.(2.8, { var w = PTPianoRoll(PT(\tendency)).window; step.(0.8, { shoot.("piano-roll", w); w.close }) });
step.(4.2, { var w = PTPianoRoll(PT(\opening)).window;  step.(0.8, { shoot.("piano-roll-notes", w); w.close }) });
step.(5.6, { var w = PT(\funnel).draw.window;   step.(0.8, { shoot.("mask-editor", w); w.close }) });
step.(7.0, { var w = PT(\arch).draw.window;     step.(0.8, { shoot.("shape-editor", w); w.close }) });

step.(8.4, { var w = PTPianoRoll(PT(\duo)).window; step.(0.8, { shoot.("piano-roll-parallel", w); w.close }) });

step.(9.8, { var w = PTPianoRoll(PT(\swelling)).window; step.(0.8, { shoot.("piano-roll-density", w); w.close }) });

step.(11.2, { "\n% screenshot(s) in %".format(shots.size, dir).postln });
)
