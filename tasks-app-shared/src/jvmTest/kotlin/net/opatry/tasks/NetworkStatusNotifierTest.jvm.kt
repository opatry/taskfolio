/*
 * Copyright (c) 2025 Olivier Patry
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.opatry.tasks

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import net.opatry.network.NetworkStatusNotifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.virtualNow(): Instant {
    return Instant.fromEpochMilliseconds(testScheduler.currentTime)
}

fun assertInstantIn(
    expectedRange: ClosedRange<Instant>,
    actual: Instant,
    tolerance: Duration = 10.milliseconds,
    message: String? = null,
) {
    val prefix = if (message.isNullOrBlank()) "" else "$message\n"
    assertTrue(
        actual >= expectedRange.start - tolerance,
        "${prefix}Expected $actual to be at least ${expectedRange.start} (with tolerance of $tolerance)"
    )
    assertTrue(
        actual <= expectedRange.endInclusive + tolerance,
        "${prefix}Expected $actual to be at most ${expectedRange.endInclusive} (with tolerance of $tolerance)"
    )
}

fun assertInstantGreaterThan(lowerBound: Instant, actual: Instant, message: String? = null) {
    val prefix = if (message.isNullOrBlank()) "" else "$message\n"
    assertTrue(
        actual >= lowerBound,
        "${prefix}Expected $actual to be at least $actual"
    )
}

fun assertInstantLowerThan(upperBound: Instant, actual: Instant, message: String? = null) {
    val prefix = if (message.isNullOrBlank()) "" else "$message\n"
    assertTrue(
        actual <= upperBound,
        "${prefix}Expected $actual to be at most $actual"
    )
}

fun assertDurationEquals(
    expected: Duration,
    actual: Duration,
    tolerance: Duration = 10.milliseconds,
    message: String? = null,
) {
    val prefix = if (message.isNullOrBlank()) "" else "$message\n"
    assertTrue(
        actual in (expected - tolerance)..(expected + tolerance),
        "${prefix}Expected $actual to be within $tolerance of $expected"
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatusNotifierTest {
    @Suppress("TestFunctionName")
    private fun TestScope.NetworkStatusNotifierTest(isNetworkAvailable: () -> Boolean) =
        NetworkStatusNotifier(
            dispatcher = StandardTestDispatcher(testScheduler),
            checkNetwork = isNetworkAvailable
        )

    @Test
    fun `when network is ON then should notify true`() = runTest {
        val notifier = NetworkStatusNotifierTest { true }
        assertTrue(notifier.networkStateFlow().first())
    }

    @Test
    fun `when network is OFF then should notify false`() = runTest {
        val notifier = NetworkStatusNotifierTest { false }
        assertFalse(notifier.networkStateFlow().first())
    }

    @Test
    fun `when network is ON then becomes OFF new state should be notified`() = runTest {
        var callCount = 0
        val notifier = NetworkStatusNotifierTest {
            ++callCount == 1
        }

        val flow = notifier.networkStateFlow()

        val collectedStates = mutableListOf<Boolean>()
        val collectorJob = launch {
            flow.toList(collectedStates)
        }

        advanceTimeBy(5.5.seconds)
        collectorJob.cancelChildren()

        assertEquals(2, collectedStates.size)
        assertTrue(collectedStates.first())
        assertFalse(collectedStates.last())
    }

    @Test
    fun `when network is OFF then becomes ON new state should be notified`() = runTest {
        var callCount = 0
        val notifier = NetworkStatusNotifierTest {
            ++callCount == 2
        }

        val flow = notifier.networkStateFlow()

        val collectedStates = mutableListOf<Boolean>()
        val collectorJob = launch {
            flow.toList(collectedStates)
        }

        advanceTimeBy(5.5.seconds)
        collectorJob.cancelChildren()

        assertEquals(2, collectedStates.size)
        assertFalse(collectedStates.first())
        assertTrue(collectedStates.last())
    }

    @Test
    fun `when network is ON then it should check again after 5 seconds`() = runTest {
        val checkTimes = mutableListOf<Instant>()
        val notifier = NetworkStatusNotifierTest {
            checkTimes += virtualNow()
            true
        }

        val flow = notifier.networkStateFlow()

        val t0 = virtualNow()
        val collectorJob = launch {
            flow.collect {}
        }

        val t1 = virtualNow()
        advanceTimeBy(250.milliseconds)
        assertEquals(1, checkTimes.size)

        advanceTimeBy(5.seconds)
        collectorJob.cancelChildren()
        assertEquals(2, checkTimes.size)
        val t2 = virtualNow()

        assertInstantLowerThan(checkTimes[0], t0, "T0 should be lower than first collection time")
        assertInstantLowerThan(checkTimes[1], t1, "T1 should be lower than second collection time")
        assertInstantGreaterThan(checkTimes[1], t2, "T2 should be greater than second collection time")
        assertDurationEquals(5.seconds, checkTimes[1] - checkTimes[0], message = "There should be 5 seconds between the 2 collections")
        assertInstantIn(checkTimes[0]..checkTimes[1], t1)
    }

    @Test
    fun `when network is OFF then it should check again after 5 seconds`() = runTest {
        val checkTimes = mutableListOf<Instant>()
        val notifier = NetworkStatusNotifierTest {
            checkTimes += virtualNow()
            true
        }

        val flow = notifier.networkStateFlow()

        val t0 = virtualNow()
        val collectorJob = launch {
            flow.collect {}
        }

        val t1 = virtualNow()
        advanceTimeBy(250.milliseconds)
        assertEquals(1, checkTimes.size)

        advanceTimeBy(5.seconds)
        collectorJob.cancelChildren()
        assertEquals(2, checkTimes.size)
        val t2 = virtualNow()

        assertInstantLowerThan(checkTimes[0], t0, "T0 should be lower than first collection time")
        assertInstantLowerThan(checkTimes[1], t1, "T1 should be lower than second collection time")
        assertInstantGreaterThan(checkTimes[1], t2, "T2 should be greater than second collection time")
        assertDurationEquals(5.seconds, checkTimes[1] - checkTimes[0], message = "There should be 5 seconds between the 2 collections")
        assertInstantIn(checkTimes[0]..checkTimes[1], t1)
    }

    @Test
    fun `when network state is ON then polling delay should stay at 5 seconds`() = runTest {
        var callCount = 0
        val notifier = NetworkStatusNotifierTest {
            callCount++
            true
        }

        val flow = notifier.networkStateFlow()

        val collectorJob = launch {
            flow.collect {}
        }

        advanceTimeBy(250.milliseconds)
        assertEquals(1, callCount)

        advanceTimeBy(5.seconds)
        assertEquals(2, callCount)

        // called 2 times more in 10 seconds
        advanceTimeBy(10.seconds)
        assertEquals(4, callCount)

        collectorJob.cancelChildren()
    }

    @Test
    fun `when network state is OFF then polling delay should increase up to 15 seconds`() = runTest {
        var callCount = 0
        val notifier = NetworkStatusNotifierTest {
            callCount++
            false
        }

        val flow = notifier.networkStateFlow()

        val collectorJob = launch {
            flow.collect {}
        }

        advanceTimeBy(250.milliseconds)
        assertEquals(1, callCount)

        advanceTimeBy(5.5.seconds)
        assertEquals(2, callCount)

        advanceTimeBy(5.5.seconds)
        assertEquals(2, callCount)

        advanceTimeBy(10.seconds)
        assertEquals(3, callCount)

        advanceTimeBy(5.seconds)
        assertEquals(3, callCount)

        advanceTimeBy(10.seconds)
        assertEquals(4, callCount)

        collectorJob.cancelChildren()
    }

    @Test
    fun `when network state switches then polling delay should be reset to 5 seconds`() = runTest {
        var callCount = 0
        val notifier = NetworkStatusNotifierTest {
            ++callCount % 2 == 0
        }

        val flow = notifier.networkStateFlow()

        val collectorJob = launch {
            flow.collect {}
        }

        advanceTimeBy(250.milliseconds)
        assertEquals(1, callCount)
        advanceTimeBy(5.seconds)
        assertEquals(2, callCount)

        advanceTimeBy(10.seconds)
        // called 2 times more in 10 seconds
        assertEquals(4, callCount)

        collectorJob.cancelChildren()
    }

    @Test
    fun `when coroutine is cancelled then flow should stop polling`() = runTest {
        var callCount = 0
        val notifier = NetworkStatusNotifierTest {
            ++callCount
            true
        }

        val flow = notifier.networkStateFlow()

        val collectorJob = launch {
            flow.collect {}
        }

        advanceTimeBy(250.milliseconds)
        assertEquals(1, callCount)
        advanceTimeBy(10.seconds)
        assertEquals(3, callCount)

        collectorJob.cancelChildren()

        advanceTimeBy(50.seconds)
        assertEquals(3, callCount)
    }

    @Test
    fun `when state is ON from on poll to another should not notify twice`() = runTest {
        var callCount = 0
        val notifier = NetworkStatusNotifierTest {
            ++callCount
            true
        }

        val flow = notifier.networkStateFlow()

        val collectedStates = mutableListOf<Boolean>()
        val collectorJob = launch {
            flow.toList(collectedStates)
        }

        advanceTimeBy(15.seconds)

        collectorJob.cancelChildren()

        assertEquals(3, callCount)
        assertEquals(1, collectedStates.size)
    }

    @Test
    fun `when state is OFF from on poll to another should not notify twice`() = runTest {
        var callCount = 0
        val notifier = NetworkStatusNotifierTest {
            ++callCount
            false
        }

        val flow = notifier.networkStateFlow()

        val collectedStates = mutableListOf<Boolean>()
        val collectorJob = launch {
            flow.toList(collectedStates)
        }

        advanceTimeBy(15.seconds)

        collectorJob.cancelChildren()

        assertEquals(2, callCount)
        assertEquals(1, collectedStates.size)
    }
}