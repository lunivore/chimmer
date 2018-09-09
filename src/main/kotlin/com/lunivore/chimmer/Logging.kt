package com.lunivore.chimmer

import org.apache.logging.log4j.LogManager.getLogger
import org.apache.logging.log4j.Logger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Logging<in R : Any> : ReadOnlyProperty<R, Logger> {

    override fun getValue(thisRef: R, property: KProperty<*>) = getLogger(getClassForLogging(thisRef.javaClass))

    private fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> {
        return javaClass.takeIf { !it.kotlin.isCompanion } ?: javaClass.enclosingClass
    }
}