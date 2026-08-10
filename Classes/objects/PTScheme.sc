// PTScheme - a script for remaking several objects in a fixed order.
//
// Two reasons to have one, both from the AC Toolbox: convenience and design.
// Convenience, because a section that reads a generated shape needs the shape
// remade first, and doing that by hand every time is tedious. Design, because the
// order in which things are remade is itself a compositional decision.
//
//   PTScheme(\round, (
//       members: "curve theme themeUp",
//       reset:   "howMany"
//   )).make.apply;
//
// Making the scheme resolves the list. Applying it remakes each member in turn,
// which discards their previous results, after resetting any controllers named in
// the reset slot: without that, controllers would carry on from where they were
// and a second pass would not repeat the first.

PTScheme : PTObject {

	*ptType { ^\scheme }

	*slotSpecs {
		^[
			[\members, "", "objects to remake, in order"],
			[\reset, "", "controllers to reset before each pass, if any"]
		]
	}

	*of { |name ... members|
		^this.new(name, (members: members.collect(_.asString).join(" "))).make
	}

	realize { ^this.prNames(\members) }

	prNames { |key|
		var listed = spec.valueAsList(key);
		if(listed.isNil) { ^[] };
		if(listed.isKindOf(PTObject)) { ^[listed.name] };
		^listed.asArray.collect { |item|
			if(item.isKindOf(PTObject)) { item.name } { item.asSymbol }
		}
	}

	names { ^this.prRequireMade.value }

	objects { ^this.names.collect { |n| PT(n) }.reject(_.isNil) }

	controllers { ^this.prNames(\reset).collect { |n| PT(n) }.reject(_.isNil) }

	size { ^this.names.size }

	canApply { ^true }

	// Run the script: reset the controllers, then remake every member in order.
	apply {
		var made;
		this.controllers.do(_.reset);
		made = this.objects;
		made.do { |object| object.remake };
		if(PT.verbose) {
			"PT: scheme % remade %".format(name, made.collect(_.name)).postln;
		};
		^made
	}

	// Apply the scheme several times, collecting what each pass produced. Useful
	// with a controller that walks through a series: each pass is one step of it.
	applyTimes { |times = 1, func|
		^times.collect { |i|
			var made = this.apply;
			func.value(made, i);
			made
		}
	}

	asPTValue { ^this }

	postValues { this.objects.do(_.postInfo); ^this }
}
