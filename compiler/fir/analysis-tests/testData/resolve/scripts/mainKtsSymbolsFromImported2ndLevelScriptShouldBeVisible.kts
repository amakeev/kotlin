// RUN_PIPELINE_TILL: BACKEND

// FILE: script.main.kts

@file:Import("imported.main.kts")

a

A()

E.V

O.v

foo()
bar()

// FILE: imported.main.kts

@file:Import("imported.kts")

fun bar() = 42

// FILE: imported.kts

val a = 42

class A

enum class E {
    V
}

object O {
    val v = 42
}

fun foo() = 42
