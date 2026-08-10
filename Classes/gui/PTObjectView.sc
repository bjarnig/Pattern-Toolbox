// PTObjectView - the dialog for one object.
//
// Generic: a class declares slotSpecs and its dialog is built from that, so
// every object type gets a working editor for free. The view holds no state of
// its own. It reads and writes object.spec and calls make.
//
// Typing a new name into the name field and pressing Make creates a new object
// with the same rules, which is how the AC Toolbox tutorial asks you to work.

PTObjectView {
	var <object, <window;
	var nameField, commentField, statusView;
	var fields;              // slot Symbol -> TextField

	classvar <views;         // name Symbol -> PTObjectView

	*initClass { views = IdentityDictionary.new }

	*new { |object, bounds|
		var existing = views[object.name];
		if(existing.notNil and: { existing.window.isClosed.not }) {
			existing.window.front;
			^existing
		};
		^super.new.init(object, bounds)
	}

	*closeAll {
		views.copy.do { |view| if(view.window.isClosed.not) { view.window.close } };
	}

	init { |argObject, bounds|
		object = argObject;
		fields = IdentityDictionary.new;
		views[object.name] = this;

		window = PTGUI.window(
			"% : %".format(object.name, object.type),
			bounds ?? { Rect(470, 260, 560, 44 + (object.spec.keys.size * 30) + 96) }
		);

		window.layout = VLayout(
			this.prNameRow,
			this.prSlotGrid,
			this.prCommentRow,
			this.prButtonRow,
			statusView = StaticText()
				.string_(this.statusString)
				.font_(PTGUI.mono(11))
				.stringColor_(PTGUI.color(\dim))
		).margins_(10).spacing_(6);

		object.addDependant(this);
		window.onClose_({
			object.removeDependant(this);
			views.removeAt(object.name);
		});
		window.front;
		^this
	}

	// ------------------------------------------------------------------- rows

	prNameRow {
		nameField = PTGUI.field(object.name.asString);
		nameField.font_(PTGUI.font(13, true));
		^HLayout(PTGUI.label("name", 78), nameField)
	}

	prSlotGrid {
		var rows = object.spec.keys.collect { |key|
			var field = PTGUI.field(object.spec.at(key), { |view|
				object.spec.put(key, view.string);
			});
			field.toolTip_(object.spec.docFor(key));
			PTGUI.acceptNameDrops(field);
			fields[key] = field;
			[
				PTGUI.label(key.asString, 78),
				field,
				PTGUI.button("...", { this.prOpenEditor(key) }, 30)
			]
		};
		// GridLayout.rows is variadic, so the rows must be splatted
		^GridLayout.rows(*rows).hSpacing_(6).vSpacing_(3)
	}

	prCommentRow {
		commentField = PTGUI.field(object.comment, { |view| object.comment_(view.string) });
		^HLayout(PTGUI.label("comment", 78), commentField)
	}

	prButtonRow {
		var buttons = [
			PTGUI.button("make", { this.make }),
			if(object.canDraw) { PTGUI.button("draw", { object.draw }) },
			if(object.canApply) { PTGUI.button("apply", { object.apply }) },
			if(object.canReset) { PTGUI.button("reset", { object.reset; this.refresh }) },
			PTGUI.button("specify", { this.specify }),
			PTGUI.button("variant", { this.variant }),
			PTGUI.button("play", { object.play }),
			PTGUI.button("stop", { object.stop }),
			PTGUI.button("plot", { this.plot }),
			PTGUI.button("post", { object.postSpec; object.postInfo })
		].reject(_.isNil);
		^HLayout(*buttons)
	}

	// A larger editor for one slot, with a parenthesis balance report. The AC
	// Toolbox mini-editor, which exists because these expressions get long.
	prOpenEditor { |key|
		var editorWindow, textView, report;
		editorWindow = PTGUI.window("% . %".format(object.name, key), Rect(300, 300, 460, 220));
		textView = TextView()
			.string_(object.spec.at(key))
			.font_(PTGUI.mono(13))
			.stringColor_(PTGUI.color(\text))
			.background_(PTGUI.color(\field));
		report = StaticText().font_(PTGUI.mono(11));
		textView.keyUpAction_({
			var balance = PTObjectView.parenBalance(textView.string);
			report
				.string_(if(balance == 0) { "parentheses balanced" } { "unbalanced: %".format(balance) })
				.stringColor_(if(balance == 0) { PTGUI.color(\made) } { PTGUI.color(\unmade) });
		});
		editorWindow.layout = VLayout(
			textView,
			report,
			HLayout(
				nil,
				PTGUI.button("cancel", { editorWindow.close }),
				PTGUI.button("ok", {
					object.spec.put(key, textView.string);
					fields[key].string_(textView.string);
					editorWindow.close;
				})
			)
		).margins_(8);
		editorWindow.front;
	}

	// Net parenthesis depth, ignoring string and symbol literals. Zero is balanced,
	// a positive number means unclosed, a negative one means too many closers.
	*parenBalance { |string|
		var depth = 0, i = 0, size = string.size, char;
		while { i < size } {
			char = string[i];
			case
			{ char == $" } {
				i = i + 1;
				while { (i < size) and: { string[i] != $" } } {
					if(string[i] == $\\) { i = i + 1 };
					i = i + 1;
				};
			}
			{ char == $' } {
				i = i + 1;
				while { (i < size) and: { string[i] != $' } } { i = i + 1 };
			}
			{ (char == $() or: { char == $[ } or: { char == ${ } } { depth = depth + 1 }
			{ (char == $)) or: { char == $] } or: { char == $} } } { depth = depth - 1 };
			i = i + 1;
		};
		^depth
	}

	// ---------------------------------------------------------------- actions

	// Make, honouring a changed name: a new name means a new object with the
	// same rules, and the dialog follows it.
	make {
		var wanted = nameField.string.stripWhiteSpace.asSymbol;
		this.prCommitFields;
		if(wanted != object.name and: { wanted.asString.isEmpty.not }) {
			var fresh = object.class.new(wanted);
			fresh.spec_(object.spec.copy);
			fresh.comment_(object.comment);
			this.prRetarget(fresh);
		};
		object.make;
		this.refresh;
		^object
	}

	specify {
		this.prCommitFields;
		object.specify;
		this.refresh;
	}

	variant {
		var variant = object.variant;
		this.prRetarget(variant);
		this.refresh;
		^variant
	}

	plot {
		if(object.asEvents.notNil) { ^PTPianoRoll(object) };
		if(object.isMade) { ^object.plot };
		^PTGUI.alert("PT: % has not been made yet".format(object.name))
	}

	prCommitFields {
		fields.keysValuesDo { |key, field| object.spec.put(key, field.string) };
		object.comment_(commentField.string);
	}

	prRetarget { |newObject|
		object.removeDependant(this);
		views.removeAt(object.name);
		object = newObject;
		views[object.name] = this;
		object.addDependant(this);
		window.name = "% : %".format(object.name, object.type);
	}

	// ---------------------------------------------------------------- display

	statusString {
		^if(object.isMade) {
			"made   % events   seed %".format(object.length, object.seed)
		} {
			"specified, not made"
		}
	}

	refresh {
		{
			nameField.string_(object.name.asString);
			object.spec.keys.do { |key| fields[key].string_(object.spec.at(key)) };
			commentField.string_(object.comment);
			statusView
				.string_(this.statusString)
				.stringColor_(if(object.isMade) { PTGUI.color(\made) } { PTGUI.color(\unmade) });
		}.defer;
		^this
	}

	update { |changed, what| if(what == \made) { this.refresh } }
}
