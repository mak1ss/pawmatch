# Baseline parity tooling

Generates golden test data that proves the Java port of the baseline (`com.pawmatch.inference.baseline`) produces the
same results as the team's Python reference, `pawmatch-baseline-1.0.0`.

- `reference/pawmatch.py`: copy of the ML team's baseline v1.0.0 (Team_1_544 Drive, "PawMatch Базові моделі /
  Основна модель"). **Reference only: not part of the application.** Don't edit it; replace it when the ML team
  releases a new baseline version.
- `generate_goldens.py`: runs the reference on a fixed, seeded set of profiles against **our** seed catalogue
  (`src/main/resources/db/changelog/data/breeds-v1.0.0.csv`) and writes
  `src/test/resources/inference/baseline/goldens.json`. `BaselineParityTest` compares the Java engine against it.

Regenerate after changing the baseline or the seed catalogue (Python 3.10+, standard library only):

```
python tools/baseline-parity/generate_goldens.py
```

Run it from the project root. The output is deterministic: same inputs, same file.
