// PTMidiFile - write a section as a standard MIDI file.
//
// Self-contained, so the quark carries no dependency for it. Format 0, one track,
// with the division and tempo chosen so that one tick is exactly one millisecond:
// 1000 ticks to the quarter note and a quarter note of 1000000 microseconds. That
// removes all tick arithmetic, since our events already carry times in seconds.
//
//   PT(\section1).writeMidi("~/Desktop/sketch.mid");

PTMidiFile {
	classvar <ticksPerQuarter = 1000;
	classvar <microsecondsPerQuarter = 1000000;

	*write { |path, section, tail = 0|
		var events = section.asEvents;
		var messages, bytes;
		path = path.standardizePath;
		if(path.endsWith(".mid").not and: { path.endsWith(".midi").not }) {
			path = path ++ ".mid"
		};
		messages = this.prMessages(events);
		bytes = this.prHeader ++ this.prTrack(messages, tail);
		this.prWriteBytes(path, bytes);
		if(PT.verbose) {
			"PT: wrote % note(s) to %".format(messages.size div: 2, path).postln;
		};
		^path
	}

	// -------------------------------------------------------------- the bytes

	*prHeader {
		^[$M.ascii, $T.ascii, $h.ascii, $d.ascii]
			++ this.prInt32(6)
			++ this.prInt16(0)                 // format 0: one track
			++ this.prInt16(1)                 // one track
			++ this.prInt16(ticksPerQuarter)
	}

	// [tick, [status, data ...]] pairs, note on and note off for every note.
	*prMessages { |events|
		var time = 0;
		var messages = Array.new(events.size * 2);
		events.do { |event|
			var duration = event[\dur] ? 0;
			var length = event[\sustain] ? duration;
			var channel = ((event[\chan] ? 1) - 1).clip(0, 15).asInteger;
			var velocity = (event[\velocity] ? 64).round.asInteger.clip(1, 127);
			if(event[\type] != \rest) {
				(event[\midinote] ? 60).asArray.do { |pitch|
					var note = pitch.round.asInteger.clip(0, 127);
					var onTick = (time * 1000).round.asInteger;
					var offTick = ((time + max(length, 0.001)) * 1000).round.asInteger;
					messages = messages.add([onTick, [16r90 + channel, note, velocity]]);
					messages = messages.add([offTick, [16r80 + channel, note, 0]]);
				};
			};
			time = time + duration;
		};
		// note off before note on at the same tick, so a repeated pitch retriggers
		^messages.sort { |a, b|
			if(a[0] == b[0]) { (a[1][0] bitAnd: 16rF0) <= (b[1][0] bitAnd: 16rF0) } { a[0] < b[0] }
		}
	}

	*prTrack { |messages, tail = 0|
		var body = Array.new(messages.size * 5 + 16);
		var previous = 0;
		// tempo, so that a tick is a millisecond
		body = body ++ this.prVariable(0) ++ [16rFF, 16r51, 16r03]
			++ this.prInt24(microsecondsPerQuarter);
		messages.do { |message|
			body = body ++ this.prVariable(message[0] - previous) ++ message[1];
			previous = message[0];
		};
		body = body ++ this.prVariable((tail * 1000).round.asInteger) ++ [16rFF, 16r2F, 16r00];
		^[$M.ascii, $T.ascii, $r.ascii, $k.ascii] ++ this.prInt32(body.size) ++ body
	}

	// A delta time is a variable-length quantity: seven bits per byte, high bit
	// set on every byte but the last.
	*prVariable { |value|
		var out, buffer;
		value = max(value, 0).asInteger;
		if(value == 0) { ^[0] };
		buffer = Array.new(5);
		while { value > 0 } {
			buffer = buffer.add(value bitAnd: 16r7F);
			value = value >> 7;
		};
		out = Array.new(buffer.size);
		buffer.reverseDo { |seven, i|
			out = out.add(if(i < (buffer.size - 1)) { seven bitOr: 16r80 } { seven });
		};
		^out
	}

	*prInt16 { |value| ^[(value >> 8) bitAnd: 255, value bitAnd: 255] }

	*prInt24 { |value|
		^[(value >> 16) bitAnd: 255, (value >> 8) bitAnd: 255, value bitAnd: 255]
	}

	*prInt32 { |value|
		^[(value >> 24) bitAnd: 255, (value >> 16) bitAnd: 255,
			(value >> 8) bitAnd: 255, value bitAnd: 255]
	}

	*prWriteBytes { |path, bytes|
		var file = File(path, "wb");
		if(file.isOpen.not) { Error("PTMidiFile: cannot write %".format(path)).throw };
		protect { bytes.do { |byte| file.putInt8(byte) } } { file.close };
	}
}
