// PTBrowser - the persistent window over the object registry.
//
// The AC Toolbox Objects dialog: filter by type, filter by name, select an
// object, act on it. Names drag out of the list into any slot of any dialog.

PTBrowser {
	var <window, listView, typeMenu, filterField, infoView;
	var <names;              // currently displayed, in list order

	classvar <current;
	classvar <>types;

	*initClass {
		types = [\all, \section, \stockpile, \shape, \mask, \controller, \scheme];
	}

	*new { |bounds|
		if(current.notNil and: { current.window.isClosed.not }) {
			current.window.front;
			^current
		};
		^super.new.init(bounds)
	}

	// Pure, so it can be tested without a display: name filtering is a plain
	// case-insensitive substring match, as in the AC Toolbox filter box.
	*filterNames { |allNames, pattern|
		if(pattern.isNil or: { pattern.stripWhiteSpace.isEmpty }) { ^allNames };
		pattern = pattern.stripWhiteSpace.toLower;
		^allNames.select { |name| name.asString.toLower.contains(pattern) }
	}

	init { |bounds|
		current = this;
		window = PTGUI.window("Pattern Toolbox", bounds ?? { Rect(40, 80, 400, 520) });

		typeMenu = PopUpMenu()
			.items_(types.collect(_.asString))
			.font_(PTGUI.font)
			.action_({ this.refresh });

		filterField = PTGUI.field("", { this.refresh });

		listView = ListView()
			.font_(PTGUI.mono(12))
			.background_(PTGUI.color(\field))
			.hiliteColor_(PTGUI.color(\accentSoft))
			.selectedStringColor_(PTGUI.color(\text))
			.stringColor_(PTGUI.color(\text))
			.action_({ this.prUpdateInfo })
			.beginDragAction_({ this.selectedName })
			.mouseUpAction_({ |view, x, y, mod, button, count|
				if(count == 2) { this.edit };
			});

		infoView = StaticText()
			.font_(PTGUI.mono(11))
			.stringColor_(PTGUI.color(\dim))
			.fixedHeight_(34);

		window.layout = VLayout(
			HLayout(typeMenu, filterField),
			listView,
			infoView,
			HLayout(
				PTGUI.button("edit", { this.edit }),
				PTGUI.button("make", { this.withSelected(_.make) }),
				PTGUI.button("variant", { this.withSelected { |o| o.variant; this.refresh } }),
				PTGUI.button("draw", { this.draw })
			),
			HLayout(
				PTGUI.button("play", { this.withSelected(_.play) }),
				PTGUI.button("stop", { this.withSelected(_.stop) }),
				PTGUI.button("plot", { this.plot }),
				PTGUI.button("post", { this.withSelected { |o| o.postSpec; o.postInfo } })
			),
			HLayout(
				PTGUI.button("save", { this.save }),
				PTGUI.button("load", { this.load }),
				PTGUI.button("remove", { this.remove }),
				PTGUI.button("stop all", { PT.stopAll })
			)
		).margins_(8).spacing_(5);

		PT.addDependant(this);
		window.onClose_({ PT.removeDependant(this); current = nil });
		this.refresh;
		window.front;
		^this
	}

	// ------------------------------------------------------------- selection

	selectedName {
		if(names.isNil or: { names.isEmpty }) { ^nil };
		^names[listView.value ? 0]
	}

	selected { ^PT(this.selectedName) }

	withSelected { |func|
		var object = this.selected;
		if(object.isNil) { ^PTGUI.alert("PT: nothing selected") };
		^func.value(object)
	}

	// ---------------------------------------------------------------- actions

	edit { ^this.withSelected { |object| PTObjectView(object) } }

	plot {
		^this.withSelected { |object|
			if(object.isMade.not) { ^PTGUI.alert("PT: % has not been made yet".format(object.name)) };
			if(object.asEvents.notNil) { PTPianoRoll(object) } { object.plot };
		}
	}

	draw {
		^this.withSelected { |object|
			if(object.respondsTo(\draw)) {
				object.draw
			} {
				PTGUI.alert("PT: % cannot be drawn".format(object.name))
			}
		}
	}

	remove {
		^this.withSelected { |object| PT.remove(object.name); this.refresh }
	}

	save {
		Dialog.savePanel({ |path| PT.save(path) });
	}

	load {
		Dialog.openPanel({ |path| PT.load(path); this.refresh });
	}

	// ---------------------------------------------------------------- display

	refresh {
		{
			var type = types[typeMenu.value ? 0];
			var wanted = listView.notNil.if({ this.selectedName });
			var all = PT.names(if(type == \all) { nil } { type });
			names = PTBrowser.filterNames(all, filterField.string);
			listView.items = names.collect { |name| this.prRow(name) };
			if(wanted.notNil) {
				var index = names.indexOf(wanted);
				if(index.notNil) { listView.value = index };
			};
			this.prUpdateInfo;
		}.defer;
		^this
	}

	prRow { |name|
		var object = PT(name);
		^"% %  %".format(
			if(object.isMade) { "*" } { " " },
			name.asString.padRight(22),
			object.type.asString
		)
	}

	prUpdateInfo {
		var object = this.selected;
		infoView.string_(
			if(object.isNil) { "" } {
				"% : %\n% events   seed %   %".format(
					object.name, object.type,
					object.length, object.seed ? "-", object.comment
				)
			}
		);
	}

	update { |changed, what| if(what == \objects or: { what == \made }) { this.refresh } }

	close { window.close }
}
