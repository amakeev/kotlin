// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.RequiresOptIn
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
@JvmName("isNotNull1")
fun Any?.isNotNull(): Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)
    }
    return this != null
}

@OptIn(ExperimentalContracts::class)
val Any?.isNotNull: Boolean
    get() {
        contract {
            returns(true) implies (this@isNotNull != null)
        }
        return this@isNotNull != null
    }
