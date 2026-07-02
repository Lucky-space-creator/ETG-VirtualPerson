package com.virtualwife.app.location

class GeoFenceManager {

    data class GeoFence(
        val id: String,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Int
    )

    private val fences = mutableListOf<GeoFence>()
    private val triggeredFences = mutableSetOf<String>()
    private var onFenceTriggered: ((GeoFence) -> Unit)? = null

    fun setOnFenceTriggeredListener(listener: (GeoFence) -> Unit) {
        onFenceTriggered = listener
    }

    fun addFence(fence: GeoFence) {
        fences.add(fence)
    }

    fun addFences(newFences: List<GeoFence>) {
        fences.addAll(newFences)
    }

    fun removeFence(id: String) {
        fences.removeAll { it.id == id }
        triggeredFences.remove(id)
    }

    fun clearFences() {
        fences.clear()
        triggeredFences.clear()
    }

    fun checkLocation(latitude: Double, longitude: Double) {
        fences.forEach { fence ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                latitude, longitude,
                fence.latitude, fence.longitude,
                results
            )
            val isInside = results[0] <= fence.radiusMeters
            val wasTriggered = triggeredFences.contains(fence.id)

            if (isInside && !wasTriggered) {
                triggeredFences.add(fence.id)
                onFenceTriggered?.invoke(fence)
            } else if (!isInside && wasTriggered) {
                triggeredFences.remove(fence.id)
            }
        }
    }

    fun getActiveFences(): List<GeoFence> = fences.toList()

    fun isInsideFence(fenceId: String): Boolean = triggeredFences.contains(fenceId)
}
