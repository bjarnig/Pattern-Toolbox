// PTBind - the live object. Rules only, no frozen result.
//
// Everything else in the toolbox is the AC Toolbox model: rules plus one specific
// realization of them. A bind is the half SuperCollider adds. It has no output at
// all; it is a Pbind that plays through a Pdef named after itself, so editing it
// while it sounds takes effect on the next cycle.
//
//   PTBind(\live, (clock: "150", rhythm: "Prand([1, 1, 2], inf)",
//                  pitch: "Prand(cmajor, inf)")).make.play;
//   // edit a slot, press make, and it changes underneath you
//   PT(\live).capture(\kept, 64);   // freeze what it is doing into a section
//   PT(\live).stop;
//
// The slots are a data section's, minus number: a bind does not end.

PTBind : PTObject {

	*ptType { ^\bind }

	*slotSpecs {
		^[
			[\clock, "100", "the basic beat in milliseconds"],
			[\rhythm, "1", "multiple of the clock unit, negative for a rest"],
			[\pitch, "60", "midi note number, pitch name, or array for a chord"],
			[\velocity, "\\mf", "0-127 or a dynamic symbol"],
			[\channel, "1", "midi channel"],
			[\instrument, "\\default", "SynthDef name"],
			[\extra, "", "further Pbind pairs, as an Event such as (pan: Pwhite(-1, 1))"]
		]
	}

	*of { |name, pitch, rhythm = "1", clock = "100"|
		^this.new(name, (pitch: pitch, rhythm: rhythm, clock: clock)).make
	}

	// The realization of a bind is the pattern itself, not a list of events.
	realize {
		var unit = PT.scalar(spec.value(\clock, 100)) / 1000;
		var extra = spec.value(\extra);
		var pairs = [
			// the raw rhythm is kept as a key so it can be read twice without
			// being pulled twice: once for the length, once to spot a rest
			\ptRhythm, PT.asPattern(spec.valueAsList(\rhythm, 1)),
			\dur, Pkey(\ptRhythm).abs * unit,
			\type, Pkey(\ptRhythm).collect { |r| if((r ? 1) < 0) { \rest } { \note } },
			\midinote, PT.asPatternMapped(spec.valueAsList(\pitch, 60), { |p| PT.midinote(p) }),
			\velocity, PT.asPatternMapped(spec.valueAsList(\velocity, \mf), { |v| PT.velocity(v) }),
			\amp, Pkey(\velocity) / 127,
			\chan, PT.asPattern(spec.valueAsList(\channel, 1)),
			\instrument, PT.scalar(spec.value(\instrument, \default)).asSymbol
		];
		if(extra.isKindOf(Event)) {
			extra.keysValuesDo { |key, value| pairs = pairs ++ [key, value] };
		};
		^Pbind(*pairs)
	}

	pattern { ^this.prRequireMade.value }

	asPattern { ^this.pattern }

	// A bind has no events, so it is not a section and cannot be plotted or
	// sliced. Capture it first.
	asEvents { ^nil }

	length { ^0 }

	// Pdef means an edit lands on the next cycle rather than restarting.
	play { |quant|
		^Pdef(name, this.pattern).quant_(quant).play(PT.clock)
	}

	stop { ^Pdef(name).stop }

	isPlaying { ^Pdef(name).isPlaying }

	// Freeze what it is doing into a section, as its own reproducible object.
	capture { |captureName, number = 64|
		^PTCapture(
			captureName ?? { (name.asString ++ "Kept").asSymbol },
			(source: name.asString, number: number.asString)
		).make
	}

	plot { "PT: % is live and has no events; capture it first".format(name).warn; ^nil }
}


// PTCapture - a section frozen out of a bind, or out of any pattern.
//
// The capture keeps its rules, so it can be remade for a different stretch of the
// same live material, and its seed makes any single capture reproducible.
PTCapture : PTSection {

	*ptType { ^\section }

	*slotSpecs {
		^[
			[\source, "", "a bind, or any pattern producing events"],
			[\number, "64", "how many events to take"]
		]
	}

	realize {
		var source = spec.value(\source);
		var count = PT.scalar(spec.value(\number, 64)).asInteger;
		var pattern, stream, events;

		if(source.isNil) { ^[] };
		pattern = if(source.isKindOf(PTBind)) { source.pattern } { PT.asPattern(source) };
		if(pattern.isKindOf(Pattern).not) {
			Error("PT: % is not something that produces events".format(spec.at(\source))).throw
		};

		stream = pattern.asStream;
		events = Array.new(count);
		count.do {
			var event = stream.next(());
			if(event.isNil) { ^events };
			events = events.add(PTCapture.tidy(event));
		};
		^events
	}

	// Fill in what a section needs and drop the bookkeeping, but keep everything
	// else the bind set, so an extra key such as pan survives being frozen.
	*tidy { |event|
		var out = event.copy;
		out.removeAt(\ptRhythm);
		out[\dur] = out[\dur] ? 0;
		out[\sustain] = out[\sustain] ? out[\dur];
		out[\midinote] = out[\midinote] ? 60;
		out[\velocity] = out[\velocity] ? 64;
		out[\chan] = out[\chan] ? 1;
		out[\instrument] = out[\instrument] ? \default;
		^out
	}
}
