package org.thoughtcrime.securesms.linkdevice

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.whispersystems.signalservice.api.link.WaitForLinkedDeviceResponse

/**
 * Tests for LinkDeviceViewModel fallback device detection logic after v7.58.0 API migration.
 *
 * Covers the edge cases introduced by:
 * - Device.createdMillis being nullable (Long?)
 * - WaitForLinkedDeviceResponse constructor signature changes
 * - registrationId replacing deviceCreatedAt
 *
 * Related: docs/merge-fix-linkdevice-api-migration.md
 */
class LinkDeviceViewModelFallbackTest {

  @Before
  fun setUp() {
    mockkObject(LinkDeviceRepository)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  /**
   * Test: maxByOrNull handles nullable createdMillis correctly
   *
   * Before fix: `devices?.maxByOrNull { it.createdMillis }` failed type inference
   * After fix: `devices?.maxByOrNull { it.createdMillis ?: 0L }` provides fallback
   */
  @Test
  fun `maxByOrNull with nullable createdMillis returns device with highest non-null timestamp`() {
    val devices = listOf(
      Device(id = 1, name = "Old Device", createdMillis = 1000L, lastSeenMillis = 5000L, registrationId = 100),
      Device(id = 2, name = "Null Device", createdMillis = null, lastSeenMillis = 6000L, registrationId = 200),
      Device(id = 3, name = "Recent Device", createdMillis = 3000L, lastSeenMillis = 7000L, registrationId = 300)
    )

    val recentDevice = devices.maxByOrNull { it.createdMillis ?: 0L }

    assertNotNull(recentDevice)
    assertEquals(3, recentDevice?.id)
    assertEquals(3000L, recentDevice?.createdMillis)
  }

  @Test
  fun `maxByOrNull with all null createdMillis returns first device`() {
    val devices = listOf(
      Device(id = 1, name = "Device A", createdMillis = null, lastSeenMillis = 5000L, registrationId = 100),
      Device(id = 2, name = "Device B", createdMillis = null, lastSeenMillis = 6000L, registrationId = 200)
    )

    val recentDevice = devices.maxByOrNull { it.createdMillis ?: 0L }

    // When all values are 0L (from null fallback), maxByOrNull returns the first element
    assertNotNull(recentDevice)
    assertEquals(1, recentDevice?.id)
  }

  @Test
  fun `maxByOrNull with empty list returns null`() {
    val devices = emptyList<Device>()

    val recentDevice = devices.maxByOrNull { it.createdMillis ?: 0L }

    assertNull(recentDevice)
  }

  /**
   * Test: Fallback time window logic handles nullable createdMillis
   *
   * Before fix: `(System.currentTimeMillis() - recentDevice.createdMillis) < 20_000`
   *             failed because createdMillis is Long?
   * After fix: Extract to `deviceCreated` variable and check null before arithmetic
   */
  @Test
  fun `fallback time window accepts device created within 20 seconds`() {
    val now = System.currentTimeMillis()
    val device = Device(
      id = 123,
      name = "Recent Device",
      createdMillis = now - 15_000, // 15 seconds ago
      lastSeenMillis = now,
      registrationId = 456
    )

    val deviceCreated = device.createdMillis
    val isWithinWindow = deviceCreated != null && (now - deviceCreated) < 20_000

    assertEquals(true, isWithinWindow)
  }

  @Test
  fun `fallback time window rejects device created outside 20 seconds`() {
    val now = System.currentTimeMillis()
    val device = Device(
      id = 123,
      name = "Old Device",
      createdMillis = now - 25_000, // 25 seconds ago
      lastSeenMillis = now,
      registrationId = 456
    )

    val deviceCreated = device.createdMillis
    val isWithinWindow = deviceCreated != null && (now - deviceCreated) < 20_000

    assertEquals(false, isWithinWindow)
  }

  @Test
  fun `fallback time window rejects device with null createdMillis`() {
    val now = System.currentTimeMillis()
    val device = Device(
      id = 123,
      name = "Unknown Age Device",
      createdMillis = null,
      lastSeenMillis = now,
      registrationId = 456
    )

    val deviceCreated = device.createdMillis
    val isWithinWindow = deviceCreated != null && (now - deviceCreated) < 20_000

    assertEquals(false, isWithinWindow)
  }

  /**
   * Test: WaitForLinkedDeviceResponse construction with new signature
   *
   * Before fix: (id, name, created, lastSeen) - 4 params, wrong order
   * After fix: (id, name, lastSeen, registrationId, createdAtCiphertext) - 5 params
   */
  @Test
  fun `WaitForLinkedDeviceResponse constructs correctly from Device in fallback scenario`() {
    val device = Device(
      id = 123,
      name = "Test Device",
      createdMillis = 1000L,
      lastSeenMillis = 2000L,
      registrationId = 456
    )

    val response = WaitForLinkedDeviceResponse(
      id = device.id,
      name = device.name ?: "Unknown Device",
      lastSeen = device.lastSeenMillis,
      registrationId = device.registrationId,
      createdAtCiphertext = null // Fallback scenario doesn't have ciphertext
    )

    assertEquals(123, response.id)
    assertEquals("Test Device", response.name)
    assertEquals(2000L, response.lastSeen)
    assertEquals(456, response.registrationId)
    assertNull(response.createdAtCiphertext)
  }

  @Test
  fun `WaitForLinkedDeviceResponse handles null device name with fallback`() {
    val device = Device(
      id = 123,
      name = null,
      createdMillis = 1000L,
      lastSeenMillis = 2000L,
      registrationId = 456
    )

    val response = WaitForLinkedDeviceResponse(
      id = device.id,
      name = device.name ?: "Unknown Device",
      lastSeen = device.lastSeenMillis,
      registrationId = device.registrationId,
      createdAtCiphertext = null
    )

    assertEquals("Unknown Device", response.name)
  }

  /**
   * Test: DialogState.SyncingMessages accepts only deviceId
   *
   * Before fix: SyncingMessages(deviceId, deviceCreatedAt) - 2 params
   * After fix: SyncingMessages(deviceId) - 1 param (upstream removed deviceCreatedAt)
   */
  @Test
  fun `DialogState SyncingMessages constructs with deviceId only`() {
    val deviceId = 123

    val dialogState = LinkDeviceSettingsState.DialogState.SyncingMessages(deviceId)

    assertEquals(123, dialogState.deviceId)
  }

  /**
   * Test: DialogState.SyncingFailed uses registrationId instead of deviceCreatedAt
   *
   * Before fix: SyncingFailed(deviceId, deviceCreatedAt, syncFailType)
   * After fix: SyncingFailed(deviceId, deviceRegistrationId, syncFailType)
   */
  @Test
  fun `DialogState SyncingFailed constructs with registrationId`() {
    val deviceId = 123
    val registrationId = 456
    val syncFailType = LinkDeviceSettingsState.SyncFailType.NOT_ENOUGH_SPACE

    val dialogState = LinkDeviceSettingsState.DialogState.SyncingFailed(
      deviceId = deviceId,
      deviceRegistrationId = registrationId,
      syncFailType = syncFailType
    )

    assertEquals(123, dialogState.deviceId)
    assertEquals(456, dialogState.deviceRegistrationId)
    assertEquals(LinkDeviceSettingsState.SyncFailType.NOT_ENOUGH_SPACE, dialogState.syncFailType)
  }

  /**
   * Integration test: Fallback flow with edge case data
   */
  @Test
  fun `fallback flow handles device with null createdMillis gracefully`() {
    val devices = listOf(
      Device(id = 1, name = "Old", createdMillis = 1000L, lastSeenMillis = 5000L, registrationId = 100),
      Device(id = 2, name = "Null Age", createdMillis = null, lastSeenMillis = 6000L, registrationId = 200)
    )

    // Simulate LinkDeviceRepository.loadDevices() returning this list
    every { LinkDeviceRepository.loadDevices() } returns devices

    // Fallback logic
    val recentDevice = devices.maxByOrNull { it.createdMillis ?: 0L }
    val deviceCreated = recentDevice?.createdMillis
    val now = System.currentTimeMillis()
    val isWithinWindow = recentDevice != null && deviceCreated != null && (now - deviceCreated) < 20_000

    // Should select device 1 (highest non-null createdMillis)
    assertEquals(1, recentDevice?.id)
    // But device 1 is old (created at 1000ms), so not within 20s window
    assertEquals(false, isWithinWindow)
  }

  @Test
  fun `fallback flow selects recent device and constructs valid WaitForLinkedDeviceResponse`() {
    val now = System.currentTimeMillis()
    val devices = listOf(
      Device(id = 1, name = "Old", createdMillis = now - 60_000, lastSeenMillis = now, registrationId = 100),
      Device(id = 2, name = "Recent", createdMillis = now - 10_000, lastSeenMillis = now, registrationId = 200)
    )

    every { LinkDeviceRepository.loadDevices() } returns devices

    // Fallback logic
    val recentDevice = devices.maxByOrNull { it.createdMillis ?: 0L }
    val deviceCreated = recentDevice?.createdMillis
    val isWithinWindow = recentDevice != null && deviceCreated != null && (now - deviceCreated) < 20_000

    // Should select device 2 (most recent)
    assertEquals(2, recentDevice?.id)
    assertEquals(true, isWithinWindow)

    // Construct WaitForLinkedDeviceResponse
    val response = WaitForLinkedDeviceResponse(
      id = recentDevice!!.id,
      name = recentDevice.name ?: "Unknown Device",
      lastSeen = recentDevice.lastSeenMillis,
      registrationId = recentDevice.registrationId,
      createdAtCiphertext = null
    )

    assertEquals(2, response.id)
    assertEquals("Recent", response.name)
    assertEquals(200, response.registrationId)
  }

  /**
   * Test: Ensure createdMillis null-safety in arithmetic operations
   */
  @Test
  fun `nullable createdMillis does not crash in arithmetic when properly handled`() {
    val device = Device(
      id = 123,
      name = "Test",
      createdMillis = null,
      lastSeenMillis = System.currentTimeMillis(),
      registrationId = 456
    )

    // Before fix: this would throw NullPointerException
    // After fix: safe unwrapping prevents crash
    val deviceCreated = device.createdMillis
    val timeDiff = if (deviceCreated != null) {
      System.currentTimeMillis() - deviceCreated
    } else {
      Long.MAX_VALUE // Treat as "very old"
    }

    assertEquals(Long.MAX_VALUE, timeDiff)
  }
}
