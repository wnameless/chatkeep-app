---
ARCHIVE_FORMAT_VERSION: 1.0
ARCHIVE_TYPE: conversation_summary
CREATED_DATE: YYYY-MM-DD
ORIGINAL_PLATFORM: [Claude/ChatGPT/Gemini/etc.]

INSTRUCTIONS_FOR_AI: |
  ## Purpose
  This is an archived conversation that has been summarized and preserved for future reference.
  The conversation has been condensed to capture only the meaningful phases and outcomes.

  ## File Structure
  1. This metadata header (YAML front matter)
  2. Conversation summary sections (Initial Query, Key Insights, Follow-up Explorations, References)
  3. Conversation Artifacts section (outputs created during the conversation)
  4. Attachments section (inputs provided to the conversation)
  5. Workarounds Used section (if applicable)
  6. Archive Metadata section

  ## Artifact vs Attachment
  - **Artifacts**: Outputs CREATED during the conversation (code, poems, documents, analyses, etc.)
  - **Attachments**: Inputs PROVIDED to the conversation (uploaded files, documents, images, etc.)
  Both are preserved but serve different purposes.

  ## Artifact Format
  Artifacts use this wrapper structure:

  <!-- ARTIFACT_START: type="code" language="python" title="Script Name" version="final" -->
  [artifact content]
  <!-- ARTIFACT_END -->

  Artifact attributes:
  - type: Category of artifact (code, poem, document, design, analysis, etc.)
  - title: Descriptive name
  - language: (optional) For code artifacts
  - version: (optional) Version identifier
  - Only final or significant milestone versions are included

  ## Attachment Format
  Attachments are located near the bottom in wrapped format:

  <!-- MARKDOWN_START: filename="example.md" -->
  [content here]
  <!-- MARKDOWN_END: filename="example.md" -->

  Important notes about attachments:
  - ALL attachments have been converted to markdown format, regardless of original type
  - Images are embedded as base64-encoded data URIs in markdown image syntax: ![alt](data:image/png;base64,...)
  - PDFs, Word docs, spreadsheets, etc. are converted to markdown tables or text
  - The filename in the wrapper preserves the original filename for reference
  - Some attachments may be summarized if they were too large - check for ⚠️ WARNING markers
  - Check the "Workarounds Used" section to see if any attachments were modified during archiving

  ## Archive Completeness
  Check the ARCHIVE_COMPLETENESS field:
  - COMPLETE: All attachments are fully converted and intact
  - PARTIAL: Some attachments were summarized or simplified
  - SUMMARIZED: Most/all attachments required summarization

  ## How to Process This Archive
  1. Read this entire file to understand the full context
  2. The summarized sections contain the core knowledge - treat them as primary context
  3. Artifacts show what was created/produced during the conversation
  4. Attachments show what was provided as input to the conversation
  5. When a section references an artifact or attachment, locate it by title/filename
  6. All content is already in markdown and directly readable - no extraction needed
  7. If attachments have ⚠️ WARNING markers, they were modified during archiving - see notes

  ## When User Uploads This File
  - Confirm you've loaded the archive and understood the topic
  - Be ready to continue the conversation from where it left off
  - You can reference the summary, artifacts, and attachments
  - Treat the archived information as established context, not as a question
  - Artifacts represent finalized work that can be built upon or referenced

ATTACHMENT_COUNT: 0
ARTIFACT_COUNT: 0
ARCHIVE_COMPLETENESS: COMPLETE
WORKAROUNDS_COUNT: 0
TOTAL_FILE_SIZE: 0 KB
---

# [Conversation Topic/Title]

**Date:** YYYY-MM-DD
**Tags:** [tag1, tag2, tag3]

_Note: Tags can also be provided without brackets (e.g., `tag1, tag2, tag3`). Both formats are supported for maximum compatibility with different AI platforms._

---

## Initial Query

[Describe what you were trying to accomplish, the problem you needed to solve, or the question you asked]

**Attachments referenced:** [filename1.md, filename2.png]
**Artifacts created:** [Script Name, Document Title]

---

## Key Insights

[Main findings, solutions, or understanding gained from the conversation. This is the core value of the archived conversation.]

**Key points:**
- [Important finding 1]
- [Important finding 2]
- [Important finding 3]

**Attachments referenced:** [filename3.md]
**Artifacts created:** [Analysis Name]

---

## Follow-up Explorations

[Important tangents, deeper dives, or related topics that were explored. Only include if they added significant value.]

**Attachments referenced:** [filename4.md]
**Artifacts created:** [Final Script Version]

---

## References/Links

[Any external sources, tools, documentation, concepts, or contextual information mentioned during the conversation]

**Two types of references:**

1. **External Links** (with URLs):
   - [Link description](https://example.com)
   - Link description: https://example.com

2. **Descriptive References** (without URLs):
   - CAP Theorem: Consistency, Availability, Partition tolerance tradeoffs
   - MongoDB versions mentioned: 4.0 (feature A), 4.2 (feature B), current stable 8.x
   - Design patterns, concepts, dates, or other inline metadata

_Note: References can include URLs (external links) or be descriptive (concepts, theories, contextual information). Both are valuable - the description itself conveys the nature of the reference._

---

## Conversation Artifacts

_This section preserves the valuable outputs created during the conversation._

<!-- ARTIFACT_START: type="code" language="python" title="Data Processing Script" version="final" -->
# Final version after debugging and optimization
# - v1: Basic processing
# - v2: Added error handling
# - v3 (final): Performance improvements and logging

def process_data(input_file):
    """
    Process data from input file and generate report
    """
    # [implementation here]
    pass

if __name__ == "__main__":
    process_data("data.csv")
<!-- ARTIFACT_END -->

<!-- ARTIFACT_START: type="poem" title="Whispers of Dawn" version="final" -->
Morning light breaks through the mist,
Gentle rays on dewdrops kissed,
Nature wakes with soft refrain,
Welcoming the day again.
<!-- ARTIFACT_END -->

<!-- ARTIFACT_START: type="document" title="Project Proposal Summary" version="v2" -->
# Project Proposal: Customer Analytics Platform

## Executive Summary
[Final proposal after incorporating feedback]

## Objectives
- Objective 1
- Objective 2

## Timeline
[Timeline details]
<!-- ARTIFACT_END -->

---

## Attachments

<!-- MARKDOWN_START: filename="document1.md" -->

[Your markdown content here]

<!-- MARKDOWN_END: filename="document1.md" -->

<!-- MARKDOWN_START: filename="image1.png" -->

![image1.png](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==)

<!-- MARKDOWN_END: filename="image1.png" -->

<!-- MARKDOWN_START: filename="large_document.pdf" -->

**⚠️ NOTE: This attachment was summarized due to size limitations.**
- Original size: 150 pages / 2.5 MB
- Summarization level: Partial
- Content preserved: Executive summary, key findings, main data tables, conclusions

# Document Summary

[Summarized content preserving the most important information]

<!-- MARKDOWN_END: filename="large_document.pdf" -->

<!-- MARKDOWN_START: filename="spreadsheet1.xlsx" -->

# Spreadsheet: spreadsheet1.xlsx

| Column A | Column B | Column C |
|----------|----------|----------|
| Data 1   | Data 2   | Data 3   |
| Data 4   | Data 5   | Data 6   |

<!-- MARKDOWN_END: filename="spreadsheet1.xlsx" -->

---

## Workarounds Used

_This section documents any limitations encountered during archiving._

**Example entries (remove if no workarounds were used):**

- **large_document.pdf**: Summarized to key sections (30% of original) due to context length limitations. Full document was 150 pages; preserved executive summary, methodology, key findings, and conclusions. Detailed appendices were omitted.

**If no workarounds were needed, replace above with:**

None - All attachments were successfully converted to full markdown format.

---

## Archive Metadata

**Original conversation date:** YYYY-MM-DD  
**Archive created:** YYYY-MM-DD  
**Archive version:** 1.0  
**Archive completeness:** COMPLETE / PARTIAL / SUMMARIZED  
**Total attachments:** 0  
**Total artifacts:** 0  
**Attachments with workarounds:** 0  
**Estimated reading time:** [X minutes]

---

_End of archived conversation_
