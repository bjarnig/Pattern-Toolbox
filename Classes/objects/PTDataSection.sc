// PTDataSection - the workhorse. Each parameter is specified separately and
// calculated independently of the others, exactly as in an AC Toolbox data
// section.
//
//   clock       the basic beat in milliseconds. PT.bpm(160) also works.
//   number      how many notes, or PT.untilTime(seconds)
//   rhythm      a multiple of the clock unit. Negative values are rests.
//   pitch       a midi note number, a pitch name, or an array for a chord
//   velocity    0-127 or a dynamic symbol
//   channel     midi channel, kept for export
//   instrument  the SynthDef to play with

PTDataSection : PTSection {

	*ptType { ^\section }

	*slotSpecs {
		^[
			[\clock, "100", "the basic beat in milliseconds"],
			[\number, "50", "number of notes, or PT.untilTime(seconds)"],
			[\rhythm, "1", "multiple of the clock unit, negative for a rest"],
			[\pitch, "60", "midi note number, pitch name, or array for a chord"],
			[\velocity, "\\mf", "0-127 or a dynamic symbol"],
			[\channel, "1", "midi channel"],
			[\instrument, "\\default", "SynthDef name used for playback"]
		]
	}

	realize {
		var unit, numberSpec, limit, count;
		var rhythms, pitches, velocities, channels, instrument;
		var events, elapsed = 0, index = 0;

		unit = PT.scalar(spec.value(\clock, 100)) / 1000;   // seconds per clock unit
		numberSpec = PT.scalar(spec.value(\number, 0));

		if(numberSpec.isKindOf(PTUntilTime)) {
			limit = numberSpec.seconds;
			count = inf;
		} {
			count = numberSpec.asInteger;
		};

		// tools such as PT.fromNumber need to know how many notes are coming
		PT.prSetCurrentNumber(if(count == inf) { nil } { count });

		rhythms = PT.asStream(spec.valueAsList(\rhythm, 1));
		pitches = PT.asStream(spec.valueAsList(\pitch, 60));
		velocities = PT.asStream(spec.valueAsList(\velocity, \mf));
		channels = PT.asStream(spec.valueAsList(\channel, 1));
		instrument = PT.scalar(spec.value(\instrument, \default));

		events = Array.new(if(count == inf) { 256 } { count });

		while {
			(index < count) and: { limit.isNil or: { elapsed < (limit - 1e-9) } }
		} {
			var rhythm, pitch, velocity, channel, event, duration;
			rhythm = rhythms.next;
			pitch = pitches.next;
			velocity = velocities.next;
			channel = channels.next;
			if([rhythm, pitch, velocity, channel].any(_.isNil)) {
				^events   // a source ran dry: the section stops there
			};
			duration = rhythm.abs * unit;
			event = (
				dur: duration,
				midinote: PT.midinote(pitch),
				velocity: PT.velocity(velocity),
				chan: channel,
				instrument: instrument.asSymbol
			);
			if(rhythm < 0) { event[\type] = \rest };
			events = events.add(event);
			elapsed = elapsed + duration;
			index = index + 1;
		};

		PT.prSetCurrentNumber(nil);
		^events
	}
}
