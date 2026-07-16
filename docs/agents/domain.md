# Domain docs

This is a single-context repository.

## Before exploring

Read these sources when they exist:

- `CONTEXT.md` at the repository root
- relevant ADRs under `docs/adr/`

If these files do not exist, proceed silently. Do not suggest creating them
upfront. They should be created when domain terms or architectural decisions
are actually resolved.

## Expected structure

```
/
├── CONTEXT.md
├── docs/
│   ├── adr/
│   └── agents/
└── src/
```

## Domain vocabulary

When naming a domain concept in an issue, refactoring proposal, hypothesis
or test, use the terminology defined in `CONTEXT.md`.

Do not replace defined terms with synonyms. If a required concept is absent
from the glossary, reconsider whether the term belongs to the project or
record the gap for a future domain discussion.

## Architectural decisions

Read ADRs relevant to the area being changed.

If proposed work contradicts an existing ADR, identify the conflict
explicitly instead of silently overriding the prior decision.
