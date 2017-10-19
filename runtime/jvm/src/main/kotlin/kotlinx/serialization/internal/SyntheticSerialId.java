/*
 *  Copyright 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlinx.serialization.internal;

import kotlinx.serialization.SerialId;

import java.lang.annotation.Annotation;

class SyntheticSerialId implements SerialId {

    private final int id;

    public SyntheticSerialId(int id) {
        this.id = id;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return SerialId.class;
    }

    @Override
    public int id() {
        return this.id;
    }
}
