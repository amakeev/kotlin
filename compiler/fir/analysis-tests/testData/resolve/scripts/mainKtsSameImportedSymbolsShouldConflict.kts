// RUN_PIPELINE_TILL: BACKEND

// FILE: script.main.kts

@file:Import("first.kts")
@file:Import("second.kts")

<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()

// FILE: first.kts

fun foo() = 1

// FILE: second.kts

fun foo() = 2
