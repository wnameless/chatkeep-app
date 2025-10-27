# AI Conversation Archiving System v1.0

This file contains everything needed to archive AI conversations into a standardized, portable markdown format.

## How to Use This File

**For creating an archive:**

**Option 1: Explicit instruction**
1. Upload this file to your AI chatbot
2. Say: "Please archive our conversation following the instructions in this file"
3. The AI will generate a complete archive that you can save

**Option 2: Implicit execution** (Quick method)
1. Upload or paste this file to your AI chatbot
2. Press enter (no additional instruction needed)
3. The AI will recognize this as an archiving request and begin the process automatically

**For loading an archive:**
- Simply upload the archived .md file to any AI chatbot
- The embedded instructions will tell the AI how to process it

---

## Part 1: Archiving Instructions for AI

### ⚠️ CRITICAL: What This File Is

**When you receive this file, understand:**

**DO NOT:**
- ❌ Summarize this file
- ❌ Explain what this file contains
- ❌ Describe the archiving process
- ❌ Treat this as content to analyze

**DO:**
- ✅ Recognize this as archiving instructions
- ✅ Apply these instructions to the conversation that occurred BEFORE this file was uploaded
- ✅ Archive the prior conversation, not this instruction file
- ✅ Begin the archiving process immediately

**This is an instruction set, not content. Execute it, don't explain it.**

---

### CRITICAL: Language Handling - Three Contexts

**1. Template Structure (MUST be English):**
- Section headings: `## Initial Query`, `## Key Insights`, `## Attachments`, etc.
- YAML keys: `ARCHIVE_FORMAT_VERSION`, `CREATED_DATE`, `ORIGINAL_PLATFORM`, etc.
- Wrapper syntax: `:::artifact`, `:::attachment`, `:::`
- Structure keywords and formatting

**Why:** Backend parsers expect specific English keywords for processing.

**2. Archive Infrastructure (MUST be conversation language):**
- User interaction: Questions, confirmations, explanations to user
- Summaries: Initial Query, Key Insights, Follow-up Explorations
- Descriptions ABOUT artifacts and attachments
- Notes and explanations (workarounds, context, purposes)
- Metadata descriptions

**Why:** This is the AI communicating about the conversation - use the conversation's language.

**3. Artifact & Attachment Content (MUST be original language):**
- Artifact content: Keep in whatever language the user created it (code, poems, documents, analyses)
- Attachment content: Keep in whatever language the file was originally in
- May be multilingual, mixed languages, or different from conversation language

**Why:** Preserve content exactly as created or provided - don't translate or change.

**Example:**

✅ **Chinese conversation with English code and French document:**
```markdown
## Initial Query

用户需要审查一份法语合同并创建数据处理脚本。  ← Chinese (conversation language)

**Attachments referenced:** [contrat.pdf]
**Artifacts created:** [数据处理器]

:::attachment filename="contrat.pdf"
# Contrat de Service
Bonjour, ce document...  ← French (original language, not translated)
:::

:::artifact type="code" language="python" title="数据处理器"
def process_data():
    """Process customer data"""  ← English (as created by user)
    return result
:::
```

**Key Rules:**
- ✅ Structure in English: `## Initial Query`, `:::artifact`, YAML keys
- ✅ Summaries in conversation language (Chinese)
- ✅ Content in original language (French document, English code)
- ❌ Don't translate summaries to English
- ❌ Don't translate content to match conversation language

### IMPORTANT: Start Immediately, Don't Explain

When a user uploads or pastes this file (with or without additional instructions):
- **DO NOT** explain what this file contains
- **DO NOT** walk through all the steps in this prompt
- **DO NOT** describe the archiving process in detail before starting

Instead:
1. Proceed directly to Step 0 (scan and acknowledge the conversation)
2. Continue through the remaining steps
3. Keep all interactions concise

The user already knows what this file does - they want action, not explanation.

---

When a user asks you to archive a conversation, follow these steps:

### Step 0: Acknowledge the Archiving Request

**When you receive this file, you are being asked to archive the conversation that occurred BEFORE this file was uploaded.**

**Do this immediately:**

1. **Scan the conversation history** up to the point where this file was uploaded
2. **Count and identify:**
   - How many user messages?
   - How many AI responses?
   - How many attachments were uploaded or pasted?
   - How many artifacts (code, documents, etc.) were created?
   - What is the main topic?
3. **Briefly confirm** in the conversation's language: "I see a conversation with [X] exchanges about [topic]. I'll now archive it."
4. **Proceed directly to Step 1**

**This step forces you to recognize there IS a conversation to archive. You cannot complete this step by analyzing this instruction file - you must look at the prior conversation.**

### Step 1: Choose Delivery Method

**Default: Use Simple Response (recommended for 90%+ of archives)**

Generate the archive as a markdown code block in your response. This works reliably for most conversations.

**When to use alternatives:**

**If the simple response truncates or fails:**
1. Tell the user: "The archive is large and may not fit in a simple response. I can use [Artifact/Canvas/Code View] instead. Would you like me to do that?"
2. Wait for user confirmation
3. Use the alternative delivery method

**If user explicitly requests a different method:**
- **Large Document Feature (Artifact/Canvas/Code View):** Available on some platforms for better rendering of large documents
- **MCP Storage:** Saves directly to file system (requires MCP tools to be connected)

**CRITICAL: Format Integrity for Canvas/Artifacts/Code Blocks**

If using Canvas, Artifacts, or Code View features:

⚠️ **WARNING: These features sometimes apply platform-specific formatting that breaks the template!**

**Before generating the archive:**
1. Understand that the output MUST be plain markdown - NO platform-specific enhancements
2. DO NOT let the platform add metadata wrappers, special headers, or formatting
3. The output should be IDENTICAL to what you would produce in a simple response
4. After generation, verify the structure matches the template exactly

**What to avoid:**
- ❌ Platform-generated headers or metadata boxes
- ❌ Special formatting blocks not in the template
- ❌ Interactive elements or buttons
- ❌ Platform-specific syntax extensions
- ❌ Any wrapper that's not in the template (`:::artifact`, `:::attachment`)

**What the output should be:**
- ✅ Pure markdown following the template exactly
- ✅ Standard markdown syntax only
- ✅ All wrapper syntax exactly as shown in template
- ✅ No platform-specific additions

**If you cannot produce clean markdown in Canvas/Artifacts:**
- Warn the user: "This platform's [feature name] may add extra formatting. Would you prefer simple response instead?"
- Wait for user confirmation

### Step 1.5: Detect and Unpack Loaded Archives

**IMPORTANT: Archives are context, not attachments. When users load archives, they are providing prior context to continue from. ALWAYS merge archives - never include them as attachments.**

**Before proceeding, scan the conversation for loaded archive files:**

1. **Identify loaded archives:**
   - Look for uploaded or pasted files containing both:
     - `ARCHIVE_FORMAT_VERSION` in YAML frontmatter
     - `INSTRUCTIONS_FOR_AI` in YAML frontmatter
   - BUT exclude files containing instruction markers:
     - "Part 1: Archiving Instructions for AI"
     - "### Step 0: Acknowledge the Archiving Request"
     - "Part 2: Archive Template Structure"
   - These criteria identify generated archives, not the instruction file

2. **If NO archives found:**
   - Skip to Step 2
   - This is a fresh conversation with no prior context

3. **If archives ARE found (one or more), merge them:**

   **Extract artifacts from all loaded archives:**
   - Parse each `:::artifact` wrapper
   - Note: artifact type, title, version, content

   **Extract attachments from all loaded archives:**
   - Parse each `:::attachment` wrapper
   - Note: filename, content

   **Extract summary context (for merged narratives):**
   - Note the Initial Query from each archive
   - Note the Key Insights from each archive
   - This helps create a continuous narrative

4. **Understand the merge strategy:**
   - **Artifacts:** Include ALL artifacts (from archives + new ones created)
     - If an artifact was modified/evolved: only include newest version
     - If an artifact is unchanged: carry it forward as-is
     - **How to determine "newest" when multiple archives have same artifact:**
       - Check version numbers/identifiers (v3 > v2 > v1, final > draft)
       - If version info unclear, assume last-loaded archive has newest version
   - **Attachments:** Include ALL attachments (from archives + new ones)
     - Carry forward all attachments from loaded archives
   - **Summaries:** Create a **merged narrative** blending old and new
     - Don't separate "old conversation" vs "new conversation"
     - Tell a unified story across archive boundaries

**After completing this step, proceed to Step 2 with the merged context in mind.**

### Step 2: Understand the Template
Review Part 2 of this document which contains the complete template structure. Understand the YAML front matter, the attachment wrapper format, and the artifact wrapper format.

### Step 3: Fill Out Metadata
In the YAML front matter at the top of the archive:
- Set CREATED_DATE to today's date (YYYY-MM-DD)
- Set ORIGINAL_PLATFORM to your platform name (Claude, ChatGPT, Gemini, etc.)
- Set DELIVERY_METHOD (optional) to the method used: simple_response, large_document, or mcp_storage

**Note:** You don't need to count artifacts or attachments - the backend will calculate these automatically from the actual `:::artifact` and `:::attachment` markers in your archive.

### Step 4: Create the Conversation Title
Give this conversation a clear, descriptive title that summarizes the main topic. This will be the H1 heading of the archive.

### Step 5: Summarize Each Phase

**⚠️ LANGUAGE REMINDER: Write all summaries in the conversation's language (not English, not content language)!**

**IMPORTANT: If archives were loaded (detected in Step 1.5), create MERGED narratives that blend old and new context into a unified story. Don't separate "previous conversation" from "new conversation" - tell one continuous narrative.**

Write concise summaries for each phase:

**Initial Query:**

*If NO archives were loaded:*
- What was the user trying to accomplish?
- What problem were they solving?
- What question did they ask?

*If archives WERE loaded:*
- **Blend contexts:** "Building on previous work in [topic from archive], the user sought to..."
- Reference prior context naturally: "Continuing from earlier exploration of [X], the focus shifted to..."
- Create continuity: Show how this conversation extends or builds upon previous work
- **Don't separate old vs new** - create one unified narrative

- List any attachments or artifacts mentioned (including carried-forward ones)

**Key Insights:**

*If NO archives were loaded:*
- What were the main findings or solutions?
- What understanding was gained?
- Extract 3-5 key points as bullet points

*If archives WERE loaded:*
- **Merge insights across conversations:** Combine findings from loaded archives with new discoveries
- Show evolution: "The initial approach [from archive] was refined by..."
- Highlight continuity: "Building on earlier insights about [X], we discovered..."
- **Present unified findings** that span the entire knowledge journey, not just this session

- Extract 3-5 key points (can span old and new conversations)
- List any attachments or artifacts referenced

**Follow-up Explorations:**

*If NO archives were loaded:*
- What deeper questions were explored?
- What tangents added value?
- Only include if there were significant follow-ups

*If archives WERE loaded:*
- **Show cumulative exploration:** "After resolving [X from archive], attention turned to..."
- Connect threads: How do new explorations relate to previous ones?
- Demonstrate progression: Each conversation should advance the understanding

- Only include if there were significant follow-ups
- List any attachments or artifacts referenced

**Examples of Merged Narratives:**

❌ **Bad (Separated):**
```
Initial Query:
Previous conversation: User worked on data validation.
New conversation: User improved error handling.
```

✅ **Good (Merged):**
```
Initial Query:
Building on a data validation system developed previously, the user sought to enhance
error handling and add comprehensive logging. The focus was on making the validator
production-ready by addressing edge cases discovered in earlier testing.
```

### Step 6: Extract References
List all external links, documentation, concepts, and contextual information mentioned during the conversation. Include two types:

1. **External Links** - URLs to documentation, tools, articles (e.g., "MongoDB Documentation: https://docs.mongodb.com")
2. **Descriptive References** - Important concepts, contextual info, or other references without URLs (e.g., "CAP Theorem: Consistency, Availability, Partition tolerance tradeoffs" or "MongoDB versions: 4.0, 4.2, 8.x")

Both types are valuable and should be included. The description itself conveys the nature of the reference.

### Step 7: Preserve Conversation Artifacts

**⚠️ LANGUAGE REMINDER: Keep artifact content in its original language (as created by user)! Only descriptions ABOUT artifacts should be in conversation language.**

**What are Conversation Artifacts?**

Artifacts are the valuable outputs CREATED during the conversation, such as:
- Code snippets, scripts, programs
- Poems, stories, creative writing
- Documents, reports, proposals
- Designs, specifications, architectures
- Analyses, summaries, research findings
- Any other content generated through the conversation

**Which artifacts to preserve:**
- ✅ **Final versions** - The completed, working, or polished output
- ✅ **Significant milestones** - Important intermediate versions that represent major progress
- ❌ **Minor iterations** - Don't include every small revision or debugging attempt

**Artifact Continuity (When Archives Were Loaded):**

**IMPORTANT: If archives were loaded in Step 1.5, apply the full carry-forward strategy:**

1. **Include NEW artifacts** created in THIS conversation
   - All artifacts generated during the current session
   - Use normal versioning and evolution notes

2. **Include UNMODIFIED artifacts** from loaded archives
   - If an artifact from a loaded archive was never touched, modified, or discussed → carry it forward as-is
   - Preserve its original content, attributes, and version number

3. **For MODIFIED artifacts** (evolved from loaded archives):
   - Only include the NEWEST version (created in this conversation)
   - Document the evolution with version progression
   - Example: `version="v4"` with note `# v1-v3 from previous conversation, v4 adds error handling`

4. **Determine if an artifact was modified:**
   - If user asked to improve/update/debug an existing artifact → modified
   - If artifact's title or purpose was referenced in conversation → likely modified
   - If artifact was never mentioned → unmodified (carry forward as-is)

**Examples of Artifact Continuity:**

**Scenario: Archive has 3 artifacts, user modified 1 in new conversation**

```markdown
## Loaded Archive Had:
- data_validator.py (v2)
- report_generator.py (v1)
- config_parser.py (v1)

## New Conversation:
- User improved data_validator.py → created v3
- Other artifacts never mentioned

## New Archive Should Include:
:::artifact type="code" language="python" title="Data Validator" version="v3"
# v1-v2 from previous conversation (basic validation, error handling)
# v3 (this session): Added logging and performance optimization

[v3 code here]
:::

:::artifact type="code" language="python" title="Report Generator" version="v1"
[v1 code - unchanged]
:::

:::artifact type="code" language="python" title="Config Parser" version="v1"
[v1 code - unchanged]
:::
```

**Key Points:**
- Modified artifact: Only newest version (v3), with evolution note spanning all versions
- Unmodified artifacts: Carried forward exactly as they were
- All 3 artifacts appear in new archive (full carry-forward)

**How to preserve artifacts:**

For each significant artifact created during the conversation:

1. Use the artifact wrapper syntax:

```
:::artifact type="[type]" language="[language]" title="[title]" version="[version]"
[artifact content]
:::
```

**Note:** Only `:::` at the start of a line closes the artifact. If your content needs `:::` at line start, indent it or escape with `\:::`.

2. **Required attributes:**
   - `type`: The artifact category (code, poem, document, design, analysis, script, etc.)
   - `title`: Descriptive name for the artifact

3. **Optional attributes:**
   - `language`: For code artifacts (python, bash, javascript, etc.)
   - `version`: Version identifier (final, v2, milestone-1, etc.)
   - `iterations`: Brief note about evolution (e.g., "after 3 revisions")

4. **Documenting evolution:**
   If there were multiple iterations, add a brief comment at the start of the artifact explaining what changed:

```
:::artifact type="code" language="python" title="Data Validator" version="final"
# Final version after 3 iterations:
# - v1: Basic validation only
# - v2: Added error handling  
# - v3 (final): Added logging and performance optimization

[final code here]
:::
```

5. **When to mention evolution:**
   - If the iterations were significant to understanding the solution
   - If multiple approaches were tried before finding the right one
   - If debugging or refinement was a key part of the process
   - Keep evolution notes brief (2-5 lines maximum)

**Examples:**

```markdown
:::artifact type="poem" title="Autumn Reflections" version="final"
[poem content after revisions]
:::

:::artifact type="code" language="bash" title="Backup Script" version="final" iterations="3"
# Working version after debugging file permission issues
[script content]
:::

:::artifact type="document" title="Project Proposal" version="v3"
# Final proposal incorporating stakeholder feedback
[document content]
:::
```

### Step 7.5: Filter System Prompts and Handle Archives

**IMPORTANT:** Before processing attachments, identify and filter system files. Handle archives specially - they should be MERGED (as done in Step 1.5), not included as attachments.

**Three categories of files:**

**1. EXCLUDE (Archiving Instruction File) - ALWAYS**

**Detection (content-based, filename-independent):**
Exclude any file containing **ANY** of these unique markers:
- "Part 1: Archiving Instructions for AI"
- "### Step 0: Acknowledge the Archiving Request"
- "Part 2: Archive Template Structure"

**Why content-based?**
- Users may rename the file (chatkeep-archiving-spec.md, prompt.md, etc.)
- Users may paste it without a filename
- Only the instruction file has these markers - generated archives never do

**Action:**
- Do NOT include in Attachments section at all
- Do NOT create any placeholder or mention
- Completely omit from the archive

**2. MERGE (Generated Archive Files) - Always Merged**

**Detection:**
Files containing BOTH:
- `ARCHIVE_FORMAT_VERSION` in YAML frontmatter
- `INSTRUCTIONS_FOR_AI` in YAML frontmatter

BUT NOT containing ANY instruction markers from category 1

**Action:**
- Do NOT include as attachments (they're context, not content)
- Should already be unpacked in Step 1.5
- Their artifacts and attachments will be carried forward
- Do NOT mention the archive file itself in Attachments section
- **Archives are ALWAYS merged, never included as attachments**

**3. EXCLUDE (Other System Prompts) - Conditional**

**Detection - Exclude if ALL of these are true:**
- File was pasted or uploaded but never discussed
- Content is never referenced in conversation
- Content appears to be instructions/prompts rather than data
- Not actively used or analyzed in the conversation

**Action:**
- Do NOT include in Attachments section
- These are operational files, not conversation content

**What to INCLUDE (process normally as attachments):**

✅ **Conversation-relevant content:**
- Files that were discussed, analyzed, or debugged
- Data files, documents, images used in the conversation
- Reference materials actively used
- Even pasted content, if it was the subject of conversation

**Examples with Content-Based Detection:**

❌ **EXCLUDE - Category 1 (Instruction File):**
```
File: "my-prompt.md" (pasted, no filename visible)
Content contains: "Part 1: Archiving Instructions for AI"
→ This is the instruction file - EXCLUDE completely
→ Detection based on CONTENT, not filename
```

🔄 **MERGE - Category 2 (Generated Archive):**
```
File: "conversation_archive_2024.md" (uploaded)
Content contains: ARCHIVE_FORMAT_VERSION + INSTRUCTIONS_FOR_AI
Content does NOT contain: "Part 1: Archiving Instructions"
→ This is a generated archive - MERGE (handled in Step 1.5)
→ Do NOT include as attachment
→ Artifacts and attachments already extracted
```

❌ **EXCLUDE - Category 3 (Unused System Prompt):**
```
File: "template.md" (pasted, never referenced)
→ System template not used in conversation - EXCLUDE
```

✅ **INCLUDE - Conversation Content:**
```
File: "bug_report.md" (pasted, discussed extensively)
→ User asked for help analyzing this - INCLUDE
```

✅ **INCLUDE - Data File:**
```
File: "data.csv" (uploaded)
→ User uploaded for analysis - INCLUDE
```

**After filtering:**
- Excluded files (categories 1 & 3) should NOT appear anywhere
- Merged archives (category 2) should NOT appear in Attachments section
- Only conversation-relevant files appear as attachments

### Step 8: Process Attachments (With Workarounds If Needed)

**⚠️ LANGUAGE REMINDER: Preserve attachment content in its original language! Don't translate. Only notes ABOUT attachments should be in conversation language.**

**Note:** After filtering out system prompts in Step 7.5, process only the remaining conversation-relevant attachments.

**Attachment Carry-Forward (When Archives Were Loaded):**

**IMPORTANT: If archives were loaded in Step 1.5, include attachments from TWO sources:**

1. **Attachments from THIS conversation:**
   - Files uploaded or pasted during the current session (excluding archives themselves)
   - Process normally using standard conversion rules below

2. **Attachments from MERGED archives (full carry-forward):**
   - Extract ALL attachments from loaded archives (already done in Step 1.5)
   - Include them in the new archive's Attachments section
   - Preserve their original content and format

**Why full carry-forward for attachments?**
- Attachments are inputs/context that remain relevant
- Even if not referenced in new conversation, they provide complete context
- Users expect archives to be cumulative knowledge bases
- Losing attachments would break continuity

**Example - Multiple Archives Merged:**

```markdown
## Scenario:
- Archive 1 had: report.pdf, data.csv
- Archive 2 had: analysis.md
- New conversation added: updated_data.csv

## New Archive Attachments Section Should Include:

:::attachment filename="report.pdf"
[content]
:::

:::attachment filename="data.csv"
[content]
:::

:::attachment filename="analysis.md"
[content]
:::

:::attachment filename="updated_data.csv"
[content]
:::
```

**All 4 attachments included - full cumulative history preserved.**

---

**Preferred approach for new attachments:** Convert all attachments to full markdown format using the wrapper syntax.

**Standard Conversion Process:**

For each attachment in the conversation (after filtering):

1. Convert it to markdown format
2. Wrap it using this exact syntax:

```
:::attachment filename="original_filename.ext"
[converted markdown content]
:::
```

**Note:** Only `:::` at the start of a line closes the attachment. If your content needs `:::` at line start, indent it or escape with `\:::`.

**Standard conversion rules:**
- Images: Convert to `![filename](data:image/[type];base64,[encoded_data])`
- Spreadsheets/CSV: Convert to markdown tables
- PDFs/Word docs: Convert to formatted markdown text
- Code files: Preserve in markdown code blocks with language tags
- Plain text: Keep as-is

**If You Encounter Limitations:**

**IMPORTANT:** We strongly prefer complete attachments. Only use workarounds if absolutely necessary due to technical limitations.

**For large files or context limitations:**

1. FIRST, attempt to convert the complete attachment
2. If that fails due to size/context limits, create an intelligent summary:
   - For documents: Extract main points, structure, key data, and important details
   - For spreadsheets: Preserve column headers, data types, and representative sample rows (first 20-50 rows if possible)
   - For code: Keep important functions/classes/logic with comments explaining omitted parts
   - For data files: Preserve schema, structure, and statistical summaries

3. Add a clear note in the wrapper indicating summarization:

```
:::attachment filename="large_document.pdf"
**⚠️ NOTE: This attachment was summarized due to size limitations.**
- Original size: [X] pages / [Y] KB
- Summarization level: [Partial/Significant]
- Content preserved: [Describe what was kept]

[Summarized content here]
:::
```

**For unsupported file types:**

Create a descriptive placeholder with all available metadata:

```
:::attachment filename="complex_image.png"
**⚠️ NOTE: Unable to fully process this file type.**
- File type: PNG image
- Original filename: complex_image.png
- File size: [if known]
- Context description: [Describe what the image contained based on conversation context]
- Visual content: [Describe colors, layout, text, diagrams, etc.]
- Purpose in conversation: [Why this image was important]
- Processing limitation: [Explain specifically why you couldn't process it]
:::
```

**For images you cannot encode to base64:**
1. Describe the image content in as much detail as possible
2. Include any text visible in the image
3. Describe layout, colors, diagrams, charts, or visual elements
4. Explain the image's relevance to the conversation
5. Mark clearly as a workaround with reason

**Transparency Requirements:**
- **ALWAYS** indicate when you use a workaround with a ⚠️ WARNING marker
- **ALWAYS** explain specifically why (size limit, capability limit, format unsupported, etc.)
- **ALWAYS** preserve as much information as possible
- **ALWAYS** describe what information was lost or simplified
- Add details to the "Workarounds Used" section at the end

**Priority Order (Most to Least Preferred):**
1. ✅ **Full conversion** - Complete attachment in proper markdown format (ALWAYS TRY THIS FIRST)
2. ⚠️ **Intelligent summarization** - Preserve key information with clear documentation
3. ⚠️ **Detailed placeholder** - Comprehensive description with all available metadata
4. ❌ **Never omit** - Never completely skip an attachment without at least creating a placeholder

### Step 9: Document Workarounds

If you used any workarounds, fill out the "Workarounds Used" section with:
- Filename of each affected attachment
- What workaround was applied
- Why it was necessary
- What information was preserved vs. lost

If no workarounds were needed, state: **"None - All attachments were successfully converted to full markdown format."**

### Step 10: Complete Archive Metadata

At the bottom of the file, fill in:
- Original conversation date
- Archive created date  
- Archive version (use 1.0)
- Total attachments count
- Total artifacts count
- Estimated reading time

### Output Format

**Depends on the delivery method chosen in Step 1:**

**Simple Response:**
- Provide the complete archived markdown file as a single code block
- User can copy and paste to save
- Do not truncate or summarize the archive itself - give the full, complete archive

**Large Document Feature (Artifact/Canvas):**
- Create the archive in your platform's large document feature
- Title it appropriately (e.g., "Conversation Archive - [Topic]")
- User can download directly from the artifact/canvas interface
- Provide the complete archive, no truncation

**MCP Storage:**
- Save the archive file to the user's chosen storage location
- Use a descriptive filename: `conversation_archive_[topic]_[date].md`
- Confirm the save location and filename to the user
- Example: "Archive saved to ~/Documents/conversation_archive_mongodb_setup_2025-01-15.md"

### Delivery Method Details

**Understanding Each Delivery Method:**

**1. Simple Response (Traditional Method)**
- Archive is generated as a markdown code block in the chat
- Works on all platforms, no special features required
- Best for small archives (<100 KB)
- User must manually copy and save to a file
- May be slow to render for large archives
- Risk of truncation if archive exceeds chat response limits

**2. Large Document Feature (Platform-Specific)**
- Creates archive in a separate viewing area
- Different names on different platforms:
  - **Claude:** Artifacts
  - **ChatGPT:** Canvas
  - **Gemini:** Canvas
  - **Other platforms:** May have similar features with different names
- Better for medium-sized archives (100 KB - 1 MB)
- Easier to download and view
- No manual copy-paste required
- Check if your platform supports this before using

**3. MCP Storage (Most Reliable for Large Archives)**
- Requires MCP (Model Context Protocol) tools to be connected
- Saves directly to file system or cloud storage
- Best for large archives (>500 KB)
- No size limitations
- No copy-paste needed
- File is immediately available on user's system

**Fallback Strategy:**
If your chosen delivery method fails:
1. Try the next most appropriate method based on size
2. Inform the user of the change
3. Never fail silently - always provide the archive somehow

**Examples of Fallback:**
- "Large document feature unavailable, using simple response instead"
- "MCP storage not found, creating artifact instead"
- "Archive too large for chat response, attempting to save via MCP"

### Important Guidelines

- **Completeness first:** Always attempt full conversion before using workarounds
- **Preserve valuable outputs:** Include all significant artifacts created during the conversation
- **Be transparent:** Clearly mark and explain any compromises
- **Preserve context:** Even if you can't include full content, preserve enough information to understand what was there
- **Be concise but comprehensive:** Capture essence, not verbosity
- **Maintain structure:** Keep the exact template structure from Part 2
- **No silent failures:** Never skip attachments without documentation
- **Technical accuracy:** Preserve all important technical details and specifics
- **Final versions preferred:** For artifacts, include final or milestone versions, not every iteration


### Final Format Verification (CRITICAL FOR CANVAS/ARTIFACTS)

**If using Canvas/Artifacts/Code View, verify before delivery:**

1. **Check template structure:**
   - [ ] YAML frontmatter starts with `---` and ends with `---`
   - [ ] All section headings are exactly as in template (English)
   - [ ] All wrapper syntax uses exact format from template
   - [ ] No extra platform-generated sections or formatting

2. **Check content language:**
   - [ ] All summaries/notes/descriptions are in conversation's language
   - [ ] All artifact content is in original language (as created)
   - [ ] All attachment content is in original language (as provided)
   - [ ] Only structure/headings/YAML keys are in English

3. **Check markdown purity:**
   - [ ] No platform-specific syntax extensions
   - [ ] No interactive elements or special blocks
   - [ ] Output is copyable as plain text
   - [ ] Would work identically in simple response

**If verification fails:**
- Regenerate using simple response instead
- Warn user: "Platform formatting detected. Switching to simple response for template integrity."

### Quality Checklist

Before outputting, verify:

**1. Template Structure**
- [ ] YAML metadata complete (version, type, date, platform)
- [ ] All sections present and in correct order
- [ ] Wrapper syntax correct (`:::artifact`, `:::attachment`)

**2. Language Handling**
- [ ] Structure in English (headings, YAML keys, wrappers)
- [ ] Summaries in conversation's language
- [ ] Content in original language (artifacts/attachments as-is)

**3. Content Filtering**
- [ ] System prompts excluded (archiving specs, instruction files)
- [ ] Only conversation-relevant files included
- [ ] Artifacts show final/important versions only

**4. Attachments & Artifacts**
- [ ] All attachments converted or documented with workarounds
- [ ] Summary references match actual artifacts/attachments
- [ ] No excluded items referenced in summaries

**5. Delivery Method**
- [ ] Using simple response (or alternative if needed)
- [ ] If Canvas/Artifacts: verified pure markdown, no platform formatting
- [ ] Output complete and ready to save

**6. Archive Continuity (If Applicable)**
- [ ] If archives were loaded: artifacts and attachments carried forward
- [ ] Archive files themselves NOT included as attachments
- [ ] Summaries create unified narrative (not separated old/new)
- [ ] Modified artifacts show version progression

---

## Comprehensive Examples: Archive Merging in Action

These examples demonstrate the complete context merge workflow when users load archives and continue conversations.

### Example 1: Single Archive Continuation (Simple Case)

**Setup:**
- User loads `data_project_archive_2024-01-15.md`
- Archive contains:
  - Artifacts: data_validator.py (v2), config.json (v1)
  - Attachments: requirements.txt, sample_data.csv
  - Summary: "Created data validation system with error handling"
- User continues: "Add logging to the validator"

**What AI Should Do:**

1. **Step 1.5:** Detect archive, extract 2 artifacts + 2 attachments
2. **Step 5:** Create merged narrative:
   ```
   Initial Query:
   Building on a data validation system previously developed, the user sought
   to enhance it with comprehensive logging capabilities for better debugging
   and monitoring in production environments.
   ```
3. **Step 7:** Include 3 artifacts:
   - data_validator.py v3 (modified - new version with logging)
   - config.json v1 (unchanged - carried forward)
4. **Step 8:** Include 2 carried-forward attachments + any new ones

**Result:** New archive seamlessly continues the story, preserves all context.

---

### Example 2: Multiple Archives Merged (Complex Case)

**Setup:**
- User loads TWO archives in same conversation:
  - `project_phase1_archive_2024-01-10.md`:
    - Artifacts: analyzer.py (v1), report_template.md (v1)
    - Attachments: raw_data.csv
  - `project_phase2_archive_2024-01-20.md`:
    - Artifacts: analyzer.py (v2), visualizer.py (v1)
    - Attachments: config.yaml
- User continues: "Optimize the analyzer for large datasets"

**What AI Should Do:**

1. **Step 1.5:** Detect BOTH archives
   - Merge artifacts: analyzer.py v2 (latest), report_template.md v1, visualizer.py v1
   - Merge attachments: raw_data.csv, config.yaml

2. **Step 5:** Create unified narrative spanning all phases:
   ```
   Initial Query:
   Continuing from a multi-phase project involving data analysis and visualization,
   the user sought to optimize the analyzer (previously developed through two iterations)
   to handle large datasets efficiently. The project had progressed from basic analysis
   to visualization, and now focused on performance enhancement.

   Key Insights:
   - Phase 1 established core analysis algorithms [from phase1 archive]
   - Phase 2 improved accuracy and added visualization [from phase2 archive]
   - Phase 3 (current) optimized for production-scale data with caching and parallel processing
   ```

3. **Step 7:** Include 4 artifacts:
   ```
   :::artifact type="code" language="python" title="Data Analyzer" version="v3"
   # v1 from phase1 archive: Basic analysis
   # v2 from phase2 archive: Improved accuracy
   # v3 (this session): Performance optimization for large datasets

   [v3 code with optimizations]
   :::

   :::artifact type="document" title="Report Template" version="v1"
   [unchanged content]
   :::

   :::artifact type="code" language="python" title="Visualizer" version="v1"
   [unchanged content]
   :::
   ```

4. **Step 8:** Include 2 carried-forward attachments:
   ```
   :::attachment filename="raw_data.csv"
   [content]
   :::

   :::attachment filename="config.yaml"
   [content]
   :::
   ```

**Result:** Single cumulative archive spanning 3 conversation phases, full context preserved, clear version progression.

---

### Example 3: User Runs Archiving Multiple Times (Within Single Conversation)

**Setup:**
- User has conversation about topic X
- At 10:00 AM: User pastes archiving prompt → AI generates archive1
- Conversation continues
- At 11:00 AM: User pastes archiving prompt again

**What AI Should Do:**

**For second archiving (11:00 AM):**

1. **Step 1.5:** NO archives to load (archive1 is AI-generated output in chat, not a loaded file)
   - Archive1 is just conversation history, not an attachment
   - Treat this as fresh archiving of entire conversation

2. **Step 7.5:** Archive1 is AI-generated content in this conversation
   - Do NOT include archive1 as an artifact or attachment
   - The second archive naturally includes all content from the full conversation

3. **Generate complete archive:** Captures entire conversation from start to current moment
   - Includes all artifacts (even those in archive1, since they're part of conversation)
   - Includes all attachments (user-provided files, not archive1 itself)
   - Summaries cover the full conversation timeline

**Result:** Second archive is comprehensive and self-contained, no redundancy with archive1 (which was output, not input).

---

## Part 2: Archive Template Structure

**The section below shows the exact structure and format to use when creating an archive. Follow this template precisely:**

```markdown
---
ARCHIVE_FORMAT_VERSION: 1.0
ARCHIVE_TYPE: conversation_summary
CREATED_DATE: YYYY-MM-DD
ORIGINAL_PLATFORM: [Claude/ChatGPT/Gemini/etc.]
DELIVERY_METHOD: simple_response  # Optional: simple_response | large_document | mcp_storage

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

  ## Metadata Fields
  The DELIVERY_METHOD field in the YAML header is optional:
  - DELIVERY_METHOD (optional): How the archive was delivered (simple_response, large_document, mcp_storage)
  Archives without this field are still valid (backward compatible with v1.0).

  ## Language in Archives
  Archives use three language contexts:
  1. **Template structure** (English): Section headings, YAML keys, wrapper syntax
  2. **Archive infrastructure** (conversation language): Summaries, descriptions, notes
  3. **Content itself** (original language): Artifacts and attachments preserved as created/provided

  Example: A Chinese conversation creating a German poem will have Chinese summaries with German poem content.

  ## Artifact vs Attachment
  - **Artifacts**: Outputs CREATED during the conversation (code, poems, documents, analyses, etc.)
  - **Attachments**: Inputs PROVIDED to the conversation (uploaded files, documents, images, etc.)
  Both are preserved but serve different purposes.

  ## Artifact Format
  Artifacts use this wrapper structure:

  :::artifact type="code" language="python" title="Script Name" version="final"
  [artifact content]
  :::

  Artifact attributes:
  - type: Category of artifact (code, poem, document, design, analysis, etc.)
  - title: Descriptive name
  - language: (optional) For code artifacts
  - version: (optional) Version identifier
  - Only final or significant milestone versions are included

  ## Attachment Format
  Attachments are located near the bottom in wrapped format:

  :::attachment filename="example.md"
  [content here]
  :::

  Important notes about attachments:
  - ALL attachments have been converted to markdown format, regardless of original type
  - System prompts and instruction files are automatically excluded (e.g., the archiving system itself)
  - Only conversation-relevant attachments are included (files actually discussed or used in the conversation)
  - Pasted content that was never referenced in the conversation is filtered out
  - Images are embedded as base64-encoded data URIs in markdown image syntax: ![alt](data:image/png;base64,...)
  - PDFs, Word docs, spreadsheets, etc. are converted to markdown tables or text
  - The filename in the wrapper preserves the original filename for reference
  - Some attachments may be summarized if they were too large - check for ⚠️ WARNING markers
  - Check the "Workarounds Used" section to see if any attachments were modified during archiving

  ## Attachment Filtering
  Not all attachments from the original conversation appear in this archive:
  - System prompts and instruction files are automatically excluded
  - The archiving instruction file itself (detected by content, not filename) is never included
  - Pasted content that was never discussed or referenced is filtered out
  - Only conversation-relevant attachments are preserved
  - Archives loaded as context are ALWAYS merged (never included as attachments)

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

:::artifact type="code" language="python" title="Data Processing Script" version="final"
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
:::

:::artifact type="poem" title="Whispers of Dawn" version="final"
Morning light breaks through the mist,
Gentle rays on dewdrops kissed,
Nature wakes with soft refrain,
Welcoming the day again.
:::

:::artifact type="document" title="Project Proposal Summary" version="v2"
# Project Proposal: Customer Analytics Platform

## Executive Summary
[Final proposal after incorporating feedback]

## Objectives
- Objective 1
- Objective 2

## Timeline
[Timeline details]
:::

---

## Attachments

:::attachment filename="document1.md"

[Your markdown content here]

:::

:::attachment filename="image1.png"

![image1.png](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==)

:::

:::attachment filename="large_document.pdf"

**⚠️ NOTE: This attachment was summarized due to size limitations.**
- Original size: 150 pages / 2.5 MB
- Summarization level: Partial
- Content preserved: Executive summary, key findings, main data tables, conclusions

# Document Summary

[Summarized content preserving the most important information]

:::

:::attachment filename="spreadsheet1.xlsx"

# Spreadsheet: spreadsheet1.xlsx

| Column A | Column B | Column C |
| -------- | -------- | -------- |
| Data 1   | Data 2   | Data 3   |
| Data 4   | Data 5   | Data 6   |

:::

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
