// PTGUI - shared look and small helpers for the Pattern Toolbox windows.
//
// One place for the palette, the type scale and the metrics, so the browser,
// the object dialogs and the plots read as one piece of software.

PTGUI {
	classvar <>fontName = "Helvetica";
	classvar <>monoName = "Menlo";
	classvar <>fontSize = 12;

	classvar palette;

	*initClass {
		palette = IdentityDictionary[
			\background   -> Color(0.16, 0.16, 0.17),
			\panel        -> Color(0.21, 0.21, 0.23),
			\field        -> Color(0.12, 0.12, 0.13),
			\text         -> Color(0.88, 0.88, 0.90),
			\dim          -> Color(0.55, 0.55, 0.58),
			\accent       -> Color(0.42, 0.68, 0.92),
			\accentSoft   -> Color(0.42, 0.68, 0.92, 0.25),
			\made         -> Color(0.52, 0.80, 0.55),
			\unmade       -> Color(0.85, 0.62, 0.35),
			\rest         -> Color(0.45, 0.45, 0.48),
			\grid         -> Color(1, 1, 1, 0.06),
			\gridStrong   -> Color(1, 1, 1, 0.14)
		];
	}

	*color { |key| ^palette[key] ?? { Color.gray } }

	*font { |size, bold = false|
		^Font(fontName, size ? fontSize, bold)
	}

	*mono { |size|
		^Font(monoName, size ? fontSize)
	}

	// ------------------------------------------------------------- components

	*label { |string, width|
		var view = StaticText()
			.string_(string)
			.font_(this.font)
			.stringColor_(this.color(\dim))
			.align_(\right);
		if(width.notNil) { view.fixedWidth_(width) };
		^view
	}

	*heading { |string|
		^StaticText()
			.string_(string)
			.font_(this.font(14, true))
			.stringColor_(this.color(\text))
	}

	*field { |string, action|
		^TextField()
			.string_(string ? "")
			.font_(this.mono)
			.stringColor_(this.color(\text))
			.background_(this.color(\field))
			.action_(action)
	}

	*button { |string, action, width|
		var view = Button()
			.states_([[string, this.color(\text), this.color(\panel)]])
			.font_(this.font)
			.action_(action);
		if(width.notNil) { view.fixedWidth_(width) };
		^view
	}

	// Make a text field accept a name dragged from the browser, inserting it
	// rather than replacing what is already there.
	*acceptNameDrops { |textField|
		textField
			.canReceiveDragHandler_({ View.currentDrag.isKindOf(Symbol)
				or: { View.currentDrag.isKindOf(String) } })
			.receiveDragHandler_({ |view|
				var current = view.string ? "";
				var dropped = View.currentDrag.asString;
				view.string_(
					if(current.stripWhiteSpace.isEmpty) { dropped } { current ++ " " ++ dropped }
				);
				view.doAction;
			});
		^textField
	}

	*window { |name, bounds|
		var window = Window(name, bounds);
		window.view.background_(this.color(\background));
		^window
	}

	*alert { |string|
		string.warn;
		^nil
	}
}
