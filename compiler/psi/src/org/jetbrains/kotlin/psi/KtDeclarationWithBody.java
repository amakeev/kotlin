/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface KtDeclarationWithBody extends KtDeclaration {
    @Nullable
    KtExpression getBodyExpression();

    @Nullable
    PsiElement getEqualsToken();

    @Override
    @Nullable
    String getName();

    @Nullable
    default KtContractEffectList getContractDescription() {
        return null;
    }

    default boolean hasContractEffectList() {
        return getContractDescription() != null;
    }

    /**
     * @return whether the declaration has an expression body.
     *
     * @see #hasBody()
     * @see #hasBlockBody()
     */
    default boolean hasExpressionBody() {
        return hasBody() && !hasBlockBody();
    }

    /**
     * @return whether the declaration has a block body.
     *
     * @see #hasBody()
     * @see #hasExpressionBody()
     */
    boolean hasBlockBody();

    /**
     * @return whether the declaration has a body (expression or block).
     *
     * @see #hasBlockBody()
     * @see #hasExpressionBody()
     */
    boolean hasBody();

    boolean hasDeclaredReturnType();

    @NotNull
    List<KtParameter> getValueParameters();

    /**
     * @return the body expression as a {@code KtBlockExpression}, or {@code null} if the body expression
     *         is not present or is not a block expression.
     */
    @Nullable
    default KtBlockExpression getBodyBlockExpression() {
        KtExpression bodyExpression = getBodyExpression();
        if (bodyExpression instanceof KtBlockExpression) {
            return (KtBlockExpression) bodyExpression;
        }

        return null;
    }
}

