// PTbeta - beta distributed values between two bounds.
//
// The AC Toolbox beta-value, which its documentation reaches for constantly when
// converting masks. With both shape parameters below 1 the values cluster at the
// two boundaries, so a converted mask follows its outline instead of filling
// evenly; above 1 they cluster in the middle.
//
//   PTbeta(0.0, 100, 0.1, 0.1)   hugs the edges: the mask reads as two lines
//   PTbeta(0.0, 100, 3, 3)       hugs the centre
//
// Integer bounds give integer results, following the rest of the toolbox.

PTbeta : Pattern {
	var <>lo, <>hi, <>prob1, <>prob2, <>length;

	*new { |lo = 0.0, hi = 1.0, prob1 = 0.5, prob2 = 0.5, length = inf|
		^super.newCopyArgs(lo, hi, prob1, prob2, length)
	}

	storeArgs { ^[lo, hi, prob1, prob2, length] }

	// Johnk's method: cheap and exact for the small shape parameters that make
	// this generator interesting. The iteration cap keeps a pathological
	// parameter pair from hanging the interpreter.
	*unitValue { |prob1 = 0.5, prob2 = 0.5|
		var u, v, x, y, sum, tries = 0;
		prob1 = max(prob1, 1e-6);
		prob2 = max(prob2, 1e-6);
		while { tries < 1000 } {
			u = 1.0.rand;
			v = 1.0.rand;
			if((u > 0) and: { v > 0 }) {
				x = u ** (1 / prob1);
				y = v ** (1 / prob2);
				sum = x + y;
				if((sum <= 1) and: { sum > 0 }) { ^x / sum };
			};
			tries = tries + 1;
		};
		^0.5
	}

	*value { |lo = 0.0, hi = 1.0, prob1 = 0.5, prob2 = 0.5|
		var result = lo + ((hi - lo) * this.unitValue(prob1, prob2));
		if(lo.isInteger and: { hi.isInteger }) { ^result.round(1).asInteger };
		^result
	}

	embedInStream { |inval|
		length.do {
			inval = PTbeta.value(lo, hi, prob1, prob2).yield;
		};
		^inval
	}
}
