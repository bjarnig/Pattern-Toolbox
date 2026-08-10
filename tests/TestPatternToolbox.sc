// Phase 0 test suite. Run with: TestPatternToolbox.run

TestPatternToolbox : UnitTest {

	setUp { PT.removeAll }
	tearDown { PT.removeAll }

	// ------------------------------------------------------------ conversions

	test_pitchNames {
		this.assertEquals(PT.note(\c4), 60, "c4 is middle C");
		this.assertEquals(PT.note(\c5), 72, "c5");
		this.assertEquals(PT.note(\cs4), 61, "cs4 is a semitone above c4");
		this.assertEquals(PT.note('c#4'), 61, "c#4 uses the AC Toolbox spelling");
		this.assertEquals(PT.note(\ef4), 63, "ef4 is e flat");
		this.assertEquals(PT.note(\f4), 65, "f4 is a note name, not a flat");
		this.assertEquals(PT.note('c-1'), 0, "c-1 is the lowest pitch");
		this.assertEquals(PT.note(\g9), 127, "g9 is the highest pitch");
		this.assertEquals(PT.note(60), 60, "numbers pass through");
		this.assertEquals(PT.note([\c4, \e4, \g4]), [60, 64, 67], "chords convert elementwise");
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

	// --------------------------------------------------------- gui, pure parts

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
