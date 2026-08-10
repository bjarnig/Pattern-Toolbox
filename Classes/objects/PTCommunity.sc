// PTCommunity - a group of sections joined in name only.
//
// A community holds names, not music. Its use is that it gives you a handle on a
// group: make them all, play them in turn, or fold them into a combination.
//
// Two ways to fill one:
//
//   members  "opening middle close"     an explicit list
//   source + number                     that many variants of one section
//
// The second is the AC Toolbox idiom of generating many variants of a section and
// then treating them as material. The variants are named source1, source2, ... so
// remaking the community regenerates them in place rather than piling up names.

PTCommunity : PTObject {

	*ptType { ^\community }

	*slotSpecs {
		^[
			[\members, "", "section names"],
			[\source, "", "a section to vary, if members is empty"],
			[\number, "", "how many variants to generate from source"]
		]
	}

	*of { |name ... members|
		^this.new(name, (members: members.collect(_.asString).join(" "))).make
	}

	*variantsOf { |name, source, number|
		^this.new(name, (source: source.asString, number: number.asString)).make
	}

	realize {
		var listed = spec.valueAsList(\members);
		var source, number;

		if(listed.notNil) {
			^this.prNamesOf(listed)
		};

		if(spec.isEmptyAt(\source)) { ^[] };
		source = spec.value(\source);
		if(source.isKindOf(PTObject).not) {
			Error("PT: community % has no source section".format(name)).throw
		};
		number = if(spec.isEmptyAt(\number)) { 1 } { spec.value(\number).asInteger };

		^number.collect { |i|
			var variantName = (source.name.asString ++ (i + 1)).asSymbol;
			var variant = source.class.new(variantName);
			variant.spec_(source.spec.copy);
			variant.comment_("variant % of %".format(i + 1, source.name));
			variant.make;
			variantName
		}
	}

	prNamesOf { |listed|
		if(listed.isKindOf(PTObject)) { ^[listed.name] };
		^listed.asArray.collect { |item|
			case
				{ item.isKindOf(PTObject) } { item.name }
				{ item.isKindOf(Symbol) } { item }
				{ item.asSymbol }
		}
	}

	// -------------------------------------------------------------- the group

	names { ^this.prRequireMade.value }

	objects { ^this.names.collect { |n| PT(n) }.reject(_.isNil) }

	sections { ^this.objects.select { |o| o.isKindOf(PTSection) } }

	size { ^this.names.size }

	do { |func| ^this.objects.do(func) }

	makeAll { this.objects.do(_.make); ^this }

	// Fold the group into a combination, so it can be heard as one thing.
	asSequence { |sequenceName, delay = 0|
		var tokens = if(delay > 0) {
			this.names.join(" " ++ delay ++ " ")
		} {
			this.names.join(" ")
		};
		^PTSequence(sequenceName ?? { (name ++ "Seq").asSymbol }, (sections: tokens)).make
	}

	asParallel { |parallelName, stagger = 0|
		var tokens = this.names.collect { |n, i|
			if((stagger > 0) and: { i > 0 }) { "% %".format(i * stagger, n) } { n.asString }
		}.join(" ");
		^PTParallel(parallelName ?? { (name ++ "Par").asSymbol }, (sections: tokens)).make
	}

	// A community is used as its list of names.
	asPTValue { ^this }

	postValues {
		this.objects.do(_.postInfo);
		^this
	}
}
