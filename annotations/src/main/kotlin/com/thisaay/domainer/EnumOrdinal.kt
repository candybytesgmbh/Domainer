package com.thisaay.domainer

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
annotation class EnumOrdinal(val clazz: KClass<*>)
