package io.github.task320.earthstep.core.data.repository

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.model.GoogleAccount
import io.github.task320.earthstep.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/** P7-6: サインイン状態のDataStoreへの保存。 */
class GoogleAuthRepositoryImplTest {

    private lateinit var repository: GoogleAuthRepositoryImpl

    @Before
    fun setUp() {
        repository = GoogleAuthRepositoryImpl(InMemoryPreferencesDataStore())
    }

    @Test
    fun `既定では未サインイン`() = runBlocking<Unit> {
        assertThat(repository.signedInAccount.first()).isNull()
    }

    @Test
    fun `保存したアカウントが読み出せる`() = runBlocking<Unit> {
        val account = GoogleAccount(id = "123", email = "walker@example.com", displayName = "Walker")

        repository.setSignedInAccount(account)

        assertThat(repository.signedInAccount.first()).isEqualTo(account)
    }

    @Test
    fun `emailやdisplayNameが無くても保存できる`() = runBlocking<Unit> {
        val account = GoogleAccount(id = "123", email = null, displayName = null)

        repository.setSignedInAccount(account)

        assertThat(repository.signedInAccount.first()).isEqualTo(account)
    }

    @Test
    fun `nullを渡すとサインアウト状態になる`() = runBlocking<Unit> {
        repository.setSignedInAccount(GoogleAccount(id = "123", email = "walker@example.com", displayName = null))

        repository.setSignedInAccount(null)

        assertThat(repository.signedInAccount.first()).isNull()
    }
}
