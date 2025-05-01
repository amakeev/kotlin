/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.util.defaultValueForType
import org.jetbrains.kotlin.ir.util.implicitCastIfNeededTo

open class SharedVariablesManager<out S : Symbols>(protected val symbols: S) {
    open fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val valueType = originalDeclaration.type

        val provider = symbols.getRefProvider(valueType)
        val refType = provider.makeType(valueType)

        // TODO: If the initializer sets the value to the type default, use initializer offsets here
        val refConstructorCall =
            IrConstructorCallImpl.fromSymbolOwner(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, refType, provider.refConstructor
            ).apply {
                refType.arguments.mapTo(typeArguments) { it.typeOrFail }
            }

        return with(originalDeclaration) {
            IrVariableImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, IrVariableSymbolImpl(), name, refType,
                isVar = false,
                isConst = false,
                isLateinit = false,
            ).also {
                it.initializer = refConstructorCall
            }
        }
    }

    open fun defineSharedValue(originalDeclaration: IrVariable, sharedVariableDeclaration: IrVariable): IrStatement {
        val initializer = originalDeclaration.initializer ?: return sharedVariableDeclaration
        val default = IrConstImpl.defaultValueForType(initializer.startOffset, initializer.endOffset, originalDeclaration.type)
        if (initializer is IrConst && initializer.value == default.value) {
            // The property is preinitialized to the default value, so an explicit set is not required.
            return sharedVariableDeclaration
        }
        val initializationStatement = IrSetValueImpl(
            initializer.startOffset,
            initializer.endOffset,
            symbols.irBuiltIns.unitType,
            originalDeclaration.symbol,
            initializer,
            origin = null,
        )
        val sharedVariableInitialization = setSharedValue(sharedVariableDeclaration.symbol, initializationStatement)
        return with(originalDeclaration) {
            IrCompositeImpl(
                startOffset, endOffset, symbols.irBuiltIns.unitType, null,
                listOf(sharedVariableDeclaration, sharedVariableInitialization)
            )
        }
    }

    open fun getSharedValue(sharedVariableSymbol: IrValueSymbol, originalGet: IrGetValue): IrExpression =
        with(originalGet) {
            val provider = symbols.getRefProvider(type)
            val propertyGetter = provider.elementProperty.owner.getter!!
            val receiver = IrGetValueImpl(startOffset, endOffset, sharedVariableSymbol)
            val propertyGet = IrCallImpl(
                startOffset,
                endOffset,
                provider.propertyType(type),
                propertyGetter.symbol,
                propertyGetter.typeParameters.size,
                origin,
            ).also {
                it.arguments[0] = receiver
            }

            // In the case of `ObjectRef`, the type of the `element` property is always nullable,
            // but the type of the original variable may not be.
            propertyGet.implicitCastIfNeededTo(type)
        }

    open fun setSharedValue(sharedVariableSymbol: IrValueSymbol, originalSet: IrSetValue): IrExpression =
        with(originalSet) {
            val provider = symbols.getRefProvider(originalSet.symbol.owner.type)
            val propertySetter = provider.elementProperty.owner.setter!!
            val receiver = IrGetValueImpl(startOffset, endOffset, sharedVariableSymbol)
            IrCallImpl(
                startOffset,
                endOffset,
                symbols.irBuiltIns.unitType,
                propertySetter.symbol,
                propertySetter.typeParameters.size,
                origin,
            ).also {
                it.arguments[0] = receiver
                it.arguments[1] = value
            }
        }
}
