// PTMask - two lines: a field that changes over time.
//
// G. M. Koenig's tendency mask, as adapted in Project 2 and then by the AC
// Toolbox. Events happen in the area between the lines, which requires
// interpretation: converting a mask picks a value between its boundaries at each
// point in time.
//
//   mask1.convert(100, c3, c5)                       100 pitches inside the field
//   mask1.convert(100, c3, c5, PTbeta(0, 100, 0.1))  hugging the boundaries
//   PT.readFrom(cmajor, mask1, 100)                  constrained to a stockpile
//
// The two lines are scaled together, so the lowest point anywhere in the mask
// becomes lo and the highest becomes hi.

PTMask : PTObject {

	*ptType { ^\mask }

	*slotSpecs {
		^[
			[\top, "", "the upper line: points, a list expression, or a pattern"],
			[\bottom, "", "the lower line"],
			[\number, "", "how many points to draw (required for patterns)"]
		]
	}

	*specify { |name, top, bottom| ^this.new(name, (top: top, bottom: bottom)).make }

	*generate { |name, top, bottom, number|
		^this.new(name, (top: top, bottom: bottom, number: number)).make
	}

	*draw { |name|
		^this.new(name ?? { \mask1 }, (
			top: PTCurve.flat(75).asCompileString,
			bottom: PTCurve.flat(25).asCompileString
		)).make.draw
	}

	realize {
		var number = if(spec.isEmptyAt(\number)) { nil } { PT.scalar(spec.value(\number)).asInteger };
		^[\top, \bottom].collect { |key|
			var source = spec.valueAsList(key);
			if(source.isNil) {
				PTCurve.flat(if(key == \top) { 75 } { 25 })
			} {
				PT.collectValues(source, number).collect(_.asFloat)
			}
		}
	}

	// ------------------------------------------------------------- conversion

	top { ^this.prRequireMade.value[0] }
	bottom { ^this.prRequireMade.value[1] }

	// n values, each chosen between the boundaries. positions is a 0 to 100
	// source describing where between the lower and upper line to land; the
	// default is uniform.
	convert { |n, lo = 0, hi = 1, positions, round|
		^PTCurve.convertMask(
			this.top, this.bottom,
			n ?? { PT.fromNumber ? 100 },
			lo, hi, positions, round
		)
	}

	asPattern { |n, lo = 0, hi = 1, positions, round|
		^Pseq(this.convert(n, lo, hi, positions, round), inf)
	}

	indices { |size, n, positions|
		^PTCurve.readIndices(this.value, size, n ?? { 100 }, positions)
	}

	// The width of the field at each point, in the given range. Useful for
	// relating density or dynamics to how open the mask is.
	spread { |n, lo = 0, hi = 1|
		var upper = PTCurve.sample(this.top, n);
		var lower = PTCurve.sample(this.bottom, n);
		var min = min(upper.minItem, lower.minItem);
		var max = max(upper.maxItem, lower.maxItem);
		upper = PTCurve.scale(upper, lo, hi, min, max);
		lower = PTCurve.scale(lower, lo, hi, min, max);
		^PTCurve.roundToRange(n.collect { |i| (upper[i] - lower[i]).abs }, lo, hi)
	}

	asPTValue { ^this }

	asStream { ^PT.asStream(this.convert(100, 0, 1)) }

	// ------------------------------------------------------------------ display

	plot { ^this.value.flop.plot(name.asString) }

	// edit gives the text dialog like any other object; draw gives the mouse.
	draw { ^PTCurveEditor(this) }

	length { ^if(value.isNil) { 0 } { value[0].size } }
}
