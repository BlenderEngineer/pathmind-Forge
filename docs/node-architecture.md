# Node Architecture

`Node.java` is the compatibility shell for editor state, serialization, and legacy call sites. New behavior should not be added there by default.

Command execution is split by behavior family:

- `NodeInventoryCommandExecutor`: hotbar, drop, slot clicks, screen clicks, and item movement.
- `NodeGuiCommandExecutor`: UI Utils integration and player GUI open/close.
- `NodeNavigationCommandExecutor`: Baritone/pathing commands and navigation guards.
- `NodeTextIoCommandExecutor`: book and sign writing.
- `NodeFlowCommandExecutor`: start/stop chain, run preset, and stop all.

Shared node state is also split out:

- `NodeRuntimeState`: transient execution/runtime values.
- `NodeLayoutState`: position, size, and layout values.
- `NodeInteractionState`: selection, dragging, and input interaction state.
- `NodeAttachments`: parent/child attachment bookkeeping.

When adding a node type, prefer the smallest owner:

1. Add type metadata through the existing node definition/behavior registry when possible.
2. Put command execution in the executor for that behavior family, or create a new executor if it is a distinct family.
3. Keep `Node.java` wrappers thin and behavior-free.
4. Keep cross-family helpers package-private only when they are genuinely shared.

The goal is for `Node.java` to keep shrinking over time while preserving old save data and public APIs.
