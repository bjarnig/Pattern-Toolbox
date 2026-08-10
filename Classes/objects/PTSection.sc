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
