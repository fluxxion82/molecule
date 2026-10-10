/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.molecule

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isPositive
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class WindowAnimationFrameClockTest {
  @Test fun ticksWithTime() = runTest {
    if (js("typeof window === 'undefined'")) {
      return@runTest
    }
    val frameTimeA = WindowAnimationFrameClock.withFrameNanos { it }
    val frameTimeB = WindowAnimationFrameClock.withFrameNanos { it }
    assertThat(frameTimeA).all {
      isPositive()
      isLessThan(frameTimeB)
    }
  }

  @Test fun exceptionInOnFramePropagates() = runTest {
    if (js("typeof window === 'undefined'")) {
      return@runTest
    }
    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    assertFailure {
      WindowAnimationFrameClock.withFrameNanos<Unit> {
        throw runtimeException
      }
    }.isSameInstanceAs(runtimeException)
  }

  @Test fun cancellationDoesNotInvokeOnFrame() = runTest {
    if (js("typeof window === 'undefined'")) {
      return@runTest
    }
    var frames = 0
    // Start undispatched so the coroutine is suspended inside withFrameNanos before we cancel it.
    val job = launch(start = UNDISPATCHED) {
      WindowAnimationFrameClock.withFrameNanos { frames++ }
    }
    job.cancelAndJoin()

    // Wait for a frame to ensure the canceled callback was removed and not merely ignored.
    WindowAnimationFrameClock.withFrameNanos { }
    assertThat(frames).isEqualTo(0)
  }
}
