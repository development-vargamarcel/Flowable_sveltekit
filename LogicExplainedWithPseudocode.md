# Logic Explained With Pseudocode

```text
PROCESS ChangeWorkflow
  INPUT: change__description

  STEP doc__updateUsage
    WRITE usage.md with new behavior and updated version

  STEP doc__updateExamples
    WRITE examples.md with at least one relevant example

  STEP doc__updatePseudocode
    WRITE LogicExplainedWithPseudocode.md to match current logic

  STEP ver__incrementSemantic
    IF change is breaking THEN increment major
    ELSE IF change adds backward-compatible functionality THEN increment minor
    ELSE increment patch
    APPLY zero-padded format MAJOR.MINOR.PATCH

  OUTPUT: updated documentation and incremented version
END PROCESS
```
