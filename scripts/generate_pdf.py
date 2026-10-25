import os
import re
import subprocess
import mistune

def generate_pdf():
    workspace = r"c:\Users\aatha\Desktop\PM - Projects\Android_apptimer_productivity"
    md_file = os.path.join(workspace, "PRD.md")
    pdf_file = os.path.join(workspace, "PRD_Focus_and_Reminder.pdf")
    temp_html = os.path.join(workspace, "scripts", "temp_prd.html")

    with open(md_file, "r", encoding="utf-8") as f:
        md_text = f.read()

    # Pre-process math and LaTeX notation
    processed_md = md_text

    # Replace block LaTeX math
    processed_md = re.sub(
        r'\\\[\s*U\(t\)\s*\\geq\s*B\s*\\\]',
        '<div class="math-box"><div class="math-title">Blocking Condition</div><div class="math-formula"><em>U</em>(<em>t</em>) &ge; <em>B</em></div></div>',
        processed_md
    )
    processed_md = re.sub(
        r'\\\[\s*t_\{?\\text\{unlock\}\}?\s*\\geq\s*t_\{?\\text\{block\}\}?\s*\+\s*30\\text\{\s*minutes\}\s*\\\]',
        '<div class="math-box"><div class="math-title">Unlock Condition</div><div class="math-formula"><em>t</em><sub>unlock</sub> &ge; <em>t</em><sub>block</sub> + 30 minutes</div></div>',
        processed_md
    )

    # Inline math
    processed_md = re.sub(r'\\\(B = 30\\\)', '<em>B</em> = 30', processed_md)
    processed_md = re.sub(r'\\\(W = 60\\\)', '<em>W</em> = 60', processed_md)
    processed_md = re.sub(r'\\\(U\(t\)\\\)', '<em>U</em>(<em>t</em>)', processed_md)

    # Render markdown to HTML
    body_html = mistune.html(processed_md)

    # Replace priority text with styled badges
    body_html = re.sub(
        r'<p><strong>Priority: P0(.*?)</strong></p>',
        r'<div class="priority-container"><span class="badge badge-p0">PRIORITY P0\1</span></div>',
        body_html
    )
    body_html = re.sub(
        r'<p><strong>Priority: P1(.*?)</strong></p>',
        r'<div class="priority-container"><span class="badge badge-p1">PRIORITY P1\1</span></div>',
        body_html
    )
    body_html = re.sub(
        r'<p><strong>Priority: P2(.*?)</strong></p>',
        r'<div class="priority-container"><span class="badge badge-p2">PRIORITY P2\1</span></div>',
        body_html
    )

    # Convert the top metadata section into a sleek header card
    top_header_replacement = """
    <div class="doc-header">
        <div class="header-badge">PRODUCT REQUIREMENTS DOCUMENT</div>
        <h1 class="doc-title">Focus & Reminder</h1>
        <p class="doc-subtitle">Android Productivity App | Enforceable App-Usage Limits &amp; High-Attention Reminders</p>
        <div class="meta-grid">
            <div class="meta-item"><span class="meta-label">Version</span><span class="meta-value">1.1</span></div>
            <div class="meta-item"><span class="meta-label">Status</span><span class="meta-value status-draft" style="color: #166534;">Scope Frozen (Decisions Resolved)</span></div>
            <div class="meta-item"><span class="meta-label">Platform</span><span class="meta-value">Android 10+ (API 29–35)</span></div>
            <div class="meta-item"><span class="meta-label">Product Category</span><span class="meta-value">Digital Wellbeing &amp; Productivity</span></div>
            <div class="meta-item"><span class="meta-label">Working Name</span><span class="meta-value">Focus &amp; Reminder (temporary)</span></div>
            <div class="meta-item"><span class="meta-label">Target Release</span><span class="meta-value">MVP (Local-First)</span></div>
        </div>
    </div>
    """

    # Remove the first H1 and its meta paragraph since we have doc-header
    body_html = re.sub(r'<h1>.*?</h1>', '', body_html, count=1)
    body_html = re.sub(r'<p><strong>Version:</strong>.*?</p>', '', body_html, flags=re.DOTALL, count=1)
    body_html = re.sub(r'<hr\s*/?>', '', body_html, count=1) # remove first hr

    # Format Success Metrics and Default Configuration tables
    # Enhance Definition of Success box at the end
    body_html = re.sub(
        r'<p><strong>Definition of success:</strong> (.*?)</p>',
        r'<div class="success-box"><div class="success-title">Definition of Success</div><p>\1</p></div>',
        body_html
    )

    full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>PRD: Focus & Reminder — Android Productivity App</title>
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
    .status-draft {{
        color: #b45309;
        font-weight: 600;
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
        margin-top: 14px;
        margin-bottom: 6px;
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

    /* Badges */
    .priority-container {{
        margin: 6px 0 10px 0;
    }}
    .badge {{
        display: inline-block;
        font-size: 7pt;
        font-weight: 700;
        letter-spacing: 0.05em;
        text-transform: uppercase;
        padding: 2px 7px;
        border-radius: 4px;
    }}
    .badge-p0 {{
        background: #fee2e2;
        color: #991b1b;
        border: 1px solid #fecaca;
    }}
    .badge-p1 {{
        background: #fef3c7;
        color: #92400e;
        border: 1px solid #fde68a;
    }}
    .badge-p2 {{
        background: #e0f2fe;
        color: #075985;
        border: 1px solid #bae6fd;
    }}

    /* Math Box */
    .math-box {{
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        border-radius: 6px;
        padding: 7px 14px;
        margin: 10px 0 12px 0;
        page-break-inside: avoid;
        break-inside: avoid;
        display: inline-block;
        min-width: 200px;
    }}
    .math-title {{
        font-size: 7pt;
        text-transform: uppercase;
        font-weight: 700;
        color: #1d4ed8;
        letter-spacing: 0.04em;
        margin-bottom: 2px;
    }}
    .math-formula {{
        font-size: 10.5pt;
        font-weight: 600;
        color: #1e3a8a;
        font-family: "Cambria Math", "Georgia", serif;
    }}

    /* Success Box */
    .success-box {{
        background: #f0fdf4;
        border: 1.5px solid #86efac;
        border-radius: 6px;
        padding: 12px 16px;
        margin: 14px 0;
        page-break-inside: avoid;
        break-inside: avoid;
    }}
    .success-title {{
        font-size: 8pt;
        text-transform: uppercase;
        font-weight: 700;
        color: #166534;
        letter-spacing: 0.05em;
        margin-bottom: 4px;
    }}
    .success-box p {{
        margin: 0;
        color: #14532d;
        font-weight: 500;
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

    with open(temp_html, "w", encoding="utf-8") as f:
        f.write(full_html)

    # Use Chrome to render PDF
    chrome_path = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
    args = [
        chrome_path,
        "--headless=new",
        "--disable-gpu",
        "--no-pdf-header-footer",
        f"--print-to-pdf={pdf_file}",
        temp_html
    ]

    print("Running Chrome to generate PDF...")
    res = subprocess.run(args, capture_output=True, text=True)

    if os.path.exists(temp_html):
        os.remove(temp_html)

    if os.path.exists(pdf_file):
        size_kb = os.path.getsize(pdf_file) / 1024
        print(f"Success! Generated {pdf_file} ({size_kb:.1f} KB)")
    else:
        print("Error: PDF file was not created. Output:", res.stdout, res.stderr)

if __name__ == "__main__":
    generate_pdf()
