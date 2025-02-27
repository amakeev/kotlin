/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import kotlin.reflect.KProperty

sealed class IrAttribute<T : Any>(val name: String) {
    override fun toString(): String = name
}

class IrIndexBasedAttribute<T : Any>(
    val registry: IrIndexBasedAttributeRegistry,
    val id: Int,
    name: String
) : IrAttribute<T>(name) {
    internal val bitMask = 1L shl id
    internal val prefixMask = if (id == 0) 0L else -1L ushr (64 - id)

    operator fun getValue(thisRef: IrElementBase, property: KProperty<*>): T? {
        return thisRef.getAttributeInternal(this)
    }

    operator fun setValue(thisRef: IrElementBase, property: KProperty<*>, value: T?) {
        thisRef.setAttributeInternal(this, value)
    }

    override fun toString(): String = "$name ($id)"
}

open class IrIndexBasedAttributeRegistry {
    private val attributePerIndex = ArrayList<IrIndexBasedAttribute<*>>()

    fun <T : Any> create(name: String): IrIndexBasedAttribute<T> {
        val id = attributePerIndex.size
        require(id < 64) { "Too many index-based attributes" }
        val attribute = IrIndexBasedAttribute<T>(this, id, name)
        attributePerIndex.add(attribute)
        return attribute
    }

    fun getById(id: Int): IrIndexBasedAttribute<*> = attributePerIndex[id]

    companion object : IrIndexBasedAttributeRegistry()
}
