// PTCurve - the arithmetic behind shapes and masks.
//
// All pure class methods, so the conversions can be tested without a display
// and without building objects. PTShape and PTMask are thin wrappers over this.
//
// A curve is a plain Array of numbers in any range. What matters is its contour;
// its absolute values are meaningless until it is converted.

PTCurve {
	classvar <>drawResolution = 100;   // points captured by the drawing editors

	// Resample a curve to n points by linear interpolation along its length.
	*sample { |points, n|
		if(points.isNil or: { points.isEmpty }) { ^Array.fill(n, 0) };
		if(n <= 0) { ^[] };
		if(n == 1) { ^[points[0]] };
		if(points.size == 1) { ^Array.fill(n, points[0]) };
		^points.resamp1(n)
	}

	// Map a curve into lo..hi, preserving its shape. A flat curve lands in the
	// middle of the range rather than collapsing onto one edge.
	*scale { |points, lo, hi, sourceMin, sourceMax|
		var min = sourceMin ?? { points.minItem };
		var max = sourceMax ?? { points.maxItem };
		if(min == max) { ^Array.fill(points.size, (lo + hi) / 2) };
		^points.collect { |value| value.linlin(min, max, lo, hi) }
	}

	// If the caller asked for an integer range and did not ask for a rounding
	// unit, round to whole numbers. This is the AC Toolbox rule: integers in,
	// integers out, which is what keeps pitch conversions usable.
	*roundToRange { |values, lo, hi, round|
		if(round.notNil) { ^values.collect { |v| v.round(round) } };
		if(lo.isInteger and: { hi.isInteger }) { ^values.collect { |v| v.round(1).asInteger } };
		^values
	}

	// One line: n values following the contour, mapped into lo..hi.
	*convertShape { |points, n, lo = 0, hi = 1, round|
		var sampled = this.sample(points, n);
		^this.roundToRange(this.scale(sampled, lo, hi), lo, hi, round)
	}

	// Two lines: n values, each chosen between the boundaries at that point.
	// Both lines are scaled together, so the mask keeps its shape: the lowest
	// point anywhere becomes lo and the highest becomes hi.
	//
	// positions is a stream of 0..100, a position between the lower boundary (0)
	// and the upper one (100). The default is uniform, as in the AC Toolbox.
	*convertMask { |top, bottom, n, lo = 0, hi = 1, positions, round|
		var upper = this.sample(top, n);
		var lower = this.sample(bottom, n);
		var min = min(upper.minItem, lower.minItem);
		var max = max(upper.maxItem, lower.maxItem);
		var stream = PT.asStream(positions ?? { Pwhite(0.0, 100.0) });
		var values;
		upper = this.scale(upper, lo, hi, min, max);
		lower = this.scale(lower, lo, hi, min, max);
		values = n.collect { |i|
			var a = min(upper[i], lower[i]);
			var b = max(upper[i], lower[i]);
			var position = (stream.next ? 50) / 100;
			a + ((b - a) * position)
		};
		^this.roundToRange(values, lo, hi, round)
	}

	// Use a curve as a reading order over a collection: the lowest point of the
	// curve reads the first element, the highest the last. This is read-from.
	*readIndices { |points, size, n, positions|
		var values;
		if(size < 1) { ^[] };
		values = if(points.isKindOf(Array) and: { points.size == 2 }
			and: { points[0].isKindOf(Array) }) {
			this.convertMask(points[0], points[1], n, 0, size - 1, positions)
		} {
			this.convertShape(points, n, 0, size - 1)
		};
		^values.collect { |v| v.round(1).asInteger.clip(0, size - 1) }
	}

	// A flat line, the starting state of the drawing editors.
	*flat { |value = 50, size| ^Array.fill(size ?? { drawResolution }, value) }

	// Fill in the gap left by a fast mouse drag between two captured points.
	*interpolateInto { |points, fromIndex, fromValue, toIndex, toValue|
		var step, count;
		if(fromIndex.isNil) { points[toIndex] = toValue; ^points };
		if(fromIndex == toIndex) { points[toIndex] = toValue; ^points };
		count = (toIndex - fromIndex).abs;
		step = if(toIndex > fromIndex) { 1 } { -1 };
		count.do { |i|
			var index = fromIndex + (step * (i + 1));
			points[index] = fromValue.blend(toValue, (i + 1) / count);
		};
		^points
	}
}
