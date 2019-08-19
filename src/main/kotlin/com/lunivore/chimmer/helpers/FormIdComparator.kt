package com.lunivore.chimmer.helpers

import com.lunivore.chimmer.FormId


@UseExperimental(ExperimentalUnsignedTypes::class)
class FormIdComparator(private val loadOrder: List<String>) : Comparator<FormId.Key>{
    override fun compare(o1: FormId.Key?, o2: FormId.Key?): Int {
        if(o1 == null || o2 == null) throw IllegalArgumentException("This should never happen as FormIds are never null")

        val o1Index = loadOrder.indexOf(o1.master)
        val o2Index = loadOrder.indexOf(o2.master)

        if (o1Index == -1  || o2Index == -1)
            throw IllegalArgumentException("Error comparing FormIds; o1.master=${o1.master}, o2.master=${o2.master}, loadOrder=$loadOrder")

        return if (o1Index == o2Index) o1.unindexed.compareTo(o2.unindexed) else o1Index.compareTo(o2Index)
    }
}