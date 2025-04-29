/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.kotlin.psi.KtPropertyAccessor;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyAccessorStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KotlinPropertyAccessorStubImpl extends KotlinStubBaseImpl<KtPropertyAccessor> implements KotlinPropertyAccessorStub {
    private final boolean isGetter;
    private final boolean hasBody;
    private final boolean hasBlockBody;

    public KotlinPropertyAccessorStubImpl(StubElement parent, boolean isGetter, boolean hasBody, boolean hasBlockBody) {
        super(parent, KtStubElementTypes.PROPERTY_ACCESSOR);
        this.isGetter = isGetter;
        this.hasBody = hasBody;
        this.hasBlockBody = hasBlockBody;
    }

    @Override
    public boolean isGetter() {
        return isGetter;
    }

    @Override
    public boolean hasBody() {
        // Workaround for KT-77208
        return hasBody/* || hasBlockBody*/;
    }

    @Override
    public boolean hasBlockBody() {
        return hasBlockBody;
    }
}
