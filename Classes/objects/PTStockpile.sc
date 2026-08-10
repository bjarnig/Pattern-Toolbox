// PTStockpile - a named collection of values.
//
// The AC Toolbox has three separate stockpile dialogs (specify, generate,
// construct). Here there is one slot, source, and the mode is inferred:
//
//   "60 62 64 65 67 69 71"      a bare list, as in specify-stockpile
//   "c4 d4 e4 f4 g4 a4 b4"      the same, with pitch names
//   "(0..11).scramble"          an expression, as in construct-stockpile
//   "Pwhite(60, 72)" + number   a pattern drawn n times, as in generate
//
// A stockpile binds to its Array, so ~c_major inside another spec is the list.

PTStockpile : PTObject {

	*ptType { ^\stockpile }

	*slotSpecs {
		^[
			[\source, "", "values, an expression producing a list, or a pattern"],
			[\number, "", "how many values to draw (required for patterns)"]
		]
	}

	*specify { |name, values| ^this.new(name, (source: values)).make }

	*generate { |name, generator, number|
		^this.new(name, (source: generator, number: number)).make
	}

	realize {
		var source = spec.valueAsList(\source);
		var number = if(spec.isEmptyAt(\number)) { nil } { PT.scalar(spec.value(\number)).asInteger };
		if(source.isNil) { ^[] };
		^PT.collectValues(source, number)
	}

	// A stockpile is used as its values, not as an object.
	asPTValue { ^value }

	asStream { ^PT.asStream(this.prRequireMade.value) }

	// ---------------------------------------------------------------- queries

	size { ^this.length }
	at { |index| ^this.prRequireMade.value[index] }
	minItem { ^this.prRequireMade.value.minItem }
	maxItem { ^this.prRequireMade.value.maxItem }

	plot { |name| ^this.prRequireMade.value.plot(name ?? { this.name.asString }) }

	postValues { |perLine = 10|
		this.prRequireMade;
		value.clump(perLine).do { |row|
			row.collect { |v| v.asString.padLeft(8) }.join(" ").postln;
		};
		^this
	}
}
