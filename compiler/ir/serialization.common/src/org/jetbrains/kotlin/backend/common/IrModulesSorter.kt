/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrTreeSymbolsVisitor
import org.jetbrains.kotlin.ir.util.SymbolVisitor
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.DFS

// TODO: This is a temporary workaround until OSIP-695 is implemented. Needs to be removed in KT-77244.
@Suppress("unused")
object IrModulesSorter {
    fun reverseTopoOrder(modules: Collection<IrModuleFragment>): List<IrModuleFragment> {
        if (modules.size <= 1) return modules.toList()

        val dependencies: MutableMap<IrModuleFragment, MutableSet<IrModuleFragment>> = hashMapOf()

        for (inspectedModule in modules) {
            val dependenciesOfInspectedModule: MutableSet<IrModuleFragment> = dependencies.getOrPut(inspectedModule) { hashSetOf() }

            val symbolVisitor = object : SymbolVisitor {
                override fun visitDeclaredSymbol(symbol: IrSymbol) = Unit

                override fun visitSymbol(symbol: IrSymbol) {
                    if (!symbol.isBound) return

                    val packageFragment = (symbol.owner as? IrDeclaration)?.getPackageFragment() ?: return

                    val referencedModule = when (packageFragment) {
                        is IrFile -> packageFragment.module
                        is IrExternalPackageFragment -> TODO()
                        else -> return
                    }

                    if (referencedModule != inspectedModule)
                        dependenciesOfInspectedModule += referencedModule
                }
            }

            inspectedModule.acceptVoid(object : IrTreeSymbolsVisitor(symbolVisitor) {
                override fun visitType(container: IrElement, type: IrType) {
                    (type as? IrSimpleType)?.classifier?.let { symbolVisitor.visitSymbol(it) }
                }
            })
        }

        return DFS.topologicalOrder(modules) { module -> dependencies[module].orEmpty() }.reversed()
    }
}
