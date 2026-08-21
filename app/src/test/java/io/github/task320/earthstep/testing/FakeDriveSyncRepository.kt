package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.repository.DriveSyncRepository

/** メモリ上だけで完結する [DriveSyncRepository]。 */
class FakeDriveSyncRepository(private var remoteJson: String? = null, private val uploadSucceeds: Boolean = true) :
    DriveSyncRepository {

    var lastUploaded: String? = null
        private set

    override suspend fun download(accessToken: String): String? = remoteJson

    override suspend fun upload(accessToken: String, json: String): Boolean {
        if (!uploadSucceeds) return false
        lastUploaded = json
        remoteJson = json
        return true
    }
}
