// PTShape - a curve, some motion over time.
//
// A shape carries no scale of its own. It is a contour, and it means nothing
// until it is converted into a range:
//
//   shape1.convert(100, c3, c5)      100 pitches following the contour
//   PT.readFrom(cmajor, shape1, 60)  60 notes read from a stockpile in that order
//
// Three ways to make one, all through the same source slot:
//
//   "100 80 60 40 60 80 100"     specified, evenly spaced points
//   "Pseries(0, 1)" + number     generated
//   drawn with the mouse         the editor writes the point array into source

PTShape : PTObject {

	*ptType { ^\shape }

	*slotSpecs {
		^[
			[\source, "", "points, an expression producing a list, or a pattern"],
			[\number, "", "how many points to draw (required for patterns)"]
		]
	}

	*specify { |name, points| ^this.new(name, (source: points)).make }

	*generate { |name, generator, number|
		^this.new(name, (source: generator, number: number)).make
	}

	// Open the drawing editor on a new, empty shape.
	*draw { |name|
		^this.new(name ?? { \shape1 }, (source: PTCurve.flat.asCompileString)).make.draw
	}

	realize {
		var source = spec.valueAsList(\source);
		var number = if(spec.isEmptyAt(\number)) { nil } { spec.value(\number).asInteger };
		if(source.isNil) { ^PTCurve.flat };
		^PT.collectValues(source, number).collect(_.asFloat)
	}

	// ------------------------------------------------------------- conversion

	points { ^this.prRequireMade.value }

	// n values following the contour, mapped into lo..hi. Integer bounds give
	// integer results, so convert(100, c3, c5) yields note numbers.
	convert { |n, lo = 0, hi = 1, round|
		^PTCurve.convertShape(this.points, n ?? { PT.fromNumber ? 100 }, lo, hi, round)
	}

	// The same, as a pattern, for use where a stream is wanted.
	asPattern { |n, lo = 0, hi = 1, round|
		^Pseq(this.convert(n, lo, hi, round), inf)
	}

	// Reading order over a collection of the given size.
	indices { |size, n| ^PTCurve.readIndices(this.points, size, n ?? { 100 }) }

	// A shape used bare in a slot is its contour.
	asPTValue { ^this }

	asStream { ^PT.asStream(this.points) }

	// ------------------------------------------------------------------ display

	plot { ^this.points.plot(name.asString, minval: this.points.minItem, maxval: this.points.maxItem) }

	// edit gives the text dialog like any other object; draw gives the mouse.
	draw { ^PTCurveEditor(this) }

	length { ^if(value.isNil) { 0 } { value.size } }
}
