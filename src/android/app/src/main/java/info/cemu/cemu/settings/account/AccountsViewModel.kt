package info.cemu.cemu.settings.account

import androidx.lifecycle.ViewModel
import info.cemu.cemu.nativeinterface.NativeAccount
import info.cemu.cemu.nativeinterface.NativeAccount.MAX_ACCOUNT_COUNT
import info.cemu.cemu.nativeinterface.NativeAccount.MIN_ACCOUNT_COUNT
import info.cemu.cemu.nativeinterface.NativeActiveSettings
import info.cemu.cemu.nativeinterface.NativeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAccount(
    val persistentId: Int,
    val networkService: Int,
)

data class CreateAccount(
    val persistentId: Int?,
    val miiName: String,
)

data class OnlineFilesStatus(
    val hasRequiredOnlineFiles: Boolean,
    val isOTPPresent: Boolean,
    val isSEEPREOMPresent: Boolean,
)

sealed class CreateAccountError {
    object InvalidPersistentId : CreateAccountError()
    object EmptyPersistentId : CreateAccountError()
    data class ConflictingPersistentId(
        val existingPersistentId: Int,
        val existingMiiName: String,
    ) : CreateAccountError()

    object EmptyMiiName : CreateAccountError()
}

class AccountsViewModel : ViewModel() {
    private val _accounts = MutableStateFlow(NativeAccount.getAccounts().toList())
    val accounts = _accounts.asStateFlow()

    val onlineFilesStatus = OnlineFilesStatus(
        hasRequiredOnlineFiles = NativeActiveSettings.hasRequiredOnlineFiles(),
        isOTPPresent = NativeAccount.isOTPPresent(),
        isSEEPREOMPresent = NativeAccount.isSEEPROMPresent(),
    )

    private fun getActiveAccount(persistentId: Int): ActiveAccount {
        val networkService = NativeSettings.getAccountNetworkService(persistentId)

        return ActiveAccount(
            persistentId = persistentId,
            networkService = networkService,
        )
    }

    private val _activeAccount =
        MutableStateFlow(getActiveAccount(NativeSettings.getAccountPersistentId()))
    val activeAccount = _activeAccount.asStateFlow()

    fun setActiveAccount(persistentId: Int) {
        if (!accounts.value.any { it.persistentId == persistentId }) {
            return
        }

        NativeSettings.setAccountPersistentId(persistentId)
        _activeAccount.value = getActiveAccount(persistentId)
    }

    fun setNetworkServiceForActiveAccount(networkService: Int) {
        val activeAccount = activeAccount.value
        NativeSettings.setAccountNetworkService(activeAccount.persistentId, networkService)
        _activeAccount.value = activeAccount.copy(networkService = networkService)
    }

    fun deleteActiveAccount() {
        if (accounts.value.size <= MIN_ACCOUNT_COUNT) {
            return
        }

        val activeAccountPersistentId = activeAccount.value.persistentId

        NativeAccount.deleteAccount(activeAccountPersistentId)
        refreshAccountList()
        _activeAccount.value = getActiveAccount(_accounts.value.first().persistentId)
    }


    fun validateCreateAccount(createAccountData: CreateAccount): CreateAccountError? {
        if (createAccountData.persistentId == null) {
            return CreateAccountError.EmptyPersistentId
        }

        if (createAccountData.persistentId.toUInt() < NativeAccount.MIN_PERSISTENT_ID) {
            return CreateAccountError.InvalidPersistentId
        }

        val existingAccount =
            accounts.value.firstOrNull { it.persistentId == createAccountData.persistentId }
        if (existingAccount != null) {
            return CreateAccountError.ConflictingPersistentId(
                existingAccount.persistentId,
                existingAccount.miiName
            )
        }

        if (createAccountData.miiName.isBlank()) {
            return CreateAccountError.EmptyMiiName
        }

        return null
    }

    fun saveAccount(account: NativeAccount.Account) {
        NativeAccount.saveAccount(account)
        refreshAccountList()
    }

    fun createAccount(createAccountData: CreateAccount) {
        if (validateCreateAccount(createAccountData) != null) {
            return
        }

        if (accounts.value.size >= MAX_ACCOUNT_COUNT) {
            return
        }

        NativeAccount.createAccount(createAccountData.persistentId!!, createAccountData.miiName)
        refreshAccountList()
    }

    fun getActiveAccountValidationErrors(): Array<NativeAccount.OnlineValidationError> {
        val persistentId = activeAccount.value.persistentId
        val accounts = accounts.value
        val activeAccount =
            accounts.firstOrNull { it.persistentId == persistentId }
                ?: accounts.firstOrNull()
                ?: return arrayOf()

        return NativeAccount.getAccountValidationErrors(activeAccount.persistentId)
    }

    fun refreshAccountList() {
        _accounts.value = NativeAccount.getAccounts().toList()
    }
}