#!/usr/bin/env python3
"""Build doc/tutorial.html from doc/TUTORIAL.md.

Regenerate after editing the tutorial:

    python3 doc/build-tutorial.py

The markdown is the source. This script only renders it, so nothing here should
change what the tutorial says.
"""

import html
import pathlib
import re

import markdown

HERE = pathlib.Path(__file__).parent
SOURCE = HERE / "TUTORIAL.md"
TARGET = HERE / "tutorial.html"


# --------------------------------------------------------------- sclang colour

def highlight(code: str) -> str:
    """Colour comments and string literals only.

    Slot values are strings, so colouring them is the useful part: the reader can
    see at a glance which text goes into a field. Anything more ambitious risks
    mangling the code, which would be worse than plain text.
    """
    out, i, n = [], 0, len(code)
    while i < n:
        char = code[i]
        if char == '"':
            j = i + 1
            while j < n and code[j] != '"':
                if code[j] == "\\":
                    j += 1
                j += 1
            piece = code[i : min(j + 1, n)]
            out.append(f'<span class="s">{html.escape(piece)}</span>')
            i = j + 1
        elif char == "/" and i + 1 < n and code[i + 1] == "/":
            j = code.find("\n", i)
            j = n if j == -1 else j
            out.append(f'<span class="c">{html.escape(code[i:j])}</span>')
            i = j
        else:
            out.append(html.escape(char))
            i += 1
    return "".join(out)


# ------------------------------------------------------------------- rendering

def render_body(text: str) -> str:
    body = markdown.markdown(
        text,
        extensions=["tables", "fenced_code", "toc"],
        output_format="html5",
    )

    # Two kinds of block, set differently. A ```supercollider fence is code you
    # run; a bare fence is a parameter listing, shown the way the AC Toolbox
    # tutorial shows the contents of a dialog.
    def code_block(match):
        return (
            '<pre class="code"><code>'
            + highlight(html.unescape(match.group(1)))
            + "</code></pre>"
        )

    body = re.sub(
        r'<pre><code class="language-supercollider">(.*?)</code></pre>',
        code_block,
        body,
        flags=re.S,
    )
    body = re.sub(
        r"<pre><code>(.*?)</code></pre>",
        r'<pre class="listing"><code>\1</code></pre>',
        body,
        flags=re.S,
    )

    # Reader actions keep Berg's bullet, but as a styled marker rather than a
    # character sitting inside the sentence.
    body = body.replace("<li>• ", '<li class="action">')
    body = body.replace("<p>• ", '<p class="action">')

    # A run of "**Label** value" lines is a summary, which reads as a list of
    # pairs rather than as a paragraph with invisible line breaks.
    def summary(match):
        lines = [line for line in match.group(1).split("\n") if line.strip()]
        rows = []
        for line in lines:
            start = re.match(r"^<strong>(.*?)</strong>\s*(.*)$", line)
            if start:
                rows.append([start.group(1), start.group(2)])
            elif rows:
                # a value wrapped onto the next line in the source
                rows[-1][1] += " " + line.strip()
            else:
                # a prose paragraph that merely opens in bold: leave it alone
                return match.group(0)
        items = "".join(
            f"<dt>{key.strip()}</dt><dd>{value.strip()}</dd>" for key, value in rows
        )
        return f'<dl class="summary">{items}</dl>'

    body = re.sub(r"<p>(<strong>.*?)</p>", summary, body, flags=re.S)

    # Chapter numbers into the margin rail. The numbering is real: the tutorials
    # are a sequence, and the reader is told to follow it.
    body = re.sub(
        r'<h2 id="(tutorial-\d+[^"]*)">Tutorial (\d+): (.*?)</h2>',
        r'<h2 id="\1"><span class="num">\2</span>\3</h2>',
        body,
    )

    # Every table can scroll on its own rather than pushing the page sideways.
    body = body.replace("<table>", '<div class="scroll"><table>').replace(
        "</table>", "</table></div>"
    )
    return body


SHELL = """<meta charset="utf-8">
<title>Using the Pattern Toolbox</title>
<style>
:root {{
  --ground: #ffffff;
  --ink: #16181d;
  --ink-soft: #5c626e;
  --ink-faint: #8b919c;
  --rule: #e4e6ea;
  --panel: #f7f8fa;
  --accent: #1a4fd6;

  --sans: "Avenir Next", Avenir, "Segoe UI", system-ui, sans-serif;
  --serif: Charter, "Bitstream Charter", "Iowan Old Style", Palatino,
           "Palatino Linotype", Georgia, serif;
  --mono: "PT Mono", Menlo, "DejaVu Sans Mono", Consolas, monospace;

  --measure: 66ch;
  --rail: 3.5rem;
}}

*, *::before, *::after {{ box-sizing: border-box; }}

body {{
  margin: 0;
  background: var(--ground);
  color: var(--ink);
  font-family: var(--serif);
  font-size: 17px;
  line-height: 1.62;
  -webkit-font-smoothing: antialiased;
}}

.page {{
  max-width: calc(var(--measure) + var(--rail) + 3rem);
  margin: 0 auto;
  padding: 5rem 1.5rem 8rem calc(1.5rem + var(--rail));
}}

/* ------------------------------------------------------------------ headings */

h1, h2, h3, h4 {{
  font-family: var(--sans);
  font-weight: 600;
  text-wrap: balance;
  letter-spacing: -0.011em;
  color: var(--ink);
}}

h1 {{
  font-size: 2.6rem;
  line-height: 1.1;
  margin: 0 0 0.4rem;
  letter-spacing: -0.028em;
}}

h1 + h2 {{
  font-family: var(--serif);
  font-style: italic;
  font-weight: 400;
  font-size: 1.25rem;
  color: var(--ink-soft);
  border: 0;
  padding: 0;
  margin: 0 0 4rem;
}}

h2 {{
  font-size: 1.5rem;
  margin: 4.5rem 0 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--rule);
  position: relative;
}}

h2 .num {{
  position: absolute;
  left: calc(var(--rail) * -1);
  top: 1.5rem;
  width: calc(var(--rail) - 1rem);
  text-align: right;
  font-family: var(--mono);
  font-size: 0.85rem;
  font-weight: 400;
  line-height: 1.75;
  color: var(--accent);
}}

h3 {{ font-size: 1.075rem; margin: 2.75rem 0 0.85rem; }}

/* ---------------------------------------------------------------------- text */

p, ul, ol, dl {{ margin: 0 0 1.25rem; max-width: var(--measure); }}

a {{ color: var(--accent); text-decoration-thickness: 1px; text-underline-offset: 2px; }}
a:focus-visible {{ outline: 2px solid var(--accent); outline-offset: 3px; border-radius: 2px; }}

strong {{ font-weight: 600; }}

/* the example-file note under each chapter heading */
h2 + p > em:only-child {{
  font-family: var(--sans);
  font-style: normal;
  font-size: 0.82rem;
  letter-spacing: 0.01em;
  color: var(--ink-faint);
}}

ul, ol {{ padding-left: 1.15rem; }}
li {{ margin-bottom: 0.35rem; }}
li::marker {{ color: var(--ink-faint); }}

/* Berg's reader actions: do this, now */
li.action, p.action {{
  list-style: none;
  position: relative;
  padding-left: 1.4rem;
  color: var(--ink-soft);
}}
li.action {{ margin-left: -1.15rem; }}
li.action::before, p.action::before {{
  /* a CSS unicode escape, not the character: the ASCII pass over the finished
     page would turn a literal bullet into an HTML entity, which CSS cannot read */
  content: "\\2022";
  position: absolute;
  left: 0.35rem;
  color: var(--accent);
  font-weight: 700;
}}

blockquote {{
  margin: 1.25rem 0;
  padding-left: 1rem;
  border-left: 2px solid var(--rule);
  color: var(--ink-soft);
}}
blockquote p {{ margin: 0; }}

hr {{ display: none; }}

/* ---------------------------------------------------------------------- code */

code {{
  font-family: var(--mono);
  font-size: 0.85em;
  color: var(--ink);
}}

pre {{
  margin: 1.5rem 0;
  padding: 1rem 1.15rem;
  overflow-x: auto;
  font-size: 0.82rem;
  line-height: 1.62;
  tab-size: 4;
}}
pre code {{ font-size: inherit; }}

/* code you run */
pre.code {{
  background: var(--panel);
  border-left: 2px solid var(--accent);
  border-radius: 0 3px 3px 0;
}}
pre.code .c {{ color: var(--ink-faint); }}
pre.code .s {{ color: var(--accent); }}

/* the contents of a dialog, as the AC Toolbox tutorial shows them */
pre.listing {{
  background: none;
  border: 1px solid var(--rule);
  border-radius: 3px;
  color: var(--ink-soft);
}}

/* -------------------------------------------------------------------- tables */

.scroll {{ overflow-x: auto; margin: 1.5rem 0; max-width: var(--measure); }}

table {{
  border-collapse: collapse;
  width: 100%;
  font-family: var(--sans);
  font-size: 0.87rem;
  font-variant-numeric: tabular-nums;
}}

th {{
  text-align: left;
  font-weight: 600;
  font-size: 0.72rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--ink-faint);
  border-bottom: 1px solid var(--ink);
  padding: 0 1.25rem 0.5rem 0;
}}

td {{
  vertical-align: top;
  padding: 0.55rem 1.25rem 0.55rem 0;
  border-bottom: 1px solid var(--rule);
}}
td:last-child, th:last-child {{ padding-right: 0; }}
td code {{ color: var(--ink); }}

/* ------------------------------------------------------------------ summary */

dl.summary {{
  display: grid;
  grid-template-columns: 7.5rem 1fr;
  gap: 0.4rem 1.25rem;
  margin: 1.25rem 0 0;
  padding-top: 1rem;
  border-top: 1px solid var(--rule);
  font-size: 0.92rem;
}}
dl.summary dt {{
  font-family: var(--sans);
  font-size: 0.72rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--ink-faint);
  padding-top: 0.3em;
}}
dl.summary dd {{
  margin: 0;
  color: var(--ink-soft);
  font-family: var(--sans);
  font-size: 0.88rem;
}}

/* ---------------------------------------------------------------- contents */

#contents + ul {{
  font-family: var(--sans);
  font-size: 0.95rem;
  list-style: none;
  padding: 0;
  columns: 2;
  column-gap: 2.5rem;
}}
#contents + ul li {{ margin-bottom: 0.5rem; break-inside: avoid; }}
#contents + ul a {{ text-decoration: none; color: var(--ink); }}
#contents + ul a:hover {{ color: var(--accent); text-decoration: underline; }}

/* ------------------------------------------------------------------ closing */

.page > p:last-child {{
  margin-top: 4rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--rule);
  font-size: 0.85rem;
  color: var(--ink-faint);
}}

@media (max-width: 700px) {{
  :root {{ --rail: 0rem; }}
  body {{ font-size: 16px; }}
  .page {{ padding: 3rem 1.25rem 5rem; }}
  h1 {{ font-size: 2rem; }}
  h2 .num {{ position: static; display: block; text-align: left; width: auto; }}
  #contents + ul {{ columns: 1; }}
  dl.summary {{ grid-template-columns: 1fr; gap: 0.15rem; }}
  dl.summary dd {{ margin-bottom: 0.6rem; }}
}}

@media print {{
  .page {{ padding: 0; max-width: none; }}
  pre, .scroll {{ break-inside: avoid; }}
  h2 {{ break-after: avoid; }}
}}
</style>

<article class="page">
{body}
</article>
"""


def main() -> None:
    body = render_body(SOURCE.read_text(encoding="utf-8"))
    page = SHELL.format(body=body)
    # Escape every non-ASCII character rather than relying on the container to
    # declare an encoding. Published inside another document's head, a meta
    # charset of ours may never be read, and the bullets turn into mojibake.
    page = page.encode("ascii", "xmlcharrefreplace").decode("ascii")
    TARGET.write_text(page, encoding="ascii")
    print(f"wrote {TARGET} ({TARGET.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
