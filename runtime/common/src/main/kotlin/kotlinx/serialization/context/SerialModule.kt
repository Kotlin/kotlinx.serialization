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

import kotlinx.serialization.*

/**
 * SerialModule is a collection of classes associated with its serializers,
 * postponed for runtime resolution. Its single purpose is to register all
 * serializers it has in the given [MutableSerialContext].
 *
 * Typically, one can create a module (using library implementation or anonymous class)
 * per file, or per package, to hold
 * all serializers together and then register it in some [AbstractSerialFormat].
 *
 * @see AbstractSerialFormat.install
 */
interface SerialModule {

    /**
     * Registers everything it has in the [context].
     *
     * @see MutableSerialContext.registerSerializer
     * @see MutableSerialContext.registerPolymorphicSerializer
     */
    fun registerIn(context: MutableSerialContext)
}
