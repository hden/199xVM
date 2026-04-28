# Loader-aware resolution architecture

This is the target architecture for class identity, class loading, linking, and
symbolic reference resolution in 199xVM.

It describes the steady-state model. It is not an implementation plan or a
debugging note.

The normative reference is JVMS Chapter 5, "Loading, Linking, and Initializing,"
Java SE 25:

- <https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-5.html>

---

## Problem statement

The VM must preserve JVM class identity when it loads, links, resolves, or
exposes a symbolic reference through Java-visible reflection surfaces.

A class or interface is not identified by name alone. At run time, JVMS Chapter 5
identifies a non-array class or interface by:

> binary name + defining loader

The same binary name may legally refer to different classes under different
defining loaders. A global name table is not enough. If 199xVM repairs loader
context after lookup, it may already have picked the wrong class, field, method,
constructor, static storage, run-time package, or `Class` mirror.

Loader context belongs in the resolution path. Bytecode execution must receive
targets that already carry the right class identity.

---

## Scope

Covered here:

- class and interface identity
- defining loader and initiating loader records
- array class identity
- run-time package identity
- `Class` mirror identity
- symbolic references in run-time constant pools
- class, field, method, and interface method resolution
- loading constraints imposed during preparation and resolution
- JVM-style failure surfaces for invalid or missing linkage targets

Out of scope: unrelated runtime behavior, scheduling, broad reflection support,
annotation processing, resource loading, JDK shim coverage, and performance work
unless it directly affects Chapter 5 identity or resolution semantics.

---

## JVMS facts to preserve

### Loading and identity

JVMS Chapter 5 distinguishes loading, linking, and initialization.

- Loading finds a binary representation of a class or interface and creates the
  class or interface from that representation.
- Linking verifies, prepares, and resolves symbolic references so the class or
  interface can execute.
- Initialization executes the class or interface initialization method.

After creation, a class or interface is identified by its binary name and
defining loader. Its run-time package is the package name plus that defining
loader.

### Defining and initiating loaders

If a loader directly loads a class or interface, that loader defines it. It is
the defining loader.

If a loader starts loading a class or interface, either directly or by
delegation, that loader initiates loading. It is an initiating loader. The
initiating loader and defining loader can be different.

199xVM must record initiating-loader relationships because loading constraints
are checked against those records.

### Array classes

Array classes do not have external binary representations.

When an array class is created:

- if its component type is a reference type, the VM recursively loads and creates
  the component type using the loader associated with the array creation;
- the VM creates the array class itself;
- if the component type is a reference type, the array class has the defining
  loader of the component type;
- otherwise, the array class has the bootstrap loader as its defining loader;
- the loader associated with the array creation is recorded as an initiating
  loader of the array class.

The defining loader of the referring class matters for array creation, but it is
not used to load the array class as if the array class had an external class file.

### Symbolic references

A run-time constant pool contains symbolic references and static constants.
Symbolic references may refer to:

- classes and interfaces
- fields
- class methods
- interface methods
- method types
- method handles
- dynamically computed constants and call sites

This file covers class/interface, field, method, and interface method
resolution. Method handles, method types, `CONSTANT_Dynamic`, and
`invokedynamic` must still respect the identity model, but their full behavior
is out of scope.

### Resolution results and errors

Resolution dynamically turns a symbolic reference into a concrete result.

For class, field, method, and interface method references:

- successful resolution of the same symbolic reference must produce the same
  entity on later attempts;
- failed resolution of the same symbolic reference must fail with the same error
  on later attempts;
- errors must be thrown at a program point that directly or indirectly uses the
  symbolic reference.

An implementation may choose when to resolve, except where JVMS fixes the timing.
That freedom does not allow resolution to drop loader context.

---

## Identity model

### Binary name

The Java-visible binary name is the name part of non-array class/interface
identity.

The implementation may also keep class-file or internal-name forms such as
`java/lang/String`. Those forms are implementation details. They do not replace
the Java-visible binary name in `Class` mirror behavior or in the architecture.

### Defining loader

The defining loader is the loader that defines the class or interface.

For classes defined through `ClassLoader.defineClass`, the defining loader is the
receiver loader of that `defineClass` operation.

### Class identity

For non-array classes and interfaces:

> class identity = (binary name, defining loader)

That gives us these rules:

- same binary name + same defining loader denotes the same class or interface;
- same binary name + different defining loader may denote different classes or
  interfaces;
- lookup, storage, hierarchy walking, member resolution, and `Class` mirrors must
  preserve the defining loader component.

### Array identity

Array identity is derived from the array name, component type, and the defining
loader assigned by JVMS array creation rules.

Array classes are not ordinary non-array classes backed by class files.

### Run-time package identity

A run-time package is determined by:

> package name + defining loader

Package-private and protected access checks must use run-time package identity,
not package name alone.

### VM identity handles

The implementation may use internal class handles or lookup keys to distinguish
classes with the same binary name.

Internal handles are not Java-visible names. A `Class` mirror must expose the
Java-visible name and defining loader, not an internal lookup key.

---

## Loader records

Defining-loader and initiating-loader state must stay separate.

Required state:

- `(defining loader, binary name) -> class identity`
- `class identity -> binary name`
- `class identity -> defining loader`
- `(initiating loader, name) -> class identity`
- `class identity -> Class mirror`

Array-name keys must preserve the JVMS array-name form. Non-array keys must
preserve the Java binary name even if the interpreter uses slash-separated
internal names internally.

The `Class` mirror mapping must be stable for the same class identity.

---

## Resolution contracts

### Common input and output

Resolution starts with:

- a symbolic reference from the run-time constant pool;
- the class or interface `D` whose run-time constant pool contains that symbolic
  reference;
- the active loader and access-control context implied by JVMS Chapter 5.

Resolution must return a concrete target or a JVM linkage/access error.

A resolved target must carry enough identity to avoid redoing global name lookup
at execution time:

- a resolved class target identifies the class identity;
- a resolved field target identifies the declaring class identity, field name,
  and field descriptor;
- a resolved method target identifies the declaring class or interface identity,
  method name, and method descriptor.

### Class and interface resolution

To resolve a symbolic reference from `D` to a class or interface `C` denoted by
name `N`:

1. Use the defining loader of `D` to load and thereby create the class or
   interface denoted by `N`.
2. If `C` is an array class whose element type is a reference type, recursively
   resolve the symbolic reference to the element type.
3. Apply access control from `D` to `C`.

Resolution must not look up `N` in a global binary-name table before applying the
defining loader of `D`.

### Field resolution

To resolve a symbolic reference from `D` to a field:

1. Resolve the symbolic reference to class or interface `C` named by the field
   reference.
2. Search for a field with the referenced name and descriptor:
   - first in `C`;
   - then recursively in the direct superinterfaces of `C`;
   - then recursively in the superclass of `C`, if any.
3. If lookup fails, throw `NoSuchFieldError`.
4. Apply access control from `D` to the resolved field.
5. Impose loading constraints for class/interface names mentioned in the field
   descriptor, between the defining loader of `D` and the defining loader of the
   class or interface that actually declares the field.

Field resolution must not use materialized static storage as proof that the field
exists. Storage selection follows successful resolution; it does not define
resolution.

### Method resolution

To resolve a symbolic reference from `D` to a method in class `C`:

1. Resolve the symbolic reference to `C`.
2. If `C` is an interface, throw `IncompatibleClassChangeError`.
3. Search `C` and its superclasses for the referenced method.
4. For signature polymorphic methods, method lookup may succeed by name even when
   `C` does not declare a method with the exact descriptor from the reference.
5. If class/superclass lookup fails, search the superinterfaces of `C` using the
   JVMS maximally-specific method rules.
6. If lookup fails, throw `NoSuchMethodError`.
7. Apply access control from `D` to the resolved method.
8. Impose loading constraints for class/interface names mentioned in the method
   descriptor, between the defining loader of `D` and the defining loader of the
   class or interface that actually declares the method.

Invocation bytecodes may perform additional instruction-specific checks, such as
static versus instance member checks. Those checks operate on the resolved method
target.

### Interface method resolution

To resolve a symbolic reference from `D` to an interface method in interface `C`:

1. Resolve the symbolic reference to `C`.
2. If `C` is not an interface, throw `IncompatibleClassChangeError`.
3. Search for the referenced name and descriptor:
   - first in `C`;
   - then as a public, non-static method of `Object`;
   - then among maximally-specific superinterface methods;
   - then among other non-private, non-static superinterface methods permitted by
     JVMS interface method resolution.
4. If lookup fails, throw `NoSuchMethodError`.
5. Apply access control from `D` to the resolved method.
6. Impose loading constraints for class/interface names mentioned in the method
   descriptor, between the defining loader of `D` and the defining loader of the
   class or interface that actually declares the method.

Interface method resolution is not method resolution with a different starting
type. It has its own JVMS lookup and error rules.

---

## Loading constraints

Loading constraints preserve type safety when different loaders may resolve the
same name to different classes.

199xVM must impose and check loading constraints at the JVMS-defined points:

- during preparation, for descriptor types involved in overriding and method
  selection relationships;
- during field resolution, for descriptor types of the resolved field;
- during method resolution, for descriptor types of the resolved method;
- during interface method resolution, for descriptor types of the resolved
  interface method;
- after recording an initiating-loader relationship.

When a new initiating-loader record or loading constraint violates existing
constraints, the VM must retract the new record or constraint and throw
`LinkageError`.

The architecture must keep explicit constraint state, not just resolved member
targets.

---

## Error behavior

The VM must surface linkage and access failures instead of hiding them behind
fallback behavior.

Required failures:

- same binary name defined twice by the same defining loader fails with
  `LinkageError` behavior;
- a user-defined loader result whose name does not match the requested name fails
  with `NoClassDefFoundError` behavior;
- failed class loading propagates as the JVMS-defined loading failure;
- missing fields fail with `NoSuchFieldError`;
- missing methods fail with `NoSuchMethodError`;
- resolving a method reference to an interface, or an interface method reference
  to a class, fails with `IncompatibleClassChangeError`;
- failed access control fails with the corresponding access error;
- loading constraint violations fail with `LinkageError`.

The VM must not mask those failures by:

- silently returning default values;
- treating a missing call target as a no-op;
- falling back to an unrelated same-name class under another loader;
- repairing identity after a global lookup has already selected the wrong owner.

---

## Execution contract

Execution must consume resolved targets.

Bytecodes such as `new`, `anewarray`, `checkcast`, `instanceof`, `getfield`,
`getstatic`, `putfield`, `putstatic`, `invokevirtual`, `invokespecial`,
`invokestatic`, and `invokeinterface` must operate on targets resolved under the
JVMS context of the referring class or interface.

Execution may cache resolved targets only if the cache key preserves the resolved
identity. A cache keyed only by binary name is invalid for loader-sensitive
targets.

---

## Non-goals

Out of scope here:

- full `invokedynamic` behavior;
- full method handle or method type support;
- full module-system access behavior;
- broad reflection or annotation completeness;
- resource lookup completeness;
- library-specific behavior;
- performance caching without a correctness proof for loader-sensitive state.

These areas can build on this identity and resolution model, but this file does
not specify them.

---

## Acceptance criteria

The architecture is satisfied when:

- non-array class/interface identity is represented as binary name plus defining
  loader;
- defining loader and initiating loader records are distinct;
- array class creation follows JVMS array loader rules;
- run-time package identity includes defining loader;
- `Class` mirrors expose Java-visible names and defining loaders rather than
  internal lookup keys;
- class/interface resolution begins from the defining loader of the referring
  class or interface;
- field, method, and interface method resolution preserve the declaring owner
  identity and descriptor;
- loading constraints are represented, imposed, checked, and retracted on failure
  at the required points;
- execution paths consume resolved loader-aware targets instead of performing
  global name lookup;
- loader-sensitive caches, if any, are keyed by resolved identity;
- invalid linkage targets surface JVM-style errors instead of fallback behavior.
