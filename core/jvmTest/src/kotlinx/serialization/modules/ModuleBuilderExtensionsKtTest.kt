package kotlinx.serialization.modules

import kotlinx.serialization.*
import org.junit.*

import kotlin.reflect.KClass

class ModuleBuilderExtensionsKtTest {
    interface IA {
        val propertyA: Double
    }

    interface IB {
        val propertyB: UInt
    }

    interface IC : IB {
        val propertyC: Double
    }

    @Serializable
    open class GrandGrandParent(
        val grandgrand: String
    ) : IA {
        override val propertyA: Double = 1.0
    }

    @Serializable
    open class GrandParent : GrandGrandParent, IB {
        val grand: Int

        constructor(grandgrand: String, grand: Int) : super(grandgrand) {
            this.grand = grand
        }

        override val propertyB: UInt = 4u
    }

    @Serializable
    open class Parent : GrandParent, IC {
        val parent: Float

        constructor(grandgrand: String, grand: Int, parent: Float) : super(grandgrand, grand) {
            this.parent = parent
        }

        override val propertyC: Double
            get() = 1.5

    }

    @Serializable
    class Child : Parent {
        constructor(grandgrand: String, grand: Int, parent: Float) : super(grandgrand, grand, parent)
    }

    private fun <Base : Any, T : Base> SerializersModule.assertPoly(
        serializer: KSerializer<T>,
        base: KClass<Base>,
        obj: T
    ) =
        kotlin.test.assertEquals(
            serializer,
            getPolymorphic(base, obj),
            "No serializer for ${obj::class} with base $base in module"
        )

    @Test
    fun testPolymorphicUnsafely() {
        val module = SerializersModule {
            polymorphicUnsafely(Parent::class, Child::class, Child.serializer())
            polymorphicUnsafely(Parent::class, Parent::class, Parent.serializer())
            polymorphicUnsafely(GrandParent::class, Parent::class, Parent.serializer())
            polymorphicUnsafely(GrandParent::class, Child::class, Child.serializer())
        }
        val parent = Parent("gg", 1, 1.1f)
        val child = Child("gg_child", 2, 2.2f)
        module.apply {
            assertPoly(Child.serializer(), Parent::class, child)
            assertPoly(Child.serializer(), GrandParent::class, child)
            assertPoly(Parent.serializer(), Parent::class, parent)
            assertPoly(Parent.serializer(), GrandParent::class, parent)
        }
    }

    @Test
    fun testPolymorphicAllSuperClasses() {
        val module = SerializersModule {
            polymorphicAllSuperClasses(Child::class)
        }
        val child = Child("gg_child", 2, 2.2f)
        val childSerializer = Child.serializer()
        module.apply {
            assertPoly(childSerializer, Parent::class, child)
            assertPoly(childSerializer, GrandParent::class, child)
            assertPoly(childSerializer, GrandGrandParent::class, child)
            assertPoly(childSerializer, IA::class, child)
            assertPoly(childSerializer, IB::class, child)
            assertPoly(childSerializer, Any::class, child)
        }
    }

    @Test
    fun testPolymorphicSuperRecursive() {
        val module = SerializersModule {
            polymorphicSuperRecursive(Child::class)
        }
        val grandGrandParent = GrandGrandParent("gg")
        val grandParent = GrandParent("gg_grand", 0)
        val parent = Parent("gg_parent", 1, 1.1f)
        val child = Child("gg_child", 2, 2.2f)
        module.apply {
            assertPoly(Child.serializer(), Parent::class, child)
            assertPoly(Child.serializer(), GrandParent::class, child)
            assertPoly(Child.serializer(), GrandGrandParent::class, child)
            assertPoly(Child.serializer(), IA::class, child)
            assertPoly(Child.serializer(), IB::class, child)
            assertPoly(Child.serializer(), Any::class, child)
            assertPoly(Parent.serializer(), GrandParent::class, parent)
            assertPoly(Parent.serializer(), GrandGrandParent::class, parent)
            assertPoly(Parent.serializer(), IA::class, parent)
            assertPoly(Parent.serializer(), IB::class, parent)
            assertPoly(Parent.serializer(), Any::class, parent)
            assertPoly(GrandParent.serializer(), GrandGrandParent::class, grandParent)
            assertPoly(GrandParent.serializer(), IA::class, grandParent)
            assertPoly(GrandParent.serializer(), IB::class, grandParent)
            assertPoly(GrandParent.serializer(), Any::class, grandParent)
            assertPoly(GrandGrandParent.serializer(), IA::class, grandGrandParent)
            assertPoly(GrandGrandParent.serializer(), Any::class, grandGrandParent)
        }
    }
}