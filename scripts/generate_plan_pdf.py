import os
import re
import subprocess
import tempfile
import mistune

def generate_plan_pdf():
    workspace = r"c:\Users\aatha\Desktop\PM - Projects\Android_apptimer_productivity"
    md_file = os.path.join(workspace, "Development_Plan.md")
    pdf_file = os.path.join(workspace, "Development_Plan_Focus_and_Reminder.pdf")

    with open(md_file, "r", encoding="utf-8") as f:
        md_text = f.read()

    processed_md = md_text

    # Style the governing rule callout
    processed_md = re.sub(
        r'\*\*Validate platform feasibility → define architecture → implement one vertical slice at a time → test on a real device → expand\.\*\*',
        r'<div class="callout-box"><div class="callout-title">Governing Rule</div><div class="callout-text">Validate platform feasibility &rarr; define architecture &rarr; implement one vertical slice at a time &rarr; test on a real device &rarr; expand.</div></div>',
        processed_md
    )

    # Style Phase headers
    processed_md = re.sub(
        r'### (Phase \d+ — [^\n]+)',
        r'### <span class="phase-badge">\1</span>',
        processed_md
    )

    # Render markdown to HTML
    body_html = mistune.html(processed_md)

    # Convert top header
    top_header_replacement = """
    <div class="doc-header">
        <div class="header-badge">ENGINEERING & PRODUCT ROADMAP</div>
        <h1 class="doc-title">Development Roadmap</h1>
        <p class="doc-subtitle">Focus & Reminder App | Phased Validation, Architecture, Vibe-Coding & Release Plan</p>
        <div class="meta-grid">
            <div class="meta-item"><span class="meta-label">Purpose</span><span class="meta-value">Execution Roadmap & Technical Sequencing</span></div>
            <div class="meta-item"><span class="meta-label">Target App</span><span class="meta-value">Focus & Reminder (Android)</span></div>
            <div class="meta-item"><span class="meta-label">Approach</span><span class="meta-value">Bounded Increments & Physical Device Validation</span></div>
            <div class="meta-item"><span class="meta-label">Core Tech</span><span class="meta-value">Native Android | Kotlin | Jetpack Compose</span></div>
            <div class="meta-item"><span class="meta-label">Current Phase</span><span class="meta-value phase-current">Phase 0 (Requirements Consolidation)</span></div>
            <div class="meta-item"><span class="meta-label">Architecture</span><span class="meta-value">Local-First (No Cloud Dependency)</span></div>
        </div>
    </div>
    """

    # Remove the first H1 and purpose line
    body_html = re.sub(r'<h1>.*?</h1>', '', body_html, count=1)
    body_html = re.sub(r'<p><strong>Purpose:</strong>.*?</p>', '', body_html, count=1)

    # Style final principle callout box
    body_html = re.sub(
        r'<p><strong>Final principle:</strong> (.*?)</p>',
        r'<div class="principle-box"><div class="principle-title">Final Principle</div><p>\1</p></div>',
        body_html
    )

    full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Development Roadmap: Focus & Reminder App</title>
<style>
    @charset "UTF-8";

    @page {{
        size: A4 portrait;
        margin: 16mm 16mm 16mm 16mm;
    }}

    * {{
        box-sizing: border-box;
    }}

    body {{
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        color: #1e293b;
        background: #ffffff;
        font-size: 9.5pt;
        line-height: 1.5;
        margin: 0;
        padding: 0;
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
    }}

    /* Header Styling */
    .doc-header {{
        border-bottom: 2px solid #e2e8f0;
        padding-bottom: 16px;
        margin-bottom: 20px;
    }}
    .header-badge {{
        display: inline-block;
        font-size: 7.5pt;
        font-weight: 700;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        background: #e0e7ff;
        color: #3730a3;
        padding: 3px 8px;
        border-radius: 4px;
        margin-bottom: 8px;
    }}
    .doc-title {{
        font-size: 22pt;
        font-weight: 800;
        color: #0f172a;
        margin: 0 0 4px 0;
        letter-spacing: -0.02em;
    }}
    .doc-subtitle {{
        font-size: 10.5pt;
        color: #475569;
        margin: 0 0 14px 0;
        font-weight: 400;
    }}
    .meta-grid {{
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 8px 14px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 6px;
        padding: 10px 14px;
    }}
    .meta-item {{
        display: flex;
        flex-direction: column;
    }}
    .meta-label {{
        font-size: 7pt;
        color: #64748b;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.04em;
    }}
    .meta-value {{
        font-size: 8.5pt;
        color: #0f172a;
        font-weight: 500;
    }}
    .phase-current {{
        color: #2563eb;
        font-weight: 700;
    }}

    /* Headings */
    h2 {{
        font-size: 13pt;
        font-weight: 700;
        color: #0f172a;
        border-bottom: 1px solid #e2e8f0;
        padding-bottom: 4px;
        margin-top: 22px;
        margin-bottom: 10px;
        letter-spacing: -0.01em;
        page-break-after: avoid;
        break-after: avoid;
    }}
    h3 {{
        font-size: 10.5pt;
        font-weight: 600;
        color: #1e293b;
        margin-top: 16px;
        margin-bottom: 8px;
        page-break-after: avoid;
        break-after: avoid;
    }}
    h4 {{
        font-size: 9.5pt;
        font-weight: 600;
        color: #334155;
        margin-top: 10px;
        margin-bottom: 4px;
        page-break-after: avoid;
        break-after: avoid;
    }}

    p {{
        margin: 0 0 8px 0;
        text-align: justify;
    }}

    ul, ol {{
        margin: 0 0 10px 0;
        padding-left: 20px;
    }}
    li {{
        margin-bottom: 3px;
    }}

    /* Phase Badges */
    .phase-badge {{
        display: inline-block;
        background: #f1f5f9;
        color: #0f172a;
        border-left: 4px solid #2563eb;
        padding: 3px 8px;
        border-radius: 0 4px 4px 0;
        font-size: 10pt;
        font-weight: 700;
    }}

    /* Callout & Principle Boxes */
    .callout-box {{
        background: #f0fdf4;
        border: 1.5px solid #86efac;
        border-radius: 6px;
        padding: 10px 14px;
        margin: 12px 0 16px 0;
        page-break-inside: avoid;
        break-inside: avoid;
    }}
    .callout-title {{
        font-size: 7.5pt;
        text-transform: uppercase;
        font-weight: 700;
        color: #166534;
        letter-spacing: 0.05em;
        margin-bottom: 4px;
    }}
    .callout-text {{
        font-size: 9.5pt;
        font-weight: 600;
        color: #14532d;
    }}

    .principle-box {{
        background: #f8fafc;
        border: 1.5px solid #cbd5e1;
        border-left: 5px solid #4338ca;
        border-radius: 6px;
        padding: 12px 16px;
        margin: 16px 0;
        page-break-inside: avoid;
        break-inside: avoid;
    }}
    .principle-title {{
        font-size: 8pt;
        text-transform: uppercase;
        font-weight: 700;
        color: #3730a3;
        letter-spacing: 0.05em;
        margin-bottom: 4px;
    }}
    .principle-box p {{
        margin: 0;
        color: #1e293b;
        font-weight: 500;
    }}

    /* Tables */
    table {{
        width: 100%;
        border-collapse: collapse;
        margin: 12px 0 16px 0;
        font-size: 8.5pt;
        page-break-inside: avoid;
        break-inside: avoid;
        border: 1px solid #cbd5e1;
        border-radius: 4px;
        overflow: hidden;
    }}
    th {{
        background: #f1f5f9;
        color: #0f172a;
        text-align: left;
        font-weight: 600;
        padding: 6px 10px;
        border: 1px solid #cbd5e1;
        font-size: 8pt;
        letter-spacing: 0.02em;
    }}
    td {{
        padding: 6px 10px;
        border: 1px solid #e2e8f0;
        vertical-align: top;
    }}
    tr:nth-child(even) td {{
        background: #f8fafc;
    }}

    /* Dividers & Misc */
    hr {{
        border: 0;
        height: 1px;
        background: #e2e8f0;
        margin: 18px 0;
    }}

    blockquote {{
        border-left: 3px solid #2563eb;
        background: #f8fafc;
        margin: 10px 0;
        padding: 6px 12px;
        color: #334155;
        font-style: italic;
    }}

    code {{
        font-family: Consolas, "Courier New", monospace;
        font-size: 8pt;
        background: #f1f5f9;
        padding: 1px 4px;
        border-radius: 3px;
        color: #0f172a;
    }}
</style>
</head>
<body>
    {top_header_replacement}
    {body_html}
</body>
</html>
"""

    # Use a temporary HTML file inside the scripts directory, and delete it immediately after PDF creation
    temp_html = os.path.join(workspace, "scripts", "temp_plan.html")
    with open(temp_html, "w", encoding="utf-8") as f:
        f.write(full_html)

    chrome_path = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
    args = [
        chrome_path,
        "--headless=new",
        "--disable-gpu",
        "--no-pdf-header-footer",
        f"--print-to-pdf={pdf_file}",
        temp_html
    ]

    print("Running Chrome to generate Development Plan PDF...")
    res = subprocess.run(args, capture_output=True, text=True)

    # Clean up the temp HTML file immediately
    if os.path.exists(temp_html):
        os.remove(temp_html)

    if os.path.exists(pdf_file):
        size_kb = os.path.getsize(pdf_file) / 1024
        print(f"Success! Generated {pdf_file} ({size_kb:.1f} KB)")
    else:
        print("Error: PDF file was not created. Output:", res.stdout, res.stderr)

if __name__ == "__main__":
    generate_plan_pdf()
