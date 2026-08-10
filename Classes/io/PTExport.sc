// PTExport - getting material out of the toolbox.
//
// Four destinations, all from the same realized events:
//
//   source text   a Pbind or a Pseq of events, to paste into ordinary sclang
//   MIDI file     see PTMidiFile
//   Score         SuperCollider's offline format, for rendering to a sound file
//   MIDI out      a live pattern that sends notes to a device
//
// Source export matters pedagogically: a student who has been clicking objects can
// ask what one is as plain code, and read the answer.

PTExport {

	// ------------------------------------------------------------ source text

	// The realized events, exactly, as a pattern. Works for any section, however
	// it was made, because it exports the output rather than the rules.
	*eventSource { |section, perLine = 1|
		var lines = section.asEvents.collect { |event| "\t" ++ this.prEventSource(event) };
		^"Pseq([\n" ++ lines.join(",\n") ++ "\n], 1)"
	}

	*prEventSource { |event|
		var pairs = [];
		[\dur, \sustain, \midinote, \velocity, \chan, \instrument, \type].do { |key|
			var value = event[key];
			if(value.notNil) {
				pairs = pairs.add(key.asString ++ ": " ++ this.prValueSource(value));
			};
		};
		^"(" ++ pairs.join(", ") ++ ")"
	}

	*prValueSource { |value|
		if(value.isKindOf(Float)) { ^value.round(1e-6).asCompileString };
		^value.asCompileString
	}

	// The rules, as a Pbind. Only for objects whose slots are per parameter, so a
	// data section or a bind; anything else exports its events instead.
	*pbindSource { |object|
		var spec = object.spec;
		var unit, lines;
		if(this.canWritePbind(object).not) { ^this.eventSource(object) };
		unit = PT.scalar(spec.value(\clock, 100)) / 1000;
		lines = [
			"\t\\dur, (%) * %".format(this.prSlotSource(spec, \rhythm, "1"), unit),
			"\t\\midinote, %".format(this.prSlotSource(spec, \pitch, "60")),
			"\t\\velocity, %".format(this.prSlotSource(spec, \velocity, "64")),
			"\t\\amp, Pkey(\\velocity) / 127",
			"\t\\chan, %".format(this.prSlotSource(spec, \channel, "1")),
			"\t\\instrument, %".format(this.prSlotSource(spec, \instrument, "\\default"))
		];
		^"Pbind(\n" ++ lines.join(",\n") ++ "\n)"
	}

	*canWritePbind { |object|
		^object.isKindOf(PTDataSection) or: { object.isKindOf(PTBind) }
	}

	// Slot text turned into standalone sclang: pitch and dynamic names become
	// numbers, referenced objects become their values, and a bare token list
	// becomes the cycling Pseq that the toolbox reads it as.
	*prSlotSource { |spec, key, fallback|
		var text = spec.at(key);
		if(text.isNil or: { text.stripWhiteSpace.isEmpty }) { text = fallback };
		text = PT.listifySource(PT.resolve(text, inlineObjects: true));
		if(text.beginsWith("[")) { text = "Pseq(" ++ text ++ ", inf)" };
		^text
	}

	// --------------------------------------------------------------- rendering

	*score { |section, duration, tail = 1|
		var length = duration ?? { section.duration + tail };
		var score = section.asPattern.asScore(length);
		var defs = this.prSynthDefBundles(section);
		// Pattern:asScore emits no /d_recv at all, and an offline server knows no
		// SynthDefs, so every instrument the section names has to travel with the
		// score. Measured: without this the render comes out the right length and
		// completely silent. Set through score_ rather than Score.new, which would
		// add a second /g_new.
		if(defs.notEmpty) { score.score = defs ++ score.score };
		^score
	}

	*prSynthDefBundles { |section|
		var names = section.asEvents.collect { |event| (event[\instrument] ? \default).asSymbol };
		^names.as(Set).asArray.collect { |name|
			var desc = SynthDescLib.global[name];
			if(desc.notNil and: { desc.def.notNil }) {
				[0.0, ['/d_recv', desc.def.asBytes]]
			} {
				"PT: no SynthDef named % for the offline render".format(name).warn;
				nil
			}
		}.reject(_.isNil)
	}

	// Render offline, faster than real time, to a sound file.
	*render { |section, path, duration, headerFormat = "WAV", sampleFormat = "int24",
		sampleRate = 48000, tail = 1|

		var score = this.score(section, duration, tail);
		path = path.standardizePath;
		score.recordNRT(
			outputFilePath: path,
			headerFormat: headerFormat,
			sampleFormat: sampleFormat,
			sampleRate: sampleRate,
			options: ServerOptions.new.numOutputBusChannels_(2)
		);
		if(PT.verbose) { "PT: rendering % to %".format(section.name, path).postln };
		^path
	}

	// ---------------------------------------------------------------- midi out

	// The same events as MIDI notes. Untested against hardware here: it is built
	// on SuperCollider's own \midi event type, so it is as correct as that is.
	*midiPattern { |section, device|
		var out = device ?? { PT.midiOut };
		if(out.isNil) {
			Error("PT: no MIDI output. Set one with PT.midiOut_(MIDIOut(0)).").throw
		};
		^Pseq(
			section.asEvents.collect { |event|
				var copy = event.copy;
				copy[\type] = if(event[\type] == \rest) { \rest } { \midi };
				copy[\midiout] = out;
				copy[\midicmd] = \noteOn;
				copy[\amp] = (copy[\velocity] ? 64) / 127;
				copy
			},
			1
		)
	}
}
