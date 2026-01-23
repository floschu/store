# changelog

## `[0.5.0]` - 2025-01-22

- Update kotlin to `2.3.0`
- Fix concurrency bug in `Implementation.kt`
- Add agent skill for AI coding assistants (`.opencode/skills/floschu-store/SKILL.md`)

## `[0.4.0]` - 2025-11-21

- Add `val state: StateFlow<State>` to `EffectExecution.Context`

## `[0.3.0]` - 2025-11-09

- Update kotlin to `2.2.21`
- Fix: Effects with id's can be restarted after their completion

## `[0.2.0]` - 2025-08-24

- Remove `initialEffect` from `Store` factory functions
- Add `initialEffect` to `Reducer` factory function

## `[0.1.0]` - 2025-08-03

- initial release
