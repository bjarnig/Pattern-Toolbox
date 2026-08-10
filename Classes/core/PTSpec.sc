// PTSpec - the input half of a Pattern Toolbox object.
//
// A spec is an ordered set of named slots whose values are always Strings: the
// literal text a user typed into a dialog field. Text is the truth. Nothing
// compiled is ever stored, so a spec round-trips to source without loss.

PTSpec {
	var <keys;      // Array of Symbol, in declaration order
	var <slots;     // Symbol -> String
	var <docs;      // Symbol -> String

	*new { |slotSpecs, values|
		^super.new.init(slotSpecs, values)
	}

	init { |slotSpecs, values|
		keys = Array.new(slotSpecs.size);
		slots = IdentityDictionary.new;
		docs = IdentityDictionary.new;
		slotSpecs.do { |triple|
			var key = triple[0].asSymbol;
			keys = keys.add(key);
			slots[key] = (triple[1] ? "").asString;
			docs[key] = (triple[2] ? "").asString;
		};
		if(values.notNil) { this.putAll(values) };
	}

	at { |key| ^slots[key.asSymbol] }

	put { |key, string|
		key = key.asSymbol;
		if(slots.includesKey(key).not) {
			Error("PTSpec: no slot named %".format(key)).throw
		};
		slots[key] = if(string.isNil) { "" } { string.asString };
		^this
	}

	putAll { |values|
		values.keysValuesDo { |key, string| this.put(key, string) };
		^this
	}

	docFor { |key| ^docs[key.asSymbol] }

	isEmptyAt { |key| ^this.at(key).stripWhiteSpace.isEmpty }

	// Compile and run one slot inside the toolbox environment.
	value { |key, default|
		var src = this.at(key);
		if(src.isNil) { Error("PTSpec: no slot named %".format(key)).throw };
		if(src.stripWhiteSpace.isEmpty) { ^default };
		^this.prRequire(key, src, this.prGuard(key) { PT.eval(src) })
	}

	// Same, but a bare token run is read as a list.
	valueAsList { |key, default|
		var src = this.at(key);
		if(src.isNil) { Error("PTSpec: no slot named %".format(key)).throw };
		if(src.stripWhiteSpace.isEmpty) { ^default };
		^this.prRequire(key, src, this.prGuard(key) { PT.evalList(src) })
	}

	// A slot that will not compile comes back nil from interpret without raising,
	// which used to leave an object made and empty with only a parse message in
	// the log. Fail where the mistake is instead.
	prRequire { |key, src, result|
		if(result.isNil) {
			Error("PT: slot '%' did not produce a value. Check its syntax:\n    %"
				.format(key, src)).throw
		};
		^result
	}

	prGuard { |key, func|
		^func.protect { |error|
			if(error.notNil) {
				"PT: slot '%' failed to evaluate: %".format(key, this.at(key)).error;
			}
		}
	}

	copy { ^PTSpec.new(this.asSlotSpecs, slots) }

	asSlotSpecs { ^keys.collect { |k| [k, slots[k], docs[k]] } }

	asEvent {
		var event = Event.new;
		keys.do { |k| event[k] = slots[k] };
		^event
	}

	asCode { |indent = "\t"|
		var out = "(\n";
		keys.do { |k| out = out ++ indent ++ k.asString ++ ": " ++ slots[k].asCompileString ++ ",\n" };
		^out ++ ")"
	}

	postSpec { |width = 12|
		keys.do { |k|
			(k.asString.padRight(width) ++ slots[k]).postln;
		};
	}

	printOn { |stream| stream << "PTSpec" << keys }
}
