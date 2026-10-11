#!/usr/bin/env python3
"""Builds docs/FinSphere-Nexus-Run-and-Test-Guide.pdf from README.md.

The PDF holds the README's guide, from "Run and test, step by step" up to "Configuration": the steps, the API
reference, exception handling, timeouts, the loading screen, redirects, troubleshooting and the development setup.
README.md stays the only source, so edit it and run this again.

Needs Python 3.10+ and a Chromium for Playwright:
    pip install markdown-it-py playwright
    playwright install chromium
    python3 scripts/build-guide-pdf.py
"""

from __future__ import annotations

import html
import re
import sys
from datetime import date
from pathlib import Path

from markdown_it import MarkdownIt
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent.parent
README = ROOT / "README.md"
OUTPUT = ROOT / "docs" / "FinSphere-Nexus-Run-and-Test-Guide.pdf"
FONTS = ROOT / "frontend-service" / "src" / "main" / "resources" / "static" / "fonts"

FIRST_SECTION = "## Run and test, step by step"
END_SECTION = "## Configuration"

# Sections that start on a new page
NEW_PAGE = {"run-and-test-step-by-step", "api-reference", "exception-handling-reference", "troubleshooting",
            "run-on-your-machine-for-development"}


def slug(text: str) -> str:
    """The anchor GitHub gives a heading, so the README's own links work in the PDF."""
    text = re.sub(r"<[^>]+>", "", text).strip().lower()
    text = re.sub(r"[^\w\- ]", "", text)
    return text.replace(" ", "-")


def guide_markdown() -> str:
    readme = README.read_text(encoding="utf-8")
    start = readme.index(FIRST_SECTION)
    end = readme.index(END_SECTION, start)
    return readme[start:end]


def render(markdown: str) -> tuple[str, list[tuple[int, str, str]]]:
    md = MarkdownIt("commonmark", {"html": False, "typographer": False}).enable("table")
    tokens = md.parse(markdown)
    headings: list[tuple[int, str, str]] = []
    for index, token in enumerate(tokens):
        if token.type == "heading_open":
            level = int(token.tag[1])
            text = tokens[index + 1].content
            anchor = slug(text)
            token.attrSet("id", anchor)
            if level == 2 and anchor in NEW_PAGE:
                token.attrSet("class", "new-page")
            headings.append((level, text, anchor))
        elif token.type == "link_open":
            href = token.attrGet("href") or ""
            if href.startswith("http"):
                token.attrSet("class", "external")
    body = md.renderer.render(tokens, md.options, {})
    # Steps such as "1. What you need" carry their number in a separate span for the margin numeral
    body = re.sub(r'<h3 id="([^"]+)">(\d+)\. ', r'<h3 id="\1"><span class="step">\2</span>', body)
    # Short tables stay on one page, long ones break between rows and repeat their header
    body = re.sub(r"<table>(.*?)</table>",
                  lambda table: ('<table class="keep">' if table.group(1).count("<tr>") <= 11 else "<table>")
                  + table.group(1) + "</table>", body, flags=re.S)
    return body, headings


def contents(headings: list[tuple[int, str, str]]) -> str:
    items = []
    for level, text, anchor in headings:
        if level == 2:
            items.append(f'<li class="toc-section"><a href="#{anchor}">{html.escape(text)}</a></li>')
        elif level == 3:
            items.append(f'<li class="toc-step"><a href="#{anchor}">{html.escape(text)}</a></li>')
    return "\n".join(items)


STYLE = """
@font-face { font-family: "Anek Latin"; src: url("%(fonts)s/anek-latin-var.woff2") format("woff2");
  font-weight: 400 700; font-stretch: 75%% 100%%; }
@font-face { font-family: "Atkinson Hyperlegible Next";
  src: url("%(fonts)s/atkinson-hyperlegible-next-var.woff2") format("woff2"); font-weight: 400 700; }

:root {
  --plum: #2f2348; --plum-mid: #6a55a6; --lavender: #ece8f6; --lavender-line: #ddd6f1; --lavender-faint: #f6f4fb;
  --note: #f5b800; --note-soft: #fff3c4; --ink: #1e1b2a; --muted: #5c586c; --rule: #e3e0ea; --link: #5636b0;
}

@page {
  size: A4;
  margin: 20mm 18mm 18mm 18mm;
  @bottom-left { content: "FinSphere Nexus 2.0, run and test guide"; font-family: "Atkinson Hyperlegible Next";
    font-size: 7.5pt; color: #858196; vertical-align: middle; }
  @bottom-right { content: "Page " counter(page) " of " counter(pages); font-family: "Atkinson Hyperlegible Next";
    font-size: 7.5pt; color: #858196; vertical-align: middle; }
}
@page :first {
  margin: 0;
  @bottom-left { content: none; }
  @bottom-right { content: none; }
}

* { box-sizing: border-box; }
html { font-family: "Atkinson Hyperlegible Next", sans-serif; color: var(--ink); font-size: 9.6pt;
  line-height: 1.5; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
body { margin: 0; }

/* ------------------------------------------------------------ cover */
.cover { height: 297mm; padding: 30mm 22mm 24mm; background: var(--plum); color: #fff; display: flex;
  flex-direction: column; break-after: page; position: relative; overflow: hidden; }
.cover .product { font-family: "Anek Latin"; font-weight: 600; font-size: 13pt; letter-spacing: 0.01em;
  color: #cbc1e8; margin: 0; }
.cover h1 { font-family: "Anek Latin"; font-stretch: 85%%; font-weight: 700; font-size: 46pt; line-height: 1.02;
  margin: 10mm 0 8mm; max-width: 140mm; }
.cover .lead { font-size: 12.5pt; line-height: 1.5; color: #ece7f8; max-width: 125mm; margin: 0; }
.cover .board { position: absolute; right: 22mm; bottom: 34mm; display: grid; grid-template-columns: repeat(3, 15mm);
  gap: 3.2mm; }
.cover .board span { width: 15mm; height: 15mm; border-radius: 2.4mm; background: rgba(255,255,255,0.13); }
.cover .board span.taken { background: rgba(203,193,232,0.55); }
.cover .board span.prized { background: var(--note); }
.cover .meta { margin-top: auto; font-size: 10pt; color: #b5abd2; line-height: 1.7; }
.cover .meta strong { color: #fff; font-weight: 600; }

/* ------------------------------------------------------------ contents */
.contents { break-after: page; }
.contents h2 { margin-top: 0; }
.contents ol { list-style: none; padding: 0; margin: 0; columns: 1; }
.contents li a { color: var(--ink); text-decoration: none; }
.toc-section { font-family: "Anek Latin"; font-weight: 600; font-size: 12pt; margin: 4.5mm 0 1.2mm;
  padding-top: 2.5mm; border-top: 0.3mm solid var(--rule); }
.toc-section:first-child { border-top: 0; padding-top: 0; margin-top: 0; }
.toc-step { margin: 0.6mm 0 0.6mm 6mm; color: var(--muted); }
.toc-step a { color: var(--ink); }
.how { background: var(--lavender-faint); border-left: 1.2mm solid var(--plum-mid); padding: 3.5mm 5mm;
  margin: 0 0 8mm; border-radius: 0 1.6mm 1.6mm 0; }
.how p { margin: 0 0 1.5mm; max-width: none; }
.how p:last-child { margin-bottom: 0; }

/* ------------------------------------------------------------ text */
h2, h3, h4 { font-family: "Anek Latin"; color: var(--plum); break-after: avoid; line-height: 1.15; }
h2 { font-size: 21pt; font-stretch: 90%%; font-weight: 700; margin: 0 0 5mm; padding-bottom: 2.5mm;
  border-bottom: 0.6mm solid var(--plum); }
h2.new-page { break-before: page; }
h3 { font-size: 14pt; font-weight: 650; margin: 8mm 0 2.5mm; display: flex; align-items: center; gap: 2.6mm;
  break-inside: avoid; }
h3 .step { display: inline-flex; align-items: center; justify-content: center; min-width: 7.2mm; height: 7.2mm;
  border-radius: 1.6mm; background: var(--plum); color: #fff; font-size: 11pt; font-weight: 700;
  font-stretch: 85%%; flex: none; }
h4 { font-size: 11.5pt; font-weight: 650; margin: 6mm 0 2mm; color: var(--ink); }
p, li { max-width: 158mm; }
p { margin: 0 0 2.4mm; }
ul, ol { margin: 0 0 3mm; padding-left: 5.5mm; }
li { margin: 0.8mm 0; }
li > ul, li > ol { margin: 1mm 0 1.2mm; }
ol > li::marker { font-weight: 700; color: var(--plum-mid); }
ul > li::marker { color: var(--plum-mid); }
strong { font-weight: 700; }
a { color: var(--link); text-decoration: none; }
a.external { text-decoration: underline; text-decoration-color: var(--lavender-line); text-underline-offset: 0.6mm; }

code { font-family: "DejaVu Sans Mono", monospace; font-size: 0.86em; background: var(--lavender-faint);
  border: 0.2mm solid var(--lavender-line); border-radius: 0.8mm; padding: 0 0.7mm; }
pre { background: #f7f6fa; border: 0.25mm solid var(--rule); border-left: 1mm solid var(--plum-mid);
  border-radius: 1.2mm; padding: 2.8mm 3.6mm; margin: 1.5mm 0 3.5mm; break-inside: avoid; white-space: pre-wrap;
  word-break: break-word; }
pre code { background: none; border: 0; padding: 0; font-size: 8.1pt; line-height: 1.5; color: #241a3a; }

/* A step's intro, its command and the lines after it stay together */
li:has(> pre) { break-inside: avoid; }
p:has(+ pre), p:has(+ table) { break-after: avoid; }

table { width: 100%%; border-collapse: collapse; margin: 1.5mm 0 4.5mm; font-size: 8.7pt; line-height: 1.4; }
table.keep { break-inside: avoid; }
thead { display: table-header-group; }
tr { break-inside: avoid; }
th { text-align: left; font-family: "Anek Latin"; font-weight: 650; font-size: 9pt; color: var(--plum);
  background: var(--lavender); padding: 1.6mm 2.2mm; border-bottom: 0.4mm solid var(--plum-mid); }
td { padding: 1.5mm 2.2mm; border-bottom: 0.25mm solid var(--rule); vertical-align: top; }
tbody tr:nth-child(even) td { background: #fbfafd; }
td code { font-size: 0.84em; word-break: break-word; }
"""


def build_html(body: str, headings: list[tuple[int, str, str]]) -> str:
    today = date.today().strftime("%d %B %Y").lstrip("0")
    board = "".join(f'<span class="{cls}"></span>' for cls in
                    ["taken", "", "taken", "taken", "prized", "", "taken", "taken", ""])
    return f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"><title>FinSphere Nexus 2.0: Run and test guide</title>
<style>{STYLE % {"fonts": FONTS.as_uri()}}</style></head>
<body>
<section class="cover">
  <p class="product">FinSphere Nexus 2.0</p>
  <h1>Run and test guide</h1>
  <p class="lead">Start the whole system, run every business flow in the web app and in Swagger UI, and see how each
  failure is handled, one step at a time.</p>
  <div class="board" aria-hidden="true">{board}</div>
  <p class="meta"><strong>Chit funds and daily-collection loans</strong><br>
  Java 25, Spring Boot 4.1, Spring Cloud 2025.1<br>{today}</p>
</section>
<section class="contents">
  <h2>Contents</h2>
  <div class="how">
    <p><strong>How to use this guide.</strong> Follow steps 1 to 5 once to start the system. Steps 6 to 9 test it:
    the web app as an administrator and as a member, the APIs in Swagger UI, and the failure handling.</p>
    <p>Each step says what to do, what you should see, and what happens when something goes wrong. The reference
    sections after the steps list every API operation, every error, every time limit and every redirect.</p>
  </div>
  <ol>{contents(headings)}</ol>
</section>
{body}
</body></html>"""


def main() -> int:
    body, headings = render(guide_markdown())
    page = build_html(body, headings)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    html_file = OUTPUT.with_suffix(".html")
    html_file.write_text(page, encoding="utf-8")
    try:
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch()
            tab = browser.new_page()
            tab.goto(html_file.as_uri(), wait_until="networkidle")
            tab.evaluate("document.fonts.ready.then(() => true)")
            # The footer (title and page numbers) comes from the @page margin boxes, which the cover leaves out
            tab.pdf(path=str(OUTPUT), format="A4", print_background=True, prefer_css_page_size=True)
            browser.close()
    finally:
        html_file.unlink(missing_ok=True)
    print(f"Wrote {OUTPUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
