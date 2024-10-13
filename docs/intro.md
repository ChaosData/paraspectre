## Introduction, Or: What Is paraspectre?

It's a function hooking platform for (currently just) Android, focusing on the
Java side of things. Hooks are written in (J)Ruby, and support direct access
to the Java environment, including the ability to extend Java classes and
implement Java interfaces, whether they are from Android's core standard library
or defined within the application being hooked.

## Why Do I Need It?

**tl;dr** paraspectre combines several of the most useful features of
available tools with as few of said tools' painful drawbacks as possible.

You don't ***need*** it, but it will make your life easier.
There are lots of options for dynamically instrumenting Java code on Android.

 * JWDP
 * Xposed
 * Frida
 
But they all have different pros, cons, and tradeoffs.

JDWP is good at tapping methods, inspecting call frames, evaluating Java
expressions within the context of the breakpoint/method/instance. It is
notorious for its terrible interfaces, the massive runtime performance hit,
and requiring an active debugger client.

Xposed is good at ensuring consistent function hooking early in application
startup, Java agent-like characteristics related to method instrumentation,
and enabling hooks to be developed in any JVM language that runs on Android.
It is notorious for its lag in keeping up with new Android versions, and
requires that Zygote be restarted (generally requiring a full device reboot)
each time an Xposed module is updated. It also generally requires large
amounts of Java Reflection API code to interact with custom application code.

Frida directly supports manipulating object instances and bundles a large
high-quality toolkit of components useful for analyzing Applications. However,
hooks must be written in JavaScript, and its JavaScript-Java FFI layer for
Android is painful to use, heavily resembling the Java Reflection API itself.
The latter point is not a knock on Frida itself (it's awesome), but on the
limits of its JavaScript for writing Java method hooks for Android. The main
issues with Frida on Android relate to its design, which is based on having
an active client and frida-server instance to drive instrumentation.
frida-gadget alleviates some of these problems, but provides several of its own
related to dynamically instrumenting arbitrary applications on the fly. The
main issue with Frida on Android is that it is difficult to ensure that hooks
are applied before the code being targeted is ever run. The new (possibly still
undocumented) spawn gating also chips away at this problem, but relies on an
active client and frida-server instance.

### But What About paraspectre?

paraspectre tries to be a best of several worlds. Like JDWP, it supports a
REPL interface to manipulate application state at the site of a function hook.
Unlike JDWP, it does not heavily impact runtime performance and its REPL is
functional. Like Xposed (and currently because it's built on top of Xposed),
it supports consistent early hooking. Unlike Xposed, it provides first-class
access to target code without ridiculous amounts of reflection boilerplate
and doesn't require Zygote reboots to reload hooks. Like Frida, it supports
easily scripting together function hooks with a tight development-run loop.
Unlike Frida, it doesn't require active driver processes.

### And The Negatives?

Lack of support for newer Android versions, as inherited from Xposed.

