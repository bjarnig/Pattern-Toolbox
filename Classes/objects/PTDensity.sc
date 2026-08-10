// Density sections - where time comes first.
//
// A data section says "make this many notes and let the clock work out how long
// that takes". A density section inverts it: here is an amount of time, here is
// how thickly to fill it, now place the attacks. Time is in seconds and note
// durations are in milliseconds, since there is no clock unit to multiply.
//
//   PTDensity       attack points given as a percentage of the total time
//   PTDensityCurve  a shape or mask read as notes per second
//
// Both are the same underneath: work out a list of attack times in seconds, then
// hang a note on each one.

PTDensitySection : PTSection {

	*ptType { ^\section }

	*prCommonSlots {
		^[
			[\duration, "200", "note length in milliseconds"],
			[\pitch, "60", "midi note number, pitch name, or array for a chord"],
			[\velocity, "\\mf", "0-127 or a dynamic symbol"],
			[\channel, "1", "midi channel"],
			[\instrument, "\\default", "SynthDef name used for playback"]
		]
	}

	// Attack times in seconds; the subclass decides where they come from.
	prAttacks { |totalTime| ^this.subclassResponsibility(thisMethod) }

	realize {
		var totalTime = PT.scalar(spec.value(\time, 10));
		var attacks = this.prAttacks(totalTime).sort;
		var durations = PT.asStream(spec.valueAsList(\duration, 200));
		var pitches = PT.asStream(spec.valueAsList(\pitch, 60));
		var velocities = PT.asStream(spec.valueAsList(\velocity, \mf));
		var channels = PT.asStream(spec.valueAsList(\channel, 1));
		var instrument = PT.scalar(spec.value(\instrument, \default)).asSymbol;

		^PTCombination.flatten(
			attacks.collect { |time|
				var length = (durations.next ? 200) / 1000;
				[time, (
					dur: length, sustain: length,
					midinote: PT.midinote(pitches.next ? 60),
					velocity: PT.velocity(velocities.next ? 64),
					chan: channels.next ? 1,
					instrument: instrument
				)]
			}
		)
	}

	duration { ^max(PT.scalar(spec.value(\time, 10)), this.asEvents.sum { |e| e[\dur] }) }
}


// Attack points as a percentage of the total time: 0 is the beginning, 100 the
// end. A list of points sets the number of notes by its own size, whatever the
// number slot says. A generator is applied that many times and the results are
// sorted, so the density curve of the section is the density curve of the
// generator: PTbeta with small parameters crowds both ends, an exponential
// distribution crowds the beginning.
PTDensity : PTDensitySection {

	*slotSpecs {
		^[
			[\time, "10", "total time in seconds"],
			[\number, "20", "how many attacks, when they come from a generator"],
			[\attacks, "PT.attacks(1)", "attack points from 0 to 100, as a list or a generator"]
		] ++ this.prCommonSlots
	}

	prAttacks { |totalTime|
		var count = PT.scalar(spec.value(\number, 20)).asInteger;
		var points;
		// from-number must be answerable while the attacks slot is being evaluated
		PT.prSetCurrentNumber(count);
		points = spec.valueAsList(\attacks, 0);
		PT.prSetCurrentNumber(nil);

		if(points.isKindOf(SequenceableCollection) and: { points.isKindOf(Pattern).not }) {
			// a list decides the number itself
			points = points.asArray;
		} {
			points = PT.asStream(points).nextN(count);
		};
		^points.collect { |percent| (percent ? 0) / 100 * totalTime }
	}
}


// Density from a curve, read as notes per time unit between a minimum and a
// maximum. A shape gives the contour exactly; a mask gives a random count
// between its two lines at each step, so the density itself is a tendency.
PTDensityCurve : PTDensitySection {

	*slotSpecs {
		^[
			[\time, "10", "total time in seconds"],
			[\curve, "", "a shape or a mask"],
			[\min, "1", "fewest notes per time unit"],
			[\max, "20", "most notes per time unit"],
			[\unit, "1", "the time unit in seconds"]
		] ++ this.prCommonSlots
	}

	prAttacks { |totalTime|
		var curve = spec.value(\curve);
		var lowest = PT.scalar(spec.value(\min, 1));
		var highest = PT.scalar(spec.value(\max, 20));
		var unit = PT.scalar(spec.value(\unit, 1));
		var steps, densities, attacks = Array.new;

		if(curve.isNil) { Error("PT: density section % has no curve".format(name)).throw };
		steps = max((totalTime / unit).ceil.asInteger, 1);
		densities = PT.convert(curve, steps, lowest, highest);

		steps.do { |step|
			var count = (densities[step] ? 0).round.asInteger;
			var from = step * unit;
			var span = min(unit, totalTime - from);
			if(span > 0) {
				// span must be a Float here: an Integer span of 1 would make
				// span.rand always zero and stack every note on the step boundary
				count.do { attacks = attacks.add(from + (span * 1.0.rand)) };
			};
		};
		^attacks
	}
}
