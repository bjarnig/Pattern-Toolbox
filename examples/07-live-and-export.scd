// Pattern Toolbox, example 7
// The part with no AC Toolbox equivalent: playing live, and getting out.

PTStockpile(\cmajor, (source: "c3 e3 g3 c4 e4 g4 c5")).make;


// -------------------------------------------------------------- a live object

// Every other object in the toolbox keeps rules and one realization of them.
// A bind keeps only the rules. It has no events at all; it is a Pbind playing
// through a Pdef named after itself.
(
PTBind(\live, (
	clock:    "150",
	rhythm:   "Prand([1, 1, 2, -1], inf)",
	pitch:    "Prand(cmajor, inf)",
	velocity: "Pwhite(50, 110)"
)).make;
)

PT(\live).play;

// Now change something and press make again. The Pdef picks it up on the next
// cycle rather than restarting, so it changes underneath you.
PT(\live).spec.put(\pitch, "Prand(cmajor, inf) + Prand([0, 12], inf)");
PT(\live).make;

PT(\live).spec.put(\rhythm, "Prand([1, 0.5, 0.5, 2], inf)");
PT(\live).make;

// Anything else a Pbind takes goes in the extra slot
PT(\live).spec.put(\extra, "(pan: Pwhite(-0.8, 0.8), legato: 0.4)");
PT(\live).make;

PT(\live).stop;

// Or work on it in a dialog while it sounds: the object list shows it as a bind
PT.browse;
PT(\live).edit;


// ------------------------------------------------------------- keeping a take

// A bind cannot be plotted or sliced, because it has no events. Capture it and
// you get an ordinary section: plot it, transform it, combine it, save it.
PT(\live).play;
PT(\live).capture(\take1, 64);
PT(\live).capture(\take2, 64);
PT(\live).stop;

PT(\take1).plot;
PT(\take1).play;

// A capture keeps its rules, so remaking it takes a new stretch of the same live
// material, and its seed brings any take you liked back exactly.
PT(\take1).make;                   // a different 64 events
PT(\take1).reproduce;              // the ones you had

// and it is a section like any other
PTDerived(\take1Up, (source: "take1", transform: "[PT.transpose(12), PT.dedupe]")).make.play;
PTSequence(\both, (sections: "take1 1 take2")).make.play;


// ---------------------------------------------------- what is this as code?

// Useful when the GUI has been doing the thinking: ask an object what it is as
// plain SuperCollider. Object names and pitch names are written out, so the
// result stands on its own with the toolbox uninstalled.
(
PTDataSection(\sketch, (
	clock: "150", number: "16",
	rhythm: "1 2", pitch: "Prand(cmajor, inf)", velocity: "mf"
)).make;
)

PT(\sketch).asPbindSource.postln;   // the rules, as a Pbind
PT(\sketch).asSource.postln;        // the events themselves, exactly

// paste-able: this really does run
PT(\sketch).asPbindSource.interpret.play;


// ------------------------------------------------------------------- export

// A standard MIDI file. Format 0, one tick to the millisecond, chords written as
// simultaneous note ons, rests left as gaps, channel and velocity carried.
PT(\sketch).writeMidi("~/Desktop/sketch.mid");

// Render offline, faster than real time, to a sound file. The SynthDefs travel
// with the score, otherwise an offline server has nothing to instantiate.
PT(\sketch).render("~/Desktop/sketch.wav");

// A Score, if you want to add to it before rendering
PT(\sketch).asScore.score.size;

// And out to a MIDI device, if you have one
// PT.midiOut_(MIDIOut(0));
// PT(\sketch).playMidi;

PT.stopAll;
