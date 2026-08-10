// Phase 0 test suite. Run with: TestPatternToolbox.run

TestPatternToolbox : UnitTest {

	setUp { PT.removeAll }

	prAscends { |array|
		^array.every { |value, i| (i == 0) or: { value >= array[i - 1] } }
	}

	// When each note begins, in seconds from the start of the section.
	prAttackTimes { |section|
		var time = 0, out = [];
		section.asEvents.do { |event|
			if(event[\type] != \rest) { out = out.add(time) };
			time = time + (event[\dur] ? 0);
		};
		^out
	}
	tearDown { PT.removeAll }

	// ------------------------------------------------------------ conversions

	test_pitchNames {
		this.assertEquals(PT.midinote(\c4), 60, "c4 is middle C");
		this.assertEquals(PT.midinote(\c5), 72, "c5");
		this.assertEquals(PT.midinote(\cs4), 61, "cs4 is a semitone above c4");
		this.assertEquals(PT.midinote('c#4'), 61, "c#4 uses the AC Toolbox spelling");
		this.assertEquals(PT.midinote(\ef4), 63, "ef4 is e flat");
		this.assertEquals(PT.midinote(\f4), 65, "f4 is a note name, not a flat");
		this.assertEquals(PT.midinote('c-1'), 0, "c-1 is the lowest pitch");
		this.assertEquals(PT.midinote(\g9), 127, "g9 is the highest pitch");
		this.assertEquals(PT.midinote(60), 60, "numbers pass through");
		this.assertEquals(PT.midinote([\c4, \e4, \g4]), [60, 64, 67], "chords convert elementwise");
		this.assertEquals(PT.midinote("c4"), 60,
			"a String is a name, not a list of characters");
		this.assertEquals(PT.velocity("mf"), 64, "and the same for dynamics");
	}

	test_dynamics {
		this.assertEquals(PT.dyn(\mf), 64, "mf is 64, as in the AC Toolbox");
		this.assertEquals(PT.dyn(90), 90, "numbers pass through");
	}

	// -------------------------------------------------------------- name sugar

	test_resolveLeavesCodeAlone {
		this.assertEquals(PT.resolve("Pwhite(0, 10)"), "Pwhite(0, 10)", "class names untouched");
		this.assertEquals(PT.resolve("\\default"), "\\default", "symbol literals untouched");
		this.assertEquals(PT.resolve("\"c4 is a pitch\""), "\"c4 is a pitch\"",
			"string literals untouched");
		this.assertEquals(PT.resolve("Pwhite(0, 10, round: 1)"), "Pwhite(0, 10, round: 1)",
			"keyword arguments untouched");
	}

	test_resolvePitchNames {
		this.assertEquals(PT.resolve("c4"), "60", "a bare pitch name becomes a number");
		this.assertEquals(PT.resolve("Pwhite(c2, c5)"), "Pwhite(36, 72)", "inside a call too");
		this.assertEquals(PT.resolve("array.c4"), "array.c4", "method calls untouched");
	}

	test_resolveObjectNames {
		PTStockpile.specify(\cmajor, "c4 d4 e4");
		this.assertEquals(PT.resolve("Prand(cmajor, inf)"), "Prand(~cmajor, inf)",
			"a registered name becomes an environment variable");
		this.assertEquals(PT.resolve("Prand(~cmajor, inf)"), "Prand(~cmajor, inf)",
			"an explicit ~name is left alone");
	}

	test_sugarCanBeDisabled {
		PT.sugar = false;
		this.assertEquals(PT.resolve("c4"), "c4", "no rewriting when sugar is off");
		PT.sugar = true;
	}

	// -------------------------------------------------------------- stockpiles

	test_stockpileSpecified {
		var s = PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");
		this.assertEquals(s.value, [60, 62, 64, 65, 67, 69, 71], "bare pitch names become a list");
		this.assertEquals(s.size, 7, "size");
	}

	test_stockpileFromExpression {
		var s = PTStockpile.specify(\chrom, "(60..71)");
		this.assertEquals(s.value, (60..71), "an expression producing a list");
	}

	test_stockpileGenerated {
		var s = PTStockpile.generate(\pitches, "Pwhite(60, 72)", "12");
		this.assertEquals(s.size, 12, "a pattern drawn the requested number of times");
		this.assert(s.value.every { |v| v.inclusivelyBetween(60, 72) }, "values within range");
	}

	test_stockpileBindsToItsValues {
		PTStockpile.specify(\cmajor, "60 62 64");
		this.assertEquals(PT.eval("cmajor"), [60, 62, 64],
			"~name inside another spec yields the list, not the object");
	}

	// ----------------------------------------------------------- data sections

	test_dataSectionBasics {
		var s = PTDataSection(\s1, (clock: "100", number: "10", rhythm: "1",
			pitch: "60", velocity: "\\mf", channel: "1")).make;
		this.assertEquals(s.length, 10, "ten events");
		this.assertEquals(s.asEvents[0][\dur], 0.1, "rhythm 1 at clock 100 is 0.1 seconds");
		this.assertEquals(s.asEvents[0][\midinote], 60, "pitch");
		this.assertEquals(s.asEvents[0][\velocity], 64, "mf");
		this.assertFloatEquals(s.duration, 1.0, "total duration");
	}

	test_dataSectionCyclesLists {
		var s = PTDataSection(\s1, (number: "6", pitch: "c4 d4 e4")).make;
		this.assertEquals(s.extract(\pitch), [60, 62, 64, 60, 62, 64], "lists cycle");
	}

	test_dataSectionRests {
		var s = PTDataSection(\s1, (number: "4", rhythm: "1 -1 2 -3")).make;
		this.assertEquals(s.length, 4, "rests are kept as events");
		this.assertEquals(s.numNotes, 2, "but do not count as notes");
		this.assertArrayFloatEquals(s.extract(\rhythm), [0.1, 0.1, 0.2, 0.3],
			"rest length is the absolute value");
		this.assertEquals(s.asEvents[1][\type], \rest, "second event is a rest");
	}

	test_dataSectionChords {
		var s = PTDataSection(\s1, (number: "2", pitch: "[[c4, e4, g4]]")).make;
		this.assertEquals(s.asEvents[0][\midinote], [60, 64, 67],
			"a nested array is a chord; a flat one is a sequence of pitches");
	}

	test_dataSectionUntilTime {
		var s = PTDataSection(\s1, (clock: "100", number: "PT.untilTime(1)", rhythm: "1")).make;
		this.assertEquals(s.length, 10, "fills one second with 0.1 second notes");
	}

	test_dataSectionUsesStockpile {
		PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");
		PTDataSection(\s1, (number: "40", pitch: "Prand(cmajor, inf)")).make;
		this.assert(PT(\s1).extract(\pitch).every { |p| [60,62,64,65,67,69,71].includes(p) },
			"pitches all come from the stockpile");
	}

	test_bpm {
		this.assertEquals(PT.bpm(120), 500, "120 bpm is a 500 ms beat");
	}

	// ------------------------------------------------------ make, seed, variant

	test_seedReproduces {
		var s = PTDataSection(\s1, (number: "20", pitch: "Pwhite(40, 80)")).make;
		var first = s.extract(\pitch);
		s.make;
		this.assert(s.extract(\pitch) != first, "make gives a new result");
		s.reproduce;
		this.assertEquals(s.extract(\pitch), s.extract(\pitch), "reproduce is stable");
		this.assertEquals(PTDataSection(\s2, s.spec.asEvent).make(s.seed).extract(\pitch),
			s.extract(\pitch), "the same spec and seed give the same result");
	}

	test_variant {
		var s = PTDataSection(\noise1, (number: "20", pitch: "Pwhite(40, 80)")).make;
		var v = s.variant;
		this.assertEquals(v.name, \noise1a, "variants get a suffixed name");
		this.assertEquals(v.spec.at(\pitch), s.spec.at(\pitch), "same rules");
		this.assert(v.seed != s.seed, "different seed");
		this.assert(v.extract(\pitch) != s.extract(\pitch), "different result");
	}

	test_specifyDoesNotMake {
		var s = PTDataSection(\s1, (number: "5"));
		this.assert(s.isMade.not, "a new object is not made");
		s.make;
		this.assert(s.isMade, "make realizes it");
		s.specify;
		this.assert(s.isMade.not, "specify discards the realization");
	}

	// ---------------------------------------------------------------- registry

	test_registry {
		PTStockpile.specify(\a1, "1 2 3");
		PTDataSection(\b1, (number: "2"));
		this.assertEquals(PT.names, [\a1, \b1], "creation order is kept");
		this.assertEquals(PT.names(\stockpile), [\a1], "filtered by type");
		this.assert(PT(\a1).notNil, "lookup by name");
		PT.remove(\a1);
		this.assert(PT(\a1).isNil, "removal");
	}

	// ------------------------------------------------------------------ curves

	test_curveSample {
		this.assertEquals(PTCurve.sample([0, 10], 3), [0, 5, 10], "linear interpolation");
		this.assertEquals(PTCurve.sample([5], 4), [5, 5, 5, 5], "one point becomes a flat line");
		this.assertEquals(PTCurve.sample([], 3), [0, 0, 0], "an empty curve does not blow up");
		this.assertEquals(PTCurve.sample([1, 2, 3], 1), [1], "a single sample");
	}

	test_curveScale {
		this.assertEquals(PTCurve.scale([0, 50, 100], 60, 72), [60, 66, 72], "mapped into range");
		this.assertEquals(PTCurve.scale([7, 7, 7], 0, 100), [50, 50, 50],
			"a flat curve lands in the middle rather than on an edge");
	}

	test_shapeConvert {
		var shape = PTShape.specify(\line, "0 100");
		this.assertEquals(shape.convert(5, 60, 72), [60, 63, 66, 69, 72],
			"integer bounds give integer results");
		this.assertEquals(shape.convert(3, 0.0, 1.0), [0.0, 0.5, 1.0], "float bounds stay float");
		this.assertEquals(shape.convert(3, 0.0, 10.0, 3), [0.0, 6.0, 9.0],
			"the rounding unit is honoured");
	}

	test_shapeContourIsScaleFree {
		var a = PTShape.specify(\a, "0 5 10");
		var b = PTShape.specify(\b, "100 150 200");
		this.assertEquals(a.convert(3, 60, 72), b.convert(3, 60, 72),
			"only the contour matters, not the absolute values");
	}

	test_shapeGenerated {
		var shape = PTShape.generate(\ramp, "Pseries(0, 1)", "10");
		this.assertEquals(shape.length, 10, "ten points");
		this.assertEquals(shape.convert(10, 0, 90).last, 90, "the top of the curve reaches hi");
	}

	test_maskConvert {
		var mask = PTMask.specify(\m1, "100 100", "0 0");
		var values = mask.convert(200, 48, 72);
		this.assertEquals(values.size, 200, "one value per requested point");
		this.assert(values.every { |v| v.inclusivelyBetween(48, 72) }, "all inside the field");
		this.assert(values.maxItem > values.minItem, "the field is actually sampled");
	}

	test_maskNarrowFieldPinsValues {
		// a mask whose lines meet leaves no freedom at all
		var mask = PTMask.specify(\m1, "50 50", "50 50");
		this.assert(mask.convert(20, 60, 72).every { |v| v == 66 },
			"a closed field yields the middle of the range");
	}

	test_maskScalesBothLinesTogether {
		var mask = PTMask.specify(\m1, "100 100", "0 100");
		var values = mask.convert(2, 0, 100, Pseq([0, 0], inf));   // always the lower line
		this.assertEquals(values, [0, 100],
			"the lower line rises to meet the upper one, so the field closes");
	}

	test_maskSpread {
		var mask = PTMask.specify(\m1, "100 60", "0 40");
		var spread = mask.spread(2, 0, 100);
		this.assert(spread[0] > spread[1], "the field narrows over time");
	}

	test_readFrom {
		var stock = PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");
		var shape = PTShape.specify(\rise, "0 100");
		var read = PT.readFrom(stock, shape, 7);
		this.assertEquals(read, [60, 62, 64, 65, 67, 69, 71],
			"a rising line reads the stockpile in order");
		this.assertEquals(PT.readFrom(stock, PTShape.specify(\fall, "100 0"), 7).reverse,
			read, "a falling line reads it backwards");
	}

	test_readFromStaysInsideTheCollection {
		var stock = PTStockpile.specify(\three, "1 2 3");
		var mask = PTMask.specify(\m1, "100 100", "0 0");
		this.assert(PT.readFrom(stock, mask, 50).every { |v| [1, 2, 3].includes(v) },
			"never reads outside the collection");
	}

	test_convertFacade {
		var shape = PTShape.specify(\line, "0 100");
		this.assertEquals(PT.convert(shape, 3, 60, 72), shape.convert(3, 60, 72),
			"PT.convert and the method form agree");
		this.assertEquals(PT.convert([0, 100], 3, 60, 72), [60, 66, 72],
			"a plain array works as a curve too");
	}

	test_shapeInASection {
		PTMask.specify(\mask1, "100 100 100", "0 0 0");
		PTDataSection(\s1, (number: "100", pitch: "mask1.convert(PT.fromNumber, 48, 72)")).make;
		this.assertEquals(PT(\s1).length, 100, "a mask fills the whole section");
		this.assert(PT(\s1).extract(\pitch).every { |p| p.inclusivelyBetween(48, 72) },
			"pitches stay inside the mask");
	}

	// -------------------------------------------------------------- generators

	test_beta {
		var values = 400.collect { PTbeta.value(0.0, 100.0, 0.1, 0.1) };
		this.assert(values.every { |v| v.inclusivelyBetween(0, 100) }, "inside the bounds");
		this.assert(values.count { |v| (v < 10) or: { v > 90 } } > 200,
			"low shape parameters cluster at the edges");
		values = 400.collect { PTbeta.value(0.0, 100.0, 4, 4) };
		this.assert(values.count { |v| v.inclusivelyBetween(30, 70) } > 200,
			"high shape parameters cluster in the middle");
		this.assert(PTbeta.value(0, 100, 0.5, 0.5).isInteger,
			"integer bounds give integer results");
	}

	test_betaAsPattern {
		this.assertEquals(PTbeta(0.0, 1.0, 0.5, 0.5, 8).asStream.nextN(8).size, 8,
			"it behaves as an ordinary pattern");
	}

	// ------------------------------------------------------------ combinations

	prTwoSections {
		PTDataSection(\a, (clock: "100", number: "10", rhythm: "1", pitch: "60")).make;
		PTDataSection(\b, (clock: "100", number: "10", rhythm: "1", pitch: "72")).make;
	}

	test_sequence {
		var seq;
		this.prTwoSections;
		seq = PTSequence(\both, (sections: "a b")).make;
		this.assertEquals(seq.numNotes, 20, "every note of both sections");
		this.assertFloatEquals(seq.duration, 2.0, "the durations add up");
		this.assertEquals(seq.extract(\pitch).keep(10), 10.collect { 60 }, "a comes first");
		this.assertEquals(seq.extract(\pitch).drop(10), 10.collect { 72 }, "then b");
	}

	test_sequenceWithDelay {
		var seq;
		this.prTwoSections;
		seq = PTSequence(\both, (sections: "a 3 b")).make;
		this.assertFloatEquals(seq.duration, 5.0, "a number between names is a delay in seconds");
		this.assertFloatEquals(seq.asEvents[9][\dur], 3.1,
			"the gap becomes a long dur on the note before it");
		this.assertFloatEquals(seq.asEvents[9][\sustain], 0.1,
			"but that note still sounds for its own length");
	}

	test_parallel {
		var par;
		this.prTwoSections;
		par = PTParallel(\both, (sections: "a b")).make;
		this.assertEquals(par.numNotes, 20, "every note of both sections");
		this.assertFloatEquals(par.duration, 1.0, "they overlap, so the total is not the sum");
		this.assertEquals(par.extract(\pitch).keep(4), [60, 72, 60, 72],
			"the voices interleave by time");
	}

	test_parallelWithOffset {
		var par;
		this.prTwoSections;
		par = PTParallel(\both, (sections: "a 0.5 b")).make;
		this.assertFloatEquals(par.duration, 1.5, "b starts half a second in");
		this.assertEquals(par.extract(\pitch).keep(5), [60, 60, 60, 60, 60],
			"a alone until b arrives");
	}

	test_timed {
		var timed;
		this.prTwoSections;
		timed = PTTimed(\both, (sections: "a b", times: "0 2")).make;
		this.assertFloatEquals(timed.duration, 3.0, "b starts at two seconds");
		this.assertEquals(timed.extract(\pitch).keep(10), 10.collect { 60 }, "a first");
	}

	test_timedLeadingRest {
		var timed;
		this.prTwoSections;
		timed = PTTimed(\one, (sections: "a", times: "1")).make;
		this.assertEquals(timed.asEvents[0][\type], \rest, "an offset start begins with a rest");
		this.assertFloatEquals(timed.asEvents[0][\dur], 1.0, "of the right length");
		this.assertEquals(timed.numNotes, 10, "and the notes follow");
	}

	test_combinationsNest {
		var inner, outer;
		this.prTwoSections;
		inner = PTSequence(\inner, (sections: "a b")).make;
		outer = PTParallel(\outer, (sections: "inner 1 inner")).make;
		this.assertEquals(outer.numNotes, 40, "a combination is a section like any other");
		this.assertFloatEquals(outer.duration, 3.0, "the second copy starts a second late");
	}

	// ------------------------------------------------------------- transformers

	test_transpose {
		var section = PTDataSection(\s1, (number: "6", pitch: "60")).make;
		var higher = PTDerived.of(\up, "s1", "PT.transpose(12)");
		this.assertEquals(higher.extract(\pitch), 6.collect { 72 }, "transposed");
		this.assertEquals(section.extract(\pitch), 6.collect { 60 }, "the source is untouched");
	}

	test_transposeWithAGenerator {
		PTDataSection(\s1, (number: "4", pitch: "60")).make;
		this.assertEquals(PTDerived.of(\up, "s1", "PT.transpose(Pseq([0, 1, 2, 3]))").extract(\pitch),
			[60, 61, 62, 63], "a generator gives a different interval per note");
	}

	test_transformsChainLeftToRight {
		PTDataSection(\s1, (number: "3", pitch: "60")).make;
		this.assertEquals(
			PTDerived.of(\x, "s1", "[PT.transpose(24), PT.limit(60, 70)]").extract(\pitch),
			[70, 70, 70], "transpose then clip");
		this.assertEquals(
			PTDerived.of(\y, "s1", "[PT.limit(60, 70), PT.transpose(24)]").extract(\pitch),
			[84, 84, 84], "clip then transpose is a different result");
	}

	test_foldQuantizeStretch {
		PTDataSection(\s1, (clock: "100", number: "3", rhythm: "1", pitch: "80")).make;
		this.assertEquals(PTDerived.of(\f, "s1", "PT.fold(60, 72)").extract(\pitch),
			[64, 64, 64], "fold reflects back into the range");
		this.assertEquals(PTDerived.of(\q, "s1", "PT.quantize(12)").extract(\pitch),
			[84, 84, 84], "quantize rounds to a unit");
		this.assertFloatEquals(PTDerived.of(\st, "s1", "PT.stretch(2)").duration, 0.6,
			"stretch scales time");
	}

	test_velocityTransformReachesAmp {
		var section, louder;
		PTDataSection(\s1, (number: "3", velocity: "60")).make;
		louder = PTDerived.of(\loud, "s1", "PT.louder(40)");
		this.assertEquals(louder.extract(\velocity), [100, 100, 100], "velocity raised");
		this.assertFloatEquals(louder.asPattern.asStream.next(())[\amp], 100 / 127,
			"amp is derived from velocity, so the change is audible");
	}

	test_filterRejectMute {
		PTDataSection(\s1, (clock: "100", number: "4", rhythm: "1", pitch: "60 72 60 72")).make;
		this.assertEquals(
			PTDerived.of(\f, "s1", "PT.filter({ |e| e.midinote > 65 })").extract(\pitch),
			[72, 72], "filter keeps what the test accepts");
		this.assertEquals(
			PTDerived.of(\r, "s1", "PT.reject({ |e| e.midinote > 65 })").extract(\pitch),
			[60, 60], "reject is the inverse");
		this.assertFloatEquals(PTDerived.of(\f2, "s1", "PT.filter({ |e| e.midinote > 65 })").duration,
			0.2, "filtering closes the gaps, so the section gets shorter");
		this.assertFloatEquals(PTDerived.of(\m, "s1", "PT.mute({ |e| e.midinote > 65 })").duration,
			0.4, "muting keeps the timing");
		this.assertEquals(PT(\m).numNotes, 2, "but silences the notes");
	}

	test_dedupe {
		PTDataSection(\s1, (number: "6", pitch: "60 60 62 62 62 64")).make;
		this.assertEquals(PTDerived.of(\d, "s1", "PT.dedupe").extract(\pitch), [60, 62, 64],
			"immediate repetitions are dropped");
	}

	test_reverseSliceKeepDrop {
		PTDataSection(\s1, (number: "5", pitch: "60 61 62 63 64")).make;
		this.assertEquals(PTDerived.of(\r, "s1", "PT.reverse").extract(\pitch),
			[64, 63, 62, 61, 60], "reverse");
		this.assertEquals(PTDerived.of(\s, "s1", "PT.slice(1, 3)").extract(\pitch), [61, 62],
			"slice by event index, to exclusive");
		this.assertEquals(PTDerived.of(\k, "s1", "PT.keep(2)").extract(\pitch), [60, 61], "keep");
		this.assertEquals(PTDerived.of(\dr, "s1", "PT.drop(3)").extract(\pitch), [63, 64], "drop");
	}

	test_transformIf {
		PTDataSection(\s1, (number: "4", pitch: "60 72 60 72")).make;
		this.assertEquals(
			PTDerived.of(\x, "s1", "PT.transformIf({ |e| e.midinote > 65 }, PT.transpose(12))")
				.extract(\pitch),
			[60, 84, 60, 84], "only the matching events move, and nothing changes place");
	}

	test_derivedFollowsItsSource {
		var derived;
		PTDataSection(\s1, (number: "20", pitch: "Pwhite(40, 80)")).make;
		derived = PTDerived.of(\up, "s1", "PT.transpose(12)");
		this.assertEquals(derived.extract(\pitch), PT(\s1).extract(\pitch) + 12, "follows the source");
		PT(\s1).make;                    // a new realization of the source
		derived.make;
		this.assertEquals(derived.extract(\pitch), PT(\s1).extract(\pitch) + 12,
			"and follows it again after a remake");
	}

	test_derivedJoinsSeveralSources {
		var joined;
		this.prTwoSections;
		joined = PTDerived.of(\j, "a b", "PT.transpose(1)");
		this.assertEquals(joined.numNotes, 20, "several sources are joined in sequence");
		this.assertEquals(joined.extract(\pitch).keep(2), [61, 61], "then transformed");
	}

	// ------------------------------------------------------------- communities

	test_communityMembers {
		var community;
		this.prTwoSections;
		community = PTCommunity.of(\group, \a, \b);
		this.assertEquals(community.names, [\a, \b], "names, not music");
		this.assertEquals(community.size, 2, "size");
		this.assertEquals(community.sections.collect(_.name), [\a, \b], "the objects behind them");
	}

	test_communityOfVariants {
		var community;
		PTDataSection(\noise, (number: "12", pitch: "Pwhite(40, 80)")).make;
		community = PTCommunity.variantsOf(\family, \noise, 4);
		this.assertEquals(community.names, [\noise1, \noise2, \noise3, \noise4],
			"variants are named after the source");
		this.assert(community.sections.every { |s| s.isMade }, "and are made");
		this.assert(community.sections.collect(_.seed).asSet.size == 4, "each with its own seed");
		this.assertEquals(community.sections[0].spec.at(\pitch), PT(\noise).spec.at(\pitch),
			"all sharing the source rules");
	}

	test_communityVariantsDoNotPileUp {
		var community;
		PTDataSection(\noise, (number: "6", pitch: "Pwhite(40, 80)")).make;
		community = PTCommunity.variantsOf(\family, \noise, 3);
		community.make;
		community.make;
		this.assertEquals(PT.names(\section).size, 4,
			"remaking reuses the variant names rather than piling up new ones");
	}

	test_communityFolding {
		var community, sequence, parallel;
		this.prTwoSections;
		community = PTCommunity.of(\group, \a, \b);
		sequence = community.asSequence(\groupSeq);
		parallel = community.asParallel(\groupPar);
		this.assertFloatEquals(sequence.duration, 2.0, "folded into a sequence");
		this.assertFloatEquals(parallel.duration, 1.0, "or into a parallel section");
		this.assertFloatEquals(community.asSequence(\spaced, 1).duration, 3.0,
			"with a delay between members");
	}

	// ---------------------------------------------------------- note structures

	test_melodyParsing {
		var theme = PT.melody("1 c4 1 d4 2 e4 1 r 1 f4");
		this.assertEquals(theme.length, 5, "five events");
		this.assertEquals(theme.notes.collect(_.pitch), [60, 62, 64, 60, 65], "pitches");
		this.assertEquals(theme.notes.collect(_.rhythm), [1, 1, 2, 1, 1], "rhythms");
		this.assertEquals(theme.notes[3].isRest, true, "r is a rest");
		this.assertEquals(theme.soundingNotes.size, 4, "four of them sound");
		this.assertEquals(theme.duration, 6, "six clock units in total");
	}

	test_melodyChordsAndDelays {
		var theme = PT.melody("2 c4+e4+g4 1 . 1 d4");
		this.assertEquals(theme.notes[0].pitch, [60, 64, 67], "a plus sign makes a chord");
		this.assertEquals(theme.length, 2, "a delay is not a note");
		this.assertEquals(theme.duration, 4, "but it does take time");
	}

	test_noteTreeSequenceAndParallel {
		var chord = PT.par(PT.note(2, \c4), PT.note(2, \e4), PT.note(2, \g4));
		var tree = PT.seq(chord, PT.note(1, \b4));
		this.assertEquals(chord.duration, 2, "a parallel group lasts as long as its longest item");
		this.assertEquals(tree.duration, 3, "a sequence adds them up");
		this.assertEquals(tree.asTimed(0).collect { |p| p[0] }, [0, 0, 0, 2],
			"three notes at the start, then one after them");
	}

	test_noteStructureObject {
		var theme = PTNoteStructure.of(\theme, "PT.melody(\"1 c4 1 d4 2 e4\")");
		this.assertEquals(theme.size, 3, "the collection protocol works");
		this.assertEquals(theme.at(1).pitch, 62, "indexing");
		this.assertEquals(theme.beats, 4, "length in clock units");
		this.assertEquals(theme.notes.collect(_.pitch), [60, 62, 64], "the notes");
	}

	// ------------------------------------------------------------ note sections

	test_noteSectionPlaysAStructureAsWritten {
		var section;
		PTNoteStructure.of(\theme, "PT.melody(\"1 c4 1 d4 2 e4 1 r\")");
		section = PTNoteSection(\played, (clock: "250", notes: "theme")).make;
		this.assertEquals(section.length, 4, "an empty number means all of them");
		this.assertEquals(section.numNotes, 3, "the rest is kept but does not sound");
		this.assertFloatEquals(section.duration, 1.25, "five units at 250 ms");
		this.assertEquals(section.extract(\pitch), [60, 62, 64, 60], "in order");
	}

	test_noteSectionKeepsParallelism {
		var section;
		PTNoteStructure.of(\chord,
			"PT.seq(PT.par(PT.note(2, c4), PT.note(2, e4)), PT.note(1, b4))");
		section = PTNoteSection(\played, (clock: "500", notes: "chord")).make;
		this.assertArrayFloatEquals(section.extract(\rhythm), [0.0, 1.0, 0.5],
			"the two notes of the chord are coincident, so the first has no delta");
		this.assertArrayFloatEquals(section.asEvents.collect { |e| e[\sustain] }, [1.0, 1.0, 0.5],
			"but both sound for their full length");
	}

	test_noteSectionFlattensThroughAGenerator {
		PTNoteStructure.of(\theme, "PT.melody(\"1 c4 1 d4 1 e4\")");
		PTNoteSection(\chosen, (clock: "200", number: "12", notes: "Prand(theme, inf)")).make;
		this.assertEquals(PT(\chosen).length, 12, "as many notes as asked for");
		this.assert(PT(\chosen).extract(\pitch).every { |p| [60, 62, 64].includes(p) },
			"all drawn from the structure");
	}

	test_noteSectionNeedsANumberForAGenerator {
		PTNoteStructure.of(\theme, "PT.melody(\"1 c4\")");
		this.assertException(
			{ PTNoteSection(\bad, (notes: "Prand(theme, inf)")).make },
			Error,
			"an endless supply of notes with no number is an error, not a hang"
		);
	}

	test_noteSectionReadsWithAShape {
		PTNoteStructure.of(\theme, "PT.melody(\"1 c4 1 d4 1 e4 1 f4\")");
		PTShape.specify(\rise, "0 100");
		PTNoteSection(\read, (clock: "200", number: "4",
			notes: "PT.readFrom(theme, rise, PT.fromNumber)")).make;
		this.assertEquals(PT(\read).extract(\pitch), [60, 62, 64, 65],
			"a rising line reads the structure in order");
	}

	test_interpolate {
		var result, early, late;
		PTNoteStructure.of(\a, "PT.melody(\"1 c4 1 c4 1 c4\")");
		PTNoteStructure.of(\b, "PT.melody(\"1 c5 1 c5 1 c5\")");
		result = PT.interpolate(PT(\a), PT(\b), 200);
		this.assertEquals(result.size, 200, "the requested length");
		// the crossover is probabilistic, so this counts rather than checking
		// individual steps: even step one can already fall to the second object
		early = result.keep(100).count { |note| note.pitch == 60 };
		late = result.drop(100).count { |note| note.pitch == 60 };
		this.assert(early > late, "the first object gives way to the second");
		this.assert(early > 60, "clearly dominating at the start");
		this.assert(late < 40, "and clearly given way by the end");
	}

	test_sectionAsNoteMaterial {
		var notes;
		PTDataSection(\s1, (clock: "1000", number: "4", rhythm: "1", pitch: "c4 d4 e4 f4")).make;
		notes = PT.asList(PT(\s1));
		this.assertEquals(notes.collect(_.pitch), [60, 62, 64, 65], "a section supplies notes too");
		PTNoteSection(\again, (clock: "1000", notes: "PT.asList(s1)")).make;
		this.assertFloatEquals(PT(\again).duration, PT(\s1).duration,
			"and a clock of 1000 reproduces the original timing");
	}

	// --------------------------------------------------------- density sections

	test_attacksTool {
		this.assertArrayFloatEquals(PT.attacks(1, number: 5), [0, 25, 50, 75, 100],
			"equal intervals spread evenly from 0 to 100");
		this.assert(this.prAscends(PT.attacks(Pwhite(1, 10), number: 20)),
			"attack points always ascend, whatever the intervals");
	}

	test_densityFromAList {
		var section = PTDensity(\time1, (time: "10", number: "3",
			attacks: "0 10 20 30 40 50 60 70 80 90", duration: "200", pitch: "60")).make;
		this.assertEquals(section.length, 10,
			"a list of attacks sets the number itself, whatever the number slot says");
		this.assertArrayFloatEquals(
			section.asEvents.collect { |e| e[\dur] }.drop(-1), 9.collect { 1.0 },
			"ten points across ten seconds land one second apart");
		this.assertFloatEquals(section.asEvents[0][\sustain], 0.2,
			"duration is in milliseconds, independent of any clock");
	}

	test_densityFromAGenerator {
		var section = PTDensity(\t, (time: "10", number: "40",
			attacks: "Pwhite(0.0, 100)", pitch: "60")).make;
		var times = this.prAttackTimes(section);
		this.assertEquals(section.numNotes, 40, "the number slot decides");
		this.assertEquals(section.asEvents[0][\type], \rest,
			"a generator rarely lands its first attack on zero, so the section opens with silence");
		this.assert(this.prAscends(times), "attack points are sorted into ascending order");
		this.assert(times.last < 10.0, "and all of them fall inside the allotted time");
	}

	test_densityAttacksFollowTheDistribution {
		var times, middle, edges;
		PTDensity(\t, (time: "10", number: "300",
			attacks: "PTbeta(0.0, 100, 0.2, 0.2)", pitch: "60")).make;
		times = this.prAttackTimes(PT(\t));
		middle = times.count { |t| t.inclusivelyBetween(4, 6) };
		edges = times.count { |t| (t < 1) or: { t > 9 } };
		// beta with small parameters is symmetric but crowds both ends, so the
		// middle fifth of the time is thin and the outer fifth is thick
		this.assert(middle < 60, "the middle of the section is sparser than uniform would be");
		this.assert(edges > (middle * 2), "and the two ends are much thicker than the middle");
	}

	test_densityCurve {
		var section, expected = (1 + 20) / 2 * 6;
		PTShape.specify(\rise, "0 100");
		section = PTDensityCurve(\c1, (time: "6", curve: "rise",
			min: "1", max: "20", unit: "1", duration: "100", pitch: "60")).make;
		this.assert((section.length - expected).abs < 12,
			"the note count follows the area under the density curve");
		this.assert(section.length > 20, "and there are a good many of them");
	}

	test_densityCurveGrowsOverTime {
		var first, second;
		PTShape.specify(\rise, "0 100");
		PTDensityCurve(\c1, (time: "10", curve: "rise", min: "1", max: "30",
			unit: "1", duration: "50", pitch: "60")).make;
		first = PT(\c1).asEvents.keep(PT(\c1).length div: 2).sum { |e| e[\dur] };
		second = PT(\c1).duration - first;
		this.assert(first > second,
			"a rising density means the first half of the notes takes longer than the second");
	}

	test_densityCurveSpreadsWithinEachStep {
		var times;
		PTShape.specify(\flat, "50 50");
		PTDensityCurve(\c1, (time: "4", curve: "flat", min: "10", max: "10",
			unit: "1", duration: "50", pitch: "60")).make;
		times = this.prAttackTimes(PT(\c1));
		this.assertEquals(times.size, 40, "ten notes in each of four seconds");
		this.assert(times.asSet.size > 30,
			"the notes are spread inside each step, not stacked on its boundary");
	}

	test_densityCurveWithAMask {
		PTMask.specify(\band, "60 100", "10 40");
		PTDensityCurve(\c1, (time: "5", curve: "band", min: "1", max: "20", pitch: "60")).make;
		this.assert(PT(\c1).length > 5, "a mask gives a random density inside its field");
	}

	// ------------------------------------------------------------- controllers

	test_controllerRemembersBetweenSections {
		var controller = PTController.of(\howMany, "5 10 20");
		this.assertEquals(controller.next, 5, "first value");
		this.assertEquals(controller.next, 10, "second value");
		this.assertEquals(controller.next, 20, "third value");
		this.assertEquals(controller.next, 5, "and the list cycles");
		this.assertEquals(controller.history, [5, 10, 20, 5], "the history is kept");
	}

	test_controllerInASection {
		PTController.of(\howMany, "2 10 20");
		PTDataSection(\bit, (number: "howMany", pitch: "60")).specify;
		this.assertEquals(PT(\bit).make.length, 2, "the first make gets 2 notes");
		this.assertEquals(PT(\bit).make.length, 10, "the second gets 10");
		this.assertEquals(PT(\bit).make.length, 20, "the third gets 20");
	}

	test_controllerAsAStreamInAParameter {
		PTController.of(\steps, "60 62 64");
		PTDataSection(\s1, (number: "6", pitch: "steps")).make;
		this.assertEquals(PT(\s1).extract(\pitch), [60, 62, 64, 60, 62, 64],
			"used bare, a controller supplies a value per note");
		this.assertEquals(PT(\steps).history.size, 6, "and every one is recorded");
	}

	test_takeOneHoldsForTheWholeSection {
		PTController.of(\base, "60 72");
		PTDataSection(\s1, (number: "4", pitch: "PT.takeOne(base)")).make;
		this.assertEquals(PT(\s1).extract(\pitch), [60, 60, 60, 60],
			"take-one pulls a single value and holds it");
		this.assertEquals(PT(\base).history.size, 1, "only one value was consumed");
		PT(\s1).make;
		this.assertEquals(PT(\s1).extract(\pitch), [72, 72, 72, 72], "the next make moves on");
	}

	test_controllerCyclesAFinitePattern {
		var controller = PTController.of(\ramp, "Pseries(1, 1, 3)");
		this.assertEquals(4.collect { controller.next }, [1, 2, 3, 1],
			"a finite pattern cycles, so a controller never runs dry");
	}

	test_controllerReset {
		var controller = PTController.of(\c1, "1 2 3");
		controller.next; controller.next;
		controller.reset;
		this.assertEquals(controller.history, [], "reset wipes the history");
		this.assertEquals(controller.next, 1, "and starts again from the beginning");
	}

	test_controllerHistoryIsReproducible {
		var controller = PTController.of(\c1, "Pwhite(0, 1000)");
		var first = 10.collect { controller.next };
		controller.reset;
		this.assertEquals(10.collect { controller.next }, first,
			"the same seed replays the same history");
		controller.make;
		this.assert(10.collect { controller.next } != first, "a fresh make does not");
	}

	test_synchronisedControllers {
		var master = PTController.of(\master, "Pwhite(0, 100000)");
		var a = PTController.syncedTo(\a, \master);
		var b = PTController.syncedTo(\b, \master);
		var first = a.next;
		this.assertEquals(b.next, first, "the follower that asks second sees the same value");
		this.assert(a.next != first, "asking again makes the master produce a new one");
		this.assertEquals(master.history.size, 0, "the master was never asked directly");
		this.assertEquals(a.history.size, 2, "each follower records what it saw");
	}

	test_synchronisedControllersRelateTwoParameters {
		var pitches;
		PTController.of(\pick, "60 72");
		PTController.syncedTo(\forPitch, \pick);
		PTController.syncedTo(\forVelocity, \pick);
		PTDataSection(\s1, (
			number: "4", pitch: "forPitch",
			velocity: "PT.lookup(forVelocity, [60, 40, 72, 100])"
		)).make;
		pitches = PT(\s1).extract(\pitch);
		this.assertEquals(pitches, [60, 72, 60, 72], "pitch follows the shared value");
		this.assertEquals(PT(\s1).extract(\velocity), pitches.collect { |p|
			if(p == 60) { 40 } { 100 }
		}, "and velocity is derived from the very same value, note by note");
	}

	test_scalarCoercion {
		this.assertEquals(PT.scalar(5), 5, "a number passes through");
		this.assertEquals(PT.scalar([7, 8, 9]), 7, "a list gives its first value");
		this.assert(PT.scalar(Pwhite(0, 10)).isNumber, "a pattern gives one value");
	}

	// ------------------------------------------------------------------ schemes

	test_schemeRemakesInOrder {
		var scheme, order;
		PTShape.generate(\curve, "Pwhite(0, 100)", "8");
		PTDataSection(\theme, (number: "10", pitch: "curve.convert(PT.fromNumber, 48, 72)")).make;
		scheme = PTScheme.of(\round, \curve, \theme);
		this.assertEquals(scheme.names, [\curve, \theme], "the members, in order");
		order = [PT(\curve).value, PT(\theme).extract(\pitch)];
		scheme.apply;
		this.assert(PT(\curve).value != order[0], "the shape was remade");
		this.assert(PT(\theme).extract(\pitch) != order[1], "and the section that reads it");
	}

	test_schemeResetsControllers {
		var scheme;
		PTController.of(\howMany, "4 8 12");
		PTDataSection(\bit, (number: "howMany", pitch: "60")).specify;
		scheme = PTScheme(\round, (members: "bit", reset: "howMany")).make;
		scheme.apply;
		this.assertEquals(PT(\bit).length, 4, "the first pass gets the first value");
		scheme.apply;
		this.assertEquals(PT(\bit).length, 4,
			"and so does the second, because the controller was reset");
	}

	test_schemeWithoutResetCarriesOn {
		var scheme;
		PTController.of(\howMany, "4 8 12");
		PTDataSection(\bit, (number: "howMany", pitch: "60")).specify;
		scheme = PTScheme(\round, (members: "bit")).make;
		scheme.apply;
		scheme.apply;
		this.assertEquals(PT(\bit).length, 8,
			"without a reset the controller carries on, which is the point of it");
	}

	test_schemeApplyTimes {
		var lengths;
		PTController.of(\howMany, "3 6 9");
		PTDataSection(\bit, (number: "howMany", pitch: "60")).specify;
		lengths = [];
		PTScheme(\round, (members: "bit")).make.applyTimes(3, { lengths = lengths.add(PT(\bit).length) });
		this.assertEquals(lengths, [3, 6, 9], "each pass is one step of the controller");
	}

	// --------------------------------------------------------- gui, pure parts

	test_capabilitiesAreDeclaredNotProbed {
		// Object itself answers to reset, so respondsTo put a reset button on every
		// object in the toolbox. These have to be declared.
		var section = PTDataSection(\s1, (number: "2"));
		this.assert(Object.new.respondsTo(\reset),
			"the reason: something in the class library defines Object:reset");
		this.assertEquals([section.canDraw, section.canApply, section.canReset], [false, false, false],
			"a data section offers none of the three");
		this.assertEquals(PTShape.specify(\sh, "0 100").canDraw, true, "a shape can be drawn");
		this.assertEquals(PTMask.specify(\mk, "100 100", "0 0").canDraw, true, "so can a mask");
		this.assertEquals(PTController.of(\c1, "1 2").canReset, true, "a controller can be reset");
		this.assertEquals(PTScheme.of(\sc, \s1).canApply, true, "a scheme can be applied");
	}

	test_browserFilter {
		var all = [\section1, \section2, \cmajor, \Noise1];
		this.assertEquals(PTBrowser.filterNames(all, ""), all, "an empty filter shows everything");
		this.assertEquals(PTBrowser.filterNames(all, "sec"), [\section1, \section2], "substring");
		this.assertEquals(PTBrowser.filterNames(all, "NOISE"), [\Noise1], "case insensitive");
		this.assertEquals(PTBrowser.filterNames(all, "zz"), [], "no match");
	}

	test_parenBalance {
		this.assertEquals(PTObjectView.parenBalance("Prand([1, 2], inf)"), 0, "balanced");
		this.assertEquals(PTObjectView.parenBalance("Prand([1, 2], inf"), 1, "one unclosed");
		this.assertEquals(PTObjectView.parenBalance("Pseq([1])))"), -2, "two too many");
		this.assertEquals(PTObjectView.parenBalance("\"a ( string\""), 0,
			"parentheses inside a string do not count");
	}

	test_pianoRollLayout {
		var section = PTDataSection(\s1, (clock: "100", number: "4",
			rhythm: "1 -1 2 1", pitch: "c4 d4 e4 f4")).make;
		var layout = PTPianoRoll.computeLayout(section.asEvents, 800, 400);
		this.assertFloatEquals(layout[\duration], 0.5, "total duration");
		this.assertEquals(layout[\rects].size, 4, "one rectangle per event");
		this.assertEquals(layout[\rects][1][4], true, "the second event is drawn as a rest");
		this.assertEquals(layout[\loNote], 60, "lowest pitch");
		this.assert(layout[\hiNote] - layout[\loNote] >= 12, "at least an octave is shown");
		this.assert(layout[\rects][0][0] < layout[\rects][2][0], "time runs to the right");
		this.assert(layout[\rects][0][1] > layout[\rects][3][1], "pitch runs upwards");
	}

	test_pianoRollUsesSustainForWidth {
		var layout, widths;
		this.prTwoSections;
		PTParallel(\both, (sections: "a b")).make;
		layout = PTPianoRoll.computeLayout(PT(\both).asEvents, 800, 400);
		widths = layout[\rects].collect { |r| r[2] };
		this.assert(widths.every { |w| w > 1 },
			"coincident voices have a dur of zero, so the bar width must come from sustain");
		this.assertEquals(widths.asSet.size, 1, "and every note here is the same length");
	}

	test_pianoRollLayoutHandlesChordsAndEmpty {
		var events = [(dur: 1, midinote: [60, 64, 67], velocity: 64)];
		var layout = PTPianoRoll.computeLayout(events, 800, 400);
		this.assertEquals(layout[\rects].size, 3, "a chord draws one rectangle per pitch");
		this.assertEquals(PTPianoRoll.computeLayout([], 800, 400)[\rects], [],
			"an empty section does not blow up");
	}

	// ----------------------------------------------------------------- archive

	test_archiveWithAControllerDrivenCommunity {
		var path = PathName.tmp +/+ "pt-test-controller.ptx";
		var before;
		PTController.of(\howMany, "Prand([5, 10, 20], inf)");
		PTDataSection(\bit, (clock: "100", number: "howMany", pitch: "Pwhite(48, 72)")).specify;
		PTCommunity(\bits, (source: "bit", number: "5")).make;
		before = PT(\bits).sections.collect(_.length);
		PTArchive.save(path);

		PT.removeAll;
		PTArchive.load(path);
		this.assertEquals(PT(\bits).sections.collect(_.length), before,
			"a controller driven community reloads faithfully");
		this.assertEquals(PT(\howMany).history.size, 5,
			"because the community does not regenerate on load and so the controller "
			"is asked for one value per member, not two");
		File.delete(path);
	}

	test_archiveRoundTrip {
		var path = PathName.tmp +/+ "pt-test-archive.ptx";
		var before;
		PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");
		PTDataSection(\s1, (number: "20", pitch: "Prand(cmajor, inf)")).make;
		before = PT(\s1).extract(\pitch);
		PTArchive.save(path);

		PT.removeAll;
		this.assert(PT(\s1).isNil, "environment cleared");

		PTArchive.load(path);
		this.assertEquals(PT.names, [\cmajor, \s1], "both objects returned, in order");
		this.assertEquals(PT(\cmajor).value, [60, 62, 64, 65, 67, 69, 71], "stockpile restored");
		this.assertEquals(PT(\s1).extract(\pitch), before,
			"the section is byte-identical, because the seed was saved");
		File.delete(path);
	}
}
