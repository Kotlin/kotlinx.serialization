/*
 * Copyright 2018 JetBrains s.r.o.
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

package kotlinx.serialization.context

/**
 * A [SerialModule] for composing other modules
 *
 * Has convenient operator [plusAssign].
 *
 * @see SerialModule.plus
 */
class CompositeModule(modules: List<SerialModule> = listOf()): SerialModule {
    constructor(vararg modules: SerialModule) : this(modules.toList())

    private val modules: MutableList<SerialModule> = modules.toMutableList()

    override fun registerIn(context: MutableSerialContext) {
        modules.forEach { it.registerIn(context) }
    }

    public operator fun plusAssign(module: SerialModule): Unit { modules += module }
    public fun addModule(module: SerialModule) = plusAssign(module)
}

/**
 * Composes [this] module with [other].
 */
operator fun SerialModule.plus(other: SerialModule): CompositeModule {
    if (this is CompositeModule) {
        this.addModule(other)
        return this
    }
    return CompositeModule(this, other)
}
