// PTSection - the abstract playable object.
//
// A section is one or more notes: note, phrase, voice, movement, composition.
// Whatever the flavour, a section realizes to a flat Array of Events, and that
// array is the lingua franca of the whole toolbox. Durations are in seconds and
// are played on a TempoClock at 1 beat per second.
//
// Rests are kept in the array as events of type \rest, so timing survives and
// the note count stays computable.

PTSection : PTObject {

	*ptType { ^\section }

	asEvents { ^this.prRequireMade.value }

	// [absoluteStart, event] pairs, with \sustain filled in so a combination can
	// let voices overlap. Rests only advance the clock, so they are not carried
	// over: a gap comes back as a long \dur when the pairs are flattened.
	asTimedEvents { |offset = 0|
		var time = offset;
		var out = Array.new(this.length);
		this.asEvents.do { |event|
			var copy = event.copy;
			copy[\sustain] = copy[\sustain] ?? { copy[\dur] };
			if(event[\type] != \rest) { out = out.add([time, copy]) };
			time = time + (event[\dur] ? 0);
		};
		^out
	}

	// Velocity is what a section stores; amp is derived here, so any transform
	// that touches velocity is automatically audible.
	asPattern {
		var events = this.asEvents.collect { |event|
			var copy = event.copy;
			copy[\amp] = (copy[\velocity] ? 64) / 127;
			copy
		};
		^Pseq(events, 1)
	}

	// -------------------------------------------------------------- accessors

	notes { ^this.asEvents.reject { |event| event[\type] == \rest } }

	numNotes { ^this.notes.size }

	duration { ^this.asEvents.sum { |event| event[\dur] } }

	// pitch, rhythm, velocity, channel ... out of the realized events
	extract { |key|
		key = key.asSymbol;
		key = switch(key,
			\pitch, { \midinote },
			\rhythm, { \dur },
			\dynamics, { \velocity },
			\channel, { \chan },
			{ key }
		);
		^this.asEvents.collect { |event| event[key] }
	}

	// The events read back as notes, so a section can serve as note material.
	// Rhythms come out in seconds, so a note section with a clock of 1000
	// reproduces the original timing.
	asNotes {
		^this.asEvents.collect { |event|
			PTNote(
				event[\sustain] ? event[\dur] ? 1,
				event[\midinote] ? 60,
				event[\velocity] ? 64,
				event[\chan] ? 1,
				if(event[\type] == \rest) { \rest } { \note }
			)
		}
	}

	// ------------------------------------------------------------------ plots

	// No argument opens the piano roll, as the AC Toolbox Plot button does.
	// A parameter name plots that parameter alone.
	plot { |key|
		if(key.isNil) { ^PTPianoRoll(this.prRequireMade) };
		^this.extract(key).plot("% : %".format(name, key))
	}

	textScore { |limit = 200|
		var time = 0;
		"% : % events, % seconds".format(name, this.length, this.duration.round(0.001)).postln;
		this.asEvents.keep(limit).do { |event, i|
			"% % % % %".format(
				i.asString.padLeft(5),
				time.round(0.001).asString.padLeft(9),
				event[\dur].round(0.001).asString.padLeft(8),
				(if(event[\type] == \rest) { "rest" } { event[\midinote] }).asString.padLeft(12),
				event[\velocity].asString.padLeft(6)
			).postln;
			time = time + event[\dur];
		};
		^this
	}
}
