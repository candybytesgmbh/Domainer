package com.thisaay.domainer

import kotlin.reflect.KClass


@Target(AnnotationTarget.CLASS)
annotation class DomainModel(val clazz: KClass<*>)