// PTController - a stream that keeps its state outside any section.
//
// A generator written into a section's rules knows nothing about what happened in
// the last section: every make builds it afresh. A controller does remember. That
// is what makes it possible to say something about the relation between one
// section and the next rather than only within one.
//
//   PTController(\howMany, (source: "5 10 20")).make;
//   PTDataSection(\bit, (number: "howMany", pitch: "Pwhite(c3, c5)")).specify;
//   3.do { PT(\bit).make.play };        // 5 notes, then 10, then 20
//
// Used bare in a slot, a controller supplies a value every time that slot is
// calculated. PT.takeOne pulls exactly one value and hands that single value to a
// generator, so it holds for the whole section:
//
//   pitch: "PTbeta(PT.takeOne(low), PT.takeOne(high), 0.3, 0.3)"
//
// Two controllers can be synchronised to a third, so both see the same value and
// two parameters stay in step. Whichever asks first makes the master produce;
// the other gets that same value rather than a new one.
//
// The whole history is reproducible from the seed, because the values are pulled
// inside a thread whose random state was seeded at reset.

PTController : PTObject {
	var stream;
	var <generation = 0;     // how many values the master has produced
	var <currentValue;
	var lastGeneration = 0;  // for a synchronised controller: what it last saw

	*ptType { ^\controller }

	*slotSpecs {
		^[
			[\source, "", "a list, a stockpile, or a generator"],
			[\syncTo, "", "another controller to stay in step with, if any"]
		]
	}

	*of { |name, source| ^this.new(name, (source: source)).make }

	*syncedTo { |name, master| ^this.new(name, (syncTo: master.asString)).make }

	// Making a controller resets it. Its value is the history, which grows as the
	// controller is used, so it is the one object whose value changes outside make.
	realize {
		var source, inner;
		generation = 0;
		lastGeneration = 0;
		currentValue = nil;
		stream = nil;
		if(this.master.isNil) {
			source = spec.valueAsList(\source);
			if(source.isNil) {
				Error("PT: controller % has neither a source nor a syncTo".format(name)).throw
			};
			// A list source cycles, so a finite pattern must cycle too: a controller
			// is asked for values an unknown number of times and must never run dry.
			inner = if(source.isKindOf(Pattern)) {
				Pn(source, inf).asStream
			} {
				PT.asStream(source)
			};
			// pulling happens inside a seeded thread, so the history is reproducible
			stream = Routine { loop { inner.next.yield } };
			stream.randSeed = seed;
		};
		^[]
	}

	master {
		if(spec.isEmptyAt(\syncTo)) { ^nil };
		^spec.value(\syncTo)
	}

	// ------------------------------------------------------------- the values

	// The next value, recorded in the history.
	next {
		var result = this.prProduce;
		value = (value ? []).add(result);
		PT.refresh(this);
		^result
	}

	prProduce {
		var owner = this.master;
		if(owner.notNil) {
			if(owner.isKindOf(PTController).not) {
				Error("PT: % is not a controller".format(owner)).throw
			};
			// only advance the master if this follower has already seen its value
			if(lastGeneration >= owner.generation) { owner.advance };
			lastGeneration = owner.generation;
			^owner.currentValue
		};
		^this.advance
	}

	// Master side: produce one new value and count it.
	advance {
		if(stream.isNil) { this.make };
		currentValue = stream.next;
		if(currentValue.isNil) {
			"PT: controller % produced nothing; check its source".format(name).warn;
		};
		generation = generation + 1;
		^currentValue
	}

	// The AC Toolbox take-one: one value, for the whole section.
	takeOne { ^this.next }

	reset { ^this.make(seed) }

	history { ^value ? [] }

	// Used bare in a slot, a controller supplies a value per calculation.
	asStream { ^Pfunc({ this.next }).asStream }

	asPTValue { ^this }

	// ------------------------------------------------------------------ display

	plot { ^this.history.plot(name.asString) }

	postValues { |perLine = 10|
		if(this.history.isEmpty) { "% : no values yet".format(name).postln; ^this };
		this.history.clump(perLine).do { |row|
			row.collect { |v| v.asString.padLeft(8) }.join(" ").postln;
		};
		^this
	}
}
