// PTObject - the abstract base of every Pattern Toolbox object.
//
// The central idea, taken from the AC Toolbox: an object records BOTH the rules
// by which it was specified (spec) and one specific result of applying those
// rules (value). Make runs the rules. Variant runs the same rules into a new
// name. Unlike the AC Toolbox, the random seed of each realization is stored,
// so any result can be reproduced exactly.

PTObject {
	var <name;
	var <>spec;
	var <value;      // the realization; nil until made
	var <seed;       // the randSeed that produced value
	var <madeAt;
	var <>comment;

	*ptType { ^this.subclassResponsibility(thisMethod) }
	*slotSpecs { ^[] }

	*new { |name, values|
		^super.new.initPTObject(name, values)
	}

	initPTObject { |argName, values|
		name = argName.asSymbol;
		spec = PTSpec(this.class.slotSpecs, values);
		comment = "";
		PT.register(this);
	}

	type { ^this.class.ptType }

	// ------------------------------------------------------------------- make

	// Make with a fresh seed, as clicking Make in the AC Toolbox does.
	// Passing a seed reproduces an earlier realization exactly.
	make { |argSeed|
		seed = argSeed ?? { PT.newSeed };
		value = this.prSeeded { this.realize };
		madeAt = Date.getDate.stamp;
		PT.refresh(this);
		this.changed(\made);
		PT.changed(\made);
		^this
	}

	reproduce { ^this.make(seed) }

	// Store the rules without applying them (AC Toolbox: Cmd-click Make).
	specify {
		value = nil;
		PT.refresh(this);
		this.changed(\made);
		PT.changed(\made);
		^this
	}

	// Re-run the rules in place, discarding the old result. This is what a
	// PTScheme does to each of its members.
	remake { ^this.make }

	// Same rules, new name, new result.
	variant { |newName|
		var object = this.class.new(newName ?? { PT.uniqueName(name) });
		object.spec_(spec.copy);
		object.comment_(comment);
		^object.make
	}

	// Subclasses do their work here and return the realized value.
	realize { ^this.subclassResponsibility(thisMethod) }

	// Run func with a thread-local random seed, so realization is reproducible
	// without disturbing the interpreter's own random state.
	prSeeded { |func|
		var result, routine;
		routine = Routine { result = func.value };
		routine.randSeed = seed;
		routine.next;
		^result
	}

	isMade { ^value.notNil }

	// -------------------------------------------------------------- interface

	// What ~name resolves to inside another object's spec.
	asPTValue { ^this }

	asStream { ^PT.asStream(this.asPTValue) }

	asArray { ^this.prRequireMade.value }

	// Playable objects override this.
	asEvents { ^nil }

	asPattern {
		var events = this.asEvents;
		if(events.isNil) { ^nil };
		^Pseq(events.collect(_.copy), 1)
	}

	play { |repeats = 1|
		var pattern = this.asPattern;
		if(pattern.isNil) {
			"PT: % is not playable".format(name).warn; ^nil
		};
		^Pdef(name, if(repeats == 1) { pattern } { Pn(pattern, repeats) }).play(PT.clock)
	}

	stop { ^Pdef(name).stop }

	prRequireMade {
		if(value.isNil) {
			Error("PT: object % has not been made yet".format(name)).throw
		};
		^this
	}

	// --------------------------------------------------------------------- gui

	edit { ^PTObjectView(this) }

	plot { "PT: % cannot be plotted".format(name).warn; ^nil }

	// --------------------------------------------------------------- reporting

	length { ^if(value.isNil) { 0 } { value.size } }

	info {
		^(name: name, type: this.type, made: this.isMade,
			length: this.length, seed: seed, madeAt: madeAt, comment: comment)
	}

	postInfo {
		"  %  %  [%]  seed: %  %".format(
			name.asString.padRight(20),
			this.type.asString.padRight(12),
			this.length,
			seed ? "-",
			comment
		).postln;
	}

	postSpec {
		"% (%)".format(name, this.type).postln;
		spec.postSpec;
		if(comment.size > 0) { ("// " ++ comment).postln };
		^this
	}

	// What gets written to an archive. Usually the spec as it stands, but an
	// object whose realization creates other objects may need to freeze something.
	prCodeSpec { ^spec }

	asCode {
		^"PT.def(%, %, %, seed: %, comment: %);".format(
			name.asCompileString,
			this.class.name,
			this.prCodeSpec.asCode,
			seed.asCompileString,
			comment.asCompileString
		)
	}

	printOn { |stream|
		stream << this.class.name << "(" << name << ")"
	}
}
