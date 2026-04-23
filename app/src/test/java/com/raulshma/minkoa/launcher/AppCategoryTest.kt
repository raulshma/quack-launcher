package com.raulshma.minkoa.launcher

import org.junit.Assert.*
import org.junit.Test

class AppCategoryTest {

    @Test
    fun allCategoriesHaveLabels() {
        for (category in AppCategory.entries) {
            assertTrue(category.label.isNotBlank())
        }
    }

    @Test
    fun categoryLabelsAreHumanReadable() {
        assertEquals("Social", AppCategory.Social.label)
        assertEquals("Productivity", AppCategory.Productivity.label)
        assertEquals("Games", AppCategory.Games.label)
        assertEquals("Entertainment", AppCategory.Entertainment.label)
        assertEquals("Communication", AppCategory.Communication.label)
        assertEquals("Media", AppCategory.Media.label)
        assertEquals("Tools", AppCategory.Tools.label)
        assertEquals("Shopping", AppCategory.Shopping.label)
        assertEquals("Finance", AppCategory.Finance.label)
        assertEquals("Health", AppCategory.Health.label)
        assertEquals("Travel", AppCategory.Travel.label)
        assertEquals("Education", AppCategory.Education.label)
        assertEquals("Other", AppCategory.Other.label)
    }

    @Test
    fun fromAppInfoWithNullReturnsOther() {
        assertEquals(AppCategory.Other, AppCategory.fromAppInfo(null))
    }

    @Test
    fun allCategoriesAreDistinct() {
        val labels = AppCategory.entries.map { it.label }
        assertEquals(labels.size, labels.toSet().size)
    }
}
