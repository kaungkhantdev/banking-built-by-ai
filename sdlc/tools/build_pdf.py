#!/usr/bin/env python3
"""Combine the SDLC markdown docs into one self-contained HTML, stdlib only.
Handles: headings, tables, fenced code, lists, checkboxes, blockquotes,
images (inlined base64), hr, inline bold/italic/code. Mermaid fences are
dropped from the PDF (the rendered PNGs are embedded instead)."""
import base64, html, mimetypes, os, re, sys

# tools/ lives under the repo root; resolve paths relative to this file
TOOLS = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(TOOLS)          # repo root: README.md + output PDF
DOCS = os.path.join(ROOT, "docs")      # the 12 markdown chapters + diagrams/
FILES = [(ROOT, "README.md")] + [(DOCS, f) for f in [
         "01-planning.md", "02-analysis.md",
         "03-design.md", "04-diagrams.md", "05-implementation-plan.md",
         "06-testing-strategy.md", "07-tech-stack.md", "08-deployment-infra.md",
         "09-backend-api.md", "10-mobile-app.md", "11-admin-dashboard.md",
         "12-gitops-deployment.md"]]

def inline_img(src):
    # image paths in the chapters are relative to docs/ (e.g. diagrams/erd.png)
    p = src if os.path.isabs(src) else os.path.join(DOCS, src)
    if not os.path.exists(p):
        return ""
    mime = mimetypes.guess_type(p)[0] or "image/png"
    b64 = base64.b64encode(open(p, "rb").read()).decode()
    return f'<div class="imgwrap"><img src="data:{mime};base64,{b64}"/></div>'

def inline_fmt(t):
    t = html.escape(t)
    t = re.sub(r'`([^`]+)`', r'<code>\1</code>', t)
    t = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', t)
    t = re.sub(r'(?<!\*)\*([^*]+)\*(?!\*)', r'<em>\1</em>', t)
    return t

def render(md):
    out, lines, i = [], md.split("\n"), 0
    in_details = False
    while i < len(lines):
        line = lines[i]
        # collapse <details>/<summary>/</details> wrappers (keep nothing—source hidden in PDF)
        if line.strip().startswith("<details"):
            in_details = True; i += 1; continue
        if line.strip().startswith("</details"):
            in_details = False; i += 1; continue
        if line.strip().startswith("<summary"):
            i += 1; continue
        # fenced code
        if line.strip().startswith("```"):
            lang = line.strip()[3:].strip()
            j = i + 1; buf = []
            while j < len(lines) and not lines[j].strip().startswith("```"):
                buf.append(lines[j]); j += 1
            i = j + 1
            if lang == "mermaid" or in_details:
                continue  # skip source dumps in PDF
            out.append("<pre><code>" + html.escape("\n".join(buf)) + "</code></pre>")
            continue
        if in_details:
            i += 1; continue
        # images
        m = re.match(r'!\[(.*?)\]\((.*?)\)', line.strip())
        if m:
            out.append(inline_img(m.group(2))); i += 1; continue
        # headings
        m = re.match(r'(#{1,6})\s+(.*)', line)
        if m:
            lvl = len(m.group(1))
            out.append(f"<h{lvl}>{inline_fmt(m.group(2))}</h{lvl}>")
            i += 1; continue
        # hr
        if re.match(r'^---+\s*$', line):
            out.append("<hr/>"); i += 1; continue
        # tables
        if "|" in line and i + 1 < len(lines) and re.match(r'^\s*\|?[\s:|-]+\|?\s*$', lines[i+1]) and "-" in lines[i+1]:
            header = [c.strip() for c in line.strip().strip("|").split("|")]
            i += 2; rows = []
            while i < len(lines) and "|" in lines[i]:
                rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")])
                i += 1
            t = "<table><thead><tr>" + "".join(f"<th>{inline_fmt(c)}</th>" for c in header) + "</tr></thead><tbody>"
            for r in rows:
                t += "<tr>" + "".join(f"<td>{inline_fmt(c)}</td>" for c in r) + "</tr>"
            t += "</tbody></table>"
            out.append(t); continue
        # blockquote
        if line.strip().startswith(">"):
            buf = []
            while i < len(lines) and lines[i].strip().startswith(">"):
                buf.append(lines[i].strip().lstrip(">").strip()); i += 1
            out.append("<blockquote>" + inline_fmt(" ".join(buf)) + "</blockquote>")
            continue
        # lists (incl checkboxes)
        if re.match(r'^\s*[-*]\s+', line):
            buf = []
            while i < len(lines) and re.match(r'^\s*[-*]\s+', lines[i]):
                item = re.sub(r'^\s*[-*]\s+', '', lines[i])
                item = item.replace("[x]", "&#9745;").replace("[ ]", "&#9744;")
                buf.append("<li>" + inline_fmt(item) + "</li>"); i += 1
            out.append("<ul>" + "".join(buf) + "</ul>")
            continue
        if re.match(r'^\s*\d+\.\s+', line):
            buf = []
            while i < len(lines) and re.match(r'^\s*\d+\.\s+', lines[i]):
                item = re.sub(r'^\s*\d+\.\s+', '', lines[i])
                buf.append("<li>" + inline_fmt(item) + "</li>"); i += 1
            out.append("<ol>" + "".join(buf) + "</ol>")
            continue
        # blank / paragraph
        if line.strip() == "":
            i += 1; continue
        out.append("<p>" + inline_fmt(line) + "</p>"); i += 1
    return "\n".join(out)

CSS = """
@font-face { font-family: 'DMSans'; font-weight: 100 900; font-style: normal;
  src: url('__FONT_URL__'); }
@page { size: A4; margin: 20mm 18mm; }
* { box-sizing: border-box; }
body { font-family: 'DMSans', -apple-system, 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px; line-height: 1.75; color: #4b5563; max-width: 100%;
  -webkit-font-smoothing: antialiased; letter-spacing: 0px; }

/* ---- Bold, near-black headings (Medium-style) ---- */
h1 { font-size: 34px; line-height: 1.12; font-weight: 800; color: #1a1a1a;
  letter-spacing: -0.8px; margin: 0 0 28px; padding-top: 8px;
  page-break-before: always; }
h1:first-of-type { page-break-before: avoid; }
h2 { font-size: 21px; font-weight: 800; color: #1a1a1a; letter-spacing: -0.4px;
  margin: 38px 0 14px; padding-bottom: 10px; border-bottom: 1px solid #e7eaf0; }
h3 { font-size: 16px; font-weight: 700; color: #242424; letter-spacing: -0.2px;
  margin: 26px 0 9px; }
h4 { font-size: 12.5px; font-weight: 700; color: #6b7280; text-transform: uppercase;
  letter-spacing: 1.2px; margin: 18px 0 7px; }
p { margin: 0 0 12px; }
strong { color: #242424; font-weight: 700; }
a { color: #2563eb; text-decoration: none; }
a.toc-link { color: inherit; }
a.toc-link code { color: #2563eb; }

/* ---- Clean borderless tables, hairline row dividers ---- */
table { border-collapse: collapse; width: 100%; margin: 18px 0; font-size: 12px;
  page-break-inside: avoid; }
th, td { padding: 10px 13px; text-align: left; vertical-align: top;
  border-bottom: 1px solid #edeff3; }
th { color: #8a92a0; font-weight: 700; text-transform: uppercase;
  font-size: 10px; letter-spacing: 0.6px; border-bottom: 1.5px solid #e0e3ea; }
td { color: #4b5563; }
tr:last-child td { border-bottom: none; }

/* ---- Soft inline code + light code cards ---- */
code { background: #f3f4f7; padding: 1.5px 6px; border-radius: 4px;
  font-family: 'SF Mono', 'JetBrains Mono', Menlo, monospace; font-size: 11px;
  color: #4f46e5; }
pre { background: #f7f8fa; color: #3a4151; padding: 17px 20px; border-radius: 8px;
  border: 1px solid #eceef2; overflow-x: auto; font-size: 9px; line-height: 1.55;
  margin: 16px 0;
  white-space: pre-wrap; word-break: break-word; overflow-wrap: anywhere;
  font-family: 'SF Mono', 'JetBrains Mono', Menlo, monospace; }
pre code { background: none; color: #3a4151; padding: 0; font-size: 9px;
  white-space: pre-wrap; word-break: break-word; }

/* ---- Quiet pastel callouts ---- */
blockquote { border-left: 3px solid #c7d0e0; background: #f8f9fb;
  margin: 16px 0; padding: 12px 18px; color: #6b7280; border-radius: 0 6px 6px 0;
  font-size: 13px; }
hr { border: none; border-top: 1px solid #eef0f4; margin: 28px 0; }
ul, ol { margin: 12px 0; padding-left: 22px; }
li { margin: 5px 0; padding-left: 4px; }
li::marker { color: #b8c0cf; }

/* ---- Diagrams as soft framed cards ---- */
.imgwrap { text-align: center; margin: 24px 0; page-break-inside: avoid; }
.imgwrap img { max-width: 94%; height: auto; border: 1px solid #eceef2;
  border-radius: 8px; padding: 16px; background: #fff; }

/* ---- Formal report cover ---- */
.cover { position: relative; min-height: 247mm; page-break-after: always;
  padding: 0; }
.cover .topband { display: flex; justify-content: space-between; align-items: center;
  border-bottom: 2px solid #1a1a1a; padding-bottom: 14px; margin-bottom: 0; }
.cover .topband .org { font-size: 12px; font-weight: 800; color: #1a1a1a;
  letter-spacing: 0.4px; }
.cover .topband .doctype { font-size: 10px; font-weight: 700; color: #6366f1;
  letter-spacing: 1.8px; text-transform: uppercase; }
.cover .titleblock { padding: 110px 0 40px; border-bottom: 4px solid #1a1a1a; }
.cover .eyebrow { font-size: 11px; font-weight: 700; color: #6366f1;
  letter-spacing: 2px; text-transform: uppercase; margin: 0 0 22px; }
.cover h1 { font-size: 60px; line-height: 1.02; font-weight: 800; color: #1a1a1a;
  letter-spacing: -2.2px; page-break-before: avoid; margin: 0 0 26px; max-width: 92%; }
.cover .subtitle { font-size: 19px; color: #4b5563; font-weight: 600;
  margin: 0 0 6px; letter-spacing: -0.2px; }
.cover .phases { font-size: 15px; color: #9aa1ad; font-weight: 500; margin: 0; }
.cover .abstract { margin: 40px 0 0; max-width: 100%; }
.cover .abstract h4 { margin: 0 0 12px; color: #1a1a1a; font-size: 10.5px;
  letter-spacing: 1.8px; text-transform: uppercase; font-weight: 800; }
.cover .abstract p { font-size: 13.5px; line-height: 1.8; color: #4b5563;
  text-align: justify; margin: 0; }
/* colophon metadata grid pinned to the bottom */
.cover .colophon { position: absolute; bottom: 0; left: 0; right: 0;
  display: flex; border-top: 2px solid #1a1a1a; padding-top: 16px; }
.cover .colophon .cell { flex: 1; padding-right: 16px; }
.cover .colophon .k { font-size: 8.5px; font-weight: 700; color: #9aa1ad;
  letter-spacing: 1.2px; text-transform: uppercase; margin: 0 0 5px; }
.cover .colophon .v { font-size: 11.5px; font-weight: 700; color: #1a1a1a;
  margin: 0; letter-spacing: -0.1px; }
"""

import datetime
_today = datetime.date.today().strftime("%B %d, %Y")
cover = f"""<div class="cover">
<div class="topband">
  <span class="org">SDLC Technical Dossier</span>
  <span class="doctype">Confidential</span>
</div>
<div class="titleblock">
  <p class="eyebrow">Software Requirements &amp; Design Specification</p>
  <h1>Digital Banking Platform</h1>
  <p class="subtitle">Software Development Life Cycle</p>
  <p class="phases">Planning &nbsp;&middot;&nbsp; Analysis &nbsp;&middot;&nbsp; Design &nbsp;&middot;&nbsp; Implementation &nbsp;&middot;&nbsp; Testing</p>
</div>
<div class="abstract">
<h4>Abstract</h4>
<p>This dossier specifies a secure, compliant, event-driven core-banking
platform delivered as three products &mdash; a <strong>backend API</strong>, a
<strong>customer mobile app</strong> (iOS + Android), and an <strong>admin
dashboard</strong> &mdash; covering customer accounts, wallets, a double-entry
transaction ledger, wallet-to-wallet transfers, KYC, an immutable audit trail,
and role-based access control. Enterprise concerns &mdash; JWT with
rotating refresh tokens, a multi-role authorization model, an API gateway with
rate limiting, a transactional-outbox event backbone, and full observability
via Prometheus, Grafana, and distributed tracing &mdash; are treated as
first-class, launch-blocking requirements rather than later add-ons. The
document walks the five SDLC phases end to end and includes an
entity-relationship diagram plus mobile and dashboard flow diagrams that trace
every requirement to a concrete design decision.</p>
</div>
<div class="colophon">
  <div class="cell"><p class="k">Document</p><p class="v">SDLC-DBP-001</p></div>
  <div class="cell"><p class="k">Version</p><p class="v">1.0</p></div>
  <div class="cell"><p class="k">Date</p><p class="v">{_today}</p></div>
  <div class="cell"><p class="k">Classification</p><p class="v">Confidential</p></div>
  <div class="cell"><p class="k">Grade</p><p class="v">Enterprise Fintech</p></div>
</div>
</div>"""

def linkify_contents(htmlstr):
    # In the README contents table, turn `docs/NN-name.md` code cells into
    # internal links that jump to that chapter's anchor in the PDF.
    return re.sub(
        r'<code>docs/([\w-]+)\.md</code>',
        r'<a class="toc-link" href="#file-\1"><code>docs/\1.md</code></a>',
        htmlstr,
    )

body = [cover]
for base, f in FILES:
    stem = f[:-3] if f.endswith(".md") else f      # filename without .md
    rendered = render(open(os.path.join(base, f)).read())
    if f == "README.md":
        rendered = linkify_contents(rendered)
    # anchor target sits immediately before the chapter's first heading
    body.append(f'<a id="file-{stem}"></a>' + rendered)

font_url = "file://" + os.path.join(TOOLS, "fonts", "DMSans-VF.ttf")
css = CSS.replace("__FONT_URL__", font_url)
doc = f"<!DOCTYPE html><html><head><meta charset='utf-8'><style>{css}</style></head><body>{''.join(body)}</body></html>"
open(os.path.join(TOOLS, "_combined.html"), "w").write(doc)
print("wrote _combined.html", len(doc), "bytes")
