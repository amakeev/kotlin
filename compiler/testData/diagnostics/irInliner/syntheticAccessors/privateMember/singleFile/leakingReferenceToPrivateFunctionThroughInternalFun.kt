// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

class A {
    private fun foo() {}
    private inline fun privateFun() = ::foo
    internal inline fun internalFun1() = <!IR_PRIVATE_CALLABLE_REFERENCED_BY_NON_PRIVATE_INLINE_FUNCTION_CASCADING_WARNING!>privateFun()<!>
    internal inline fun internalFun2() = <!IR_PRIVATE_CALLABLE_REFERENCED_BY_NON_PRIVATE_INLINE_FUNCTION_WARNING!>::foo<!>
}
