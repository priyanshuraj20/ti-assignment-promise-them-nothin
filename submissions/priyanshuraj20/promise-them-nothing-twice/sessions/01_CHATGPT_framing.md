# Chat Conversation — ChatGPT

> **Session context:** This was the first ChatGPT session for the Trilogy **"Promise Them Nothing Twice"** assignment. The goal of this session was to understand the assignment brief, repository structure, submission requirements, and overall execution strategy before starting implementation. The detailed design and implementation discussions continued later in Antigravity.

---

## Understanding the Assignment

**User**

I just received the Trilogy assignment, and the deadline is pretty close. Before implementing anything, I want to fully understand what the company is actually asking for.

I'll share both the GitHub repository and the assignment document. Please take your time, go through everything carefully, and explain the expectations from first principles before we jump into implementation.

Repository:

`trilogy-group/ti-assignment-promise-them-nothing-twice`

---

**User**

*Uploaded assignment document.*

I also asked Claude to summarize part of the repository, but I'm still not confident that I've understood the assignment correctly.

The repository describes the expected submission structure:

```markdown
# Submission directory (template)

This folder shows the expected layout...
...
```

Before moving ahead, I'd like to understand:

- What is Trilogy actually evaluating?
- What are the expected deliverables?
- What level of engineering quality are they expecting?
- How should I plan this assignment before writing any code?

---

## Choosing the AI Workflow

**User**

Which AI model would you recommend for this assignment?

I mainly have access to Gemini, while Claude has a much smaller context window. I expect this assignment to involve a lot of long review sessions, so I'd like a good balance between response quality and token efficiency.

---

**User**

*Uploaded repository document.*

As I mentioned earlier, I don't want to jump straight into implementation for every milestone.

Instead, I'd like to follow a structured workflow:

- understand the problem,
- review the design,
- discuss architectural decisions,
- review the implementation,
- identify weaknesses,
- then implement.

Do you agree, or would you recommend a different workflow?

---

**Assistant**

I think that's the right approach.

Most candidates immediately begin writing code, but this assignment appears to evaluate engineering judgement as much as implementation.

A structured workflow—problem framing, architecture review, implementation, verification, and documentation—will likely produce a stronger submission than implementing features without reviewing design decisions first.

I'd proceed with that workflow.

---

**User**

Before we begin implementing anything, I think we should finalize the repository structure and documentation strategy.

I'd rather spend some extra time planning now than have to restructure the project halfway through development. Since the final submission includes documentation as well as code, having a clear organization from the beginning seems like the safer approach.

---

**Assistant**

I think that's a good decision.

The repository structure, documentation, and implementation should evolve together rather than independently. Locking down the overall organization early reduces unnecessary refactoring later and makes it easier to keep the codebase consistent as the project grows.

Once we're comfortable with the structure, we can focus entirely on implementation.

---

**User**

I also don't want to optimize for finishing the assignment as quickly as possible.

I'd rather submit a solution that demonstrates solid engineering judgement than rush to implement features that aren't actually required. If we have time later, we can always improve the implementation, but I'd prefer to get the fundamentals right first.

---

**Assistant**

I agree with that approach.

From the assignment description, it seems clear that the reviewers are evaluating engineering decisions in addition to implementation quality. A smaller, well-reasoned solution is generally stronger than a larger solution with unnecessary complexity.

Let's stay focused on the stated requirements first, and only consider additional improvements if they provide clear value rather than simply increasing the scope.

---

## Repository Setup

**User**

*Uploaded screenshots.*

Can you check that everything looks correct so far?

Before I continue, I'd like to make sure there aren't any Git or repository issues that might cause problems later.

---

**User**

*Uploaded screenshots.*

I have screenshots from two terminals.

One appears to be working correctly, while the other doesn't.

Can you help me identify the issue and explain what I should do next?

---

**Assistant**

The repository itself looks fine.

The issue appears to be related to the terminal session rather than the repository.

Let's fix the environment first before making any repository changes.

---

**User**

I restarted everything, and it seems to be working now.

Should I change directories before continuing?

If so, which directory should I work from?

---

**Assistant**

Yes.

Before proceeding, make sure you're working from the project root so that all subsequent commands execute relative to the correct repository.

---

**User**

Everything is open now.

I think we're ready to continue.

What's the next step?

---

## Documentation Review Strategy

**User**

*Uploaded file.*

Before implementation, I'd like to review every document carefully.

We'll review one file at a time rather than trying to improve everything at once.

These are the files Trilogy reviewers will actually read, so I'd rather spend time reviewing them properly than rush through them.

If we decide something should change, I'd prefer deliberate improvements instead of rewriting documents unnecessarily.

---

**Assistant**

I agree.

Let's review each document independently.

For every file we'll evaluate:

- technical correctness,
- clarity,
- engineering reasoning,
- reviewer perspective,
- consistency with the assignment,

before making any edits.

If the current version is already strong, we'll keep it unchanged.

---

**User**

*Uploaded file.*

Let's continue with the next document.

---

**User**

*Uploaded file.*

Here's the third document.

---

**User**

*Uploaded file.*

This is probably the most important document in the repository.

Please review it carefully.

I'd like it to sound natural while still demonstrating strong engineering reasoning.

Take your time—I think getting this document right is worth the effort.

---

**Assistant**

Understood.

Rather than editing individual sentences, I'll review it as if I were the Trilogy reviewer.

If I think a paragraph is already strong, I'll recommend keeping it.

If I think the reasoning can be improved, I'll explain why before suggesting changes.

---

**User**

*Uploaded file.*

I've made several revisions.

I think this version is stronger than the previous one, but I'd like another review before we move on.

---

**Assistant**

I'll review it again from scratch rather than assuming the earlier feedback is still correct.

If I notice new weaknesses—or if one of your revisions solves a previous concern—I'll point that out explicitly.

---

**User**

*Uploaded file.*

Here's another revision.

If you think this is the strongest version, I'd rather finalize it and move on instead of endlessly tweaking it.

---

**Assistant**

That's a good approach.

At some point additional edits stop adding value.

If this version is technically sound, clearly justified, and aligned with the assignment, I'd recommend freezing it and moving to the next document.

---

## README

**User**

Where should the `README.md` be placed?

---

**User**

*Uploaded screenshots.*

Which option should I choose?

---

**User**

*Uploaded screenshots.*

I think we've only discussed part of the setup.

Can we also walk through the remaining steps before moving on?